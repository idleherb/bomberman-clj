(ns bomberman-clj.client.components.grid
  (:require [bomberman-clj.client.components.cell :refer [cell]
                                                  :rename {cell el-cell}]))

(defn grid [grid-state players]
  (let [{:keys [v height width]} grid-state]
    [:div {:class "grid"}
     (for [row (range height)]
       [:div {:class "row" :key (str "row-" row)}
        (for [col (range width)]
          (let [cell-idx (+ (* row width) col)
                cell (nth v cell-idx)]
            ^{:key (str "cell-" col row)}
            [el-cell cell cell-idx players]))])]))
