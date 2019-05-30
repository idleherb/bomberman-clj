(ns bomberman-clj.client.components.cell)

(defn cell [cell-state players]
  (let [classes (flatten ["cell"
                          (when (nil? cell-state) "empty")
                          (when-let [block (:block cell-state)]
                            ["block"
                             (name (:type block))
                             (when (:hit block) "hit")])
                          (when (and (:bomb cell-state)
                                     (not (:detonated (:bomb cell-state))))
                            (let [{:keys [player-id]} (:bomb cell-state)
                                  remote-control? (get-in players [player-id :remote-control?])]
                              (if remote-control? "bomb-remote-controlled" "bomb")))
                          (when (:fire cell-state) "fire")
                          (when-let [item (:item cell-state)]
                            ["item"
                             (str "item-" (name (:type item)))
                             (when (:hit item) "hit")])
                          (when-let [player-id (:player-id cell-state)]
                            [player-id
                             (when (:hit (get players player-id)) "hit")])])]
    [:div {:class classes}]))
