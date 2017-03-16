(ns cross.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [cljs.tools.reader :refer [read-string]]
            [cljs.js :refer [empty-state eval js-eval]]
            [cljs.pprint :refer [pprint]]))

;; -------------------------
;; Views

(defn eval-str [s]
  (eval (empty-state)
        (read-string s)
        {:eval       js-eval
         :source-map true
         :context    :expr}
        (fn [result] result)))

(defn editor-did-mount [input]
  (fn [this]
    (let [cm (.fromTextArea  js/CodeMirror
                             (reagent/dom-node this)
                             #js {:mode "clojure"
                                  :lineNumbers true})]
      (.on cm "change" #(reset! input (.getValue %))))))

(defn editor [input]
  (reagent/create-class
   {:render (fn [] [:textarea
                   {:default-value ""
                    :auto-complete "off"}])
    :component-did-mount (editor-did-mount input)}))

(defn render-code [this]
  (->> this reagent/dom-node .-firstChild (.highlightBlock js/hljs)))

(defn result-view [output]
  (reagent/create-class
   {:render (fn []
              [:pre>code.clj
               (with-out-str (pprint @output))])
    :component-did-update render-code}))

(defn home-page []
  (let [input (atom nil)
        output (atom nil)]
    (fn []
      [:div [:h2 "Welcome to cross"]
       [editor input]
       [:div
        [:button
         {:on-click #(reset! output (eval-str @input))}
         "run"]]
       [:div
        [result-view output]]
       [:div [:a {:href "/about"} "go to about page"]]])))

(defn about-page []
  [:div [:h2 "About cross"]
   [:div [:a {:href "/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
