(ns dhscompliance.facts-test
  (:require [clojure.test :refer [deftest is testing]]
            [dhscompliance.facts :as facts]))

(deftest cisa-scrm-has-spec-basis
  (let [sb (facts/spec-basis :cisa-scrm)]
    (is (some? sb))
    (is (string? (:provenance sb)))
    (is (seq (:required-evidence sb)))
    (is (= #{"Huawei" "ZTE" "Hytera" "Hikvision" "Dahua Technology"} (:covered-vendors sb)))))

(deftest fasc-exclusion-has-spec-basis
  (let [sb (facts/spec-basis :fasc-exclusion)]
    (is (some? sb))
    (is (string? (:provenance sb)))
    (is (seq (:required-evidence sb)))))

(deftest hsar-compliance-has-spec-basis
  (let [sb (facts/spec-basis :hsar-compliance)]
    (is (some? sb))
    (is (string? (:provenance sb)))
    (is (seq (:required-evidence sb)))
    (is (= "U.S. Department of Homeland Security -- HSAR is DHS's own agency-specific acquisition regulation supplementing the FAR"
          (:owner-authority sb)))))

(deftest cfats-status-is-disambiguation-only
  (let [sb (facts/spec-basis :cfats-status)]
    (is (some? sb))
    (is (true? (:disambiguation-only? sb)))))

(deftest unknown-track-has-no-spec-basis
  (is (nil? (facts/spec-basis :unknown-track)))
  (is (nil? (facts/spec-basis :zzz))))

(deftest out-of-scope-tracks-have-no-spec-basis
  (testing "DoD-specific (DFARS/CMMC) tracks are out of this actor's scope"
    (is (nil? (facts/spec-basis :dfars)))
    (is (nil? (facts/spec-basis :cmmc)))))

(deftest required-evidence-satisfied
  (let [sb (facts/spec-basis :cisa-scrm)
        all (:required-evidence sb)]
    (is (true? (facts/required-evidence-satisfied? :cisa-scrm all)))
    (is (not (facts/required-evidence-satisfied? :cisa-scrm (take 1 all))))
    (is (nil? (facts/required-evidence-satisfied? :unknown-track all)))))

(deftest coverage-is-honest
  (let [c (facts/coverage [:cisa-scrm :fasc-exclusion :unknown-track])]
    (is (= 3 (:requested c)))
    (is (= 2 (:covered c)))
    (is (= ["unknown-track"] (:missing-tracks c)))))

(deftest filing-tracks-excludes-assess-only-entries
  (is (= #{:cisa-scrm} facts/filing-tracks))
  (is (not (contains? facts/filing-tracks :fasc-exclusion)))
  (is (not (contains? facts/filing-tracks :hsar-compliance)))
  (is (not (contains? facts/filing-tracks :cfats-status))))

(deftest covered-vendor-matches-case-insensitively
  (is (true? (facts/covered-vendor? "Huawei Technologies Co.")))
  (is (true? (facts/covered-vendor? "hikvision")))
  (is (false? (facts/covered-vendor? "Meridian Federal Solutions LLC")))
  (is (false? (facts/covered-vendor? nil))))
