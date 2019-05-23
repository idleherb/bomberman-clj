(ns bomberman-clj.client.components.num-players)

(defn num-players [state]
  (let [{:keys [players num-players]} (:game state)
        cur-num-players (count players)
        full? (= cur-num-players num-players)
        text (str cur-num-players "/" num-players " "
                  (if full? "playing" "players waiting"))]
    [:span {:class "num-players"} text]))
