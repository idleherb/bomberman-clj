(ns bomberman-clj.client.components.lobby.player-dialog
  (:require [clojure.string :as string]
            [reagent.core :as r]
            [bomberman-clj.client.actions :as a]
            [bomberman-clj.client.state :as s]))

(defn- close-dialog
  []
  (swap! s/state assoc-in [:app :player-dialog :open?] false))

(defn- on-click-join-game
  [game-id name]
  (swap! s/state assoc-in [:app :player-dialog :name] name)
  (a/join game-id name)
  (close-dialog))

(defn- on-key-down-input
  [code game-id name]
  (condp = code
    13 (on-click-join-game game-id name)
    27 (close-dialog)
    nil))

(defn player-dialog
  [game-id name]
  (let [!ref (atom nil)
        name-input (r/atom name)]
    (r/create-class
     {:component-did-mount #(some-> @!ref .focus)
      :reagent-render
      (fn []
        [:div {:class "modal"}
         [:div {:class "game-dialog"}
          [:label {:for "in-name"} "Player name"
           [:input {:ref #(reset! !ref %)
                    :name "in-name"
                    :value @name-input
                    :type "text"
                    :required true
                    :on-change #(reset! name-input (-> % .-target .-value))
                    :on-key-down #(on-key-down-input (.-keyCode %) game-id (string/trim @name-input))}]]
          [:div {:class "buttons"}
           [:button {:class "secondary"
                     :on-click close-dialog} "Cancel"]
           [:button {:class "primary"
                     :disabled (empty? (string/trim @name-input))
                     :on-click #(on-click-join-game game-id (string/trim @name-input))} "Join Game"]]]])})))
