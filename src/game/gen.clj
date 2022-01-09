(ns game.gen
  (:use
    arcadia.core
    arcadia.linear
    arcadia.2D)
  (:require
    [clojure.string :as string]
    [game.data]))

(defn set-texture [^Godot.Sprite sprite ^System.String path]
  (.SetTexture sprite (load-texture path)))

(defn mirror [col]
  (mapv (fn [row] (apply str (concat row (reverse row)))) col))

(defn parse-pattern [pattern]
  (let [rows (mirror (string/split pattern #"\n"))
        width (reduce max (map count rows))
        height (count rows)]
    [rows width height]))

(defn template-level [pattern]
  (let [holder (Godot.Node.)
        bscn (load-scene "nodes/block.tscn")
        [rows width height] (parse-pattern pattern)]
    (dorun
      (for [y (range 0 height)
            x (range 0 width)
            :let [data (get game.data/blocks (str (get-in rows [y x])))]
            :when data]
        (let [block (instance bscn)]
          (set-state block data)
          (when-let [gun (:gun data)]
            (let [gun (instance (load-scene gun))]
              (set-state block :gun gun)
              (add-child block gun)
              (.Rotate gun (rand 100))
              (position! gun (position block))))
          (when (:process data) (hook+ block :process :_ (:process data)))
          (when (:physics-process data) (hook+ block :physics-process :_ (:physics-process data)))
          (when (:rate-rand data) (set-state block :current-rate (+ (rand-int (:rate-rand data)) (:current-rate data))))
          (set-texture (first (children block)) (str "sprites/" (:sprite data)))
          (position! block (v2+ (v2* (v2 x y) 32) (v2- (v2 520 230) (v2 (* width 0.5 32) (* height 0.5 32)))))
          (add-child holder block))))
    (add-child (find-node "level") holder)))