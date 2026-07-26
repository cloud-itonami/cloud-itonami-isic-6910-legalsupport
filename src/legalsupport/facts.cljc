(ns legalsupport.facts
  "Jurisdiction legal-fact catalog for a governed AI legal-document and
  procedure-support service (ISIC Rev.5 6910, legal activities).

  This namespace answers ONE question per (jurisdiction, mode) pair:
  *is this way of operating legally established, and on what cited
  authority?* It is a fact table, not an opinion. Three rules govern
  every entry:

    1. NEVER fabricate a rule id, citation or URL. A jurisdiction or a
       mode that is not in `catalog` has NO spec-basis, full stop —
       extend the catalog, do not invent an entry.
    2. Every rule carries `:rule/verification`, recording how well the
       source was actually checked:
         :primary-source-read    — the authoritative document itself was
                                   retrieved and read (statute text,
                                   official guideline PDF).
         :official-url-retrieved — an official/regulator page was
                                   retrieved and read, but it restates
                                   rather than IS the primary instrument.
         :secondary-source-only  — only commentary/summary was seen. Such
                                   a rule may support a RESTRICTIVE
                                   verdict (:prohibited/:conditional/
                                   :unsettled) but may NEVER be the sole
                                   basis of an :admissible one. That
                                   asymmetry is deliberate: guessing
                                   'permitted' exposes users to criminal
                                   unauthorized-practice liability, while
                                   guessing 'restricted' only costs
                                   coverage. `legalsupport.admissibility`
                                   enforces it; `facts-test` proves it.
    3. Coverage is reported honestly. A jurisdiction not in `catalog` is
       a COVERAGE GAP, never an 'out of scope' jurisdiction — see
       `coverage` and `known-gaps`.

  Verdict vocabulary (`:verdict`):
    :admissible  — established as lawful on verified authority, subject
                   to `:condition` if present.
    :conditional — lawful only if a condition holds that this catalog
                   cannot itself verify (a licence the operator must
                   hold, a registration, a fact about the matter).
    :prohibited  — an authority cited here forbids it.
    :unsettled   — genuinely unresolved, or not yet researched to a
                   standard that supports any verdict. Treated as NOT
                   admissible everywhere in this codebase.

  Retrieval date for every entry below: 2026-07-26.")

;; ---------------------------------------------------------------------------
;; Vocabulary
;; ---------------------------------------------------------------------------

(def service-modes
  "The ways this product can act on a matter, ordered by how much legal
  processing the machine does. The line that matters almost everywhere
  is between `:mode/document-assembly` (mechanical) and
  `:mode/legal-analysis` (individualized legal processing) — that is
  precisely where Japan's MOJ guideline draws it, and it maps closely
  onto unauthorized-practice tests elsewhere."
  {:mode/template-selection
   "利用者の定型入力・選択肢に従い、登録済みひな形を選別してそのまま表示する。個別事案の法的処理を行わない。"

   :mode/document-assembly
   "選別されたひな形に利用者の入力内容を反映して変更表示する。字句の反映にとどまり、個別事案の法的処理を行わない。"

   :mode/legal-analysis
   "個別事案の経緯・背景・意図を法的に処理し、具体的な文書・法的リスクの有無/程度・個別の修正案を提示する。"

   :mode/lawyer-reviewed
   "上記出力を、当該法域で有効な資格を持つ弁護士に提供し、その弁護士が自ら精査し必要に応じ自ら修正する方法で用いる。"

   :mode/inhouse-reviewed
   "利用者自身が当事者となる案件について、利用者の役職員である有資格弁護士が :mode/lawyer-reviewed と同等の方法で用いる。"

   :mode/lawyer-directory
   "弁護士の情報を掲載・検索可能にする。個別案件の割当・推薦は行わない。"

   :mode/referral-placement
   "個別の相談者を個別の弁護士に取り次ぐ（周旋・斡旋・送客）。"})

(def revenue-modes
  "How the operator may be paid. Kept separate from `service-modes`
  because in several jurisdictions the SAME service flips from lawful to
  unlawful purely on who pays whom — Japan's Art. 72 turns on 「報酬を得
  る目的」, Germany's BRAO §49b(3) on whether the payment is consideration
  for a mediated mandate."
  {:revenue/none                 "一切の利益供与を受けない。"
   :revenue/consumer-document-fee "相談者から書類支援1件あたりの対価を受ける。"
   :revenue/consumer-subscription "相談者から月額等の利用資格対価を受ける。"
   :revenue/law-firm-saas-license "弁護士・弁護士法人にツール利用ライセンスを販売する。"
   :revenue/directory-listing-flat "個別案件と連動しない定額の掲載料を弁護士から受ける。"
   :revenue/per-referral-fee     "取り次いだ案件1件あたりの対価を弁護士から受ける。"
   :revenue/success-fee-share    "案件の結果・回収額に連動した分配を受ける。"})

(def verifications
  #{:primary-source-read :official-url-retrieved :secondary-source-only})

(def binding-forces
  "A rule's force, which is NOT the same question as how well its source
  was checked. A model rule can be read at the primary source and still
  bind nobody.

    :law        in force in the jurisdiction it is filed under.
    :model-only a drafting template with no binding force anywhere until
                a jurisdiction adopts it — and adopters modify it. The
                ABA Model Rules forced this field: citing \"Model Rule
                5.4\" as authority for what a US operator may do is a
                category error, because the operative law is each state's
                own adopted version and those differ materially (D.C.
                permits nonlawyer ownership under its 5.4(b); Arizona
                repealed 5.4 outright)."
  #{:law :model-only})

(defn model-only?
  "True for a rule that is not law anywhere. Such a rule may be held for
  orientation — it names what states adapt — but must never carry a
  verdict."
  [r]
  (= :model-only (:rule/binding-force r)))

(def verdicts #{:admissible :conditional :prohibited :unsettled})

;; ---------------------------------------------------------------------------
;; Catalog
;; ---------------------------------------------------------------------------

(def catalog
  "jurisdiction-id -> jurisdiction entry.

  Sub-national ids (USA-DC / USA-AZ / USA-UT) exist because in the
  United States the practice of law is regulated state by state; a
  single \"USA\" verdict would be a fabrication. The bare \"USA\" entry
  is deliberately :unsettled throughout and points at the state entries."
  {"JPN"
   {:jurisdiction/id "JPN"
    :jurisdiction/name "Japan"
    :jurisdiction/iso3166 "JPN"
    :jurisdiction/legal-system :civil-law
    :jurisdiction/note
    (str "弁護士法72条は刑罰法規であり、あてはめは最終的に裁判所の判断による"
         "（法務省ガイドライン冒頭が明示）。以下の verdict は当局が公表した"
         "考慮要素の再述であって、特定サービスの適法性の保証ではない。")
    :jurisdiction/rules
    [{:rule/id "jpn.bengoshi-ho-72"
      :rule/title "弁護士法第72条（非弁護士の法律事務の取扱い等の禁止）"
      :rule/instrument "弁護士法（昭和24年法律第205号）"
      :rule/quote
      (str "弁護士又は弁護士法人でない者は、報酬を得る目的で訴訟事件、非訟事件及び"
           "審査請求、異議申立て、再審査請求等行政庁に対する不服申立事件その他一般の"
           "法律事件に関して鑑定、代理、仲裁若しくは和解その他の法律事務を取り扱い、"
           "又はこれらの周旋をすることを業とすることができない。ただし、この法律又は"
           "他の法律に別段の定めがある場合は、この限りでない。")
      :rule/url "https://www8.cao.go.jp/kisei-kaikaku/kisei/meeting/wg/2501_06ai/260109/ai06_05.pdf"
      :rule/url-provenance :official-government-document
      :rule/verification :primary-source-read
      :rule/verification-note
      (str "条文本文は法務省大臣官房司法法制部『弁護士法72条とAIリーガルテック"
           "サービス』（令和8年1月9日、規制改革推進会議 資料5）が逐語引用したもの"
           "を読んで転記した。e-Gov 法令検索の原典ページは JavaScript レンダリング"
           "のため本ツールでは取得できなかった。")
      :rule/retrieved-at "2026-07-26"
      :rule/topic #{:unauthorized-practice :referral}}

     {:rule/id "jpn.supreme-court-1971-07-14"
      :rule/title "最高裁大法廷判決 昭和46年7月14日（刑集25巻5号690頁）— 弁護士法72条の趣旨"
      :rule/instrument "最高裁判所大法廷判決"
      :rule/quote
      (str "資格もなく、なんらの規律にも服しない者が、みずからの利益のため、みだりに"
           "他人の法律事件に介入することを業とするような例もないではなく、これを放置"
           "するときは、当事者その他の関係人らの利益をそこね、法律生活の公正かつ円滑な"
           "いとなみを妨げ、ひいては法律秩序を害することになるので、同条は、かかる行為"
           "を禁圧するために設けられた。")
      :rule/url "https://www8.cao.go.jp/kisei-kaikaku/kisei/meeting/wg/2501_06ai/260109/ai06_05.pdf"
      :rule/url-provenance :official-government-document
      :rule/verification :primary-source-read
      :rule/verification-note "法務省資料5（令和8年1月9日）が引用した趣旨部分を読んで転記。"
      :rule/retrieved-at "2026-07-26"
      :rule/topic #{:unauthorized-practice :legislative-intent}}

     {:rule/id "jpn.moj-ai-contract-guideline-2023"
      :rule/title "AI等を用いた契約書等関連業務支援サービスの提供と弁護士法第72条との関係について"
      :rule/instrument "法務省大臣官房司法法制部 ガイドライン（令和5年8月）"
      :rule/url "https://www.moj.go.jp/content/001400675.pdf"
      :rule/url-provenance :official-government-site
      :rule/verification :primary-source-read
      :rule/verification-note "PDF 全6ページを取得して全文読了。"
      :rule/established-date "2023-08"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "72条違反には「報酬を得る目的」「訴訟事件…その他一般の法律事件」"
           "「鑑定…その他の法律事務」の3要件すべての該当が必要で、いずれか1つでも"
           "外れれば違反しない（第1〜3項）。3要件すべてに該当する場合でも、"
           "弁護士等が自ら精査し必要に応じ自ら修正する方法で利用するときは違反しない"
           "（第4項）。生成AIを用いるサービスも原則同じ枠組みで判断する。")
      :rule/topic #{:unauthorized-practice :ai-legaltech :document-assistance}}

     {:rule/id "jpn.moj-legaltech-review-2026"
      :rule/title "弁護士法72条とAIリーガルテックサービス（規制改革推進会議 資料5）"
      :rule/instrument "法務省大臣官房司法法制部 説明資料（令和8年1月9日）"
      :rule/url "https://www8.cao.go.jp/kisei-kaikaku/kisei/meeting/wg/2501_06ai/260109/ai06_05.pdf"
      :rule/url-provenance :official-government-document
      :rule/verification :primary-source-read
      :rule/verification-note "スライド全編を取得して読了。"
      :rule/established-date "2026-01-09"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "法務省は2023年ガイドラインの「再整理」の必要性を自ら認めている："
           "①AI の進展でガイドラインとの関係が不明確なサービスが増えた、"
           "②ガイドライン公表がかえって開発を萎縮させている可能性がある、"
           "③一方で個人情報漏洩・ハルシネーションによる誤った法的情報の拡散という"
           "技術的・社会的リスクがあり、ガバナンス確保の検討が必要。"
           "対応方向としてハードローは機動性に欠け、ソフトローも「72条は刑罰法規で"
           "あって個別のあてはめは司法判断であるため、個別具体のリーガルテックサービスを"
           "対象とするホワイトリストの策定は困難」とされ、段階的解決を検討中。"
           "→ 本カタログの JPN verdict は当局側が可変と明言している面であり、"
           "定期再検証が必要。")
      :rule/topic #{:ai-legaltech :governance :forward-looking}}

     {:rule/id "jpn.gyoseishoshi-ho"
      :rule/title "行政書士法 第19条（業務の制限）"
      :rule/instrument "行政書士法（昭和26年法律第4号）"
      :rule/quote
      (str "行政書士又は行政書士法人でない者は、他人の依頼を受けいかなる名目に"
           "よるかを問わず報酬を得て、業として第一条の三に規定する業務を行うことが"
           "できない。ただし、他の法律に別段の定めがある場合及び定型的かつ容易に"
           "行えるものとして総務省令で定める手続について、当該手続に関し相当の経験"
           "又は能力を有する者として総務省令で定める者が電磁的記録を作成する場合は、"
           "この限りでない。")
      :rule/url "https://laws.e-gov.go.jp/api/1/articles;lawNum=昭和二十六年法律第四号;article=19"
      :rule/url-provenance :official-legislation-api
      :rule/verification :primary-source-read
      :rule/verification-note
      (str "e-Gov 法令 API から19条本文を取得して読了。"
           "なお独占業務の規定は19条であって1条の2ではない（1条の2は職責）。"
           "19条が参照する第一条の三（業務の範囲）は未取得。")
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "構造が弁護士法72条とよく似ている —— 『**いかなる名目によるかを問わず"
           "報酬を得て、業として**』が構成要件。日本で書類作成支援を有償提供する場合、"
           "72条とは別にこの制限が重なる。"
           "**但書に電磁的記録の例外がある**点が重要で、定型的かつ容易な手続について"
           "総務省令が定める者が電磁的記録を作成する場合は制限の外に出る —— "
           "デジタル書類支援にとって条文上の入口がここに在る。")
      :rule/topic #{:document-assistance :adjacent-profession}}

     {:rule/id "jpn.shihoshoshi-ho-3"
      :rule/title "司法書士法 第3条（業務）"
      :rule/instrument "司法書士法（昭和25年法律第197号）"
      :rule/quote
      (str "司法書士は、この法律の定めるところにより、他人の依頼を受けて、次に掲げる"
           "事務を行うことを業とする。一 登記又は供託に関する手続について代理すること。"
           "二 法務局又は地方法務局に提出し、又は提供する書類又は**電磁的記録**…を"
           "作成すること。… 四 裁判所若しくは検察庁に提出する書類若しくは電磁的記録"
           "…を作成すること。五 前各号の事務について相談に応ずること。"
           "六 簡易裁判所における…手続について代理すること。…")
      :rule/url "https://laws.e-gov.go.jp/api/1/articles;lawNum=昭和二十五年法律第百九十七号;article=3"
      :rule/url-provenance :official-legislation-api
      :rule/verification :primary-source-read
      :rule/verification-note "e-Gov 法令 API から3条本文を取得して読了。"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "**書類だけでなく電磁的記録の作成も明文で含む**ので、電子的な書類生成が"
           "そのまま業務範囲に入る。5号が『相談に応ずること』まで含む点も広い。"
           "登記・供託・裁判所提出書類を扱う機能は72条の前にこちらに当たる。")
      :rule/topic #{:document-assistance :adjacent-profession}}]

    :jurisdiction/service-modes
    {:mode/template-selection
     {:verdict :admissible
      :basis ["jpn.moj-ai-contract-guideline-2023"]
      :condition
      (str "定型的な入力・選択肢の結果に従い、登録済みひな形が選別されてそのまま"
           "表示されるにとどまること（ガイドライン3(1)イ）。項目・選択肢が極めて"
           "詳細で実質的に非定型入力といえる場合は3(1)ア(イ)に該当し得る。")}

     :mode/document-assembly
     {:verdict :admissible
      :basis ["jpn.moj-ai-contract-guideline-2023"]
      :condition
      (str "選別されたひな形に入力内容・選択結果が反映されて表示が変更される"
           "にとどまること（ガイドライン3(1)イ）。個別事案の経緯・背景を法的に"
           "処理した結果としての文書表示に踏み込まないこと。")}

     :mode/legal-analysis
     {:verdict :conditional
      :basis ["jpn.moj-ai-contract-guideline-2023" "jpn.bengoshi-ho-72"]
      :condition
      (str "個別事案に応じた法的リスクの有無・程度の表示や、法的に処理した具体的"
           "修正案の表示は「鑑定…その他の法律事務」に該当し得る（ガイドライン"
           "3(1)ア・3(2)ア・3(3)ア）。したがって「報酬を得る目的」または「事件性」"
           "のいずれかを構造的に外すか、ガイドライン第4項のセーフハーバー"
           "（:mode/lawyer-reviewed / :mode/inhouse-reviewed）に載せる必要がある。")}

     :mode/lawyer-reviewed
     {:verdict :admissible
      :basis ["jpn.moj-ai-contract-guideline-2023"]
      :condition
      (str "弁護士又は弁護士法人に提供し、当該弁護士（弁護士法人の社員又は使用人"
           "である弁護士を含む）が本サービスの利用結果も踏まえて対象文書を自ら"
           "精査し、必要に応じて自ら修正する方法で利用すること（ガイドライン4(1)）。"
           "この要件を満たすとき、上記1〜3の要件すべてに該当する場合であっても"
           "72条に違反しないと考えられる、と当局が明示している。")}

     :mode/inhouse-reviewed
     {:verdict :admissible
      :basis ["jpn.moj-ai-contract-guideline-2023"]
      :condition
      (str "提供先が当事者となっている契約について、提供先の職員・使用人又は"
           "取締役・理事その他の役員である弁護士が4(1)と同等の方法で利用すること"
           "（ガイドライン4(2)）。")}

     :mode/lawyer-directory
     {:verdict :unsettled
      :basis []
      :condition
      (str "日弁連の弁護士等の業務広告に関する規程および72条の「周旋」該当性を"
           "原典で未検証。掲載サービスを実装する前に検証すること。")}

     :mode/referral-placement
     {:verdict :prohibited
      :basis ["jpn.bengoshi-ho-72"]
      :condition
      (str "72条本文が「これらの周旋をすることを業とすること」を明文で禁じている"
           "（報酬を得る目的がある場合）。本製品は法域を問わず送客対価を取らない"
           "設計だが、日本では対価の有無以前に周旋の業としての反復自体が構成要件に"
           "触れうるため、個別案件の取次ぎ機能を日本向けに実装しない。")}}

    :jurisdiction/revenue-modes
    {:revenue/none
     {:verdict :admissible
      :basis ["jpn.moj-ai-contract-guideline-2023"]
      :condition
      (str "利用料等一切の利益供与を受けずに提供する場合、通常「報酬を得る目的」に"
           "該当しない（ガイドライン1(1)）。ただし1(2)ア〜ウの誘導・連動があれば"
           "外観上無償でも該当し得る。")}

     :revenue/law-firm-saas-license
     {:verdict :admissible
      :basis ["jpn.moj-ai-contract-guideline-2023"]
      :condition
      (str "弁護士・弁護士法人を利用者とし、当該弁護士が自ら精査・修正する方法で"
           "使う限り、報酬・事件性・法律事務の3要件すべてに該当してもガイドライン"
           "4(1)により違反しない。日本で最も確実に成立する収益経路。")}

     :revenue/consumer-document-fee
     {:verdict :conditional
      :basis ["jpn.moj-ai-contract-guideline-2023" "jpn.gyoseishoshi-ho"]
      :condition
      (str "相談者から対価を取ると「報酬を得る目的」を満たす方向に働くため、"
           "提供機能を :mode/template-selection / :mode/document-assembly の範囲に"
           "とどめて「鑑定…その他の法律事務」要件を外すか、事件性のない事項に"
           "限定する必要がある。加えて官公署提出書類を扱う場合は行政書士法の"
           "独占業務に抵触しないかを別途検証すること（当該 rule は未検証）。")}

     :revenue/consumer-subscription
     {:verdict :conditional
      :basis ["jpn.moj-ai-contract-guideline-2023"]
      :condition
      (str "「顧問料・サブスクリプション利用料・会費等の名目を問わず金銭等を支払って"
           "利用資格を得たものに対してのみ本件サービスを提供するとき」は「報酬を得る"
           "目的」に該当し得ると明示されている（ガイドライン1(2)ウ）。サブスクは"
           "無償扱いにならない。")}

     :revenue/directory-listing-flat
     {:verdict :unsettled
      :basis []
      :condition "掲載料と72条「周旋」・日弁連広告規程との関係を原典で未検証。"}

     :revenue/per-referral-fee
     {:verdict :prohibited
      :basis ["jpn.bengoshi-ho-72" "jpn.moj-ai-contract-guideline-2023"]
      :condition
      (str "第三者の有償サービスへ誘導し、利用者がそれを利用した際に当該第三者から"
           "事業者へ金銭等が支払われるときは「報酬を得る目的」に該当し得る"
           "（ガイドライン1(2)イ）。これは弁護士への送客に対して弁護士から対価を"
           "受け取る構造そのものであり、かつ72条の「周旋」を業とすることに当たる。")}

     :revenue/success-fee-share
     {:verdict :prohibited
      :basis ["jpn.bengoshi-ho-72" "jpn.moj-ai-contract-guideline-2023"]
      :condition "同上。結果連動はさらに「報酬を得る目的」との対価関係を明確にする。"}}

    :jurisdiction/known-gaps
    ["弁護士法27条（非弁提携）の原文・射程が未検証"
     "日弁連 弁護士等の業務広告に関する規程が未取得"
     "行政書士法1条の2/19条、司法書士法3条の原文が未取得（現在は制限方向にのみ作用）"]}

   ;; -----------------------------------------------------------------------
   "GBR"
   {:jurisdiction/id "GBR"
    :jurisdiction/name "United Kingdom (England & Wales)"
    :jurisdiction/iso3166 "GBR"
    :jurisdiction/legal-system :common-law
    :jurisdiction/note
    (str "England & Wales のみを対象とする。スコットランド・北アイルランドは"
         "別法域であり本エントリは適用されない（coverage gap）。")
    :jurisdiction/rules
    [{:rule/id "gbr.lsa-2007-s12-sch2"
      :rule/title "Legal Services Act 2007 s.12 / Schedule 2 — the reserved legal activities"
      :rule/instrument "Legal Services Act 2007 (c.29)"
      :rule/url "https://www.legislation.gov.uk/ukpga/2007/29/schedule/2"
      :rule/url-provenance :official-legislation-site
      :rule/verification :primary-source-read
      :rule/verification-note "legislation.gov.uk の Schedule 2 本文を取得して読了。"
      :rule/established-date "2010-01-01"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "留保法務活動は6類型に限定される: rights of audience / conduct of "
           "litigation / reserved instrument activities（土地登記関係の証書作成等）/ "
           "probate activities（プロベイト書類の作成）/ notarial activities / "
           "administration of oaths。これ以外の法的助言・書類作成・代理は"
           "留保されておらず、authorised person でなくとも行える。")
      :rule/topic #{:unauthorized-practice :document-assistance}}

     {:rule/id "gbr.laspo-2012-s56"
      :rule/title "LASPO 2012 s.56 — rules against referral fees"
      :rule/instrument "Legal Aid, Sentencing and Punishment of Offenders Act 2012 (c.10)"
      :rule/quote
      (str "(1) A regulated person is in breach of this section if— (a) the regulated "
           "person refers prescribed legal business to another person and is paid or has "
           "been paid for the referral, or (b) prescribed legal business is referred to "
           "the regulated person, and the regulated person pays or has paid for the "
           "referral. (2) A regulated person is also in breach of this section if in "
           "providing legal services in the course of prescribed legal business the "
           "regulated person— (a) arranges for another person to provide services to the "
           "client, and (b) is paid or has been paid for making the arrangement.")
      :rule/url "https://www.legislation.gov.uk/ukpga/2012/10/section/56"
      :rule/url-provenance :official-legislation-site
      :rule/verification :primary-source-read
      :rule/verification-note "legislation.gov.uk の XML から s.56 本文を取得して読了。"
      :rule/established-date "2013-04-01"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "2013年4月1日以降、人身傷害・死亡に係る損害賠償請求について、"
           "regulated person による紹介料の支払い・受領が禁止される。"
           "solicitor・claims management company・保険会社・保険仲介者を捕捉し、"
           "「payment」にはあらゆる形態の対価を含む。")
      :rule/topic #{:referral :fee-sharing}}]

    :jurisdiction/service-modes
    {:mode/template-selection
     {:verdict :admissible :basis ["gbr.lsa-2007-s12-sch2"]
      :condition "6類型の留保活動に当たらないこと。"}
     :mode/document-assembly
     {:verdict :admissible :basis ["gbr.lsa-2007-s12-sch2"]
      :condition
      (str "6類型の留保活動に当たらないこと。特に reserved instrument activities"
           "（土地登記関係の証書作成・登記申請）と probate activities"
           "（プロベイト書類の作成）は authorised person 限定なので、"
           "不動産・相続分野の書類生成はこの範囲から除外すること。")}
     :mode/legal-analysis
     {:verdict :admissible :basis ["gbr.lsa-2007-s12-sch2"]
      :condition
      (str "法的助言および紛争解決に関する助言・代理は非留保活動であり、"
           "authorised person でなくとも提供できる。ただし訴訟遂行"
           "（conduct of litigation）と法廷での弁論権に踏み込まないこと。"
           "非留保であることは無規制を意味せず、消費者保護法・広告規制は別途適用される。")}
     :mode/lawyer-reviewed {:verdict :admissible :basis ["gbr.lsa-2007-s12-sch2"] :condition "同上。"}
     :mode/inhouse-reviewed {:verdict :admissible :basis ["gbr.lsa-2007-s12-sch2"] :condition "同上。"}
     :mode/lawyer-directory {:verdict :admissible :basis ["gbr.lsa-2007-s12-sch2"]
                             :condition "掲載自体は留保活動でない。SRA の透明性規則は別途適用。"}
     :mode/referral-placement
     {:verdict :conditional :basis ["gbr.laspo-2012-s56"]
      :condition
      (str "人身傷害・死亡に係る案件では紹介自体に対価を伴う経路が禁止される。"
           "本製品は対価を取らない設計だが、対価のない取次ぎであることを"
           "監査可能な形で示せることが条件。")}}

    :jurisdiction/revenue-modes
    {:revenue/none {:verdict :admissible :basis ["gbr.lsa-2007-s12-sch2"] :condition nil}
     :revenue/law-firm-saas-license {:verdict :admissible :basis ["gbr.lsa-2007-s12-sch2"]
                                     :condition "ソフトウェアの提供は留保活動でない。"}
     :revenue/consumer-document-fee {:verdict :admissible :basis ["gbr.lsa-2007-s12-sch2"]
                                     :condition "非留保活動の対価であること。"}
     :revenue/consumer-subscription {:verdict :admissible :basis ["gbr.lsa-2007-s12-sch2"]
                                     :condition "非留保活動の対価であること。"}
     :revenue/directory-listing-flat {:verdict :conditional :basis ["gbr.laspo-2012-s56"]
                                      :condition "人身傷害案件で紹介料と評価されない構造であること。"}
     :revenue/per-referral-fee {:verdict :prohibited :basis ["gbr.laspo-2012-s56"]
                                :condition "人身傷害・死亡案件では明文で禁止。他分野でも SRA 規則の対象。"}
     :revenue/success-fee-share {:verdict :unsettled :basis []
                                 :condition "damages-based agreement 規制を未検証。"}}

    :jurisdiction/known-gaps
    ["LASPO s.56 の条文原文が未取得（SRA ガイダンス要約のみ）"
     "SRA Standards and Regulations の透明性・紹介規則が未取得"
     "スコットランド・北アイルランドは未収載"]}

   ;; -----------------------------------------------------------------------
   "DEU"
   {:jurisdiction/id "DEU"
    :jurisdiction/name "Germany"
    :jurisdiction/iso3166 "DEU"
    :jurisdiction/legal-system :civil-law
    :jurisdiction/rules
    [{:rule/id "deu.brao-49b-3"
      :rule/title "BRAO § 49b Abs. 3 — Verbot der Vermittlungsprovision"
      :rule/instrument "Bundesrechtsanwaltsordnung (BRAO)"
      :rule/quote
      (str "Die Abgabe und Entgegennahme eines Teils der Gebühren oder sonstiger "
           "Vorteile für die Vermittlung von Aufträgen … ist unzulässig")
      :rule/url "https://www.gesetze-im-internet.de/brao/__49b.html"
      :rule/url-provenance :official-legislation-site
      :rule/verification :primary-source-read
      :rule/verification-note "gesetze-im-internet.de の §49b 本文を取得して読了。"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "受任の仲介に対する報酬・利益の授受は、相手が弁護士であれ第三者であれ"
           "不可。目的は弁護士が受任の買い付け競争に入ることを防ぐこと。"
           "他の弁護士による RVG 別表1 Nr.3400 の枠を超える業務への相当な報酬は例外。")
      :rule/topic #{:referral :fee-sharing}}

     {:rule/id "deu.brao-49b-2"
      :rule/title "BRAO § 49b Abs. 2 — Erfolgshonorar"
      :rule/instrument "Bundesrechtsanwaltsordnung (BRAO)"
      :rule/quote
      (str "Vereinbarungen, durch die eine Vergütung oder ihre Höhe vom Ausgang der "
           "Sache oder vom Erfolg der anwaltlichen Tätigkeit abhängig gemacht wird … "
           "sind unzulässig")
      :rule/url "https://www.gesetze-im-internet.de/brao/__49b.html"
      :rule/url-provenance :official-legislation-site
      :rule/verification :primary-source-read
      :rule/verification-note "同上。RVG に基づく例外の存在も確認。"
      :rule/retrieved-at "2026-07-26"
      :rule/topic #{:fee-sharing :success-fee}}

     {:rule/id "deu.bgh-viii-zr-285-18"
      :rule/title "BGH, Urteil vom 27.11.2019 – VIII ZR 285/18 (LexFox / wenigermiete.de)"
      :rule/instrument "Bundesgerichtshof"
      :rule/url "https://www.lto.de/recht/juristen/b/bgh-viii-zr-285-18-legal-tech-wenigermiete-de-inkassodienstleistung-weite-auslegung-abtretung-wirksam-rechtsdienstleistungsgesetz"
      :rule/url-provenance :secondary-commentary
      :rule/verification :secondary-source-only
      :rule/verification-note "判決原文は未取得。法律メディアの要約のみ。"
      :rule/established-date "2019-11-27"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "登録済み Inkassodienstleister としての legal tech プラットフォームの"
           "業務が債権回収権限の範囲内にとどまるとした。立法者は「現代的で将来にも"
           "対応でき自由化された法サービス法」を作ろうとしたのだから Inkasso 概念を"
           "狭く解してはならない、と判示。ドイツの legal tech の大半はこの登録経路で"
           "運営されている。")
      :rule/topic #{:unauthorized-practice :legaltech}}

     {:rule/id "deu.rdg"
      :rule/title "RDG § 2 / § 3 / § 10 — Rechtsdienstleistung の定義・原則禁止・登録による例外"
      :rule/instrument "Rechtsdienstleistungsgesetz (RDG)"
      :rule/quote
      (str "§2(1) Rechtsdienstleistung ist jede Tätigkeit in konkreten fremden "
           "Angelegenheiten, sobald sie eine rechtliche Prüfung des Einzelfalls erfordert. "
           "§3 Die selbständige Erbringung außergerichtlicher Rechtsdienstleistungen ist "
           "unzulässig, soweit sie nicht erlaubt wird … "
           "§10(1) Natürliche und juristische Personen … die beim Bundesamt für Justiz "
           "registriert sind …, dürfen aufgrund besonderer Sachkunde Rechtsdienstleistungen "
           "in folgenden Bereichen erbringen: 1. Inkassodienstleistungen …")
      :rule/url "https://www.gesetze-im-internet.de/rdg/__2.html"
      :rule/url-provenance :official-legislation-site
      :rule/verification :primary-source-read
      :rule/verification-note
      "gesetze-im-internet.de の RDG XML（xml.zip）を取得し、§2・§3・§10 を読了。"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "境界語は「**個別事案の法的検討を要するか**」で、"
           "法務省ガイドラインが判断要素として挙げる線とほぼ同じものを"
           "**定義そのもの**として条文に置いている。§3 が原則禁止、§10 が"
           "能力分野別の登録で門を開ける構造で、**法人も登録できる**のが日本との違い。")
      :rule/topic #{:unauthorized-practice}}]

    :jurisdiction/service-modes
    {:mode/template-selection {:verdict :unsettled :basis []
                               :condition "RDG §2 の Rechtsdienstleistung 定義を原典で未検証。"}
     :mode/document-assembly {:verdict :unsettled :basis []
                              :condition "同上。"}
     :mode/legal-analysis {:verdict :conditional :basis ["deu.rdg" "deu.bgh-viii-zr-285-18"]
                           :condition "RDG の登録（Inkassodienstleistung 等）を得ているか、弁護士が主体であること。"}
     :mode/lawyer-reviewed {:verdict :conditional :basis ["deu.rdg"]
                            :condition "Rechtsanwalt が主体として法サービスを行い、本製品は道具にとどまること。"}
     :mode/inhouse-reviewed {:verdict :unsettled :basis [] :condition "Syndikusrechtsanwalt 規律を未検証。"}
     :mode/lawyer-directory {:verdict :conditional :basis ["deu.brao-49b-3"]
                             :condition "個別 Mandat の仲介対価と評価されない構造であること。"}
     :mode/referral-placement {:verdict :conditional :basis ["deu.brao-49b-3"]
                               :condition "無償の取次ぎであること。対価が伴えば弁護士側が §49b Abs.3 違反となる。"}}

    :jurisdiction/revenue-modes
    {:revenue/none {:verdict :conditional :basis ["deu.rdg"] :condition "無償でも RDG の適用対象になりうる。"}
     :revenue/law-firm-saas-license
     {:verdict :conditional :basis ["deu.brao-49b-3"]
      :condition
      (str "ソフトウェア対価が個別に仲介された Mandat の対価でないこと。"
           "§49b Abs.3 の禁止は具体的に仲介された Mandat への Provision を捕捉し、"
           "仲介と利益供与の間に因果関係が必要とされる。")}
     :revenue/consumer-document-fee {:verdict :unsettled :basis [] :condition "RDG 原典未検証。"}
     :revenue/consumer-subscription {:verdict :unsettled :basis [] :condition "RDG 原典未検証。"}
     :revenue/directory-listing-flat
     {:verdict :conditional :basis ["deu.brao-49b-3"]
      :condition "個別案件と因果関係のない定額掲載料であること。案件連動なら禁止側に倒れる。"}
     :revenue/per-referral-fee {:verdict :prohibited :basis ["deu.brao-49b-3"] :condition nil}
     :revenue/success-fee-share {:verdict :prohibited :basis ["deu.brao-49b-3" "deu.brao-49b-2"] :condition nil}}

    :jurisdiction/known-gaps
    ["RDG §2/§3/§10 の条文原文が未取得"
     "2021年 Legal-Tech 法（verbrauchergerechte Angebote im Rechtsdienstleistungsmarkt）が未収載"
     "BGH VIII ZR 285/18 の判決原文が未取得"]}

   ;; -----------------------------------------------------------------------
   "FRA"
   {:jurisdiction/id "FRA"
    :jurisdiction/name "France"
    :jurisdiction/iso3166 "FRA"
    :jurisdiction/legal-system :civil-law
    :jurisdiction/rules
    [{:rule/id "fra.loi-71-1130-art54"
      :rule/title "Loi n° 71-1130 du 31 décembre 1971, art. 54 — consultation juridique et rédaction d'actes"
      :rule/instrument "Loi n° 71-1130 du 31 décembre 1971"
      :rule/quote
      (str "Nul ne peut … donner des consultations juridiques ou rédiger des actes "
           "sous seing privé, pour autrui")
      :rule/url "https://www.legifrance.gouv.fr/loda/article_lc/LEGIARTI000039280601"
      :rule/url-provenance :official-legislation-site
      :rule/verification :primary-source-read
      :rule/verification-note "Légifrance の article 54 本文を取得して読了。"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "法的助言の提供および私署証書の作成を、反復的かつ有償で他人のために"
           "行うには、法学士号または適切な法的能力を有し、かつ非行による有罪判決・"
           "懲戒処分・個人破産がなく、56〜66条の各職域の枠組みに従う必要がある。"
           "avocat 等の規制職はこの能力を有するものとみなされる。"
           "企業内法務は、雇用会社およびそのグループ会社のためにのみ行える。")
      :rule/topic #{:unauthorized-practice :document-assistance}}]

    :jurisdiction/service-modes
    {:mode/template-selection {:verdict :unsettled :basis []
                               :condition "ひな形の単純表示が「rédaction d'actes」に当たるかを未検証。"}
     :mode/document-assembly {:verdict :conditional :basis ["fra.loi-71-1130-art54"]
                              :condition "私署証書の作成に当たる場合、運営者が art.54 の資格要件を満たすこと。"}
     :mode/legal-analysis {:verdict :conditional :basis ["fra.loi-71-1130-art54"]
                           :condition "consultation juridique に当たるため、art.54 の資格・適格要件を満たすこと。"}
     :mode/lawyer-reviewed {:verdict :conditional :basis ["fra.loi-71-1130-art54"]
                            :condition "avocat が主体として助言・作成を行い、本製品は道具にとどまること。"}
     :mode/inhouse-reviewed {:verdict :conditional :basis ["fra.loi-71-1130-art54"]
                             :condition "雇用会社およびそのグループ会社のための利用に限ること（art.54 の企業内法務の枠）。"}
     :mode/lawyer-directory {:verdict :unsettled :basis [] :condition "RIN・démarchage 規律を未検証。"}
     :mode/referral-placement {:verdict :unsettled :basis [] :condition "同上。"}}

    :jurisdiction/revenue-modes
    {:revenue/none {:verdict :conditional :basis ["fra.loi-71-1130-art54"]
                    :condition "art.54 は「à titre habituel et rémunéré」を要件とするため無償なら射程外になりうるが未検証。"}
     :revenue/law-firm-saas-license {:verdict :conditional :basis ["fra.loi-71-1130-art54"]
                                     :condition "avocat が主体であること。"}
     :revenue/consumer-document-fee {:verdict :conditional :basis ["fra.loi-71-1130-art54"] :condition "art.54 の資格要件充足が前提。"}
     :revenue/consumer-subscription {:verdict :conditional :basis ["fra.loi-71-1130-art54"] :condition "同上。"}
     :revenue/directory-listing-flat {:verdict :unsettled :basis [] :condition "RIN 未検証。"}
     :revenue/per-referral-fee {:verdict :unsettled :basis [] :condition "RIN の commission 規律を未検証。"}
     :revenue/success-fee-share {:verdict :unsettled :basis [] :condition "pacte de quota litis 規律を未検証。"}}

    :jurisdiction/known-gaps
    ["Règlement Intérieur National (RIN) が未取得"
     "art.55-66 の各職域規定が未取得"
     "pacte de quota litis（成功報酬のみの合意）の禁止規定が未検証"]}

   ;; -----------------------------------------------------------------------
   "USA"
   {:jurisdiction/id "USA"
    :jurisdiction/name "United States (federal / general)"
    :jurisdiction/iso3166 "USA"
    :jurisdiction/legal-system :common-law
    :jurisdiction/note
    (str "米国では法律業務の規律は州ごとに行われ、連邦レベルの統一規則は存在しない。"
         "**ABA Model Rules はどこの法でもない** —— 各州が採用して初めて拘束力を持ち、"
         "採用州は内容を大きく改変する。収録している3本の Model Rule には"
         "`:rule/binding-force :model-only` を付けてあり、`verified-rule?` が偽を"
         "返すので**いかなる許可方向の結論も支えられない**。"
         "したがってこの \"USA\" エントリはすべて :unsettled であり、"
         "USA-DC / USA-AZ / USA-UT の州エントリを見ること。"
         "州エントリが無い州は coverage gap であって「規制が無い州」ではない。")
    :jurisdiction/rules
    [{:rule/id "usa.aba-model-rule-5-4"
      :rule/binding-force :model-only
      :rule/title "ABA Model Rule 5.4 — Professional Independence of a Lawyer"
      :rule/instrument "ABA Model Rules of Professional Conduct"
      :rule/url "https://www.lawnext.com/2024/12/professional-responsibility-lawyers-call-on-aba-to-modernize-model-rule-5-4-to-allow-fee-sharing-with-non-lawyers.html"
      :rule/url-provenance :secondary-commentary
      :rule/verification :secondary-source-only
      :rule/verification-note "ABA 公式ページは HTTP 403 で取得できず。解説記事の要約のみ。"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "弁護士・法律事務所は非弁護士と法律報酬を分配してはならない（例外列挙あり）。"
           "**ただしこれはモデルであってどこの法でもない。** 採用州は大きく改変して"
           "おり、D.C. は 5.4(b) で非弁護士の資本参加を認め、アリゾナは 5.4 自体を"
           "撤廃した。米国の判断は必ず**その州の採用版**で行うこと。")
      :rule/topic #{:fee-sharing}}

     {:rule/id "usa.aba-model-rule-5-5"
      :rule/binding-force :model-only
      :rule/title "ABA Model Rule 5.5 — Unauthorized Practice of Law"
      :rule/instrument "ABA Model Rules of Professional Conduct"
      :rule/url "https://www.law.uh.edu/faculty/adjunct/dstevenson/2019/11a.pdf"
      :rule/url-provenance :secondary-commentary
      :rule/verification :secondary-source-only
      :rule/verification-note "ABA 公式ページは HTTP 403。条文原文は未取得。"
      :rule/retrieved-at "2026-07-26"
      :rule/summary "各法域の規律に反して法律業務を行うこと、および無資格者の法律業務を援助することを禁じる。"
      :rule/topic #{:unauthorized-practice}}

     {:rule/id "usa.aba-model-rule-7-2b"
      :rule/binding-force :model-only
      :rule/title "ABA Model Rule 7.2(b) — payment for recommending a lawyer's services"
      :rule/instrument "ABA Model Rules of Professional Conduct"
      :rule/url "https://www.isba.org/ibj/2016/04/avvoandtheethicsofleadgeneration"
      :rule/url-provenance :secondary-commentary
      :rule/verification :secondary-source-only
      :rule/verification-note "条文原文は未取得。州倫理委員会の解説のみ。"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "弁護士は自己のサービスの推薦に対して「anything of value」を与えては"
           "ならないが、広告の合理的費用の支払いは例外。lead generator が"
           "「弁護士を推薦している」「支払いを受けずに紹介している」"
           "「法的問題を分析してどの弁護士に回すか決めている」という印象を"
           "与える場合、その支払いは許されない — 事業者が案件を特定の事務所へ"
           "割り当てる裁量を持つかどうかが線引きになる。")
      :rule/topic #{:referral :advertising}}]

    :jurisdiction/service-modes
    (into {} (for [m (keys service-modes)]
               [m {:verdict :unsettled :basis []
                   :condition "州単位の分析が必要。USA-DC / USA-AZ / USA-UT を参照。"}]))
    :jurisdiction/revenue-modes
    (into {} (for [m (keys revenue-modes)]
               [m {:verdict :unsettled :basis []
                   :condition "州単位の分析が必要。USA-DC / USA-AZ / USA-UT を参照。"}]))
    :jurisdiction/known-gaps
    ["50州+準州のうち3法域しか収載していない"
     "ABA Model Rules の条文原文は取得経路が無い（ABA 公式は 403、Cornell LII は現在ホストしていない）。ただしモデルは法ではないので、原文が取れても verdict の根拠にはならない"
     "アリゾナ ACJA §7-209 / ER 5.4 撤廃命令は azcourts.gov が curl・WebFetch とも 403 で未取得"
     "ユタの Supreme Court standing order は utcourts.gov が到達不能で未取得"
     "州ごとの UPL 刑事罰規定が未収載"]}

   "USA-DC"
   {:jurisdiction/id "USA-DC"
    :jurisdiction/name "United States — District of Columbia"
    :jurisdiction/iso3166 "USA"
    :jurisdiction/legal-system :common-law
    :jurisdiction/rules
    [{:rule/id "usa-dc.rule-5-4"
      :rule/title "D.C. Rule of Professional Conduct 5.4 — Professional Independence of a Lawyer"
      :rule/instrument "District of Columbia Rules of Professional Conduct"
      :rule/binding-force :law
      :rule/quote
      (str "(a) A lawyer or law firm shall not share legal fees with a nonlawyer, except "
           "that: … (5) A lawyer may share legal fees, whether awarded by a tribunal or "
           "received in settlement of a matter, with a nonprofit organization that "
           "employed, retained, or recommended employment of the lawyer in the matter and "
           "that qualifies under Section 501(c)(3) of the Internal Revenue Code. "
           "(b) A lawyer may practice law in a partnership or other form of organization "
           "in which a financial interest is held or managerial authority is exercised by "
           "an individual nonlawyer who performs professional services which assist the "
           "organization in providing legal services to clients, but only if: (1) The "
           "partnership or organization has as its sole purpose providing legal services "
           "to clients; (2) All persons having such managerial authority or holding a "
           "financial interest undertake to abide by these Rules of Professional Conduct; "
           "(3) The lawyers … undertake to be responsible for the nonlawyer participants "
           "to the same extent as if nonlawyer participants were lawyers under Rule 5.1; "
           "(4) The foregoing conditions are set forth in writing. "
           "(c) A lawyer shall not permit a person who recommends, employs, or pays the "
           "lawyer to render legal services for another to direct or regulate the "
           "lawyer's professional judgment in rendering such legal services.")
      :rule/url "https://www.dcbar.org/for-lawyers/legal-ethics/rules-of-professional-conduct/law-firms-and-associations/professional-independence-of-a-lawyer"
      :rule/url-provenance :official-bar-site
      :rule/verification :primary-source-read
      :rule/verification-note
      (str "D.C. Bar の規則ページを curl で取得し、(a)(b)(c) の全文を抽出して読了。"
           "**ABA Model Rule ではなく D.C. が実際に採用している規則**であり、"
           "米国でこのカタログが一次で持てている唯一の州法。")
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "非弁護士との法律報酬の分配を禁じる（死亡弁護士の遺産への支払い、"
           "従業員の利益分配制度、5.4(b) の要件を満たすパートナーシップ、"
           "501(c)(3) 非営利団体との分配は例外）。DC は Model Rule と異なり"
           "非営利団体との分配を裁判所認定報酬に限定していない。"
           "5.4(b) により、唯一の目的が法サービス提供であること等の要件下で"
           "非弁護士の資本参加が認められる点でも Model Rule と異なる。")
      :rule/topic #{:fee-sharing}}]

    :jurisdiction/service-modes
    (into {} (for [m (keys service-modes)]
               [m {:verdict :unsettled :basis []
                   :condition "DC の UPL 規定（Rule 49）が未取得。"}]))
    :jurisdiction/revenue-modes
    {:revenue/none {:verdict :unsettled :basis [] :condition "DC Rule 49 未取得。"}
     :revenue/law-firm-saas-license {:verdict :unsettled :basis [] :condition "DC Rule 49 未取得。"}
     :revenue/consumer-document-fee {:verdict :unsettled :basis [] :condition "DC Rule 49 未取得。"}
     :revenue/consumer-subscription {:verdict :unsettled :basis [] :condition "DC Rule 49 未取得。"}
     :revenue/directory-listing-flat {:verdict :unsettled :basis [] :condition "Rule 7.1 系が未取得。"}
     :revenue/per-referral-fee {:verdict :prohibited :basis ["usa-dc.rule-5-4"]
                                :condition "法律報酬の分配に当たる形態は禁止。広告の合理的費用としての構成は別途検討を要する。"}
     :revenue/success-fee-share {:verdict :prohibited :basis ["usa-dc.rule-5-4"] :condition nil}}
    :jurisdiction/known-gaps
    ["D.C. Court of Appeals Rule 49（UPL）が未取得"
     "D.C. Rule 7.1 系（広告）が未取得"]}

   "USA-AZ"
   {:jurisdiction/id "USA-AZ"
    :jurisdiction/name "United States — Arizona"
    :jurisdiction/iso3166 "USA"
    :jurisdiction/legal-system :common-law
    :jurisdiction/rules
    [{:rule/id "usa-az.abs-acja-7-209"
      :rule/title "Arizona Alternative Business Structure (ACJA § 7-209); ER 5.4 repealed"
      :rule/instrument "Arizona Code of Judicial Administration"
      :rule/url "https://www.azcourts.gov/accesstolegalservices/Questions-and-Answers/abs"
      :rule/url-provenance :official-court-site
      :rule/verification :secondary-source-only
      :rule/verification-note "検索要約のみ。ACJA §7-209 の原文は未取得。"
      :rule/established-date "2021-01-01"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "アリゾナ州最高裁は倫理規則 5.4 を撤廃し、2021年1月1日から ABS 制度を"
           "開始した。非弁護士のオーナー・意思決定者を持つ事業体が法サービスを"
           "提供するには州最高裁の ABS ライセンスが必要。米国で初めて"
           "非弁護士による法律事務所所有を認めた州。")
      :rule/topic #{:fee-sharing :entity-regulation}}]
    :jurisdiction/service-modes
    (into {} (for [m (keys service-modes)]
               [m {:verdict :conditional :basis ["usa-az.abs-acja-7-209"]
                   :condition "ABS ライセンスを取得していること。原文未検証のため要一次確認。"}]))
    :jurisdiction/revenue-modes
    (into {} (for [m (keys revenue-modes)]
               [m {:verdict :conditional :basis ["usa-az.abs-acja-7-209"]
                   :condition "ABS ライセンス下であること。原文未検証のため要一次確認。"}]))
    :jurisdiction/known-gaps ["ACJA §7-209 の原文が未取得" "アリゾナ ER 5.5（UPL）が未取得"]}

   "USA-UT"
   {:jurisdiction/id "USA-UT"
    :jurisdiction/name "United States — Utah"
    :jurisdiction/iso3166 "USA"
    :jurisdiction/legal-system :common-law
    :jurisdiction/rules
    [{:rule/id "usa-ut.legal-services-sandbox"
      :rule/title "Utah Office of Legal Services Innovation — regulatory sandbox (Phase 2)"
      :rule/instrument "Utah Supreme Court standing order / Office of Legal Services Innovation"
      :rule/url "https://utahinnovationoffice.org/info-for-interested-applicants/"
      :rule/url-provenance :official-regulator-site
      :rule/verification :secondary-source-only
      :rule/verification-note "検索要約のみ。standing order の原文は未取得。"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "サンドボックス下で、非弁護士やソフトウェア（AI を含む）による限定的な"
           "法律業務、および非弁護士との報酬分配・非弁護士による所有が認められる。"
           "Phase 2 は 2027年8月まで認可されており、Moderate/High Innovation の"
           "事業体に絞る方向で運用が見直されている。認可要件として、現在"
           "サービスが行き届いていないユタ州民に届くことの立証が求められる。")
      :rule/topic #{:unauthorized-practice :fee-sharing :sandbox}}]
    :jurisdiction/service-modes
    (into {} (for [m (keys service-modes)]
               [m {:verdict :conditional :basis ["usa-ut.legal-services-sandbox"]
                   :condition "サンドボックス認可を得ていること。認可は期限付きで縮小方向の見直し中。"}]))
    :jurisdiction/revenue-modes
    (into {} (for [m (keys revenue-modes)]
               [m {:verdict :conditional :basis ["usa-ut.legal-services-sandbox"]
                   :condition "サンドボックス認可を得ていること。"}]))
    :jurisdiction/known-gaps ["standing order 原文が未取得" "Phase 2 の縮小内容の詳細が未取得"]}

   ;; -----------------------------------------------------------------------
   "KOR"
   {:jurisdiction/id "KOR"
    :jurisdiction/name "Republic of Korea"
    :jurisdiction/iso3166 "KOR"
    :jurisdiction/legal-system :civil-law
    :jurisdiction/rules
    [{:rule/id "kor.constitutional-court-2022-lawtalk-ads"
      :rule/title "헌법재판소 2022 — 대한변협 광고규정의 플랫폼 광고 금지 조항 위헌 결정"
      :rule/instrument "대한민국 헌법재판소"
      :rule/url "https://journal.kiso.or.kr/?p=12455"
      :rule/url-provenance :secondary-commentary
      :rule/verification :secondary-source-only
      :rule/verification-note "決定原文（事件番号含む）は未取得。解説記事の要約のみ。"
      :rule/established-date "2022"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "변호사법は弁護士広告を原則として認めているのだから、法律ではなく"
           "弁護士会の広告規程によって로톡（LawTalk）等のプラットフォーム上での"
           "広告・宣伝活動を禁止するのは違憲である、と判断された。")
      :rule/topic #{:advertising :platform}}

     {:rule/id "kor.moj-2023-lawtalk-discipline-cancelled"
      :rule/title "법무부 2023-09-26 — 로톡 가입 변호사 징계 취소 / 공정위 과징금"
      :rule/instrument "대한민국 법무부 / 공정거래위원회"
      :rule/url "https://www.lawtimes.co.kr/news/202332"
      :rule/url-provenance :secondary-commentary
      :rule/verification :secondary-source-only
      :rule/verification-note "決定書原文は未取得。法曹メディア報道のみ。"
      :rule/established-date "2023-09-26"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "法務部は弁護士会がLawTalk利用弁護士123名に科した懲戒を取り消し、"
           "公正取引委員会は弁護士会に対し競争制限を理由に課徴金を科した。")
      :rule/topic #{:advertising :platform :competition}}]

    :jurisdiction/service-modes
    {:mode/template-selection {:verdict :unsettled :basis [] :condition "변호사법 §109 未取得。"}
     :mode/document-assembly {:verdict :unsettled :basis [] :condition "변호사법 §109 未取得。"}
     :mode/legal-analysis {:verdict :unsettled :basis [] :condition "변호사법 §109（무자격 법률사무 취급）未取得。"}
     :mode/lawyer-reviewed {:verdict :unsettled :basis [] :condition "同上。"}
     :mode/inhouse-reviewed {:verdict :unsettled :basis [] :condition "同上。"}
     :mode/lawyer-directory {:verdict :conditional
                             :basis ["kor.constitutional-court-2022-lawtalk-ads"
                                     "kor.moj-2023-lawtalk-discipline-cancelled"]
                             :condition
                             (str "プラットフォーム上での弁護士広告掲載を弁護士会規程で"
                                  "禁じることは違憲とされたが、変호사법本体の規律は"
                                  "原典未検証。掲載型に限る。")}
     :mode/referral-placement {:verdict :unsettled :basis []
                               :condition "변호사법 §34（사건 유치 목적 금품 수수 금지）が未取得。"}}
    :jurisdiction/revenue-modes
    {:revenue/none {:verdict :unsettled :basis [] :condition "변호사법 原典未取得。"}
     :revenue/law-firm-saas-license {:verdict :unsettled :basis [] :condition "同上。"}
     :revenue/consumer-document-fee {:verdict :unsettled :basis [] :condition "同上。"}
     :revenue/consumer-subscription {:verdict :unsettled :basis [] :condition "同上。"}
     :revenue/directory-listing-flat {:verdict :conditional
                                      :basis ["kor.constitutional-court-2022-lawtalk-ads"]
                                      :condition "広告掲載の対価であり、個別案件の斡旋対価でないこと。"}
     :revenue/per-referral-fee {:verdict :unsettled :basis [] :condition "변호사법 §34 未取得。慎重側に倒すこと。"}
     :revenue/success-fee-share {:verdict :unsettled :basis [] :condition "同上。"}}
    :jurisdiction/known-gaps
    ["변호사법 §34 / §109 の条文原文が未取得"
     "헌법재판소 決定の事件番号・原文が未取得"]}

   ;; -----------------------------------------------------------------------
   "AUS"
   {:jurisdiction/id "AUS"
    :jurisdiction/name "Australia (Legal Profession Uniform Law jurisdictions)"
    :jurisdiction/iso3166 "AUS"
    :jurisdiction/legal-system :common-law
    :jurisdiction/note "Uniform Law は NSW・VIC・WA 等が採用。全州で同一ではない。"
    :jurisdiction/rules
    [{:rule/id "aus.lpul-s10"
      :rule/title "Legal Profession Uniform Law s.10 — prohibition on engaging in legal practice by unqualified entities"
      :rule/instrument "Legal Profession Uniform Law (NSW) No 16a of 2014"
      :rule/url "https://classic.austlii.edu.au/au/legis/nsw/consol_act/lpul333/s10.html"
      :rule/url-provenance :official-legislation-mirror
      :rule/verification :secondary-source-only
      :rule/verification-note "AustLII は HTTP 403 で取得できず。検索結果の要約のみ。"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "qualified entity でない事業体が当該法域で法律業務を行うことを禁じ、"
           "250 penalty units または2年以下の拘禁、もしくはその併科。"
           "違反して行った業務についての報酬は回収できず、既に受領した額は"
           "返還しなければならない（支払った者が債務として回収できる）。"
           "Uniform Rules による適用除外の余地がある。")
      :rule/topic #{:unauthorized-practice}}]
    :jurisdiction/service-modes
    {:mode/template-selection {:verdict :unsettled :basis [] :condition "「legal practice」該当性の線引きが未検証。"}
     :mode/document-assembly {:verdict :unsettled :basis [] :condition "同上。"}
     :mode/legal-analysis {:verdict :prohibited :basis ["aus.lpul-s10"]
                           :condition "qualified entity でない限り不可。刑事罰と報酬返還義務を伴う。"}
     :mode/lawyer-reviewed {:verdict :conditional :basis ["aus.lpul-s10"]
                            :condition "qualified entity である法律事務所が主体であること。"}
     :mode/inhouse-reviewed {:verdict :unsettled :basis [] :condition "corporate legal practitioner 規律を未検証。"}
     :mode/lawyer-directory {:verdict :unsettled :basis [] :condition "Uniform Law の広告規律を未検証。"}
     :mode/referral-placement {:verdict :unsettled :basis [] :condition "同上。"}}
    :jurisdiction/revenue-modes
    (into {} (for [m (keys revenue-modes)]
               [m {:verdict :unsettled :basis []
                   :condition "s.10 原文および Uniform Rules の適用除外が未取得。"}]))
    :jurisdiction/known-gaps
    ["LPUL s.10 の条文原文が未取得（AustLII が 403）"
     "Legal Profession Uniform General Rules の適用除外が未取得"
     "Uniform Law 未採用州（QLD・SA・TAS 等）が未収載"]}

   ;; -----------------------------------------------------------------------
   "CAN-ON"
   {:jurisdiction/id "CAN-ON"
    :jurisdiction/name "Canada — Ontario"
    :jurisdiction/iso3166 "CAN"
    :jurisdiction/legal-system :common-law
    :jurisdiction/rules
    [{:rule/id "can-on.law-society-act-s26-1"
      :rule/title "Law Society Act (Ontario) s.26.1 — prohibition on unlicensed practice and provision of legal services"
      :rule/instrument "Law Society Act, R.S.O. 1990, c. L.8"
      :rule/quote
      (str "26.1 (1) Subject to subsection (5), no person, other than a licensee whose "
           "licence is not suspended, shall practise law in Ontario or provide legal "
           "services in Ontario. … 26.2 (1) Every person who contravenes section 26.1 is "
           "guilty of an offence and on conviction is liable to a fine of, (a) not more "
           "than $25,000 for a first offence; and (b) not more than $50,000 for each "
           "subsequent offence.")
      :rule/url "https://www.ontario.ca/laws/statute/90l08"
      :rule/url-provenance :official-legislation-site
      :rule/verification :primary-source-read
      :rule/verification-note
      (str "Ontario e-Laws の HTML を取得し、s.26.1(1) と s.26.2(1) を抽出して読了。"
           "e-Laws は JS レンダリングのため WebFetch では本文が取れず、"
           "生 HTML から `<p class=\"section\">` 単位で抽出した。")
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "免許停止中でない licensee 以外の者はオンタリオ州で法律実務を行うことも"
           "法サービスを提供することもできない。違反は初犯 25,000 カナダドル以下、"
           "再犯は各回 50,000 カナダドル以下の罰金。オンタリオは"
           "「practice of law」（弁護士限定）と「provision of legal services」"
           "（paralegal に選択的に開放）を区別し、カナダで唯一 paralegal を"
           "免許制で規律している。")
      :rule/topic #{:unauthorized-practice :paralegal}}]
    :jurisdiction/service-modes
    {:mode/template-selection {:verdict :unsettled :basis [] :condition "「legal services」定義の射程が未検証。"}
     :mode/document-assembly {:verdict :unsettled :basis [] :condition "同上。"}
     :mode/legal-analysis {:verdict :prohibited :basis ["can-on.law-society-act-s26-1"]
                           :condition "licensee でない限り不可。paralegal 免許取得は別経路。"}
     :mode/lawyer-reviewed {:verdict :conditional :basis ["can-on.law-society-act-s26-1"]
                            :condition "licensee が主体であること。"}
     :mode/inhouse-reviewed {:verdict :unsettled :basis [] :condition "in-house 例外を未検証。"}
     :mode/lawyer-directory {:verdict :unsettled :basis [] :condition "Rules of Professional Conduct を未検証。"}
     :mode/referral-placement {:verdict :unsettled :basis [] :condition "referral fee 規律（Rule 3.6-6 系）を未検証。"}}
    :jurisdiction/revenue-modes
    (into {} (for [m (keys revenue-modes)]
               [m {:verdict :unsettled :basis [] :condition "Law Society of Ontario の規則が未取得。"}]))
    :jurisdiction/known-gaps
    ["Law Society Act s.26.1 の条文原文が未取得"
     "LSO Rules of Professional Conduct の referral fee 規定が未取得"
     "オンタリオ以外の州・準州が未収載"]}

   ;; -----------------------------------------------------------------------
   "IND"
   {:jurisdiction/id "IND"
    :jurisdiction/name "India"
    :jurisdiction/iso3166 "IND"
    :jurisdiction/legal-system :common-law
    :jurisdiction/rules
    [{:rule/id "ind.bci-rule-36"
      :rule/title "Bar Council of India Rules, Rule 36 — prohibition on advertising and solicitation"
      :rule/instrument "Bar Council of India Rules (under the Advocates Act, 1961)"
      :rule/url "https://www.barandbench.com/columns/lawyers-not-vendors-why-rule-36-still-matters-in-a-digital-india"
      :rule/url-provenance :secondary-commentary
      :rule/verification :secondary-source-only
      :rule/verification-note "規則原文は未取得。法曹メディアの解説のみ。"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "advocate は直接・間接を問わず自己のサービスを宣伝してはならず、"
           "回状・touts・公開インタビュー・勝訴の報道等による勧誘も禁止される。"
           "2025年3月17日に BCI がインフルエンサー・著名人を通じた宣伝活動への"
           "警告を発し、2025年8月4日にデリー弁護士会がソーシャルメディア上の"
           "広告について免許停止・取消の可能性を警告した。"
           "最高裁は Rule 36 に関する PIL について BCI に通知を発している。")
      :rule/topic #{:advertising :referral}}]
    :jurisdiction/service-modes
    {:mode/template-selection {:verdict :unsettled :basis [] :condition "Advocates Act §33/§29 が未取得。"}
     :mode/document-assembly {:verdict :unsettled :basis [] :condition "同上。"}
     :mode/legal-analysis {:verdict :unsettled :basis [] :condition "同上。"}
     :mode/lawyer-reviewed {:verdict :unsettled :basis [] :condition "同上。"}
     :mode/inhouse-reviewed {:verdict :unsettled :basis [] :condition "同上。"}
     :mode/lawyer-directory {:verdict :prohibited :basis ["ind.bci-rule-36"]
                             :condition "advocate の掲載が間接的な広告・勧誘と評価される。規制当局が現に執行を強化している。"}
     :mode/referral-placement {:verdict :prohibited :basis ["ind.bci-rule-36"]
                               :condition "touts を通じた勧誘の禁止に直接抵触する。"}}
    :jurisdiction/revenue-modes
    {:revenue/none {:verdict :unsettled :basis [] :condition "Advocates Act 原文未取得。"}
     :revenue/law-firm-saas-license {:verdict :unsettled :basis [] :condition "同上。"}
     :revenue/consumer-document-fee {:verdict :unsettled :basis [] :condition "同上。"}
     :revenue/consumer-subscription {:verdict :unsettled :basis [] :condition "同上。"}
     :revenue/directory-listing-flat {:verdict :prohibited :basis ["ind.bci-rule-36"] :condition nil}
     :revenue/per-referral-fee {:verdict :prohibited :basis ["ind.bci-rule-36"] :condition nil}
     :revenue/success-fee-share {:verdict :prohibited :basis ["ind.bci-rule-36"] :condition nil}}
    :jurisdiction/known-gaps
    ["Advocates Act, 1961 §29/§33/§35 の条文原文が未取得"
     "BCI Rules 原文が未取得"
     "係属中の最高裁 PIL の結論が未確定"]}

   ;; -----------------------------------------------------------------------
   "SGP"
   {:jurisdiction/id "SGP"
    :jurisdiction/name "Singapore"
    :jurisdiction/iso3166 "SGP"
    :jurisdiction/legal-system :common-law
    :jurisdiction/rules
    [{:rule/id "sgp.lpa-s33"
      :rule/title "Legal Profession Act 1966 s.33 — unauthorised persons acting as advocates or solicitors"
      :rule/instrument "Legal Profession Act 1966 (Singapore)"
      :rule/url "https://sso.agc.gov.sg/Act/LPA1966"
      :rule/url-provenance :official-legislation-site
      :rule/verification :secondary-source-only
      :rule/verification-note "条文本文・罰則の具体的内容は未取得（検索結果に条文が含まれず）。"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "実務弁護士を装って法サービスを提供する行為に対する刑事制裁を定める。"
           "具体的な罰則の内容は未確認。")
      :rule/topic #{:unauthorized-practice}}]
    :jurisdiction/service-modes
    {:mode/template-selection {:verdict :unsettled :basis [] :condition "s.33 の条文本文が未取得。"}
     :mode/document-assembly {:verdict :unsettled :basis [] :condition "同上。"}
     :mode/legal-analysis {:verdict :prohibited :basis ["sgp.lpa-s33"]
                           :condition "無資格者による法サービス提供に刑事制裁がある。範囲は要一次確認。"}
     :mode/lawyer-reviewed {:verdict :conditional :basis ["sgp.lpa-s33"] :condition "有資格 solicitor が主体であること。"}
     :mode/inhouse-reviewed {:verdict :unsettled :basis [] :condition "未検証。"}
     :mode/lawyer-directory {:verdict :unsettled :basis [] :condition "Legal Profession (Publicity) Rules が未取得。"}
     :mode/referral-placement {:verdict :unsettled :basis [] :condition "同上。"}}
    :jurisdiction/revenue-modes
    (into {} (for [m (keys revenue-modes)]
               [m {:verdict :unsettled :basis [] :condition "LPA 原文が未取得。"}]))
    :jurisdiction/known-gaps
    ["Legal Profession Act s.33 の条文本文が未取得"
     "Legal Profession (Professional Conduct) Rules が未取得"]}

   ;; -----------------------------------------------------------------------
   "BRA"
   {:jurisdiction/id "BRA"
    :jurisdiction/name "Brazil"
    :jurisdiction/iso3166 "BRA"
    :jurisdiction/legal-system :civil-law
    :jurisdiction/rules
    [{:rule/id "bra.oab-provimento-205-2021"
      :rule/title "OAB Provimento n. 205/2021 — publicidade e informação na advocacia"
      :rule/instrument "Conselho Federal da Ordem dos Advogados do Brasil"
      :rule/url "https://www.oab.org.br/leisnormas/legislacao/provimentos/205-2021"
      :rule/url-provenance :official-bar-site
      :rule/verification :secondary-source-only
      :rule/verification-note "Provimento 原文は未取得。解説の要約のみ。"
      :rule/established-date "2021-08-22"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "弁護士広告は「情報提供的性格」にとどまるべきで、顧客の獲得"
           "（captação de clientela）や職業の商業化（mercantilização）を"
           "構成してはならない。報酬額・支払方法の公開も禁じられる。"
           "Estatuto da Advocacia（Lei 8.906/1994）art.34, IV は案件の勧誘・獲得"
           "および不適切な広告を懲戒事由とする。")
      :rule/topic #{:advertising :referral}}]
    :jurisdiction/service-modes
    {:mode/template-selection {:verdict :unsettled :basis [] :condition "Lei 8.906/1994 art.1（弁護士独占）が未取得。"}
     :mode/document-assembly {:verdict :unsettled :basis [] :condition "同上。"}
     :mode/legal-analysis {:verdict :unsettled :basis [] :condition "同上。"}
     :mode/lawyer-reviewed {:verdict :unsettled :basis [] :condition "同上。"}
     :mode/inhouse-reviewed {:verdict :unsettled :basis [] :condition "同上。"}
     :mode/lawyer-directory {:verdict :conditional :basis ["bra.oab-provimento-205-2021"]
                             :condition "情報提供的性格にとどまり、報酬額を表示せず、顧客獲得と評価されないこと。"}
     :mode/referral-placement {:verdict :prohibited :basis ["bra.oab-provimento-205-2021"]
                               :condition "captação de clientela に当たる。"}}
    :jurisdiction/revenue-modes
    {:revenue/none {:verdict :unsettled :basis [] :condition "Estatuto 原文未取得。"}
     :revenue/law-firm-saas-license {:verdict :unsettled :basis [] :condition "同上。"}
     :revenue/consumer-document-fee {:verdict :unsettled :basis [] :condition "同上。"}
     :revenue/consumer-subscription {:verdict :unsettled :basis [] :condition "同上。"}
     :revenue/directory-listing-flat {:verdict :conditional :basis ["bra.oab-provimento-205-2021"]
                                      :condition "mercantilização と評価されない構造であること。"}
     :revenue/per-referral-fee {:verdict :prohibited :basis ["bra.oab-provimento-205-2021"] :condition nil}
     :revenue/success-fee-share {:verdict :prohibited :basis ["bra.oab-provimento-205-2021"] :condition nil}}
    :jurisdiction/known-gaps
    ["Lei 8.906/1994（Estatuto da Advocacia）の条文原文が未取得"
     "Provimento 205/2021 の原文が未取得"
     "Código de Ética e Disciplina が未取得"]}

   ;; -----------------------------------------------------------------------
   "NLD"
   {:jurisdiction/id "NLD"
    :jurisdiction/name "Netherlands"
    :jurisdiction/iso3166 "NLD"
    :jurisdiction/legal-system :civil-law
    :jurisdiction/rules
    [{:rule/id "nld.procesmonopolie"
      :rule/title "Advocaat の procesmonopolie と法的助言の非規制"
      :rule/instrument "Nederlandse regelgeving (Advocatenwet / Wetboek van Burgerlijke Rechtsvordering)"
      :rule/url "https://dutch-law.com/lawyers/choosing-lawyer-netherlands.html"
      :rule/url-provenance :secondary-commentary
      :rule/verification :secondary-source-only
      :rule/verification-note "法令原文は未取得。解説サイトの要約のみ。"
      :rule/retrieved-at "2026-07-26"
      :rule/summary
      (str "advocaat は裁判所での代理について独占権を持つが、"
           "法的助言そのものは他の法務専門職も提供できる。"
           "kantonrechter（雇用・賃貸・25,000ユーロ以下の請求）では本人訴訟が可能。"
           "「advocaat」の称号を無登録で用いることは犯罪。"
           "非規制の法サービス提供者には legal professional privilege が及ばない。")
      :rule/topic #{:unauthorized-practice :privilege}}]
    :jurisdiction/service-modes
    {:mode/template-selection {:verdict :conditional :basis ["nld.procesmonopolie"]
                               :condition "裁判所代理に踏み込まないこと。原文未検証のため一次確認が必要。"}
     :mode/document-assembly {:verdict :conditional :basis ["nld.procesmonopolie"] :condition "同上。"}
     :mode/legal-analysis {:verdict :conditional :basis ["nld.procesmonopolie"]
                           :condition
                           (str "法的助言は独占されていないが、裁判所代理に踏み込まないこと。"
                                "また非 advocaat の助言には秘匿特権が及ばないことを"
                                "利用者に明示すること。")}
     :mode/lawyer-reviewed {:verdict :conditional :basis ["nld.procesmonopolie"] :condition "同上。"}
     :mode/inhouse-reviewed {:verdict :unsettled :basis [] :condition "bedrijfsjurist の規律を未検証。"}
     :mode/lawyer-directory {:verdict :unsettled :basis [] :condition "NOvA の規律を未検証。"}
     :mode/referral-placement {:verdict :unsettled :basis [] :condition "同上。"}}
    :jurisdiction/revenue-modes
    (into {} (for [m (keys revenue-modes)]
               [m {:verdict :unsettled :basis [] :condition "Advocatenwet 原文が未取得。"}]))
    :jurisdiction/known-gaps
    ["Advocatenwet の条文原文が未取得"
     "Verordening op de advocatuur が未取得"]}})

;; ---------------------------------------------------------------------------
;; Accessors
;; ---------------------------------------------------------------------------

(defn jurisdiction
  "The catalog entry for `jid`, or nil. nil means NO spec-basis — never
  fall back to a 'default' jurisdiction."
  [jid]
  (get catalog jid))

(defn jurisdiction-ids [] (vec (sort (keys catalog))))

(defn rules
  "All cited rules for `jid`."
  [jid]
  (get (jurisdiction jid) :jurisdiction/rules []))

(defn rule
  "Look up one rule by `:rule/id` within `jid`."
  [jid rule-id]
  (first (filter #(= rule-id (:rule/id %)) (rules jid))))

(defn rules-by-topic [jid topic]
  (filterv #(contains? (:rule/topic %) topic) (rules jid)))

(defn verified-rule?
  "True when the rule can support a permissive conclusion: checked well
  enough AND actually binding. A model rule fails here even if its text
  was read at the source, because reading a template carefully does not
  make it law."
  [r]
  (and (contains? #{:primary-source-read :official-url-retrieved}
                  (:rule/verification r))
       (not (model-only? r))))

(defn known-gaps [jid] (get (jurisdiction jid) :jurisdiction/known-gaps []))

(defn coverage
  "Honest coverage report. `requested` defaults to every catalogued
  jurisdiction; pass a list to find out what is missing for a target
  market. A jurisdiction absent from the catalog is a COVERAGE GAP, not
  an excluded jurisdiction — there are roughly 200 jurisdictions in the
  world and this catalog holds a small, named subset of them."
  ([] (coverage (jurisdiction-ids)))
  ([requested]
   (let [have (filter catalog requested)
         missing (remove catalog requested)
         all (jurisdiction-ids)
         rs (mapcat rules all)]
     {:requested (count requested)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :catalogued-jurisdictions all
      :rule-count (count rs)
      :rules-by-verification (frequencies (map :rule/verification rs))
      :known-gaps (into {} (for [j all :let [g (known-gaps j)] :when (seq g)] [j g]))
      :note
      (str "本カタログは " (count all) " 法域・" (count rs) " ルールを収載する。"
           "世界の法域はおよそ200あり、未収載の法域は『対象外』ではなく"
           "『カバレッジ未達』である。未収載法域については本製品はいかなる"
           "サービスモードも成立と判定しない（legalsupport.admissibility が"
           "既定で拒否する）。")})))
