(ns liberator-hal.discovery-resource.core-test
  (:require
   [clojure.test :refer :all]

   [halboy.resource :as hal]
   [halboy.json :as hal-json]

   [ring.mock.request :as ring]
   [ring.middleware.keyword-params :as ring-keyword-params]
   [ring.middleware.params :as ring-params]

   [liberator-hal.discovery-resource.core :as discovery-resource]))

(def routes
  [""
   [["/" :discovery]
    ["/ping" :ping]
    ["/health" :health]]])

(def dependencies
  {:routes routes})


(defn build-handler
  ([dependencies] (build-handler dependencies {}))
  ([dependencies options]
   (let [handler (discovery-resource/build-resource-for dependencies options)
         handler (-> handler
                   ring-keyword-params/wrap-keyword-params
                   ring-params/wrap-params)]
     handler)))

(deftest has-status-200
  (let [handler (build-handler dependencies)
        request (ring/request :get "/ping")
        result (handler request)]
    (is (= (:status result) 200))))
