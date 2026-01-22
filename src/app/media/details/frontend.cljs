(ns app.media.details.frontend
  (:require
   [app.frontend.db :as db]
   [app.frontend.mod :as mod]
   [app.frontend.screen :as screen]
   [app.media.media-db.inter :as media-db]
   [clojure.core.async :as a]
   [lib.program :as p]
   [lib.ui.image :as image]
   [lib.ui.top-bar :as top-bar]))

;; 
;; 
;; 
;; 

(defn to-media-query [media-id]
  {:q/limit 25
   :q/offset 0
   :q/select [:media/id :media/title :media/year :media/popularity :media/genre-ids :media/poster-url]
   :q/order [:media/popularity :desc]
   :q/where [:q/and
             [:q/= :media/id media-id]]})



(defn- logic [i]
  (p/take-every! i ::clicked-back (fn [_] (p/put! i [:screen/push [:screen/feed]])))

  (p/take-every!
   i ::load
   (fn [[_ media-id]]
     (a/go
       (let [query-result (a/<! (media-db/query! i (to-media-query media-id)))]
         (p/put! i [:db/got-query-result query-result])))))

  (p/take-every!
   i :screen/screen-changed
   (fn [[_ [screen-name screen-payload]]]
     (when (= screen-name :screen/media-details)
       (p/put! i [::load (:media/id screen-payload)])))))



;; 
;; 
;; 
;; 


(defn view-backdrop [media]
  [image/view
   {:class "w-full aspect-video object-cover"
    :image/url (:media/backdrop-url media)
    :image/alt (:media/title media)}])

(defn view-top-bar [i media]
  [top-bar/view
   {:top-bar/on-back #(p/put! i [::clicked-back])
    :top-bar/title (:media/title media)}])

(defn view-title [media]
  [:div.text-3xl.font-bold (:media/title media)])

(defn view-overview [media]
  [:div.text-lg.text-neutral-300 (:media/overview media)])

(defn to-screen [media]
  [:screen/media-details (select-keys media [:media/id])])


(defn view [i]
  (let [payload (screen/to-screen-payload i)
        media (db/to-entity i (:media/id payload))]
    [screen/view-screen i :screen/media-details
     [view-top-bar i media]
     [view-backdrop media]
     [:div.w-full.text-center.flex.flex-col.items-center.justify-center.p-6.gap-3
      [view-title media]
      [view-overview media]]]))

(mod/reg
 {:mod/name ::mod
  :mod/view view
  :mod/logic logic})
