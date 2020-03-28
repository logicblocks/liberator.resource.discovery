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
(def metrics-route ["/metrics" :metrics])

(defn routes [extras]
  [""
   (concat
     [discovery-route
      ping-route
      health-route
      metrics-route]
     extras)])

(defn dependencies
  ([] (dependencies []))
  ([extra-routes]
   {:routes (routes extra-routes)}))

(defn handler
  ([dependencies] (handler dependencies {}))
  ([dependencies options]
   (let [handler (discovery-resource/resource-handler dependencies options)
         handler (-> handler
                   ring-keyword-params/wrap-keyword-params
                   ring-params/wrap-params)]
     handler)))

(deftest has-status-200
  (let [handler (handler (dependencies))
        request (ring/request :get "/")
        result (handler request)]
    (is (= (:status result) 200))))

(deftest includes-self-link
  (let [handler (handler (dependencies))
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= (hal/get-href resource :self) "http://localhost/"))))

(deftest includes-basic-link
  (let [route ["/thing" :thing]
        handler (handler
                  (dependencies [route])
                  {:links {:some-thing {:route-name :thing}}})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))
        link (hal/get-link resource :someThing)]
    (is (nil? (:templated link)))
    (is (= (:href link) "http://localhost/thing"))))

(deftest includes-link-with-path-param
  (let [route [["/thing/" :path-param] :thing]
        handler (handler
                  (dependencies [route])
                  {:links {:some-thing
                           {:route-name  :thing
                            :path-params {:path-param 10}}}})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))
        link (hal/get-link resource :someThing)]
    (is (nil? (:templated link)))
    (is (= (:href link) "http://localhost/thing/10"))))

(deftest includes-link-with-path-template-param
  (let [templated-route [["/thing/" :path-param] :thing]
        handler (handler
                  (dependencies [templated-route])
                  {:links {:some-thing
                           {:route-name           :thing
                            :path-template-params {:path-param :param}}}})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))
        link (hal/get-link resource :someThing)]
    (is (true? (:templated link)))
    (is (= (:href link) "http://localhost/thing/{param}"))))

(deftest includes-link-with-query-param
  (let [route ["/thing" :thing]
        handler (handler
                  (dependencies [route])
                  {:links {:some-thing
                           {:route-name   :thing
                            :query-params {:query-param 10}}}})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))
        link (hal/get-link resource :someThing)]
    (is (nil? (:templated link)))
    (is (= (:href link) "http://localhost/thing?queryParam=10"))))

(deftest includes-link-with-query-template-param
  (let [templated-route ["/thing" :thing]
        handler (handler
                  (dependencies [templated-route])
                  {:links {:some-thing
                           {:route-name            :thing
                            :query-template-params [:query-param]}}})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))
        link (hal/get-link resource :someThing)]
    (is (true? (:templated link)))
    (is (= (:href link) "http://localhost/thing{?queryParam}"))))

(deftest includes-all-specified-links
  (let [route-1 ["/thing-1" :thing-1]
        route-2 [["/thing-2/" :param-1] :thing-2]
        route-3 [["/thing-3/" :param-2] :thing-3]
        route-4 ["/thing-4" :thing-4]
        route-5 ["/thing-5" :thing-5]
        handler (handler
                  (dependencies
                    [route-1 route-2 route-3 route-4 route-5])
                  {:links {:some-thing-1
                           {:route-name :thing-1}
                           :some-thing-2
                           {:route-name  :thing-2
                            :path-params {:param-1 20}}
                           :some-thing-3
                           {:route-name           :thing-3
                            :path-template-params {:param-2 :param}}
                           :some-thing-4
                           {:route-name   :thing-4
                            :query-params {:query-param 30}}
                           :some-thing-5
                           {:route-name            :thing-5
                            :query-template-params [:query-param]}}})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= (hal/get-href resource :someThing1)
          "http://localhost/thing-1"))
    (is (= (hal/get-href resource :someThing2)
          "http://localhost/thing-2/20"))
    (is (= (hal/get-href resource :someThing3)
          "http://localhost/thing-3/{param}"))
    (is (= (hal/get-href resource :someThing4)
          "http://localhost/thing-4?queryParam=30"))
    (is (= (hal/get-href resource :someThing5)
          "http://localhost/thing-5{?queryParam}"))))

(deftest allows-links-to-be-specified-as-a-vector
  (let [route-1 ["/thing-1" :thing-1]
        route-2 ["/thing-2" :thing-2]
        route-3 ["/thing-3" :thing-3]
        handler (handler
                  (dependencies [route-1 route-2 route-3])
                  {:links [:thing-1 :thing-2 :thing-3]})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= (hal/get-href resource :thing1)
          "http://localhost/thing-1"))
    (is (= (hal/get-href resource :thing2)
          "http://localhost/thing-2"))
    (is (= (hal/get-href resource :thing3)
          "http://localhost/thing-3"))))

(deftest includes-ping-and-health-links-by-default
  (let [handler (handler (dependencies))
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= (hal/get-href resource :ping) "http://localhost/ping"))
    (is (= (hal/get-href resource :health) "http://localhost/health"))
    (is (nil? (hal/get-link resource :metrics)))))

(deftest includes-no-defaults-when-defaults-false
  (let [handler (handler (dependencies)
                  {:defaults false})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (nil? (hal/get-link resource :ping)))
    (is (nil? (hal/get-link resource :health)))
    (is (nil? (hal/get-link resource :metrics)))))

(deftest includes-specified-defaults
  (let [handler (handler (dependencies)
                  {:defaults [:ping :metrics]})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= (hal/get-href resource :ping) "http://localhost/ping"))
    (is (= (hal/get-href resource :metrics) "http://localhost/metrics"))
    (is (nil? (hal/get-link resource :health)))))

(deftest allows-defaults-to-be-overridden-globally
  (with-redefs [discovery-resource/*default-links* [:metrics :health]]
    (let [handler (handler (dependencies))
          request (ring/request :get "http://localhost/")
          result (handler request)
          resource (hal-json/json->resource (:body result))]
      (is (nil? (hal/get-link resource :ping)))
      (is (= (hal/get-href resource :health) "http://localhost/health"))
      (is (= (hal/get-href resource :metrics) "http://localhost/metrics")))))

(deftest allows-defaults-to-be-specified-as-a-map
  (let [handler (handler (dependencies)
                  {:defaults {:lb-check {:route-name :ping}}})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= (hal/get-href resource :lbCheck) "http://localhost/ping"))))

(deftest allows-global-defaults-to-be-specified-as-a-map
  (with-redefs [discovery-resource/*default-links*
                {:lb-check {:route-name :ping}}]
    (let [handler (handler (dependencies))
          request (ring/request :get "http://localhost/")
          result (handler request)
          resource (hal-json/json->resource (:body result))]
      (is (= (hal/get-href resource :lbCheck) "http://localhost/ping")))))
