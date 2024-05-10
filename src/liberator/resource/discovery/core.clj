(ns liberator.resource.discovery.core
  (:require
   [halboy.resource :as hal]
   [hype.core :as hype]
   [liberator.mixin.core :as lm-core]
   [liberator.mixin.util :as lm-util]
   [liberator.mixin.json.core :as json-mixin]
   [liberator.mixin.hypermedia.core :as hypermedia-mixin]
   [liberator.mixin.hal.core :as hal-mixin]))

(defn- link-definition-fn
  [link-name link-definition]
  (let [route-name (:route-name link-definition)
        params (dissoc link-definition :route-name)
        templated?
        (or
          (contains? params :path-template-params)
          (contains? params :query-template-params))
        templated-map
        (if templated? {:templated true} {})]
    (fn [{:keys [request router]}]
      {link-name
       (merge templated-map
         {:href
          (hype/absolute-url-for request router route-name params)})})))

(defn- link-definition-fns
  [link-definitions]
  (let [link-definitions
        (if (map? link-definitions)
          (mapv
            (fn [[link-name link-definition]]
              (merge {:link-name link-name} link-definition))
            link-definitions)
          link-definitions)]
    (mapv
      (fn [link-definition]
        (cond
          (map? link-definition)
          (link-definition-fn
            (get link-definition :link-name
              (get link-definition :route-name))
            link-definition)

          (keyword? link-definition)
          (link-definition-fn
            link-definition
            {:route-name link-definition})

          :else link-definition))
      link-definitions)))

(defn definitions
  ([_]
   {:link-definitions {}

    :self-link
    (fn [{:keys [request router]}]
      (hype/absolute-url-for request router :discovery))

    :handle-ok
    (fn [context]
      (let [self-link (lm-util/resource-attribute-as-value context :self-link)

            link-definitions
            (lm-util/resource-attribute-as-value context :link-definitions)
            link-definitions (link-definition-fns link-definitions)

            link-maps (mapv #(% context) link-definitions)
            links (apply merge link-maps)]
        (cond-> (hal/new-resource self-link)
          (some? links)
          (hal/add-links links))))}))

(defn handler
  ([dependencies] (handler dependencies {}))
  ([dependencies overrides]
   (lm-core/build-resource
     (json-mixin/with-json-mixin dependencies)
     (hypermedia-mixin/with-hypermedia-mixin dependencies)
     (hal-mixin/with-hal-mixin dependencies)
     (definitions dependencies)
     overrides)))
