(ns bomberman-clj.client.components.lobby.game-dialog
  (:require [bomberman-clj.client.actions :as a]
            [bomberman-clj.client.state :as s]))

(defn- close-dialog
  []
  (swap! s/state assoc-in [:app :game-dialog :open?] false))

(defn- sanitize-dimension
  [dim prev-dim]
  (if (odd? dim)
    dim
    (if (< dim prev-dim)
      (dec dim)
      (inc dim))))

(defn- sanitize-min-max
  [val min max]
  (cond
    (< val min) min
    (> val max) max
    :else val))

(defn game-dialog
  [name width height num-players]
  [:div {:class "modal"}
   [:div {:class "game-dialog"}
    [:label {:for "in-name"} "Name"
     [:input {:name "in-name"
              :value name
              :type "text"
              :required true
              :on-change #(swap! s/state assoc-in [:app :game-dialog :name] (-> % .-target .-value))}]]
    
    [:div {:class "row"}
     [:label {:for "in-width"} "Width"
      [:input {:name "in-width"
               :value width
               :type "number"
               :min 3
               :max 27
               :required true
               :on-change #(swap! s/state assoc-in [:app :game-dialog :width]
                                  (-> %
                                      .-target
                                      .-value
                                      (js/parseInt)
                                      (sanitize-dimension width)
                                      (sanitize-min-max 3 27)))}]]
     [:label {:for "in-height"} "Height"
      [:input {:name "in-height"
               :value height
               :type "number"
               :min 3
               :max 27
               :required true
               :on-change #(swap! s/state assoc-in [:app :game-dialog :height]
                                  (-> %
                                      .-target
                                      .-value
                                      (js/parseInt)
                                      (sanitize-dimension height)
                                      (sanitize-min-max 3 27)))}]]
     [:label {:for "in-num-players"} "Players"
      [:input {:class "in-num"
               :name "in-num-players"
               :value num-players
               :type "number"
               :min 2
               :max 8
               :required true
               :on-change #(swap! s/state assoc-in [:app :game-dialog :num-players]
                                  (-> %
                                      .-target
                                      .-value
                                      (js/parseInt)
                                      (sanitize-min-max 2 8)))}]]]
    [:div {:class "buttons"}
     [:button {:class "secondary"
               :on-click close-dialog} "Cancel"]
     [:button {:class "primary"
               :disabled (empty? name)
               :on-click (fn [] (a/open name) (close-dialog))} "Create Game"]]]])
