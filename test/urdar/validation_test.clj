(ns urdar.validation-test
  (:require [clojure.test :refer :all]
            [urdar.crossovers.validation :as v]))

(deftest valid-link
  (is (v/valid-url? "http://foo.bar/") "Ordinary valid HTTP link")
  (is (v/valid-url? "http://www.foo.bar/")
      "Ordinary valid HTTP link with WWW prefix")
  (is (v/valid-url? "http://foo.bar")
      "Ordinary valid HTTP link without trailing slash")
  (is (v/valid-url? "https://foo.bar") "Ordinary HTTP link")
  (is (v/valid-url? "http://foo.bar/baz") "HTTP link with sub-page")
  (is (v/valid-url? "http://foo.bar/baz?p=1")
      "HTTP link with sub-page and parameters")
  (is (v/valid-url? "https://foo-bar.baz") "HTTP link with dash")
  (is (v/valid-url? "http://foo.bar/baz_(foo)") "HTTP link with parenthesis")
  (is (v/valid-url? "https://foo.bar/page#top") "HTTPS link with hash"))

(deftest invalid-link
  (is (not (v/valid-url? "www.foo.bar")) "Link without protocol specifier")
  (is (not (v/valid-url? "http://")) "Empty link with protocol specifier")
  (is (not (v/valid-url? "")) "Empty string")
  (is (not (v/valid-url? "httpss://bar.baz")) "Invalid protocol specifier")
  (is (not (v/valid-url? "http://.bar.com"))
      "Link with dot placed in its beginning"))
