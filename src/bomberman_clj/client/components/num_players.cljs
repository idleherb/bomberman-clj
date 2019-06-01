(ns bomberman-clj.client.components.num-players)

(defn num-players [cur-num max-num]
  (let [full? (= cur-num max-num)
        text (str cur-num "/" max-num " "
                  (if full? "playing" "players waiting"))]
    [:span {:class "num-players"} text]))
