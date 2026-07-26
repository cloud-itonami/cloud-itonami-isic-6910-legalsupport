# cloud-itonami-isic-6910-legalsupport

Open Business Blueprint for **ISIC Rev.5 6910** (legal activities): a
**governed AI legal-document and procedure-support service** that helps
people through legal paperwork and procedures, connects them to a
licensed lawyer where one is needed, and **takes no fee for that
connection**.

The product is paid for the document and procedure work it does. It is
not a referral marketplace, and it refuses referral compensation
structurally, in every jurisdiction, whether or not local law would
permit it — see [Why no referral fee](#why-no-referral-fee).

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph) StateGraph
runtime, following the itonami actor pattern (ADR-2607011000): here it
is **LegalSupportAdvisor ⊣ LegalSupportGovernor**.

**Maturity: `:implemented`.** 54 tests / 4,417 assertions green
(`clojure -M:test`), `clojure -M:lint` clean.

---

## The idea in one paragraph

Almost everywhere in the world, a machine that reads your situation and
tells you what your contract should say is practising law, and a
non-lawyer who does that commits a crime. But a machine that assembles a
document from a template, walks you through a filing procedure, and then
hands the result to a licensed lawyer who personally examines and amends
it is doing something the same regulators explicitly bless. Japan's
Ministry of Justice wrote that second thing down in
[a 2023 guideline](https://www.moj.go.jp/content/001400675.pdf) and said
in terms that when a lawyer «自ら精査し、必要に応じ自ら修正する» —
personally examines and, where needed, personally amends — the service
does not violate Art. 72 of the Attorney Act *even if every other
element of the offence is present*. That sentence is this product's
architecture.

## What makes this different from a legal-fact wiki

The jurisdiction catalog is not documentation sitting next to the code.
`legalsupport.governor` calls `legalsupport.admissibility/evaluate` on
**every single run**, and any blocker it returns is a hard hold. A
jurisdiction nobody has researched cannot be served. A service mode the
local bar forbids cannot reach `:commit`. A committed record carries the
citations the governor relied on, so the audit ledger answers not only
*what was delivered* but *on what cited legal authority the delivery was
believed lawful*.

```text
:intake -> :advise -> :govern -> :decide -+-> :commit           (:ok? true)
                                          +-> :request-approval (:escalate?, interrupt-before)
                                          +-> :hold             (:hard? true)
```

## Where this can operate today

Generated from `legalsupport.admissibility/operating-envelope`, not
asserted by hand. `admissible` = established on primary/official
authority. `conditional` = lawful only if a condition the catalog cannot
itself check holds; an operator unlocks these by attesting to them, and
every attested run then requires human sign-off.

| Jurisdiction | Admissible service modes | Conditional | Admissible revenue modes |
|---|---|---|---|
| **JPN** | template-selection, document-assembly, lawyer-reviewed, inhouse-reviewed | 1 | law-firm-saas-license, none |
| **GBR** (England & Wales) | + legal-analysis, lawyer-directory (6 total) | 1 | + consumer-document-fee, consumer-subscription |
| DEU / FRA / NLD | — | 4 each | — |
| USA-AZ / USA-UT | — | 7 each | — |
| AUS / BRA / CAN-ON / KOR / SGP | — | 1 each | — |
| IND / USA / USA-DC | — | 0 | — |

Two jurisdictions can be served on verified authority today. That is not
a claim that the other thirteen are hostile — it is a statement about
**how far the research has got**, and it is deliberately visible rather
than smoothed over. See [Coverage is a gap, not an exclusion](#coverage-is-a-gap-not-an-exclusion).

The asymmetry between Japan and England & Wales is the single most
useful fact in the catalog: England reserves only
[six activities](https://www.legislation.gov.uk/ukpga/2007/29/schedule/2)
and leaves legal advice itself unreserved, so the machine may analyse;
Japan reserves the whole category of 法律事務 and lets the machine in only
through the lawyer-review safe harbour. Same product, structurally
different market entry.

## Why no referral fee

The user-facing reason: being paid to place a matter with a particular
lawyer gives the platform an interest that is not the client's.

The structural reason: it is banned in enough places that a single
worldwide product can only exist if the strictest rule becomes the
product's rule.

- **Japan** — 弁護士法72条 forbids 「周旋」 as a business, and the MOJ
  guideline §1(2)イ treats being paid by the third party the user was
  steered to as evidence of 「報酬を得る目的」.
- **Germany** — BRAO §49b(3): giving or taking a share of fees or other
  advantages for mediating instructions is impermissible, toward lawyers
  and third parties alike.
- **England & Wales** — LASPO 2012 ss.56-60 prohibits paying or
  receiving referral fees in personal-injury and death claims.
- **Brazil** — OAB Provimento 205/2021: advertising is informational
  only and must not amount to captação de clientela.
- **United States** — no federal answer; D.C. Rule 5.4(a) prohibits
  sharing legal fees with a nonlawyer, while Arizona repealed its ER 5.4
  entirely in 2021.

`legalsupport.admissibility/forbidden-revenue-modes` therefore refuses
`:revenue/per-referral-fee` and `:revenue/success-fee-share` everywhere,
and `governor-test` proves an Arizona-style permissive jurisdiction
cannot re-enable them.

## Coverage is a gap, not an exclusion

`legalsupport.facts/coverage` reports 15 jurisdictions, 27 cited rules —
**14 read at the primary source** and **13 seen only as secondary
commentary** (it was 8/1/18 before 2026-07-26).

Verification is not the only axis. **Three of the rules are ABA Model
Rules, which are not law anywhere** — they are a drafting template that
binds nobody until a state adopts it, and adopters change it materially
(D.C. permits nonlawyer ownership under its own 5.4(b); Arizona repealed
5.4 outright). Citing "Model Rule 5.4" as authority for what a US
operator may do is a category error, so those rules carry
`:rule/binding-force :model-only` and `verified-rule?` refuses them
**even if their text were read at the source**. The only US law the
catalog holds first-hand is D.C.'s own adopted Rule 5.4. (It was 8/1/18 until
2026-07-26, when the statute text for 行政書士法19条, 司法書士法3条, RDG
§2/§3/§10, LASPO 2012 s.56 and Ontario's Law Society Act s.26.1 was
pulled from the retrieval paths proved out in
`cloud-itonami-licensed-operator`: e-Gov's law API, legislation.gov.uk's
per-section `data.xml`, gesetze-im-internet.de's `xml.zip` and Ontario
e-Laws.) That last number is
published rather than buried, because a rule read only as commentary can
support a *restrictive* verdict but may never be the sole basis of a
permissive one. `admissibility/verdict-for` enforces the asymmetry by
downgrading any such `:admissible` to `:unsettled`, and
`facts-test/permissive-verdicts-are-never-groundless` proves no entry
escapes it.

Being wrong in the cautious direction costs coverage. Being wrong in the
permissive direction exposes a user to criminal unauthorized-practice
liability. The two are not symmetric and the code does not treat them as
if they were.

Every jurisdiction also carries `:jurisdiction/known-gaps` naming what
has *not* been checked — Japan's 弁護士法27条, the RDG's own text,
the Advocates Act, 47 US states.

## Guarantees the governor enforces

HARD (always hold, never overridable):

1. **client provenance** — the client is registered.
2. **no actuation** — the actor never files, submits, serves or pays;
   `:effect` must be `:propose`.
3. **matter basis** — the proposal cites a registered matter belonging
   to this client.
4. **admissibility** — every blocker from the jurisdiction catalog for
   the matter's jurisdiction, the proposed service mode and the
   operator's configured revenue mode.
5. **licensed reviewer** — where the mode's lawfulness rests on a
   lawyer, a registered lawyer must be licence-verified, **licensed in
   the matter's own jurisdiction**, and recorded as having personally
   reviewed and amended the output. Assigning a lawyer is not review,
   and a lawyer licensed elsewhere is not a reviewer — cross-border
   review is how a lawyer-in-the-loop design quietly becomes
   unauthorized practice.
   These three per-reviewer checks now run in
   [`cloud-itonami-licensed-operator`](https://github.com/cloud-itonami/cloud-itonami-licensed-operator),
   which was extracted from this repo so other regulated-sector actors
   could reuse them, so violations come back as `:req/licence-verified`
   / `:req/same-jurisdiction` / `:req/personally-decided`. Deciding
   *whether* a reviewer is needed stays here — that turns on the service
   mode and on 事件性, which are this actor's business.
6. **dispute containment** — a matter where a dispute has already arisen
   (「事件性」) cannot be handled in any mode without that reviewer.

ESCALATE (human sign-off regardless of confidence): introducing a client
to a lawyer; opening a new jurisdiction; any run in a mode unlocked only
by operator attestation; low confidence.

The advisor is an LLM and is structurally unable to choose the revenue
mode, sign an attestation, or assert that a lawyer reviewed the output —
all three live in the store and are read by the governor. An advisor
that could name its own revenue mode could talk itself into a paid
referral business; one that could assert "a lawyer reviewed this" could
manufacture the very fact that makes the service lawful.

## Usage

```bash
clojure -M:test          # 54 tests / 4,417 assertions
clojure -M:lint          # clj-kondo, errors fail
clojure -M:emit-datoms   # regenerate data/jurisdiction-rules.datoms.edn
```

```clojure
(require '[legalsupport.admissibility :as adm])

(adm/operating-envelope "JPN")
(adm/evaluate {:jurisdiction "JPN"
               :service-mode :mode/lawyer-reviewed
               :revenue-mode :revenue/law-firm-saas-license})
;; => {:admissible? true :blockers [] :citations [{:rule/id "jpn.moj-ai-contract-guideline-2023" ...}]}
```

`data/jurisdiction-rules.datoms.edn` is a generated, transactable
projection of the catalog (276 entities) carrying
`:source/dataset "legal-jurisdiction-rules"`, so it joins the
superproject's DataScript query plane (ADR-2607252000) alongside the
company, fleet and compliance datasets. `datoms-test` fails if it drifts
from `facts.cljc`.

## Scope: what this is not

This repository is **software and a cited fact catalog**, not legal
advice and not a licence. Whoever deploys a live instance supplies the
jurisdiction's licence or registration, the lawyer relationships, and
bears that jurisdiction's liability. The catalog restates what
authorities have published; Japan's own guideline opens by saying Art.
72 is a penal provision whose application is ultimately for the courts,
and the MOJ said in
[a January 2026 paper](https://www8.cao.go.jp/kisei-kaikaku/kisei/meeting/wg/2501_06ai/260109/ai06_05.pdf)
that the 2023 guideline needs re-organising and that a whitelist of
specific legal-tech services is not feasible. Treat the JPN verdicts as
a moving surface and re-verify them.

## Related repositories

- [`cloud-itonami-licensed-operator`](https://github.com/cloud-itonami/cloud-itonami-licensed-operator) — この repo から抽出した「有資格者の背後で動けるか」の判定 commons。reviewer 検査はここへ委譲済み
- [`cloud-itonami-isic-6910`](https://github.com/cloud-itonami/cloud-itonami-isic-6910) — same ISIC code, company incorporation / registered-agent scope
- [`cloud-itonami-isco-2611`](https://github.com/cloud-itonami/cloud-itonami-isco-2611) — a single legal practice's own case-file management
- [`cloud-itonami-assoc-6910-jpn-nichibenren`](https://github.com/cloud-itonami/cloud-itonami-assoc-6910-jpn-nichibenren) — JFBA self-regulatory rule catalog
- [`cloud-itonami-isic-7810`](https://github.com/cloud-itonami/cloud-itonami-isic-7810) — employment agency, the fleet's prior two-sided matching actor

## License

AGPL-3.0-or-later.
