(ns legalsupport.store
  "SSoT for the ISIC Rev.5 6910 AI legal-document and procedure-support
  actor (itonami actor pattern, ADR-2607011000 / CLAUDE.md Actors
  section).

  Domain:

    operator-config — the deployment's own standing configuration:
        {:revenue-mode  one key of `legalsupport.facts/revenue-modes`
         :attestations  #{[kind mode] ...} the operator has signed for,
                        unlocking `:conditional` verdicts}
      It lives in the STORE, not in the advisor's proposal, on purpose:
      the advisor is an LLM and must not be able to choose how the
      business gets paid. The governor reads the revenue mode from here
      and checks it against the matter's jurisdiction on every run.

    client  — a registered person/organization seeking help
              {:client-id :name :jurisdiction}
    lawyer  — a registered lawyer
              {:lawyer-id :name :license-jurisdiction :bar-number
               :license-verified? boolean :verified-at}
      `:license-jurisdiction` is checked against the MATTER's
      jurisdiction, not the client's residence and not the operator's
      home market. A lawyer licensed elsewhere is not a licensed
      reviewer for this matter — cross-border review is how a
      lawyer-in-the-loop design quietly becomes unauthorized practice.
    matter  — {:matter-id :client-id :jurisdiction :subject
               :dispute? boolean :reviewer-id :reviewer-self-reviewed?}
      `:dispute?` records whether a dispute has arisen over the rights
      and obligations at issue — Japan's MOJ guideline calls this
      「事件性」 and it is the hinge on which a routine document becomes a
      legal matter. `:reviewer-self-reviewed?` records that the assigned
      lawyer personally examined the output and amended it as needed;
      merely assigning a reviewer is not review.
    record  — a committed output, written ONLY via commit-record!
    ledger  — append-only audit trail, commit or hold")

(defprotocol Store
  (operator-config [s])
  (client [s client-id])
  (lawyer [s lawyer-id])
  (matter [s matter-id])
  (records-of [s client-id])
  (ledger [s])
  (set-operator-config! [s cfg])
  (register-client! [s c])
  (register-lawyer! [s l])
  (register-matter! [s m])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(def default-operator-config
  "Deny-by-default: no revenue mode configured and nothing attested. A
  deployment that never calls `set-operator-config!` can commit nothing,
  which is the correct failure mode for a legal product."
  {:revenue-mode nil :attestations #{}})

(defrecord MemStore [a]
  Store
  (operator-config [_] (:operator-config @a))
  (client [_ client-id] (get-in @a [:clients client-id]))
  (lawyer [_ lawyer-id] (get-in @a [:lawyers lawyer-id]))
  (matter [_ matter-id] (get-in @a [:matters matter-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (set-operator-config! [s cfg]
    (swap! a update :operator-config merge cfg) s)
  (register-client! [s c]
    (swap! a assoc-in [:clients (:client-id c)] c) s)
  (register-lawyer! [s l]
    (swap! a assoc-in [:lawyers (:lawyer-id l)] l) s)
  (register-matter! [s m]
    (swap! a assoc-in [:matters (:matter-id m)] m) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed]
   (->MemStore (atom (merge {:operator-config default-operator-config
                             :clients {} :lawyers {} :matters {}
                             :records [] :ledger []}
                            seed)))))

(defn assigned-reviewer
  "The lawyer record assigned to review `m`, or nil. Convenience for the
  governor — deliberately does NOT check the licence; that check is the
  governor's job and must stay visible there."
  [s m]
  (some->> (:reviewer-id m) (lawyer s)))
