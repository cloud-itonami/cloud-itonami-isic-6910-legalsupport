(ns legalsupport.governor
  "LegalSupportGovernor — the independent layer gating every output the
  advisor may propose, and the only place the legal-fact catalog is
  turned into a decision about a live matter.

  The catalog is not documentation here. `legalsupport.admissibility/evaluate`
  is called on every single run, and any blocker it returns is a HARD
  hold. A jurisdiction nobody has researched, a service mode the local
  bar forbids, a revenue mode that would make the service a paid
  referral — none of them can reach `:commit`.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance      — the client must be registered.
    2. no-actuation           — proposal :effect must be :propose. The
                               governor never files, submits, serves or
                               pays anything; it gates what may be
                               prepared and handed over.
    3. matter basis           — the proposal must cite a REGISTERED
                               matter belonging to this client.
    4. admissibility          — every blocker from
                               `admissibility/evaluate` on the MATTER's
                               jurisdiction, the proposed service mode
                               and the operator's configured revenue
                               mode. This subsumes: uncatalogued
                               jurisdiction, non-admissible service
                               mode, non-admissible revenue mode, and
                               referral compensation (refused by design
                               in every jurisdiction).
    5. licensed reviewer      — when the mode's lawfulness rests on a
                               lawyer (`requires-licensed-reviewer?`) or
                               the machine performs individualized legal
                               processing (`machine-legal-processing?`),
                               a registered lawyer must be assigned,
                               licence-verified, licensed IN THE
                               MATTER'S JURISDICTION, and recorded as
                               having personally reviewed and amended
                               the output. Japan's MOJ guideline §4(1)
                               is explicit that it is the lawyer
                               examining and amending — not the mere
                               existence of a lawyer — that makes the
                               service lawful.
    6. dispute containment    — a matter with a dispute already arisen
                               (`:dispute?`, 「事件性」) cannot be handled
                               without that licensed reviewer, whatever
                               the mode. A dispute is where document
                               help becomes a legal case.

  ESCALATION invariants (:escalate? true, human sign-off regardless of
  confidence):
    7. :op :approve-lawyer-introduction — putting a person in front of a
                               lawyer is never automatic, even though
                               this product takes no fee for it.
    8. :op :approve-jurisdiction-onboarding — opening a new market is a
                               research decision, not a runtime one.
    9. attestation-unlocked operation — when the service mode was only
                               admissible because the operator attested
                               to a condition the catalog cannot verify,
                               a human signs each run.
   10. low confidence (< `confidence-floor`)."
  (:require [legalsupport.admissibility :as adm]
            [legalsupport.store :as store]))

(def confidence-floor 0.6)

(def ^:private always-escalate-ops
  #{:approve-lawyer-introduction :approve-jurisdiction-onboarding})

(defn- reviewer-violations
  "Checks 5 and 6. `m` is the matter, `rv` the assigned lawyer record."
  [m rv service-mode]
  (let [needs? (or (adm/requires-licensed-reviewer? service-mode)
                   (adm/machine-legal-processing? service-mode)
                   (boolean (:dispute? m)))
        why (cond
              (adm/requires-licensed-reviewer? service-mode)
              "当該モードは有資格弁護士の精査を前提に成立する"
              (adm/machine-legal-processing? service-mode)
              "機械が個別事案の法的処理を行うモードである"
              :else "当該 matter に事件性（紛争）がある")]
    (when needs?
      (cond-> []
        (nil? rv)
        (conj {:rule :no-licensed-reviewer
               :detail (str why "が、レビュー担当弁護士が未登録である。")})

        (and rv (not (:license-verified? rv)))
        (conj {:rule :reviewer-license-unverified
               :detail (str "担当弁護士 " (:lawyer-id rv) " の資格が未検証である。")})

        (and rv (not= (:license-jurisdiction rv) (:jurisdiction m)))
        (conj {:rule :reviewer-wrong-jurisdiction
               :detail (str "担当弁護士の資格法域 " (pr-str (:license-jurisdiction rv))
                            " が matter の法域 " (pr-str (:jurisdiction m))
                            " と一致しない。他法域の資格者によるレビューは"
                            "レビューとして成立せず、無資格法律業務になりうる。")})

        (and rv (not (:reviewer-self-reviewed? m)))
        (conj {:rule :reviewer-did-not-self-review
               :detail (str "担当弁護士が自ら精査し必要に応じ自ら修正した記録が無い。"
                            "弁護士を割り当てただけではレビューではない。")})))))

(def ^:private prep-ops
  #{:approve-document-preparation :approve-procedure-support})

(defn- gated-service-mode
  "The service mode the admissibility catalog must be consulted about
  for this op. Introducing a client to a lawyer is gated as
  `:mode/referral-placement` even though the advisor never names that
  mode — otherwise an introduction op would slip past the jurisdiction
  check that forbids it in Japan, India and Brazil."
  [{:keys [op service-mode]}]
  (cond
    (contains? prep-ops op) service-mode
    (= :approve-lawyer-introduction op) :mode/referral-placement
    :else nil))

(defn- hard-violations [{:keys [request proposal]} client-record m rv cfg]
  (let [{:keys [op]} proposal
        prep? (contains? prep-ops op)
        gated (gated-service-mode proposal)
        eval-res (when (and gated m)
                   (adm/evaluate {:jurisdiction (:jurisdiction m)
                                  :service-mode gated
                                  :revenue-mode (:revenue-mode cfg)
                                  :attestations (:attestations cfg)}))]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail (str "effect は :propose のみ許可（governor は提出・申立て・送達・"
                          "支払いを直接実行しない）")})

      (and gated (nil? m))
      (conj {:rule :unknown-matter :detail "未登録 matter への作業準備・取次ぎは不可"})

      (and gated m (not= (:client-id m) (:client-id request)))
      (conj {:rule :matter-wrong-client :detail "matter が別 client のもの"})

      (seq (:blockers eval-res))
      (into (:blockers eval-res))

      (and prep? m)
      (into (reviewer-violations m rv (:service-mode proposal))))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a `store`
  implementing `legalsupport.store/Store`. Pure — never mutates the
  store, never files or submits anything, never contacts a lawyer."
  [request _context proposal store]
  (let [cfg (store/operator-config store)
        client-record (store/client store (:client-id request))
        m (some->> (:matter-id proposal) (store/matter store))
        rv (when m (store/assigned-reviewer store m))
        hard (hard-violations {:request request :proposal proposal}
                              client-record m rv cfg)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))
        gated (gated-service-mode proposal)
        attested? (and m gated
                       (= :conditional
                          (:verdict (adm/verdict-for (:jurisdiction m) :service gated))))]
    {:ok? (and (not hard?) (not low?) (not always-risky?) (not attested?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky? attested?))
     :escalation-reasons (cond-> []
                           low? (conj :low-confidence)
                           always-risky? (conj :always-escalate-op)
                           attested? (conj :attestation-unlocked-mode))
     :citations (when (and m gated)
                  (adm/citations (:jurisdiction m) :service gated))}))
