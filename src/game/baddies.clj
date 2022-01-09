(ns game.baddies
  (:use
    arcadia.core
    arcadia.linear
    arcadia.2D))

(defn update-bullet [o _ dt]
  (let [velocity (v2* (.Normalized (:velocity (state o))) (* (:speed (state o)) dt))]
    (when-let [collision (.MoveAndCollide o velocity false true false)]
      (let [other (.Collider collision)]
        (when-not (or (in-group? other "blocks") (in-group? other "enemies"))
          (destroy o))
        (when (in-group? other "paddle")
          (emit (find-node "paddle") "hurt" (state o :damage))
          (let [sparks (instance (load-scene "nodes/bullet-particles.tscn"))]
            (add-child (root) sparks)
            (position! sparks (.Position collision))
            (set! (.Rotation sparks) (float (+ (.Angle (.Normal collision)) 1.5708)))
            (set! (.Emitting sparks) true)
            (timeout 1 (fn [] (destroy sparks)))))))) )

(defn fire-bullet [origin target {:keys [scene damage speed] :as data}]
  (let [target_offset (v2- target origin)
        bullet (instance (load-scene scene))]
    (play-sound (rand-nth ["sound/s01.wav" "sound/s02.wav" "sound/s03.wav" "sound/s04.wav" "sound/s05.wav"]) -12)
    (add-child (find-node "level") bullet)
    (position! bullet (v2+ origin (v2* (.Normalized target_offset) 28)))
    (.LookAt bullet target)
    (.Rotate bullet 1.5708)
    (set-state bullet (assoc data :velocity target_offset))
    (hook+ bullet :physics-process :_ #'update-bullet)))

(defn update-baddy [o _ dt])

(defn radian->v2 [r]
  (v2 (Math/Cos r) (Math/Sin r)))

(defn update-baddy-physics [o _ dt]
  (when (= :flier (state o :type))
    (let []
      (if (< (rand 30) 1) (set-state o :steering (rand-nth [0 0 -0.02 0.02])))
      (set-state o :velocity (.Rotated (state o :velocity) (state o :steering)))
      (when-let [collision (.MoveAndCollide o (v2* (.Normalized (state o :velocity)) (* (state o :speed) dt)) false true false)]
        (set-state o :velocity (.Bounce (state o :velocity) (.Normal collision))))
      (when (> (.y (position o)) 500) (set-state o :velocity (v2 0 -1)))))
  (when (state o :bullet)
    (update-state o :current-rate dec)
    (when (state o :rotater)
      (.Rotate (state o :gun) (* dt 1)))
    (when (< (state o :current-rate) 1)
      (cond 
        (state o :rotater)
        (fire-bullet (position o) 
          (v2+ (position o) (radian->v2 (- (.Rotation (state o :gun))  1.5708)))
          (state o :bullet))
        :else
        (fire-bullet (position o) (v2+ (position (find-node "paddle")) (v2 (- (rand 200) 100) 0)) (state o :bullet))))
    (if-not (pos? (state o :current-rate))
      (set-state o :current-rate (+ (state o :rate) (rand-int (or (state o :rate-rand) 0)))))))