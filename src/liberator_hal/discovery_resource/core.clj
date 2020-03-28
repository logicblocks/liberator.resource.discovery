(ns liberator-hal.discovery-resource.core
  (:require
    [halboy.resource :as hal]
    [hype.core :as hype]
    [liberator-mixin.core :as mixin]
    [liberator-mixin.json.core :as json-mixin]
    [liberator-mixin.hypermedia.core :as hypermedia-mixin]
    [liberator-mixin.hal.core :as hal-mixin]))

(defn build-definitions-for
  ([dependencies] (build-definitions-for dependencies {}))
  ([{:keys [routes]}
    {:keys [links]
     :or   {}}]
   {:handle-ok
    (fn [{:keys [request]}]
      (let [resource (hal/new-resource
                       (hype/absolute-url-for request routes :discovery))
            resource (reduce
                       (fn [r [name options]]
                         (hal/add-link r name
                           (hype/absolute-url-for request routes
                             (:route-name options))))
                       resource
                       links)]
        resource))}))

(defn build-resource-for
  ([dependencies] (build-resource-for dependencies {}))
  ([dependencies options]
   (mixin/build-resource
     (json-mixin/with-json-mixin dependencies)
     (hypermedia-mixin/with-hypermedia-mixin dependencies)
     (hal-mixin/with-hal-mixin dependencies)
     (build-definitions-for dependencies options))))
