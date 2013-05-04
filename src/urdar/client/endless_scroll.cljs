(ns urdar.client.endless-scroll
  "Endless scroll effect."
  (:import goog.async.Throttle))

(defn document-height
  "Retrieves height of document."
  []
  (let [body (.-body js/document)
        html (.-documentElement js/document)]
    (max (.-scrollHeight body) (.-offsetHeight body) (.-clientHeight html)
         (.-scrollHeight html) (.-offsetHeight html))))

;;; TODO stop scrolling when end reached
(defn generate-on-scroll
  "Generates an onscroll event listenet that calls a given function when
  bottom of page is reached."
  [f]
  (let [throttle (Throttle. f 1500)
        show-next-page (fn [] (.fire throttle))]
    (fn []
     (let [max-height (document-height)
           scrolled (+ (.-pageYOffset js/window) (.-innerHeight js/window))
           left-to-scroll (- max-height scrolled)]
       (when (= left-to-scroll 0) (show-next-page))))))
