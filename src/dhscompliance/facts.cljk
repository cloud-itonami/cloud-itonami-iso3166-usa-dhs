(ns dhscompliance.facts
  "U.S. Department of Homeland Security (DHS) federal-procurement /
  supply-chain-risk compliance catalog -- the ONLY source of
  regulatory-requirement facts this actor is allowed to cite
  (`dhscompliance.governor`'s spec-basis check enforces that every
  proposal touching `:compliance/assess`, `:filing/draft`, or
  `:filing/submit` cites this catalog and nothing invented).

  Every fact below was verified via web fetch against `acquisition.gov`,
  `en.wikipedia.org` (for the statutory/legislative history of CISA, the
  SECURE Technology Act, and NDAA FY2019 Section 889), and
  `everycrsreport.com` (CRS report mirror) during this repo's research
  pass (2026-07-24). Four catalog entries, each with its own
  owner-authority and scope -- do NOT merge them into one
  undifferentiated 'DHS requirement':

    :cisa-scrm          -- Section 889 of the FY2019 NDAA covered-
                           telecommunications/video-surveillance-
                           equipment procurement ban. The statute names
                           FIVE specific vendors (Huawei, ZTE, Hytera,
                           Hikvision, Dahua) and their subsidiaries/
                           affiliates -- this is a FIXED statutory list,
                           not something CISA can unilaterally expand.
                           CISA (a DHS component, established by the
                           Cybersecurity and Infrastructure Security
                           Agency Act of 2018, effective 2018-11-26,
                           succeeding the National Protection and
                           Programs Directorate) operates federal
                           civilian cybersecurity programs (binding
                           operational directives, EINSTEIN, .gov
                           domain management) and is commonly cited
                           alongside Section 889 screening, but Section
                           889 itself is a governmentwide FAR-level
                           procurement prohibition, not a CISA-issued
                           rule.
    :fasc-exclusion      -- Federal Acquisition Security Council (FASC)
                           case-by-case supply-chain-risk removal/
                           exclusion order authority, created by the
                           Federal Acquisition Supply Chain Security Act
                           (FASCSA) title of the SECURE Technology Act
                           of 2018. FASC is a SEPARATE, BROADER
                           interagency body from the fixed 5-vendor
                           Section 889 list -- FASC first issued its
                           implementing regulations in 2021 and can
                           recommend exclusion/removal orders against
                           other vendors/products case-by-case, on
                           different statutory grounds. This actor
                           treats collapsing FASC's interagency
                           exclusion-order mechanism into 'a CISA rule'
                           as a fabrication-class HARD violation --
                           CISA participates as one member of a broader
                           interagency council, not as the sole issuing
                           authority.
    :hsar-compliance     -- Homeland Security Acquisition Regulation
                           (HSAR): DHS's OWN agency-specific acquisition
                           regulation supplementing the FAR (confirmed
                           live via acquisition.gov/hsar, effective-date
                           marker 2024-12-26 at research time, ~55
                           parts mirroring the FAR's own part structure
                           -- the same GSAM-style supplement pattern
                           GSA's own actor uses). `acquisition.gov`
                           itself carries a disclaimer that it is 'not
                           an official version of the regulation' and
                           that eCFR is the authoritative source -- this
                           catalog deliberately does NOT hardcode a
                           specific HSAR Part/subpart clause number or
                           edition as a permanent fact for the same
                           reason GSA's GSAM entry doesn't: editions
                           turn over. HSAR governs how DHS ITSELF buys
                           things; it does NOT govern other federal
                           agencies' own cybersecurity practices (that
                           is CISA's separate binding-operational-
                           directive track) -- conflating the two is a
                           fabrication-class HARD violation, symmetric
                           with the FASC/CISA one above.
    :cfats-status        -- disambiguation entry, not itself a filing
                           track: the Chemical Facility Anti-Terrorism
                           Standards (CFATS) chemical-facility-security
                           program has a documented history of
                           time-limited statutory authorization,
                           extension, and (at points) lapse since its
                           original 2007 authorization (reaffirmed with
                           modifications by the Protecting and Securing
                           Chemical Facilities from Terrorist Attacks
                           Act of 2014; further short-term extensions
                           have occurred since). This catalog
                           deliberately does NOT hardcode CFATS as
                           'currently active' or 'currently lapsed' as
                           a permanent fact -- the program's authorized
                           status has changed multiple times and was
                           NOT independently re-verified as of a single
                           current date during this research pass. Any
                           proposal touching a CFATS-relevant subject
                           MUST cite a live status check
                           (`:cfats-status-verified?`) rather than
                           assume either state from a fixed date; this
                           actor treats an unverified CFATS-status
                           assumption as a fabrication-class HARD
                           violation, not a stylistic nit.

  What this catalog deliberately does NOT claim:
    - a specific current CFATS authorization status (see above);
    - a specific FASC exclusion-order case count, chair, or full
      member-agency roster (not independently confirmed live during
      this research pass -- only its statutory origin and separateness
      from Section 889/CISA are asserted, which WAS confirmed);
    - any HSAR Part/subpart clause number or edition as a permanent
      fact (acquisition.gov's own disclaimer: verify via eCFR);
    - DFARS/CMMC (DoD-specific acquisition/cybersecurity regimes --
      out of scope for this DHS-specific actor; a sibling actor covers
      DoD's own agency-specific domain);
    - FAR-council joint ownership details (out of scope here; GSA's
      sibling actor already covers the FAR/GSAM distinction for its own
      domain -- this actor does not re-litigate FAR ownership)."
  (:require [kotoba.lang.text :as str]))

(def catalog
  {:cisa-scrm
   {:name "Section 889 (NDAA FY2019) Covered-Telecommunications/Video-Surveillance-Equipment Procurement Ban"
    :name-en "Section 889 covered-equipment screening"
    :owner-authority "Governmentwide FAR-level statutory prohibition (Section 889, NDAA FY2019) -- CISA (DHS component) commonly administers related federal cybersecurity screening context, but does not itself issue the statute"
    :basis
    "Federal agencies and their contractors may not procure or use covered telecommunications/video-surveillance equipment or services from five specifically named vendors (Huawei, ZTE, Hytera, Hikvision, Dahua) or their subsidiaries/affiliates."
    :official-portal "https://www.acquisition.gov/far-case/2019-011"
    :provenance "https://en.wikipedia.org/wiki/National_Defense_Authorization_Act_for_Fiscal_Year_2019"
    :provenance-secondary "https://www.cisa.gov/"
    :covered-vendors #{"Huawei" "ZTE" "Hytera" "Hikvision" "Dahua Technology"}
    :required-evidence [:vendor-screened-against-covered-list?
                         :subsidiary-affiliate-check-performed?
                         :screening-date]}

   :fasc-exclusion
   {:name "Federal Acquisition Security Council (FASC) Case-by-Case Supply-Chain-Risk Removal/Exclusion Order"
    :name-en "FASC exclusion-order review"
    :owner-authority "Federal Acquisition Security Council -- interagency body created by the Federal Acquisition Supply Chain Security Act (FASCSA) title of the SECURE Technology Act of 2018; DHS/CISA is ONE participating member agency, not the sole issuing authority"
    :basis
    "FASC may recommend case-by-case supply-chain-risk removal or exclusion orders against vendors/products on grounds separate from, and broader than, the fixed 5-vendor Section 889 list. FASC first issued implementing regulations in 2021."
    :official-portal "https://www.acquisition.gov/"
    :provenance "https://en.wikipedia.org/wiki/SECURE_Technology_Act"
    :required-evidence [:fasc-exclusion-order-checked?
                         :check-date]}

   :hsar-compliance
   {:name "Homeland Security Acquisition Regulation (HSAR)"
    :name-en "HSAR clause compliance"
    :owner-authority "U.S. Department of Homeland Security -- HSAR is DHS's own agency-specific acquisition regulation supplementing the FAR"
    :basis
    "DHS-issued regulation supplementing the FAR for DHS's own procurement, organized in a part-structure mirroring the FAR itself (~55 parts at research time). Governs how DHS itself buys -- does NOT govern other federal agencies' own cybersecurity practices (that is CISA's separate binding-operational-directive track, out of scope for this catalog entry)."
    :official-portal "https://www.acquisition.gov/hsar"
    :provenance "https://www.acquisition.gov/hsar"
    :required-evidence [:hsar-clauses-reviewed?
                         :dhs-contract-vehicle]}

   :cfats-status
   {:name "Chemical Facility Anti-Terrorism Standards (CFATS) -- disambiguation only, not a filing track"
    :name-en "CFATS status disambiguation"
    :owner-authority "CISA (DHS component) -- chemical-facility-security regulatory program with a documented history of time-limited authorization, extension, and lapse"
    :basis
    "This catalog does NOT assert CFATS's current authorized status as a permanent fact. Any proposal touching a CFATS-relevant subject must cite a live status check rather than assume active or lapsed status from a fixed date."
    :official-portal "https://www.cisa.gov/"
    :provenance "https://www.everycrsreport.com/reports/R43346.html"
    :disambiguation-only? true}})

(defn spec-basis
  "Look up a track's official spec-basis, or nil if unregistered
  (never invent one)."
  [track]
  (get catalog track))

(defn required-evidence-satisfied?
  "True iff `checklist` contains every key in `track`'s
  `:required-evidence`."
  [track checklist]
  (let [sb (spec-basis track)]
    (and sb
         (every? (set checklist) (:required-evidence sb [])))))

(def filing-tracks
  "Tracks the operator actually drafts/submits -- a real actuation this
  actor performs. `:cisa-scrm` is the one track with a genuine
  operator-initiated representation/screening record. `:fasc-exclusion`
  (a lookup against a government-published exclusion-order list) and
  `:hsar-compliance` (DHS's own citable acquisition-regulation
  supplement, structurally analogous to GSA's GSAM-supplement entry in
  the sibling GSA actor) are assess-only -- there is no 'submit a
  filing' action for either, and the governor's
  `filing-op-on-non-filing-track` check HARD-holds any attempt to
  `:filing/draft`/`:filing/submit` on them."
  #{:cisa-scrm})

(defn coverage
  "Honest coverage report for a batch of requested tracks: how many
  have a registered spec-basis vs. how many don't (never silently
  drop the missing ones)."
  [tracks]
  {:requested (count tracks)
   :covered (count (filter spec-basis tracks))
   :missing-tracks (mapv name (remove spec-basis tracks))})

(defn covered-vendor?
  "True iff `vendor-name` matches (case-insensitively, substring) one
  of the five statutorily-named Section 889 covered vendors."
  [vendor-name]
  (boolean
   (and vendor-name
        (some #(str/includes?
                (str/lower vendor-name)
                (str/lower %))
              (:covered-vendors (:cisa-scrm catalog))))))
