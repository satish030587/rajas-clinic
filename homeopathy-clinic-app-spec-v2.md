# Rajas Homoeo Care — Clinic App Spec (v2)

Revised after the requirements-gathering session with Dr M Ilayaraja.
Supersedes the original homeopathy-clinic-app-spec.md draft — that version assumed
a paper register and an elaborate constitutional case sheet, neither of which matches
how this clinic actually runs.

---

## 1. What changed from v1, and why

| v1 assumption | Reality from the doctor | Change |
|---|---|---|
| Replacing a paper register | Already uses MyOPD (Catalyze Systems), a Windows + Android clinic software | This is a **migration/replacement of an existing system**, not a first digitisation |
| Android app, learning project | Wants desktop/laptop first; later agreed to build both desktop/web and Android | Backend + web app **first** (what he uses daily), Android **second** (your learning project) |
| Detailed constitutional case sheet (mentals, thermals, modalities…) | His actual new-patient fields: name, age, sex, address, referred by, blood group, occupation, current medication | Case sheet model **drastically simplified**; complaint/remedy are recorded but not the classical repertorisation fields |
| Prescription slip + printing | Dispenses medicine directly with verbal/written instructions, no formal prescription slip | Prescription printing **dropped from scope** |
| Remedy reference / mini materia medica | Refers to a repertory only ~1 in 20 cases | **Dropped from scope** |
| No mention of recall | **This is the actual reason he'd switch software** — recall reminders and a welcome message are what MyOPD can't do at the volume he needs | **Recall/reminder system is now the core feature**, not an add-on |
| English only | Confirmed both English and Tamil | **Bilingual from the schema up**, not retrofitted |
| Single platform | Confirmed both web/desktop and Android | **Sequenced**, not parallel — see Section 4 |

---

## 2. Why this app, given MyOPD already exists

MyOPD (Catalyze Systems, est. 2012, 600+ doctors across India and several other
countries) is a real, mature product. Its Windows desktop version is well regarded —
reviewers specifically praise it while criticising the companion mobile app as slow
and unreliable.

**The actual gap**, found by checking MyOPD's own pricing page: SMS/WhatsApp
reminders run on a credit allowance — **500 SMS or 100 WhatsApp credits per year**,
included with purchase or annual renewal. At 15–20 patients/day (~5,000 visits/year),
that covers roughly 2% of what a welcome-message-plus-recall workflow needs.

This is precisely what the doctor named, unprompted, as both his top wish (Q10) and
the reason he'd abandon new software (Q11): recall reminders and a welcome message.

**This is not a case for rebuilding MyOPD's feature set.** Multi-language
prescription printing, cloud backup, receptionist role separation, appointment
management — all already exist there, built over 14 years. Competing on breadth is
not a good use of part-time solo effort.

**This is a case for building the recall workflow properly**, as part of a right-sized
system that also does the patient/visit/billing basics MyOPD does — but the reason
this app exists, and the feature that must not be compromised, is unlimited,
bilingual, automatic recall reminders.

**Confirmed with the doctor:** he will stop using MyOPD entirely once this system is
ready — a full replacement, not a parallel run. This raises one new requirement: a
**migration path for existing patients**. He previously said MyOPD data export
wasn't needed, but that was under the earlier companion-tool framing where MyOPD
stayed the primary record. Under a full replacement, any patient who only exists in
MyOPD becomes invisible to the new recall system unless migrated. Check whether
MyOPD offers even a basic name/phone/last-visit export; if not, plan for a manual
re-entry pass on existing patients before go-live, prioritised by who's actually due
for recall soon.

---

## 3. Feature List

### Phase 1 — MVP

**Patient Management**
- Register patient: name, age or DOB, sex, address, phone, referred by, occupation,
  blood group, current medication, preferred language (English/Tamil)
- Search by name / phone
- Patient detail: profile + full visit timeline

**Visits**
- New visit: date, complaint, remedy given (free text — no repertory dropdown),
  potency, advice/instructions given verbally
- **Next-visit-due date** — first-class field, set at the end of every visit. This
  single field is what the whole recall system runs on.
- Skin/condition photo attachment, with previous photos visible alongside the new one
  for comparison (explicitly requested — Q5)

**Recall & Reminders — the core feature**
- **Today's list**: patients due today, generated automatically from `next_visit_due`
- **Due-for-recall list**: patients overdue, grouped by how overdue (this week / this
  month / over a month)
- **Welcome message**: auto-triggered on first registration
- **Recall reminder**: triggered on/after `next_visit_due`, in the patient's
  preferred language
- v1 sending mechanism: **`wa.me` click-to-send links** — the app prepares the
  message and opens WhatsApp with it pre-filled; the doctor taps send. No WhatsApp
  Business API, no Meta approval, no monthly fee, no per-message cost. At 15–20
  patients/day this is a few seconds of tapping, not a burden.
- Message templates stored per language, editable without a code change (see data
  model) — the welcome and recall wording can be tuned after the doctor uses it a
  few weeks

**Billing**
- Consultation fee + medicine charge per visit
- Payment mode (cash/UPI) and status
- Daily collection total

**Auth**
- PIN/biometric app lock
- Token-based login to the backend

### Phase 2

- Automate reminder sending via WhatsApp Business API, once manual sending proves
  the workflow and volume (see Section 6 — do not start here)
- Reports: new vs follow-up counts, monthly revenue, recall conversion rate (did the
  reminder actually bring the patient back — MyOPD doesn't offer this, general clinic
  recall systems treat it as a core KPI)
- Data export (CSV) for the doctor's accountant
- Groups/tagging — the doctor's current MyOPD screen has a "Groups" field; worth
  asking what he uses it for, since it may already be an improvised recall mechanism
  worth preserving

### Explicitly out of scope

- Prescription printing (he doesn't give one)
- Remedy repertory/materia medica reference (used too rarely to justify building)
- Multi-doctor scheduling
- Inventory management (unless a later conversation says otherwise)
- Rebuilding MyOPD's full feature set

---

## 4. Platform Sequencing

Building web and Android in parallel against a schema that's still settling is how
solo side-projects stall. Sequenced instead:

**Stage 1 — Backend (FastAPI + PostgreSQL)**
The actual product. Patients, visits, recall dates, message templates (bilingual),
auth. Everything else is a view onto this.

**Stage 2 — Web app, opened in a browser on the clinic laptop**
This is the "desktop app" from the doctor's perspective — full-screen in Chrome/Edge,
no separate install needed initially. This is what he uses daily, so it ships first
and gets finished before anything else starts. Reuses frontend skills already in use
on the HRMS project.
*A native desktop wrapper (Electron/Tauri) can be added later purely for a desktop
icon/offline shell — same web app underneath, not a rewrite.*

**Stage 3 — Android app**
Same API, same data. Deliberately sequenced last: this is the learning project, and
it's a far better learning exercise once the domain and schema are already proven
against real clinic use rather than still shifting weekly. No delivery pressure on
this stage — the clinic is already running on Stage 2.

---

## 5. Data Model (draft v2)

```
patient
  id, name, dob_or_age, sex, address, phone, referred_by,
  occupation, blood_group, current_medication,
  preferred_language(en|ta), created_at, archived

visit
  id, patient_id, visit_date, complaint, remedy_given_text,
  potency, advice_given, next_visit_due, created_at

photo
  id, visit_id, file_path, caption, taken_at

message_template
  id, template_key(welcome|recall), language(en|ta), body_text, updated_at

message_log
  id, patient_id, template_key, language, sent_at, channel(whatsapp_manual|
  whatsapp_api|sms), status

invoice
  id, visit_id, consultation_fee, medicine_charge, total,
  payment_mode(cash|upi), payment_status, paid_at

app_user
  id, username, password_hash, role(doctor|reception), active
```

**Notes**
- No `case_sheet` table in v2 — the classical constitutional model from v1 doesn't
  match this doctor's actual practice. If a future doctor client needs it, it's a
  separate table, not a forced field on this one.
- `next_visit_due` lives on `visit`, not a separate appointments table, because
  recall — not scheduling — is the driver here. A proper appointment-booking system
  can be layered on later if walk-in/phone booking needs tracking beyond what MyOPD
  already provides.
- `message_log` matters for the recall-conversion report in Phase 2 — you can't
  measure whether a reminder worked if you didn't record that it was sent.
- Client-generated UUIDs as primary keys, still — relevant once the Android client
  is offline-capable in Stage 3.

---

## 6. On WhatsApp Automation — Do This Later, Not First

Starting with manual `wa.me` links is not a corner cut — it's the right call for v1:

- No Meta Business verification, no BSP (Business Solution Provider) sign-up, no
  template approval process, no monthly platform fee
- At 15–20 patients/day, the doctor tapping "send" on a pre-filled WhatsApp message
  is a few seconds, not a burden
- It proves the workflow and message wording actually work before you spend money
  automating them

Automate only once he's relying on it daily and the tapping itself becomes the
friction. At that point: WhatsApp's utility-message pricing in India runs
substantially cheaper than marketing-category messages, so keep recall and welcome
messages framed as utility/service content, not promotional. Pricing and the rules
around the 24-hour customer service window were mid-change as of writing (Meta
shifted to per-message billing for utility/service messages in 2025, with further
changes reported for October 2026) — check current rates on Meta's developer
documentation before committing to this stage rather than trusting anything written
earlier.

---

## 7. Bilingual — Built In, Not Retrofitted

- `patient.preferred_language` decides which template is used at send time
- `message_template` stores English and Tamil versions of each message
  independently — editable later without touching code
- UI strings: a simple key-value JSON per language for the web app; Android's
  built-in `strings.xml`-per-locale for the mobile client (this is genuinely one of
  the easier parts of Android to learn)
- **Still to confirm with the doctor**: does he want *his own* data-entry screens in
  Tamil too, or only the patient-facing messages? These are different scopes — his
  own UI in English while messages go out in Tamil is the more common and far
  cheaper setup. Don't assume; ask directly.

---

## 8. Build Order Checklist

- [x] Confirmed: full MyOPD replacement (not parallel run) — plan patient migration
- [ ] Check whether MyOPD can export existing patients (name/phone/last visit);
      if not, plan a manual re-entry pass before go-live
- [ ] Confirm: doctor's own UI language preference (English only, or also Tamil?)
- [ ] Finalise data model above, write Alembic migrations
- [ ] FastAPI: auth + patient CRUD + visit CRUD
- [ ] Message template CRUD (so wording can be edited without redeploying)
- [ ] Today's-list and due-for-recall queries
- [ ] Web app: patient list + add patient, wired to backend
- [ ] Web app: visit entry screen — this is the 60-second-budget screen, see Section 9
- [ ] Web app: recall dashboard (today's list, due list, wa.me send buttons)
- [ ] Web app: billing entry + daily total
- [ ] Photo upload + side-by-side comparison view
- [ ] Two weeks of real use on the web app before starting Android
- [ ] Android: same screens, same API, as the learning project
- [ ] Phase 2: reports, CSV export, WhatsApp API automation (only once manual
      sending is a proven bottleneck)

---

## 9. What Still Governs Everything

From the original requirements session, still true and now sharper:

1. **His time budget**: ~4 minutes of recording per patient today. Software should
   reduce this, not add to it. The visit-entry screen is the one to obsess over.
2. **Recall is the product.** Everything else is table stakes he already has in
   MyOPD. If the recall feature isn't reliable and easy to act on daily, the whole
   project has failed regardless of how good the rest is.
3. **Don't build fields he doesn't use.** His actual case-taking is short. Resist
   the pull to add back "proper" homeopathic case-taking structure he didn't ask for.
4. **Sit with him again after the recall dashboard exists**, before building
   anything past it. The two open questions above (MyOPD transition, UI language)
   should be settled by then too.
