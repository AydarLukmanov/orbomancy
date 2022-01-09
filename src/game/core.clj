(ns game.core
  (:use
    arcadia.core
    arcadia.linear
    arcadia.2D)
  (:require
    game.baddies
    game.gen)
  (:import
   [Godot Input]
   [Arcadia GodotHelpers]))

;constants
(def pspeed 420)
(def bspeed 300)
(def ul (v2 38 32))
(def lr (v2 978 565))
(def out-of-bounds 620)

;atoms
(defonce SCORE (atom 0))
(defonce BCOUNT (atom 3))
(defonce BALLS (atom []))
(defonce PWIDTH (atom 64))
(defonce PHEALTH (atom 10))
(defonce LEVEL (atom 0))

(defn start-game []
  (reset! BCOUNT 4)
  (reset! SCORE 0)
  (reset! PHEALTH 10)
  (reset! LEVEL 0))

(defn clean-scene [s]
  (dorun (map destroy (children (root))))
  (change-scene s))



(defn menu-ready [o k]
  (play-sound "music/looptober.22.ogg")
  (Input/SetMouseMode (int 0))
  (connect (find-node "New") "pressed" (fn [] 
    (start-game)
    (clean-scene "level.tscn")
    (play-sound "sound/ok.wav"))))



(defn init-ball [v]
  (let [ball (instance (load-scene "nodes/ball.tscn"))]
    (add-child (root) ball)
    (swap! BALLS conj ball)
    (position! ball v)
    (set-state ball (v2 (- (rand 100) 50) (- bspeed)))))

(defn loose-ball []
  (swap! BCOUNT dec)
  (play-sound "sound/loose.wav" -8)
  (if (= 0 @BCOUNT)
    (do (clean-scene "menu.tscn")
      (play-sound "sound/gameover.wav" -8))
    (let [paddle (find-node "paddle")] 
      (init-ball (v2+ (position paddle) (v2 0 -30))))))



(defn hurt-paddle [n]
  (swap! PHEALTH - n)
  (play-sound "sound/paddlehurt.wav" -10)
  (when (<= @PHEALTH 0)
    (clean-scene "menu.tscn")))

(defn level-ready [o k]
  (let [paddle (find-node "paddle")
        music (find-node "music")]
    (set! (.Stream music) (load-audio (game.data/get-music @LEVEL)))
    (set! (.VolumeDb music) -10)
    (.Play music 0)
    (connect* music "finished" (fn [] (.Play music 0)))
    (set-state paddle :current_rate 0)
    (add-signal paddle "hurt" System.Int32)
    (connect paddle "hurt" #'hurt-paddle)
    (Input/SetMouseMode (int 1))
    (init-ball (v2+ (position paddle) (v2 0 -30)))
    (game.gen/template-level (game.data/get-level @LEVEL))))

(defn level-input [o k e])

(defn update-block [block]
  (when (<= (:health (state block)) 0)
    (when  (= (:type (state block)) :spawn-ball)
      (play-sound "sound/pickup.wav")
      (init-ball (position block)))
    (play-sound (rand-nth ["sound/deadblock.wav" "sound/deadblock2.wav"]) -12)
    (destroy block)))

(defn update-bullet [o _ dt]
  (let [velocity (v2* (.Normalized (:velocity (state o))) (* (:speed (state o)) dt))]
    (when-let [collision (.MoveAndCollide o velocity false true false)]
      (let [other (.Collider collision)]
        (destroy o)
        (when (or (in-group? other "blocks") (in-group? other "enemies"))
          (play-sound (rand-nth ["sound/h01.wav" "sound/h02.wav" "sound/h03.wav"]) -8)
          (update-state other :health (fn [n] (- (or n 0) (:damage (state o)))))
          (update-block other)
          (let [sparks (instance (load-scene "nodes/bullet-particles.tscn"))]
            (add-child (root) sparks)
            (position! sparks (.Position collision))
            (set! (.Rotation sparks) (float (+ (.Angle (.Normal collision)) 1.5708)))
            (set! (.Emitting sparks) true)
            (timeout 1 (fn [] (destroy sparks)))))))))

(defn handle-shooting []
  (let [reticule (find-node "reticule")
        paddle (find-node "paddle")
        gun (find-node "paddle_gun")
        target_offset (v2- (position reticule) (position paddle))]
    (position! reticule (.GetMousePosition (root)))
    (update-state paddle :current_rate dec)
    (when (and (GodotHelpers/IsActionPressed  "shoot")
               (not (pos? (state paddle :current_rate))))
      (let [bullet (instance (load-scene "nodes/bullet01.tscn"))]
        (play-sound (rand-nth ["sound/shoot02.wav" "sound/shoot02.1.wav" "sound/shoot02.2.wav"]) -8)
        (add-child (find-node "level") bullet)
        (position! bullet (v2+ (position paddle) (v2* (.Normalized target_offset) 28)))
        (.LookAt bullet (position reticule))
        (.Rotate bullet 1.5708)
        (set-state bullet {:damage 0.3 :velocity target_offset :speed 500})
        (hook+ bullet :physics-process :_ #'update-bullet)
        (set-state paddle :current_rate 10)))))

(defn level-update [o k dt]
  (let [paddle (find-node "paddle")
        gun (find-node "paddle_gun")
        phalf (* @PWIDTH 0.5)
        dir (cond (GodotHelpers/IsActionPressed  "left") (v2 (- pspeed) 0)
                  (GodotHelpers/IsActionPressed  "right") (v2 pspeed 0)
                  :else (v2 0 0))
        ]
    (set! (.Text (find-node "orbs")) (str @BCOUNT))
    (set! (.Text (find-node "score")) (str @SCORE))
    (set! (.Text (find-node "lvl")) (str @LEVEL))
    (set! (.Text (find-node "phealth")) 
      (str "[" (apply str (concat (take @PHEALTH (repeat "*")) (take (- 10 @PHEALTH) (repeat ".")))) "]"))
    (.LookAt gun (position (find-node "reticule")))
    (.Rotate gun 1.5708)
    (handle-shooting)
    (position! paddle (v2+ (position paddle) (v2* dir dt)))
    (position! paddle (v2 
      (System.Math/Clamp 
        (.x (position paddle)) (+ (.x ul) phalf) (- (.x lr) phalf)) 
      (.y (position paddle))))
    (dorun (map 
      (fn [ball] 
        (when (> (.y (position ball)) out-of-bounds)
          (destroy ball)
          (if (= 1 (count (objects-in-group "balls")))
            (loose-ball))))
      (objects-in-group "balls")))
    (when (empty? (objects-in-group "blocks"))
      (swap! LEVEL inc)
      (clean-scene "level.tscn"))
    
    ))

(defn angle->radians [n] (* n (/ Math/PI 180)))

(defn mix [a b r] (+ a (* (- b a) r)))

(defn paddle-normal [ball paddle]
  (let [offset (- (.x (position ball)) (.x (position paddle)))
        ratio (* (+ 1 (/ offset (* @PWIDTH 0.5))) 0.5)
        angle  (mix 155 25 ratio)
        radians (angle->radians angle)]
    (v2* (v2 (Math/Cos radians) (- (Math/Sin radians))) 1)))

(defn hit-block [ball block collision]
  (let [sparks (instance (load-scene "nodes/hit-particles.tscn"))]
    (play-sound "sound/break.wav" -8)
    (swap! SCORE + (* (or (:points (state ball)) 1) (inc @LEVEL) (count (objects-in-group "balls"))))
    (add-child (root) sparks)
    (position! sparks (.Position collision))
    (set! (.Emitting sparks) true)
    (timeout 1 (fn [] (destroy sparks)))
    (update-state block #(update % :health dec))
    (tween block "rotation_degrees" 0 15 0.1 {:transition :circ})
    (tween block "rotation_degrees" 15 -15 0.2 {:transition :circ :delay 0.1})
    (tween block "rotation_degrees" -15 0 0.2 {:transition :circ :delay 0.3})
    (update-block block)))

(defn ball-move [^Godot.KinematicBody2D ball distance cnt]
  (let [start (position ball)]
    (when-let [collision (.MoveAndCollide ball (v2* (.Normalized (state ball)) distance) false true false)]
      (when (= cnt 2)
        (play-sound (rand-nth ["sound/z01.wav" "sound/z02.wav" "sound/z03.wav"]) -5))
      (let [end (position ball)
            remaining_distance (- distance (.Length (v2- start end)))]
        (cond 
          (in-group? (.Collider collision) "paddle")
          (set-state ball (v2* (paddle-normal ball (find-node "paddle")) bspeed))
          :else
          (set-state ball (.Bounce (state ball) (.Normal collision))))
        (if (in-group? (.Collider collision) "blocks")
          (hit-block ball (.Collider collision) collision))
        (when (and (pos? cnt) (> remaining_distance 0.001))
          
          (ball-move ball remaining_distance (dec cnt)))))))

(defn level-physics [o k dt]
  (dorun (map 
    (fn [ball]
      (ball-move ball (* (.Length (state ball)) dt) 2))
    (objects-in-group "balls"))))

'(clean-scene "menu.tscn")
'(clean-scene "level.tscn")

