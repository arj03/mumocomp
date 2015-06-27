(defproject mumocomp "0.1.1"
  :description "HTPC management system with a web frontend"
  :url "https://github.com/arj03/mumocomp"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [de.u-mass/lastfm-java "0.1.2"]
                 [org/jaudiotagger "2.0.3"]
                 [org.clojure/data.json "0.2.5"]
                 [enlive "1.1.5"]
                 [clj-http "1.1.2"]
                 [compojure "1.3.4"]
                 [ring "1.3.2"]
                 [hiccup "1.0.5"]
                 ]
  :main startup
  :aot [startup])
