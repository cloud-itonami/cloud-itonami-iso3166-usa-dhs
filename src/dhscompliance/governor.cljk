(ns dhscompliance.governor
  "DHS Federal-Procurement / Supply-Chain-Risk Compliance Governor --
  the independent compliance layer that earns the DHSCompliance-LLM the
  right to commit. The LLM has no notion of whether a vendor was
  actually screened against the FIVE statutorily-named Section 889
  covered vendors (Huawei/ZTE/Hytera/Hikvision/Dahua) and their
  subsidiaries/affiliates, whether it just conflated the Federal
  Acquisition Security Council's (FASC) broader interagency exclusion-
  order authority with a CISA-only rule (FASC is a separate body
  created by the SECURE Technology Act of 2018's FASCSA title -- CISA
  participates as one member, not the sole issuer), whether it
  conflated DHS's own HSAR procurement clauses (how DHS itself buys)
  with CISA's binding-operational-directive track (how OTHER federal
  agencies must run their own cybersecurity -- a different track
  entirely), or whether a CFATS-relevant proposal silently assumed a
  current authorization status this catalog deliberately does not
  hardcode (CFATS has a documented history of lapse/extension), so
  this MUST be a separate system able to *reject* a proposal and fall
  back to HOLD.

  `:itonami.blueprint/governor` is `:market-entry-compliance-governor`
  (blueprint.edn).

  Nine checks, in priority order, ALL HARD violations except the
  confidence/actuation gate: a human approver CANNOT override the hard
  ones. The confidence/actuation gate is SOFT: it asks a human to look
  (low confidence / actuation), and the human may approve -- but see
  `dhscompliance.phase`: for `:actuation/draft-filing`/
  `:actuation/submit-filing` NO phase ever allows auto-commit either.
  Two independent layers agree that actuation is always a human call.

    1. Spec-basis                    -- did the compliance-track
                                         proposal cite an OFFICIAL
                                         source (`dhscompliance.facts`),
                                         or invent one?
    2. Vendor-screening incomplete   -- for `:cisa-scrm`, was the
                                         subject actually screened
                                         against the FIVE statutorily-
                                         named Section 889 vendors (and
                                         their subsidiaries/affiliates),
                                         not merely asserted clean?
    3. FASC/CISA misattribution      -- did the proposal claim CISA is
                                         the SOLE issuing authority for
                                         FASC exclusion orders? FASC is
                                         a separate, broader interagency
                                         body (FASCSA/SECURE Technology
                                         Act of 2018); CISA is one
                                         member, not the sole authority.
    4. HSAR/CISA-directive conflation -- did the proposal conflate
                                         DHS's own HSAR procurement
                                         clauses (governs how DHS buys)
                                         with CISA's binding-
                                         operational-directive track
                                         (governs OTHER agencies'
                                         cybersecurity practices)? These
                                         are two different tracks.
    5. CFATS-status unverified       -- for a CFATS-relevant subject,
                                         did the proposal cite a LIVE
                                         status check
                                         (`:cfats-status-verified?`),
                                         rather than assume active or
                                         lapsed status from a fixed
                                         date? This catalog deliberately
                                         does not hardcode CFATS's
                                         current status.
    6. Filing op on non-filing track -- `:fasc-exclusion` and
                                         `:hsar-compliance` are
                                         assess-only (see
                                         `dhscompliance.facts/filing-
                                         tracks`) -- did the proposal
                                         attempt `:filing/draft`/
                                         `:filing/submit` on a track
                                         with no real submission action?
    7. Evidence incomplete           -- for `:filing/draft`/
                                         `:filing/submit`, has the
                                         track actually been assessed
                                         with a full evidence checklist
                                         on file?
    8. Engagement fee mismatch       -- for `:filing/submit`,
                                         INDEPENDENTLY recompute
                                         whether the engagement's own
                                         `:claimed-fee` equals
                                         `base-fee + monthly-rate x
                                         monitoring-months` (+ optional
                                         export-fee).
    9. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                         OR the op is `:filing/draft`/
                                         `:filing/submit` (REAL acts) ->
                                         escalate.

  Two more guards, double-draft/double-submit prevention, are enforced
  off dedicated per-track `:*-drafted?`/`:*-submitted?` facts (never a
  single `:status` value)."
  (:require [dhscompliance.facts :as facts]
            [dhscompliance.registry :as registry]
            [dhscompliance.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Drafting a real DHS compliance filing package and submitting a real
  filing are the two real-world actuation events this actor performs."
  #{:actuation/draft-filing :actuation/submit-filing})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:compliance/assess` (or `:filing/draft`/`:filing/submit`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent DHS's/CISA's/FASC's requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:compliance/assess :filing/draft :filing/submit} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案はコンプライアンス要件として扱えない"}]))))

(defn- vendor-screening-incomplete-violations
  "For `:filing/submit` on `:cisa-scrm`, INDEPENDENTLY verify the
  subject was actually screened against the FIVE statutorily-named
  Section 889 covered vendors (and subsidiaries/affiliates), not
  merely asserted clean -- mirrors GSA's `sam-registration-missing`
  check, which independently re-verifies only at the actuation op, not
  at assessment time."
  [{:keys [op subject track]} st]
  (when (and (= op :filing/submit) (= track :cisa-scrm))
    (let [e (store/engagement st subject)]
      (when-not (true? (:vendor-screened-against-covered-list? e))
        [{:rule :vendor-screening-incomplete
          :detail (str subject " はSection 889対象ベンダー(5社+関連会社)への照合が未実施")}]))))

(defn- fasc-cisa-misattribution-violations
  "A proposal that claims CISA is the SOLE issuing authority for FASC
  exclusion orders is a HARD violation. FASC is a separate, broader
  interagency body (FASCSA/SECURE Technology Act of 2018); CISA is one
  participating member, not the sole authority."
  [_request proposal]
  (when (true? (:claims-cisa-sole-fasc-authority? (:value proposal)))
    [{:rule :fasc-cisa-misattribution
      :detail "FASC除外命令の発行権限をCISA単独と主張する提案は不可 -- FASCはSECURE Technology Act(2018)のFASCSA章で設置された別個の省庁横断機関であり、CISAはその一構成員に過ぎない"}]))

(defn- hsar-cisa-directive-conflation-violations
  "A proposal that conflates DHS's own HSAR procurement clauses
  (governs how DHS itself buys) with CISA's binding-operational-
  directive track (governs OTHER agencies' cybersecurity practices) is
  a HARD violation -- two different tracks."
  [_request proposal]
  (when (true? (:conflates-hsar-with-cisa-directive? (:value proposal)))
    [{:rule :hsar-cisa-directive-conflation
      :detail "HSAR(DHS自身の調達規則)とCISAのbinding operational directive(他省庁のサイバー実務を規律する別トラック)を同一視する提案は不可"}]))

(defn- cfats-status-unverified-violations
  "For `:filing/draft`/`:filing/submit` on a CFATS-relevant subject,
  INDEPENDENTLY verify a LIVE status check was cited rather than an
  assumed active/lapsed status from a fixed date -- this catalog
  deliberately does not hardcode CFATS's current status."
  [{:keys [op subject]} st]
  (when (contains? #{:filing/draft :filing/submit} op)
    (let [e (store/engagement st subject)]
      (when (and (true? (:cfats-relevant? e))
                 (not (true? (:cfats-status-verified? e))))
        [{:rule :cfats-status-unverified
          :detail (str subject " はCFATS対象施設だが現行の認可ステータスをライブ確認していない -- 固定日付からの推定は不可")}]))))

(defn- filing-op-on-non-filing-track-violations
  "`:fasc-exclusion` and `:hsar-compliance` are assess-only tracks --
  there is no real-world 'submit a filing' action for either (see
  `dhscompliance.facts/filing-tracks`). A `:filing/draft`/
  `:filing/submit` proposal on a non-filing track is a HARD violation:
  never invent an actuation that doesn't exist."
  [{:keys [op track]} _st]
  (when (and (contains? #{:filing/draft :filing/submit} op)
             (not (contains? facts/filing-tracks track)))
    [{:rule :filing-op-on-non-filing-track
      :detail (str (name track) " は提出対象トラックではない(assess専用) -- filing/draft・filing/submitの実行動作が存在しない")}]))

(defn- evidence-incomplete-violations
  "For `:filing/draft`/`:filing/submit`, the track's required
  evidence checklist must actually be satisfied."
  [{:keys [op subject track]} st]
  (when (contains? #{:filing/draft :filing/submit} op)
    (let [assessment (store/assessment-of st subject track)]
      (when-not (and assessment
                     (facts/required-evidence-satisfied?
                      track (:checklist assessment)))
        [{:rule :evidence-incomplete
          :detail (str subject "/" (name track) " の必要書類が充足していない状態での提案")}]))))

(defn- engagement-fee-mismatch-violations
  "For `:filing/submit`, INDEPENDENTLY recompute whether the
  engagement's own claimed fee equals base + months x rate (+ optional
  export-fee) -- the same ground-truth-recompute discipline sibling
  actors use, matched against this repo's own three revenue lines."
  [{:keys [op subject]} st]
  (when (= op :filing/submit)
    (let [e (store/engagement st subject)]
      (when-not (registry/engagement-fee-matches-claim? e)
        [{:rule :engagement-fee-mismatch
          :detail (str subject " の申告手数料(" (:claimed-fee e)
                      ")が独立再計算値(" (registry/compute-engagement-fee e) ")と一致しない")}]))))

(defn- already-drafted-violations
  [{:keys [op subject track]} st]
  (when (= op :filing/draft)
    (when (store/engagement-track-drafted? st subject track)
      [{:rule :already-drafted
        :detail (str subject "/" (name track) " は既にドラフト済み")}])))

(defn- already-submitted-violations
  [{:keys [op subject track]} st]
  (when (= op :filing/submit)
    (when (store/engagement-track-submitted? st subject track)
      [{:rule :already-submitted
        :detail (str subject "/" (name track) " は既に提出済み")}])))

(defn check
  "Censors a DHSCompliance-LLM proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (vendor-screening-incomplete-violations request st)
                           (fasc-cisa-misattribution-violations request proposal)
                           (hsar-cisa-directive-conflation-violations request proposal)
                           (cfats-status-unverified-violations request st)
                           (filing-op-on-non-filing-track-violations request st)
                           (evidence-incomplete-violations request st)
                           (engagement-fee-mismatch-violations request st)
                           (already-drafted-violations request st)
                           (already-submitted-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :track      (:track request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
