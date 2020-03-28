(ns liberator-hal.discovery-resource.core-test
  (:require
    [clojure.test :refer :all]

    [halboy.resource :as hal]
    [halboy.json :as hal-json]

    [ring.mock.request :as ring]
    [ring.middleware.keyword-params :as ring-keyword-params]
    [ring.middleware.params :as ring-params]

    [liberator-hal.discovery-resource.core :as discovery-resource]))

(def discovery-route ["/" :discovery])
(def ping-route ["/ping" :ping])
(def health-route ["/health" :health])

(defn build-routes [extras]
  [""
   (concat
     [discovery-route
      ping-route
      health-route]
     extras)])

(defn build-dependencies
  ([] (build-dependencies []))
  ([extra-routes]
   {:routes (build-routes extra-routes)}))

(defn build-handler
  ([dependencies] (build-handler dependencies {}))
  ([dependencies options]
   (let [handler (discovery-resource/build-resource-for dependencies options)
         handler (-> handler
                   ring-keyword-params/wrap-keyword-params
                   ring-params/wrap-params)]
     handler)))

(deftest has-status-200
  (let [handler (build-handler (build-dependencies))
        request (ring/request :get "/")
        result (handler request)]
    (is (= (:status result) 200))))

(deftest includes-self-link
  (let [handler (build-handler (build-dependencies))
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= (hal/get-href resource :self) "http://localhost/"))))

(deftest includes-specified-non-templated-link
  (let [non-templated-route ["/thing" :thing]
        handler (build-handler
                  (build-dependencies [non-templated-route])
                  {:links {:some-thing {:route-name :thing}}})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= (hal/get-href resource :someThing) "http://localhost/thing"))))

(deftest includes-specified-non-templated-links
  (let [non-templated-route-1 ["/thing-1" :thing-1]
        non-templated-route-2 ["/thing-2" :thing-2]
        handler (build-handler
                  (build-dependencies
                    [non-templated-route-1
                     non-templated-route-2])
                  {:links {:some-thing-1 {:route-name :thing-1}
                           :some-thing-2 {:route-name :thing-2}}})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= (hal/get-href resource :someThing1) "http://localhost/thing-1"))
    (is (= (hal/get-href resource :someThing2) "http://localhost/thing-2"))))
