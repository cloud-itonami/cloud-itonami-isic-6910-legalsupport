(ns legalsupport.admissibility-test
  (:require [clojure.test :refer [deftest is testing]]
            [legalsupport.facts :as facts]
            [legalsupport.admissibility :as adm]))

(deftest uncatalogued-jurisdiction-is-denied
  (testing "研究していない法域は『対象外』ではなく『成立と判定しない』"
    (let [v (adm/verdict-for "ZWE" :service :mode/document-assembly)]
      (is (= :uncovered (:verdict v)))
      (is (re-find #"カバレッジ未達" (:reason v))))
    (is (false? (adm/admissible? "ZWE" :service :mode/document-assembly)))
    (is (false? (adm/admissible-with? "ZWE" :service :mode/document-assembly
                                      #{[:service :mode/document-assembly]}))
        "未収載法域は attestation でも解錠できない")))

(deftest unknown-mode-and-kind-are-denied-without-throwing
  (is (= :uncovered (:verdict (adm/verdict-for "JPN" :service :mode/does-not-exist))))
  (is (= :uncovered (:verdict (adm/verdict-for "JPN" :nonsense :mode/document-assembly))))
  (is (false? (adm/admissible? "JPN" :service :mode/does-not-exist))))

(deftest only-admissible-counts-as-admissible
  (testing ":conditional / :prohibited / :unsettled はいずれも成立ではない"
    (is (true? (adm/admissible? "JPN" :service :mode/lawyer-reviewed)))
    (is (false? (adm/admissible? "JPN" :service :mode/legal-analysis))
        ":conditional は既定では成立ではない")
    (is (false? (adm/admissible? "JPN" :service :mode/referral-placement)))
    (is (false? (adm/admissible? "JPN" :service :mode/lawyer-directory)))))

(deftest attestation-unlocks-conditional-only
  (let [att #{[:service :mode/legal-analysis]
              [:service :mode/referral-placement]
              [:service :mode/lawyer-directory]}]
    (is (true? (adm/admissible-with? "JPN" :service :mode/legal-analysis att))
        ":conditional は attestation で解錠できる")
    (is (false? (adm/admissible-with? "JPN" :service :mode/referral-placement att))
        ":prohibited は attestation で解錠できない")
    (is (false? (adm/admissible-with? "JPN" :service :mode/lawyer-directory att))
        ":unsettled は attestation で解錠できない")))

(deftest secondary-only-permissions-are-downgraded
  (testing "二次情報のみに支えられた :admissible は :unsettled に降格される"
    ;; 合成の伝聞ルールを注入する。実在の secondary-only ルールを名指しすると、
    ;; そのルールが一次に格上げされた瞬間にこのテストが機構でなく出典の
    ;; 都合で落ちる（実際 jpn.gyoseishoshi-ho の格上げで落ちた）。
    (with-redefs [facts/catalog
                  (-> facts/catalog
                      (update-in ["JPN" :jurisdiction/rules]
                                 conj {:rule/id "synthetic.hearsay"
                                       :rule/title "伝聞のみのルール"
                                       :rule/url "https://example.invalid/hearsay"
                                       :rule/url-provenance :secondary-commentary
                                       :rule/verification :secondary-source-only
                                       :rule/verification-note "テスト用の合成ルール"
                                       :rule/retrieved-at "2026-07-26"})
                      (assoc-in ["JPN" :jurisdiction/service-modes :mode/lawyer-directory]
                                {:verdict :admissible
                                 :basis ["synthetic.hearsay"]
                                 :condition "捏造された許可"}))]
      (let [v (adm/verdict-for "JPN" :service :mode/lawyer-directory)]
        (is (= :unsettled (:verdict v)))
        (is (= :admissible (:downgraded-from v)))
        (is (re-find #"secondary-source-only" (:reason v))))
      (is (false? (adm/admissible? "JPN" :service :mode/lawyer-directory))))))

(deftest referral-compensation-is-refused-in-every-jurisdiction
  (testing "法域が許していても本製品は送客対価を取らない"
    (doseq [jid (facts/jurisdiction-ids)
            rev adm/forbidden-revenue-modes]
      (let [r (adm/evaluate {:jurisdiction jid
                             :service-mode :mode/document-assembly
                             :revenue-mode rev
                             :attestations (set (for [k [:service :revenue]
                                                      m (concat (keys facts/service-modes)
                                                                (keys facts/revenue-modes))]
                                                  [k m]))})]
        (is (false? (:admissible? r))
            (str jid " で " rev " が通ってしまった"))
        (is (some #(= :referral-compensation-refused-by-design (:rule %)) (:blockers r))
            (str jid " で design 拒否の blocker が出ていない"))))))

(deftest japan-envelope-matches-the-moj-guideline
  (let [env (adm/operating-envelope "JPN")
        svc (set (get-in env [:service-modes :admissible]))
        rev (set (get-in env [:revenue-modes :admissible]))]
    (is (:covered? env))
    (testing "ガイドライン第4項のセーフハーバーと、3(1)イの定型処理が成立する"
      (is (contains? svc :mode/lawyer-reviewed))
      (is (contains? svc :mode/inhouse-reviewed))
      (is (contains? svc :mode/template-selection))
      (is (contains? svc :mode/document-assembly)))
    (testing "機械が単独で法的処理を行うモードと取次ぎは成立しない"
      (is (not (contains? svc :mode/legal-analysis)))
      (is (not (contains? svc :mode/referral-placement))))
    (testing "弁護士事務所向けライセンスが日本で最も確実な収益経路"
      (is (contains? rev :revenue/law-firm-saas-license)))
    (testing "サブスク・従量課金は『報酬を得る目的』を満たす方向なので既定では成立しない"
      (is (not (contains? rev :revenue/consumer-subscription)))
      (is (not (contains? rev :revenue/consumer-document-fee))))
    (is (= (vec (sort adm/forbidden-revenue-modes))
           (get-in env [:revenue-modes :refused-by-design])))
    (is (seq (:known-gaps env)))))

(deftest england-envelope-follows-the-reserved-activities-list
  (let [env (adm/operating-envelope "GBR")
        svc (set (get-in env [:service-modes :admissible]))]
    (testing "非留保業務なので法的助言まで成立する — 日本との構造的な違い"
      (is (contains? svc :mode/legal-analysis))
      (is (contains? svc :mode/lawyer-directory)))
    (is (not (contains? (set (get-in env [:revenue-modes :admissible]))
                        :revenue/per-referral-fee)))))

(deftest evaluate-reports-citations-for-what-it-allowed
  (let [r (adm/evaluate {:jurisdiction "JPN"
                         :service-mode :mode/lawyer-reviewed
                         :revenue-mode :revenue/law-firm-saas-license})]
    (is (true? (:admissible? r)))
    (is (empty? (:blockers r)))
    (is (seq (:citations r)))
    (is (every? #(re-find #"^https://" (str (:rule/url %))) (:citations r)))
    (is (some #(= "jpn.moj-ai-contract-guideline-2023" (:rule/id %)) (:citations r)))))

(deftest evaluate-blocks-and-explains
  (let [r (adm/evaluate {:jurisdiction "JPN"
                         :service-mode :mode/legal-analysis
                         :revenue-mode :revenue/consumer-subscription})]
    (is (false? (:admissible? r)))
    (is (= #{:service-mode-not-admissible :revenue-mode-not-admissible}
           (set (map :rule (:blockers r)))))
    (is (every? #(seq (:detail %)) (:blockers r)))))

(deftest global-summary-is-consistent-with-per-jurisdiction-verdicts
  (let [g (adm/global-summary)]
    (is (= (facts/jurisdiction-ids) (:jurisdictions g)))
    (doseq [[mode {:keys [admissible]}] (:service-modes g)
            jid admissible]
      (is (adm/admissible? jid :service mode)
          (str "global-summary が " jid "/" mode " を誤って成立としている")))
    (is (= (vec (sort adm/forbidden-revenue-modes)) (:refused-by-design g)))))

(deftest reviewer-and-processing-classifiers
  (is (adm/requires-licensed-reviewer? :mode/lawyer-reviewed))
  (is (adm/requires-licensed-reviewer? :mode/inhouse-reviewed))
  (is (not (adm/requires-licensed-reviewer? :mode/document-assembly)))
  (is (adm/machine-legal-processing? :mode/legal-analysis))
  (is (not (adm/machine-legal-processing? :mode/template-selection))))
