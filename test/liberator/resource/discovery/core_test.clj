(ns liberator.resource.discovery.core-test
  (:require
   [clojure.test :refer [deftest is]]

   [halboy.resource :as hal]
   [halboy.json :as hal-json]

   [hype.core :as hype]
   [ring.mock.request :as ring]
   [ring.middleware.keyword-params :as ring-keyword-params]
   [ring.middleware.params :as ring-params]

   [liberator.resource.discovery.core :as discovery-resource]))

(def discovery-route ["/" :discovery])
(def ping-route ["/ping" :ping])
(def health-route ["/health" :health])
(def metrics-route ["/metrics" :metrics])

(defn router [extras]
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
   {:router (router extra-routes)}))

(defn resource-handler
  ([dependencies] (resource-handler dependencies {}))
  ([dependencies overrides]
   (-> (discovery-resource/handler dependencies overrides)
     ring-keyword-params/wrap-keyword-params
     ring-params/wrap-params)))

(deftest has-status-200
  (let [handler (resource-handler (dependencies))
        request (ring/request :get "/")
        result (handler request)]
    (is (= 200 (:status result)))))

(deftest includes-self-link
  (let [handler (resource-handler (dependencies))
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "http://localhost/" (hal/get-href resource :self)))))

(deftest includes-discovery-link
  (let [handler (resource-handler (dependencies))
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "http://localhost/" (hal/get-href resource :discovery)))))

(deftest includes-basic-link
  (let [route ["/thing" :thing]
        handler (resource-handler
                  (dependencies [route])
                  {:link-definitions
                   {:some-thing {:route-name :thing}}})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))
        link (hal/get-link resource :someThing)]
    (is (nil? (:templated link)))
    (is (= "http://localhost/thing" (:href link)))))

(deftest includes-link-with-path-param
  (let [route [["/thing/" :path-param] :thing]
        handler (resource-handler
                  (dependencies [route])
                  {:link-definitions
                   {:some-thing
                    {:route-name  :thing
                     :path-params {:path-param 10}}}})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))
        link (hal/get-link resource :someThing)]
    (is (nil? (:templated link)))
    (is (= "http://localhost/thing/10" (:href link)))))

(deftest includes-link-with-path-template-param
  (let [templated-route [["/thing/" :path-param] :thing]
        handler (resource-handler
                  (dependencies [templated-route])
                  {:link-definitions
                   {:some-thing
                    {:route-name           :thing
                     :path-template-params {:path-param :param}}}})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))
        link (hal/get-link resource :someThing)]
    (is (true? (:templated link)))
    (is (= "http://localhost/thing/{param}" (:href link)))))

(deftest includes-link-with-query-param
  (let [route ["/thing" :thing]
        handler (resource-handler
                  (dependencies [route])
                  {:link-definitions
                   {:some-thing
                    {:route-name   :thing
                     :query-params {:query-param 10}}}})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))
        link (hal/get-link resource :someThing)]
    (is (nil? (:templated link)))
    (is (= "http://localhost/thing?queryParam=10" (:href link)))))

(deftest includes-link-with-query-template-param
  (let [templated-route ["/thing" :thing]
        handler (resource-handler
                  (dependencies [templated-route])
                  {:link-definitions
                   {:some-thing
                    {:route-name            :thing
                     :query-template-params [:query-param]}}})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))
        link (hal/get-link resource :someThing)]
    (is (true? (:templated link)))
    (is (= "http://localhost/thing{?queryParam}" (:href link)))))

(deftest includes-all-links-specified-in-link-definitions
  (let [route-1 ["/thing-1" :thing-1]
        route-2 [["/thing-2/" :param-1] :thing-2]
        route-3 [["/thing-3/" :param-2] :thing-3]
        route-4 ["/thing-4" :thing-4]
        route-5 ["/thing-5" :thing-5]
        handler (resource-handler
                  (dependencies
                    [route-1 route-2 route-3 route-4 route-5])
                  {:link-definitions
                   {:some-thing-1
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
    (is (= "http://localhost/thing-1"
          (hal/get-href resource :someThing1)))
    (is (= "http://localhost/thing-2/20"
          (hal/get-href resource :someThing2)))
    (is (= "http://localhost/thing-3/{param}"
          (hal/get-href resource :someThing3)))
    (is (= "http://localhost/thing-4?queryParam=30"
          (hal/get-href resource :someThing4)))
    (is (= "http://localhost/thing-5{?queryParam}"
          (hal/get-href resource :someThing5)))))

(deftest allows-link-definitions-to-be-specified-as-a-vector-of-route-names
  (let [route-1 ["/thing-1" :thing-1]
        route-2 ["/thing-2" :thing-2]
        route-3 ["/thing-3" :thing-3]
        handler (resource-handler
                  (dependencies [route-1 route-2 route-3])
                  {:link-definitions [:thing-1 :thing-2 :thing-3]})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "http://localhost/thing-1"
          (hal/get-href resource :thing1)))
    (is (= "http://localhost/thing-2"
          (hal/get-href resource :thing2)))
    (is (= "http://localhost/thing-3"
          (hal/get-href resource :thing3)))))

(deftest allows-link-definitions-to-be-specified-as-a-vector-of-maps
  (let [route-1 ["/thing-1" :thing-1]
        route-2 ["/thing-2" :thing-2]
        route-3 [["/thing-3/" :id] :thing-3]
        handler (resource-handler
                  (dependencies [route-1 route-2 route-3])
                  {:link-definitions [{:route-name :thing-1}
                                      {:route-name :thing-2}
                                      {:route-name  :thing-3
                                       :path-params {:id 1}}]})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "http://localhost/thing-1"
          (hal/get-href resource :thing1)))
    (is (= "http://localhost/thing-2"
          (hal/get-href resource :thing2)))
    (is (= "http://localhost/thing-3/1"
          (hal/get-href resource :thing3)))))

(deftest allows-link-definitions-to-be-specified-as-a-vector-of-single-link-fns
  (let [route-1 ["/thing-1" :thing-1]
        route-2 ["/thing-2" :thing-2]
        route-3 [["/thing-3/" :id] :thing-3]

        handler
        (resource-handler
          (dependencies [route-1 route-2 route-3])
          {:link-definitions
           [(fn [{:keys [request router]}]
              {:thing-2
               {:href (hype/absolute-url-for request router :thing-2)}})
            (fn [{:keys [request router]}]
              {:thing-1
               {:href (hype/absolute-url-for request router :thing-1)}})]})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "http://localhost/thing-2"
          (hal/get-href resource :thing2)))))

(deftest allows-link-definitions-to-be-specified-as-a-vector-of-multi-link-fns
  (let [route-1 ["/thing-1" :thing-1]
        route-2 ["/thing-2" :thing-2]
        route-3 [["/thing-3/" :id] :thing-3]

        handler
        (resource-handler
          (dependencies [route-1 route-2 route-3])
          {:link-definitions
           [(fn [{:keys [request router]}]
              {:thing-1
               {:href (hype/absolute-url-for request router :thing-1)}
               :thing-2
               {:href (hype/absolute-url-for request router :thing-2)}})
            (fn [{:keys [request router]}]
              {:thing-3
               {:href (hype/absolute-url-for request router :thing-3
                        {:path-params {:id 123}})}})]})
        request (ring/request :get "http://localhost/")
        result (handler request)
        resource (hal-json/json->resource (:body result))]
    (is (= "http://localhost/thing-1"
          (hal/get-href resource :thing1)))
    (is (= "http://localhost/thing-2"
          (hal/get-href resource :thing2)))
    (is (= "http://localhost/thing-3/123"
          (hal/get-href resource :thing3)))))
