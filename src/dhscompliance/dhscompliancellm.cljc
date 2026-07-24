(ns dhscompliance.dhscompliancellm
  "DHSCompliance-LLM client -- the *contained intelligence node* for
  the USA-DHS (Department of Homeland Security) compliance actor.

  It normalizes engagement intake, drafts a per-track (`:cisa-scrm` /
  `:fasc-exclusion` / `:hsar-compliance`) compliance evidence
  checklist, drafts the filing-draft action, and drafts the
  filing-submit action. CRITICAL: it is a smart-but-untrusted advisor.
  It returns a *proposal* (with a rationale + the fields it cited),
  never a committed record or a real DHS filing. Every output is
  censored downstream by `dhscompliance.governor` before anything
  touches the SSoT, and `:filing/draft`/`:filing/submit` proposals
  NEVER auto-commit at any phase -- see README Actuation.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. Three request-level test-injection flags exist purely to
  exercise the governor's fabrication-defense checks against a
  deliberately bad proposal, mirroring every sibling actor's `:no-
  spec?` pattern:

    :no-spec?                          -- forces an unregistered track
                                          (no spec-basis at all).
    :claim-cisa-sole-fasc-authority?   -- forces a proposal that
                                          claims CISA is the SOLE
                                          issuing authority for FASC
                                          exclusion orders
                                          (`:claims-cisa-sole-fasc-
                                          authority? true`).
    :conflate-hsar-cisa-directive?     -- forces a proposal that
                                          conflates DHS's own HSAR
                                          procurement clauses with
                                          CISA's binding-operational-
                                          directive track
                                          (`:conflates-hsar-with-cisa-
                                          directive? true`)."
  (:require [dhscompliance.facts :as facts]
            [dhscompliance.store :as store]))

(defn- normalize-intake
  [_db {:keys [patch]}]
  {:summary    (str "engagement intake record updated: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :engagement/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- assess-track
  "Per-track (`:cisa-scrm` / `:fasc-exclusion` / `:hsar-compliance`)
  compliance evidence checklist draft. `:no-spec?`/`:claim-cisa-sole-
  fasc-authority?`/`:conflate-hsar-cisa-directive?` inject the failure
  modes we must defend against."
  [_db {:keys [track no-spec? claim-cisa-sole-fasc-authority? conflate-hsar-cisa-directive?]}]
  (let [track (if no-spec? :unknown-track track)
        sb (facts/spec-basis track)]
    (cond
      (nil? sb)
      {:summary    (str (name track) " の公式spec-basisが見つかりません")
       :rationale  "dhscompliance.facts に未登録のトラック。要件を推測で作らない。"
       :cites      []
       :effect     :assessment/set
       :value      {:track track :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}

      claim-cisa-sole-fasc-authority?
      {:summary    (str (name track) " (" (:owner-authority sb) ") -- テスト注入: FASC権限のCISA単独帰属")
       :rationale  "テスト注入: FASC除外命令の発行権限をCISA単独と誤って主張するケース (正: SECURE Technology Act(2018)のFASCSA章で設置された省庁横断機関、CISAは一構成員)"
       :cites      [(:provenance sb)]
       :effect     :assessment/set
       :value      {:track track :checklist (:required-evidence sb) :spec-basis (:provenance sb)
                    :claims-cisa-sole-fasc-authority? true}
       :stake      nil
       :confidence 0.85}

      conflate-hsar-cisa-directive?
      {:summary    (str (name track) " -- テスト注入: HSAR/CISA directive混同")
       :rationale  "テスト注入: DHS自身のHSAR調達条項とCISAのbinding operational directive(他省庁向け)を同一視するケース"
       :cites      [(:provenance sb)]
       :effect     :assessment/set
       :value      {:track track :checklist (:required-evidence sb) :spec-basis (:provenance sb)
                    :conflates-hsar-with-cisa-directive? true}
       :stake      nil
       :confidence 0.85}

      :else
      {:summary    (str (name track) " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式ソース: " (:provenance sb) " / 基盤: " (:basis sb))
       :cites      [(:basis sb) (:provenance sb)]
       :effect     :assessment/set
       :value      {:track track
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :basis (:basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- propose-draft
  "Draft the actual FILING-DRAFT action for `track`. ALWAYS `:stake
  :actuation/draft-filing`."
  [db {:keys [subject track]}]
  (let [e (store/engagement db subject)]
    {:summary    (str subject "/" (name track) " 向け提出ドラフト提案"
                      (when e (str " (operator=" (:operator e) ")")))
     :rationale  (if e
                   (str "track=" (name track) " portal=" (:portal e))
                   "engagementが見つかりません")
     :cites      (if e [subject (name track)] [])
     :effect     :engagement/mark-drafted
     :value      {:engagement-id subject :track track}
     :stake      :actuation/draft-filing
     :confidence (if e 0.9 0.3)}))

(defn- propose-submit
  "Draft the actual FILING-SUBMIT action for `track`. ALWAYS `:stake
  :actuation/submit-filing` -- real-world DHS filing submission.
  Reflects readiness across the gates the governor independently
  re-verifies: Section 889 vendor screening (`:cisa-scrm` only) and
  CFATS live-status verification when the engagement is CFATS-relevant."
  [db {:keys [subject track]}]
  (let [e (store/engagement db subject)
        vendor-screening-ok? (or (not= track :cisa-scrm)
                                  (:vendor-screened-against-covered-list? e))
        cfats-ok? (or (not (:cfats-relevant? e))
                       (:cfats-status-verified? e))]
    {:summary    (str subject "/" (name track) " 向け提出提案"
                      (when e (str " (operator=" (:operator e) ")")))
     :rationale  (if e
                   (str "vendor-screened-against-covered-list?=" (:vendor-screened-against-covered-list? e)
                        " cfats-relevant?=" (:cfats-relevant? e)
                        " cfats-status-verified?=" (:cfats-status-verified? e)
                        " claimed-fee=" (:claimed-fee e))
                   "engagementが見つかりません")
     :cites      (if e [subject (name track)] [])
     :effect     :engagement/mark-submitted
     :value      {:engagement-id subject :track track}
     :stake      :actuation/submit-filing
     :confidence (if (and e vendor-screening-ok? cfats-ok?)
                   0.9 0.3)}))

(defprotocol Advisor
  (-advise [this db request] "Return a proposal map for `request`."))

(defrecord MockAdvisor []
  Advisor
  (-advise [_ db {:keys [op] :as request}]
    (case op
      :engagement/intake   (normalize-intake db request)
      :compliance/assess   (assess-track db request)
      :filing/draft        (propose-draft db request)
      :filing/submit       (propose-submit db request)
      {:summary "unknown op" :rationale "unsupported" :cites []
       :effect :noop :value {} :stake nil :confidence 0.0})))

(defn mock-advisor [] (->MockAdvisor))

(defn trace [request proposal]
  {:t :advisor-proposal
   :op (:op request)
   :subject (:subject request)
   :track (:track request)
   :summary (:summary proposal)
   :confidence (:confidence proposal)
   :stake (:stake proposal)})
