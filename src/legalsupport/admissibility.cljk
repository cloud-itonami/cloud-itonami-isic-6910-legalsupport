(ns legalsupport.admissibility
  "Pure decision layer over `legalsupport.facts`: given a jurisdiction, a
  service mode and a revenue mode, is this way of operating legally
  established, and on what cited authority?

  Three properties this namespace guarantees, each proved by a test:

    1. DENY BY DEFAULT. An uncatalogued jurisdiction, an uncatalogued
       mode, and every verdict other than `:admissible` all resolve to
       not-admissible. There is no fallback jurisdiction and no
       'probably fine' path. `legalsupport.governor` calls only through
       here, so a jurisdiction nobody has researched cannot be served.

    2. NO PERMISSION FROM AN UNVERIFIED SOURCE. An `:admissible` verdict
       is honoured only if at least one of its basis rules was actually
       read at the primary or official level (`facts/verified-rule?`).
       A verdict marked `:admissible` whose entire basis is secondary
       commentary is downgraded to `:unsettled` here, with
       `:downgraded-from` recorded. Restrictive verdicts need no such
       backing — being wrong in the cautious direction costs coverage,
       being wrong in the permissive direction exposes users to criminal
       unauthorized-practice liability.

    3. REFERRAL COMPENSATION IS REFUSED EVERYWHERE. `forbidden-revenue-modes`
       is a product-level rule, not a legal conclusion: this service is
       paid for the document and procedure work it does, never for
       placing a matter with a lawyer. It therefore refuses
       `:revenue/per-referral-fee` and `:revenue/success-fee-share` even
       in a jurisdiction that would permit them. Germany (BRAO §49b(3)),
       England & Wales for injury claims (LASPO s.56), Brazil (OAB
       Provimento 205/2021) and Japan (弁護士法72条) each ban some part
       of it; running one product worldwide is only possible if the
       strictest rule is the product's rule."
  (:require [legalsupport.facts :as facts]))

(def forbidden-revenue-modes
  "Refused in every jurisdiction by product design, regardless of local
  law. See property 3 in the namespace docstring."
  #{:revenue/per-referral-fee :revenue/success-fee-share})

(def ^:private kind->key
  {:service :jurisdiction/service-modes
   :revenue :jurisdiction/revenue-modes})

(defn- basis-rules [jid entry]
  (keep #(facts/rule jid %) (:basis entry)))

(defn verdict-for
  "Resolve the verdict for `mode` of `kind` (`:service` | `:revenue`) in
  jurisdiction `jid`. Always returns a map with at least `:verdict`,
  `:jurisdiction`, `:kind` and `:mode`; never nil, never throws on an
  unknown input.

  Extra keys when resolution succeeded: `:basis` (rule ids),
  `:basis-rules` (the cited rule maps), `:condition`, and
  `:downgraded-from` when property 2 fired."
  [jid kind mode]
  (let [base {:jurisdiction jid :kind kind :mode mode}
        j (facts/jurisdiction jid)
        k (kind->key kind)]
    (cond
      (nil? k)
      (assoc base :verdict :uncovered
             :reason (str "unknown kind " (pr-str kind) " — expected :service or :revenue"))

      (nil? j)
      (assoc base :verdict :uncovered
             :reason (str "法域 " (pr-str jid) " はカタログ未収載。spec-basis が無いため成立と判定しない"
                          "（『対象外』ではなくカバレッジ未達）。"))

      :else
      (let [entry (get-in j [k mode])]
        (if (nil? entry)
          (assoc base :verdict :uncovered
                 :reason (str "mode " (pr-str mode) " は " jid " のカタログに無い。"))
          (let [rs (basis-rules jid entry)
                verified? (boolean (some facts/verified-rule? rs))
                declared (:verdict entry)
                effective (if (and (= :admissible declared) (not verified?))
                            :unsettled
                            declared)]
            (cond-> (merge base (select-keys entry [:basis :condition])
                           {:verdict effective :basis-rules (vec rs)})
              (not= effective declared)
              (assoc :downgraded-from declared
                     :reason (str "基礎ルールがいずれも secondary-source-only のため "
                                  ":admissible を :unsettled に降格した（一次出典の検証が必要）。")))))))))

(defn admissible?
  "True only for an `:admissible` verdict that survived the
  verified-source check. Everything else — `:conditional`,
  `:prohibited`, `:unsettled`, `:uncovered` — is false.

  `:conditional` is deliberately NOT admissible: it means lawfulness
  turns on a fact this catalog cannot check (a licence the operator
  holds, a registration, a property of the matter). Such a mode can only
  be unlocked by an operator attestation — see `admissible-with?`."
  [jid kind mode]
  (= :admissible (:verdict (verdict-for jid kind mode))))

(defn admissible-with?
  "Like `admissible?` but lets an operator unlock `:conditional` modes by
  attesting, per (kind, mode), that the stated condition holds.

  `attestations` is a set of `[kind mode]` tuples the operator has
  signed for. An attestation NEVER unlocks `:prohibited`, `:unsettled`
  or `:uncovered` — you cannot attest your way past a cited prohibition
  or past the absence of research."
  [jid kind mode attestations]
  (let [{:keys [verdict]} (verdict-for jid kind mode)]
    (or (= :admissible verdict)
        (and (= :conditional verdict)
             (contains? (set attestations) [kind mode])))))

(defn conditions
  "The condition string attached to (jid, kind, mode), or nil."
  [jid kind mode]
  (:condition (verdict-for jid kind mode)))

(defn citations
  "The cited rules backing (jid, kind, mode) — id, title, url and how
  well the source was verified. This is what an operator shows a
  regulator when asked why they believed they could do this."
  [jid kind mode]
  (mapv #(select-keys % [:rule/id :rule/title :rule/url :rule/verification
                         :rule/verification-note])
        (:basis-rules (verdict-for jid kind mode))))

(defn operating-envelope
  "Everything this product may do in `jid`, split by verdict. The
  `:admissible` lists are the envelope an operator can run today; the
  `:conditional` lists are what they can unlock by attestation; the
  `:blocked` lists are what they must not build for this market.

  `:revenue-modes :refused-by-design` records the referral-compensation
  modes this product declines even where local law would permit them."
  [jid]
  (let [modes (fn [kind ks]
                (let [vs (map (fn [m] (verdict-for jid kind m)) ks)
                      by (group-by :verdict vs)
                      ids #(mapv :mode (get by % []))]
                  {:admissible (ids :admissible)
                   :conditional (ids :conditional)
                   :blocked (vec (concat (ids :prohibited) (ids :unsettled) (ids :uncovered)))
                   :detail (vec vs)}))
        svc (modes :service (keys facts/service-modes))
        rev (modes :revenue (keys facts/revenue-modes))
        strip (fn [m] (update m :admissible #(vec (remove forbidden-revenue-modes %))))]
    {:jurisdiction jid
     :covered? (some? (facts/jurisdiction jid))
     :service-modes svc
     :revenue-modes (-> rev
                        strip
                        (update :conditional #(vec (remove forbidden-revenue-modes %)))
                        (assoc :refused-by-design (vec (sort forbidden-revenue-modes))))
     :known-gaps (facts/known-gaps jid)}))

(defn evaluate
  "One-shot admissibility decision for a concrete request. Returns

    {:admissible? bool
     :blockers    [{:rule ... :detail ...}]
     :service     <verdict-for map>
     :revenue     <verdict-for map>
     :citations   [...]}

  `req` keys:
    :jurisdiction  jurisdiction id of THE MATTER (not of the operator)
    :service-mode  a key of `facts/service-modes`
    :revenue-mode  a key of `facts/revenue-modes`
    :attestations  optional set of [kind mode] the operator has signed

  This is the single entry point `legalsupport.governor` uses, so every
  blocker it can produce is a hard hold in the actor."
  [{:keys [jurisdiction service-mode revenue-mode attestations]}]
  (let [sv (verdict-for jurisdiction :service service-mode)
        rv (verdict-for jurisdiction :revenue revenue-mode)
        att (set attestations)
        ok-s (admissible-with? jurisdiction :service service-mode att)
        ok-r (admissible-with? jurisdiction :revenue revenue-mode att)
        blockers
        (cond-> []
          (contains? forbidden-revenue-modes revenue-mode)
          (conj {:rule :referral-compensation-refused-by-design
                 :detail (str "収益モード " (pr-str revenue-mode)
                              " は法域を問わず本製品の設計上拒否する"
                              "（案件の取次ぎではなく、書類・手続支援そのものの対価で運営する）。")})

          (not ok-s)
          (conj {:rule :service-mode-not-admissible
                 :detail (str "法域 " (pr-str jurisdiction) " における "
                              (pr-str service-mode) " の verdict は "
                              (pr-str (:verdict sv)) "。"
                              (or (:reason sv) (:condition sv) ""))})

          (not ok-r)
          (conj {:rule :revenue-mode-not-admissible
                 :detail (str "法域 " (pr-str jurisdiction) " における "
                              (pr-str revenue-mode) " の verdict は "
                              (pr-str (:verdict rv)) "。"
                              (or (:reason rv) (:condition rv) ""))}))]
    {:admissible? (empty? blockers)
     :blockers blockers
     :service sv
     :revenue rv
     :citations (vec (concat (citations jurisdiction :service service-mode)
                             (citations jurisdiction :revenue revenue-mode)))}))

(defn requires-licensed-reviewer?
  "Modes whose lawfulness rests on a licensed lawyer personally
  reviewing and, where needed, personally amending the output — Japan's
  MOJ guideline §4 is the explicit statement of this, and the same shape
  recurs wherever the machine's output is only lawful as a lawyer's
  instrument."
  [service-mode]
  (contains? #{:mode/lawyer-reviewed :mode/inhouse-reviewed} service-mode))

(defn machine-legal-processing?
  "Modes in which the machine itself performs individualized legal
  processing. These are the ones that trip unauthorized-practice tests
  when no licensed reviewer stands behind them."
  [service-mode]
  (contains? #{:mode/legal-analysis} service-mode))

(defn global-summary
  "Cross-jurisdiction rollup: for each service and revenue mode, which
  catalogued jurisdictions currently admit it. Useful for deciding what
  to build once and ship everywhere versus what must be market-specific."
  []
  (let [js (facts/jurisdiction-ids)
        roll (fn [kind ks]
               (into {} (for [m ks]
                          [m {:admissible (vec (filter #(admissible? % kind m) js))
                              :conditional (vec (filter #(= :conditional (:verdict (verdict-for % kind m))) js))}])))]
    {:jurisdictions js
     :service-modes (roll :service (keys facts/service-modes))
     :revenue-modes (roll :revenue (keys facts/revenue-modes))
     :refused-by-design (vec (sort forbidden-revenue-modes))}))
