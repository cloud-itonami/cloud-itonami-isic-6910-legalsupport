# Business Model: Governed AI Legal-Document and Procedure Support

## Classification

- Repository: `cloud-itonami-isic-6910-legalsupport`
- ISIC Rev.5: `6910` (legal activities)
- ISCO-08 adjacency: `2611` (lawyers), `2619` (legal professionals nec)
- Social impact: access-to-justice, consumer-protection, legal-cost-reduction

## Customer

Two sides, one of which pays.

- **Individuals and small businesses** who need a document produced or a
  procedure navigated, and who cannot afford an hour of a lawyer's time
  to find out whether they need a lawyer at all.
- **Lawyers and law firms** who want the mechanical part of intake,
  drafting and procedure tracking done before it reaches them, with the
  review step they are professionally required to perform made explicit
  and auditable.

## Offer

- template selection and document assembly for routine, non-disputed matters
- procedure checklists and filing-step support
- individualized legal analysis **only** where the jurisdiction admits it,
  or behind a licensed lawyer's personal review
- a lawyer directory, where the jurisdiction admits one
- an audit ledger recording, per delivery, the cited legal authority the
  delivery relied on

## Revenue

Permitted, per jurisdiction:

| Mode | Where it is established today |
|---|---|
| `:revenue/law-firm-saas-license` | JPN, GBR |
| `:revenue/consumer-document-fee` | GBR |
| `:revenue/consumer-subscription` | GBR |
| `:revenue/directory-listing-flat` | conditional in DEU, KOR, BRA |

Refused by design in every jurisdiction:

| Mode | Why |
|---|---|
| `:revenue/per-referral-fee` | JPN 弁護士法72条 (周旋); DEU BRAO §49b(3); GBR LASPO s.56 (PI); BRA OAB Prov. 205/2021; USA-DC Rule 5.4(a) |
| `:revenue/success-fee-share` | same, plus DEU BRAO §49b(2) |

Japan is the sharpest case and worth stating plainly. The MOJ guideline
§1(2) says that even a service that *looks* free constitutes 「報酬を得る
目的」 where the operator steers users to a third party's paid service and
is paid by that third party (ア/イ), **or where the service is offered
only to those who paid a subscription or membership fee (ウ)**. So in
Japan neither a referral cut nor a consumer subscription is a free pass;
the reliable path is selling the tool to law firms whose lawyers
personally review its output, which the guideline's §4(1) safe harbour
covers even when every other element of the offence is present.

## Trust Controls

- no filing, submission, service or payment is ever performed by the
  actor — `:effect` must be `:propose`
- no delivery in a jurisdiction absent from the cited fact catalog
- no delivery in a service mode the jurisdiction does not admit
- a licensed reviewer, licensed **in the matter's own jurisdiction**,
  must be recorded as having personally reviewed and amended any output
  whose lawfulness depends on that review
- a matter where a dispute has already arisen requires that reviewer in
  every mode
- introducing a client to a lawyer always requires human sign-off, and
  is refused outright where the jurisdiction forbids placement
- no permissive conclusion rests on a source that was only read as
  secondary commentary
- every committed record carries its citations

## Go-to-market implication

The catalog says where to start, and it does not say "everywhere".

**England & Wales first for the consumer product.** Only six activities
are reserved under the Legal Services Act 2007; legal advice itself is
not among them, so the machine may analyse and the consumer may pay
directly. This is the widest admissible envelope in the catalog.

**Japan first for the law-firm product.** The consumer paths are
`:conditional` at best, but selling the tool into firms is squarely
inside a published safe harbour, and the MOJ's own January 2026 paper
frames legal tech as something it wants to enable rather than suppress —
while warning about hallucinated legal information and confidentiality
leakage, which is precisely what the governor and the audit ledger
address.

**Germany, France, Netherlands, Arizona, Utah: conditional.** Each needs
an operator-held registration, licence or sandbox authorisation before
anything unlocks; the code models that as an attestation and then makes
every attested run pass a human.

**Everywhere else: do the research first.** Thirteen catalogued
jurisdictions currently admit nothing, and roughly 185 jurisdictions are
not catalogued at all. That is a work queue, not a verdict.

## Non-goals

- becoming a paid referral marketplace
- practising law in any jurisdiction
- claiming a licence, registration or bar admission the operator does not hold
- presenting the catalog as legal advice, or as a substitute for counsel
  in the operator's own market
