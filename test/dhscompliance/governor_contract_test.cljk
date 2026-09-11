(ns dhscompliance.governor-contract-test
  "The governor contract as executable tests -- this vertical's own
  Trust Controls implemented faithfully, and the integration test
  running the compiled StateGraph end-to-end. The single invariant
  under test:

    DHSCompliance-LLM never drafts or submits a filing the DHS
    Federal-Procurement / Supply-Chain-Risk Compliance Governor would
    reject, `:filing/draft`/`:filing/submit` NEVER auto-commit at any
    phase, `:engagement/intake` MAY auto-commit when clean, and every
    decision (commit OR hold) leaves exactly one ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [dhscompliance.store :as store]
            [dhscompliance.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :dhs-compliance-operator :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- reject! [actor tid]
  (g/run* actor {:approval {:status :rejected :by "op-1"}} {:thread-id tid :resume? true}))

(defn- assess!
  [actor tid-prefix subject track]
  (exec-op actor (str tid-prefix "-assess") {:op :compliance/assess :subject subject :track track} operator)
  (approve! actor (str tid-prefix "-assess")))

(defn- draft!
  [actor tid-prefix subject track]
  (exec-op actor (str tid-prefix "-draft") {:op :filing/draft :subject subject :track track} operator)
  (approve! actor (str tid-prefix "-draft")))

(deftest clean-intake-auto-commits
  (testing "integration: engagement/intake at phase 3 auto-commits through the full compiled graph"
    (let [[db actor] (fresh)
          res (exec-op actor "t1"
                    {:op :engagement/intake :subject "eng-1"
                     :patch {:id "eng-1" :operator "Meridian Federal Solutions LLC"}} operator)]
      (is (= :commit (get-in res [:state :disposition])))
      (is (= "Meridian Federal Solutions LLC" (:operator (store/engagement db "eng-1"))) "SSoT actually updated")
      (is (= 1 (count (store/ledger db)))))))

(deftest compliance-assess-always-needs-approval
  (testing "assess is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :compliance/assess :subject "eng-1" :track :cisa-scrm} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/assessment-of db "eng-1" :cisa-scrm)))))))

(deftest fabricated-track-is-held
  (testing "a compliance/assess proposal with no official spec-basis -> HOLD"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :compliance/assess :subject "eng-1" :track :cisa-scrm :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/assessment-of db "eng-1" :cisa-scrm)) "no assessment written"))))

(deftest fasc-cisa-misattribution-is-held-and-unoverridable
  (testing "claiming CISA is the sole issuing authority for FASC exclusion orders -> HARD hold (flagship correctness check)"
    (let [[db actor] (fresh)
          res (exec-op actor "t3b"
                    {:op :compliance/assess :subject "eng-1" :track :fasc-exclusion :claim-cisa-sole-fasc-authority? true} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:fasc-cisa-misattribution} (-> (store/ledger db) first :basis)))
      (is (nil? (store/assessment-of db "eng-1" :fasc-exclusion)) "no assessment written"))))

(deftest hsar-cisa-directive-conflation-is-held-and-unoverridable
  (testing "conflating DHS's own HSAR procurement clauses with CISA's binding-operational-directive track -> HARD hold"
    (let [[db actor] (fresh)
          res (exec-op actor "t3c"
                    {:op :compliance/assess :subject "eng-1" :track :hsar-compliance :conflate-hsar-cisa-directive? true} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:hsar-cisa-directive-conflation} (-> (store/ledger db) first :basis))))))

(deftest draft-without-assessment-is-held
  (testing "filing/draft before any compliance assessment -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :filing/draft :subject "eng-1" :track :cisa-scrm} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest vendor-screening-incomplete-is-held-and-unoverridable
  (testing "submitting cisa-scrm without a completed Section 889 covered-vendor screen -> HARD hold"
    (let [[db actor] (fresh)
          _ (assess! actor "t5pre" "eng-4" :cisa-scrm)
          _ (draft! actor "t5pre" "eng-4" :cisa-scrm)
          res (exec-op actor "t5" {:op :filing/submit :subject "eng-4" :track :cisa-scrm} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:vendor-screening-incomplete} (-> (store/ledger db) last :basis)))
      (is (empty? (store/submit-history db))))))

(deftest cfats-status-unverified-is-held-and-unoverridable
  (testing "drafting a CFATS-relevant filing without a live CFATS status check -> HARD hold"
    (let [[db actor] (fresh)
          _ (assess! actor "t6pre" "eng-5" :cisa-scrm)
          res (exec-op actor "t6" {:op :filing/draft :subject "eng-5" :track :cisa-scrm} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:cfats-status-unverified} (-> (store/ledger db) last :basis)))
      (is (empty? (store/draft-history db))))))

(deftest engagement-fee-mismatch-is-held
  (testing "claimed fee that doesn't equal base + months x rate (+ optional export) -> HOLD"
    (let [[db actor] (fresh)
          _ (assess! actor "t7pre" "eng-3" :cisa-scrm)
          _ (draft! actor "t7pre" "eng-3" :cisa-scrm)
          res (exec-op actor "t7" {:op :filing/submit :subject "eng-3" :track :cisa-scrm} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:engagement-fee-mismatch} (-> (store/ledger db) last :basis)))
      (is (empty? (store/submit-history db))))))

(deftest submit-always-escalates-then-human-decides
  (testing "integration: a clean fully-assessed submit still ALWAYS interrupts for human approval"
    (let [[db actor] (fresh)
          _ (assess! actor "t9pre" "eng-1" :cisa-scrm)
          _ (draft! actor "t9pre" "eng-1" :cisa-scrm)
          r1 (exec-op actor "t9" {:op :filing/submit :subject "eng-1" :track :cisa-scrm} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, submit record drafted"
        (let [r2 (approve! actor "t9")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:cisa-scrm-submitted? (store/engagement db "eng-1"))))
          (is (= 1 (count (store/submit-history db))) "one draft submit record"))))))

(deftest draft-always-escalates-then-human-decides
  (testing "a clean fully-assessed draft still ALWAYS interrupts for human approval"
    (let [[db actor] (fresh)
          _ (assess! actor "t10pre" "eng-1" :cisa-scrm)
          r1 (exec-op actor "t10" {:op :filing/draft :subject "eng-1" :track :cisa-scrm} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, draft record drafted"
        (let [r2 (approve! actor "t10")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:cisa-scrm-drafted? (store/engagement db "eng-1"))))
          (is (= 1 (count (store/draft-history db))) "one draft record"))))))

(deftest submit-escalate-then-reject-holds-and-ssot-untouched
  (testing "a clean fully-assessed submit interrupts, and a human REJECTION holds without touching the SSoT"
    (let [[db actor] (fresh)
          _ (assess! actor "t14pre" "eng-1" :cisa-scrm)
          _ (draft! actor "t14pre" "eng-1" :cisa-scrm)
          r1 (exec-op actor "t14" {:op :filing/submit :subject "eng-1" :track :cisa-scrm} operator)]
      (is (= :interrupted (:status r1)))
      (let [r2 (reject! actor "t14")]
        (is (= :hold (get-in r2 [:state :disposition])))
        (is (false? (:cisa-scrm-submitted? (store/engagement db "eng-1"))) "SSoT untouched on rejection")
        (is (empty? (store/submit-history db)))
        (is (some #{:approval-rejected} (map :t (store/ledger db))))))))

(deftest engagement-double-draft-is-held
  (testing "drafting the same engagement/track twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t11pre" "eng-1" :cisa-scrm)
          _ (draft! actor "t11pre" "eng-1" :cisa-scrm)
          res (exec-op actor "t11" {:op :filing/draft :subject "eng-1" :track :cisa-scrm} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-drafted} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/draft-history db))) "still only the one earlier draft"))))

(deftest engagement-double-submit-is-held
  (testing "submitting the same engagement/track twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t12pre" "eng-1" :cisa-scrm)
          _ (draft! actor "t12pre" "eng-1" :cisa-scrm)
          _ (exec-op actor "t12a" {:op :filing/submit :subject "eng-1" :track :cisa-scrm} operator)
          _ (approve! actor "t12a")
          res (exec-op actor "t12" {:op :filing/submit :subject "eng-1" :track :cisa-scrm} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-submitted} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/submit-history db))) "still only the one earlier submit"))))

(deftest filing-op-on-assess-only-track-is-held
  (testing "fasc-exclusion and hsar-compliance are assess-only -- filing/draft on either -> HARD hold"
    (let [[db actor] (fresh)
          _ (assess! actor "t13pre-a" "eng-1" :fasc-exclusion)
          res-a (exec-op actor "t13a" {:op :filing/draft :subject "eng-1" :track :fasc-exclusion} operator)]
      (is (= :hold (get-in res-a [:state :disposition])))
      (is (some #{:filing-op-on-non-filing-track} (-> (store/ledger db) last :basis))))
    (let [[db actor] (fresh)
          _ (assess! actor "t13pre-b" "eng-1" :hsar-compliance)
          res-b (exec-op actor "t13b" {:op :filing/submit :subject "eng-1" :track :hsar-compliance} operator)]
      (is (= :hold (get-in res-b [:state :disposition])))
      (is (some #{:filing-op-on-non-filing-track} (-> (store/ledger db) last :basis))))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :engagement/intake :subject "eng-1"
                          :patch {:id "eng-1" :operator "Meridian Federal Solutions LLC"}} operator)
      (exec-op actor "b" {:op :compliance/assess :subject "eng-1" :track :cisa-scrm :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
