(ns bomberman-clj.client.components.gameover)

(defn gameover [gameover-state players]
  [:div {:class "gameover modal"}
   (if-let [winner-id (:winner gameover-state)]
     (str (:name (get players winner-id)) " wins!")
     "no winner")])
