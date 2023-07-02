(ns liberator.resource.discovery.core
  (:require
   [halboy.resource :as hal]
   [hype.core :as hype]
   [liberator.mixin.core :as mixin]
   [liberator.mixin.json.core :as json-mixin]
   [liberator.mixin.hypermedia.core :as hypermedia-mixin]
   [liberator.mixin.hal.core :as hal-mixin]))

(def ^:dynamic *default-links* [:ping :health])

(defn- normalise-links
  [links]
  (letfn [(->link-definition [l link-name]
            (merge l {link-name {:route-name link-name}}))]
    (cond
      (false? links) {}
      (map? links) links
      :else (reduce ->link-definition {} links))))

(defn- add-link
  [resource request router link-name
   {:keys [route-name] :as options}]
  (let [params (dissoc options :route-name)
        templated? (or
                     (contains? params :path-template-params)
                     (contains? params :query-template-params))
        href (hype/absolute-url-for request router route-name params)
        templated-map (if templated? {:templated true} {})
        href-map {:href href}]
    (hal/add-link resource link-name
      (merge templated-map href-map))))

(defn definitions
  ([dependencies] (definitions dependencies {}))
  ([{:keys [router]}
    {:keys [links
            defaults]
     :or   {links    {}
            defaults *default-links*}}]
   {:handle-ok
    (fn [{:keys [request]}]
      (let [links (merge
                    (normalise-links defaults)
                    (normalise-links links))
            resource (hal/new-resource
                       (hype/absolute-url-for request router :discovery))
            resource (reduce
                       (fn [r [name options]]
                         (add-link r request router name options))
                       resource links)]
        resource))}))

(defn handler
  ([dependencies] (handler dependencies {}))
  ([dependencies options]
   (mixin/build-resource
     (json-mixin/with-json-mixin dependencies)
     (hypermedia-mixin/with-hypermedia-mixin dependencies)
     (hal-mixin/with-hal-mixin dependencies)
     (definitions dependencies options))))
