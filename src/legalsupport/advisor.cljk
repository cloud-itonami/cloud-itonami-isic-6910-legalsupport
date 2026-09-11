(ns legalsupport.advisor
  "Legal Support Advisor — proposes one operation for a matter: prepare a
  document, support a procedure, introduce the client to a lawyer, or
  open a new jurisdiction. Swappable mock/LLM.

  The advisor ONLY proposes. Three things it structurally cannot do,
  because they are read from the store by `legalsupport.governor` rather
  than taken from the proposal:

    - choose how the operator gets paid (`:revenue-mode` lives in
      `store/operator-config`),
    - attest that a `:conditional` legal condition is satisfied
      (`:attestations` likewise),
    - decide that a lawyer reviewed the output
      (`:reviewer-self-reviewed?` lives on the matter record).

  That containment is the point: an LLM that could name its own revenue
  mode could talk itself into a paid-referral business, and an LLM that
  could assert 'a lawyer reviewed this' could manufacture the very fact
  that makes the service lawful.

  A proposal:
    {:op :approve-document-preparation
        | :approve-procedure-support
        | :approve-lawyer-introduction
        | :approve-jurisdiction-onboarding
     :effect :propose
     :matter-id str
     :service-mode  a key of `legalsupport.facts/service-modes`
     :output-kind   :template | :assembled-document | :procedure-checklist
                    | :legal-analysis
     :stake kw :confidence n :rationale str}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])))

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake matter-id service-mode output-kind] :as request}]
  {:op op
   :effect :propose
   :matter-id matter-id
   :service-mode service-mode
   :output-kind output-kind
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name (or op :unknown))
                   " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a legal-document and procedure-support advisor. Given a
   request, propose an :op, the :matter-id, the :service-mode you would
   operate in, the :output-kind you would produce, an honest :confidence
   and a :stake.

   Never propose a :service-mode whose output performs individualized
   legal processing unless a licensed reviewer is already assigned to
   the matter — the governor independently checks the jurisdiction's
   rules, the assigned lawyer's licence and jurisdiction, and whether
   that lawyer personally reviewed the output, and will hold the run
   otherwise. Introducing a client to a lawyer and opening a new
   jurisdiction always require human sign-off regardless of confidence.
   Do not propose fees, revenue models or referral compensation of any
   kind; you are not the party that decides how this service is paid.")

(defn- parse-proposal [content]
  (try
    (let [p (edn/read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
