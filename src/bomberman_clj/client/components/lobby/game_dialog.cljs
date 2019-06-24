(ns bomberman-clj.client.components.lobby.game-dialog
  (:require [clojure.string :as string]
            [reagent.core :as r]
            [bomberman-clj.client.actions :as a]
            [bomberman-clj.client.state :as s]))

(defn- close-dialog
  []
  (swap! s/state assoc-in [:app :game-dialog :open?] false))

(defn- sanitize-min-max
  [val min max]
  (cond
    (< val min) min
    (> val max) max
    :else val))

(defn- sanitize-dimension
  [dim min max]
  (let [dim (sanitize-min-max dim min max)]
    (if (odd? dim)
      dim
      (dec dim))))

(defn- on-click-open-game
  [name width height num-players]
  (let [name (string/trim name)
        width (sanitize-dimension width 5 27)
        height (sanitize-dimension height 5 27)
        num-players (sanitize-min-max num-players 2 8)
        game-dialog-state (-> (get-in @s/state [:app :game-dialog])
                              (assoc :name name
                                     :width width
                                     :height height
                                     :num-players num-players))]
    (swap! s/state assoc [:app :game-dialog] game-dialog-state)
    (a/open name width height num-players)
    (close-dialog)))

(defn- on-key-down-input
  [code name width height num-players]
  (condp = code
    13 (on-click-open-game name width height num-players)
    27 (close-dialog)
    nil))

(defn game-dialog
  [name width height num-players]
  (let [!ref (atom nil)
        name-input (r/atom name)
        width-input (r/atom width)
        height-input (r/atom height)
        num-players-input (r/atom num-players)]
    (r/create-class
     {:component-did-mount #(some-> @!ref .focus)
      :reagent-render
      (fn []
        [:div {:class "modal"}
         [:div {:class "game-dialog"}
          [:label {:for "in-name"} "Name"
           [:input {:ref #(reset! !ref %)
                    :name "in-name"
                    :value @name-input
                    :type "text"
                    :required true
                    :on-change #(reset! name-input (-> % .-target .-value))
                    :on-key-down #(on-key-down-input (.-keyCode %)
                                                     @name-input
                                                     @width-input
                                                     @height-input
                                                     @num-players-input)}]]
          [:div {:class "row"}
           [:label {:for "in-width"} "Width"
            [:input {:name "in-width"
                     :value @width-input
                     :type "number"
                     :min 5
                     :max 27
                     :required true
                     :on-change #(let [num (-> % .-target .-value (js/parseInt))]
                                   (when-not (js/Number.isNaN num)
                                     (reset! width-input num)))}]]
           [:label {:for "in-height"} "Height"
            [:input {:name "in-height"
                     :value @height-input
                     :type "number"
                     :min 5
                     :max 27
                     :required true
                     :on-change #(let [num (-> % .-target .-value (js/parseInt))]
                                   (when-not (js/Number.isNaN num)
                                     (reset! height-input num)))}]]
           [:label {:for "in-num-players"} "Players"
            [:input {:class "in-num"
                     :name "in-num-players"
                     :value @num-players-input
                     :type "number"
                     :min 2
                     :max 8
                     :required true
                     :on-change #(let [num (-> % .-target .-value (js/parseInt))]
                                   (when-not (js/Number.isNaN num)
                                     (reset! num-players-input num)))}]]]
          [:div {:class "buttons"}
           [:button {:class "secondary"
                     :on-click close-dialog} "Cancel"]
           [:button {:class "primary"
                     :disabled (empty? (string/trim @name-input))
                     :on-click #(on-click-open-game @name-input
                                                    @width-input
                                                    @height-input
                                                    @num-players-input)} "Create Game"]]]])})))
