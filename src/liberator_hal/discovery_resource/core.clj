(ns liberator-hal.discovery-resource.core
  (:require
   [halboy.resource :as hal]
   [hype.core :as hype]
   [liberator-mixin.core :as mixin]
   [liberator-mixin.json.core :as json-mixin]
   [liberator-mixin.hypermedia.core :as hypermedia-mixin]
   [liberator-mixin.hal.core :as hal-mixin]))

(defn- add-link
  [resource request routes link-name
   {:keys [route-name] :as options}]
  (let [params (dissoc options :route-name)
        templated? (or
                     (contains? params :path-template-params)
                     (contains? params :query-template-params))
        href (hype/absolute-url-for request routes route-name params)
        templated-map (if templated? {:templated true} {})
        href-map {:href href}]
    (hal/add-link resource link-name
      (merge templated-map href-map))))

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
                         (add-link r request routes name options))
                       resource links)]
        resource))}))

(defn build-resource-for
  ([dependencies] (build-resource-for dependencies {}))
  ([dependencies options]
   (mixin/build-resource
     (json-mixin/with-json-mixin dependencies)
     (hypermedia-mixin/with-hypermedia-mixin dependencies)
     (hal-mixin/with-hal-mixin dependencies)
     (build-definitions-for dependencies options))))
