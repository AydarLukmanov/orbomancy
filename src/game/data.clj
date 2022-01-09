(ns game.data
  (:use
    arcadia.core
    arcadia.linear)
  (:require 
    game.baddies))

(def blocks {
  "1" {
    :sprite "block01.png"
    :health 1
    :points 1}
  "2" {
    :sprite "block02.png"
    :health 2
    :points 3}
  "3" {
    :sprite "block06.png"
    :health 5
    :points 100}
  "b" {
    :sprite "block04.png"
    :health 1
    :points 10
    :type :spawn-ball}
  "t" {
    :sprite "block03.png"
    :health 2
    :points 20
    :type :turret
    :rotater true
    :gun "nodes/gun02.tscn"
    :rate 40
    :current-rate 40
    :rate-rand 40
    :bullet {
      :scene "nodes/bullet02.tscn" 
      :damage 1
      :speed 160}
    :process #'game.baddies/update-baddy
    :physics-process #'game.baddies/update-baddy-physics}
  "e" {
    :sprite "block05.png"
    :health 3
    :points 30
    :type :flier
    :speed 60
    :velocity (v2 0 1)
    :steering 0
    :rate 80
    :current-rate 80
    :rate-rand 40
    :bullet {
      :scene "nodes/bullet03.tscn" 
      :damage 1
      :speed 230}
    :process #'game.baddies/update-baddy
    :physics-process #'game.baddies/update-baddy-physics}})

(def l01 "
......
......
11t111
111111
......")

(def l02 "
......e
.b....2
11t1112
1222112
....111")

(def l03 "
......e.
...t.b2.
11t11121
b22211t1
22331221")

(def l04 "
...e11212121
e.b211332211
1.12eb2t1t12
..22e1.1..12
..1332 1 112")

(def l05 "
.e.e11b1 1b1
e.t.11223211
1.11t12t1.12
3.22.1.13.12
..1331 1 112")

(def l06 "
.e.e11b1b1b12
e.t.112232112
1.11t12t1.12t
3.22.1.1.e12t
..1131 1..122
..11113331112")

(def l07 "
.e.e11b1 1 12
e.t.t1t223312
1.31t12t1e12t
e.22.1.1.e12t
3.1111 1..122
..33111331112")

(def l08 "
3e.e11b1 1 12
e3t.t1t222112
1.11t12t1e12t
e.22.1.1.e12t
331111 1..122
..133111eee12
3311113311112")

(defn get-level [n]
  (get [l01 l02 l03 l04 l05 l06 l07] n l08))

(def musics (shuffle ["music/looptober.21.ogg" "music/looptober.14.ogg" "music/looptober.20.ogg"]))

(defn get-music [n]
  (get (vec (take 10 (cycle musics))) n))