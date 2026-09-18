# Rajas Homoeo Care — Clinic App Spec (v3)

Revised after the second requirements round with Prof. Dr. M. Ilayaraja (Sept 2026),
including his answers to the open questions. Supersedes
`homeopathy-clinic-app-spec-v2.md`.

v2 is still right about *why* this app exists — recall is the product — but it assumed
one user on one device, and that assumption no longer holds.

**All major decisions in this document are settled.** The only open item is carried over
from v2 and is not blocking (§9).

---

## 1. What changed from v2, and why

| v2 assumption | What the doctor now says | Change |
|---|---|---|
| One user, one device | **Two devices**: receptionist takes preliminary data, doctor sees it when the patient walks in | Roles, login, and **shared live data** — the biggest change in this revision |
| Offline-first Room, backend optional/later | Two devices must see the same record within seconds | **Backend is now mandatory.** Hosting confirmed: **cloud server** (§3) |
| Doctor records everything himself | Reception records identity + vitals; doctor records the clinical part | Split the visit into a **reception half and a doctor half** |
| No vitals captured | Blood group, **height, weight, BP**; pulse optional | New per-visit `vitals` |
| Advice is free text | Six standard cards, printed today; **one card per patient**, chosen to match | **Card presets** replace free-text advice |
| One remedy per visit | **Any number of medicines**, all covered by that one card | Medicines become a list |
| Photos attach to a visit, for skin comparison | Also wants **external scan/lab reports** grouped and compared across months | Investigations become their own record |
| Two messages: welcome + recall | Six messages in real daily use | **Message library** + clinic profile |
| Prescription printing dropped from scope | He hands out a **pre-printed card** | Partly reversed — app records the card and can share it (§4.4) |
| No appointment booking | Confirms appointments by phone; wants to see who is expected | **Appointments**, shown on Today (§4.8) |
| UI in English, messages bilingual — open question | **Both. Every screen in Tamil and English, plus the messages** | Full bilingual UI with an in-app language switch (§4.7) |
| MyOPD export "not needed", then "check if possible" | **MyOPD cannot export. Re-entry is manual** | Migration becomes a real workstream with app support (§4.11) |
| Pharmacy not mentioned | Wants a pharmacy device later that reads the prescription | New **Phase D**, deliberately last |

---

## 2. Status check — what the v2 Android app already does

The doctor asked to confirm that the consultation entry is already built. **Yes, it is.**
Shipped and verified on a device in the `mobile/` module:

- **Chief complaint** — free text on the visit screen
- **Medicine given** — free text `remedy` + `potency`
- **Next visit due** — first-class field with quick 7/15/30/45-day chips; this is what
  the whole recall system runs on
- **Fees** — consultation fee + medicine charge, cash/UPI, paid/unpaid, daily collection total
- Plus: patient register with search, visit timeline, photo attach and side-by-side
  comparison, PIN/biometric lock, bilingual welcome + recall messages sent via `wa.me`,
  and the recall dashboard (due today / overdue this week / this month / over a month)

So §4.3 is **not new work** — it is existing work that gets *reorganised* so the doctor
stops retyping what reception already entered, and *extended* so medicines become a list
with a card attached.

What is **not** built: any backend endpoints (the FastAPI app has models only, no routes),
users or roles, vitals, investigations, appointments, the four extra message templates,
and the in-app language switch.

---

## 3. Architecture: cloud backend, two devices

**Confirmed: a cloud server, and reliable WiFi covering both the front desk and the
consulting room.** That settles the shape of the system.

Reception types on device A. Two minutes later the doctor opens the same patient on
device B and must see those vitals. Two phones cannot see each other's local database, so:

- **The FastAPI backend is the source of truth.** It gets built first — it does not exist today
- **Android keeps a local cache** and shows it instantly, then refreshes from the server
- **Writes queue locally when the network drops.** WiFi is reliable, but a consultation
  must never be blocked by a router reboot. This is cheap insurance, not a sync engine

**Practical cloud notes**, since this is now a hosted system holding patient data:

- A small VM (2 vCPU / 2–4 GB) is enough for one clinic at ~20 patients/day
- **Automated daily database backups are not optional.** This becomes the only copy of
  the clinic's records once MyOPD is switched off
- HTTPS with a real certificate, not self-signed — the phones will refuse it otherwise
- Photos and scan reports are the bulk of the storage; budget for them growing
  continuously and never shrinking
- Token login per user, tokens expiring on a sensible schedule

**This is a real increase in scope.** Two-device handoff is not a screen; it is the
backend, authentication, file storage and a cache layer that v2 postponed. Everything in
§4 depends on it landing first.

---

## 4. Feature specs

### 4.1 Two roles — reception and doctor

Two login accounts, each on its own device. Login does not replace the PIN lock — the PIN
stays as the *device* lock; the login identifies *who* is using it.

**Reception can:**
- Register a new patient / edit patient details
- Record vitals for today's visit
- Start a visit — this puts the patient into the doctor's waiting queue
- See today's list and the recall list; send appointment, reminder, dispatch and review messages
- Attach scan reports the patient hands over at the desk
- **Cannot** see or edit complaint, medicines, or fees

**Doctor can:**
- Everything reception can, plus
- The clinical half: complaint, medicines, card, next visit due
- **Fees and payment — confirmed doctor-only**
- Edit message templates, cards and clinic settings

**The handoff, as a visit lifecycle:**

```
reception starts visit  →  WAITING  →  doctor opens it  →  IN_CONSULTATION  →  COMPLETED
     (vitals)                                              (clinical + fees)
```

The doctor's Today screen gains a **Waiting** section at the top: who is in the waiting
room now, in arrival order, vitals already filled in. He taps a name and lands in the
consultation screen with the top half populated and read-only.

This is the feature he actually asked for. "I don't want to enter" means: when he opens
the patient, name, age, blood group, height, weight and BP are already there.

### 4.2 Vitals at reception

Recorded **per visit**, because weight and BP change:

- Height (cm) — defaulted from the last visit, since it rarely changes
- Weight (kg)
- Blood pressure (systolic / diastolic)
- **Pulse — optional field, confirmed**

Blood group stays on the **patient** record where it already is; it never changes.

Per-visit weight and BP means they can be trended later. Not building the chart now, just
not making it impossible.

### 4.3 Medicines and the card

Confirmed by the doctor: **any number of medicines, and one card out of the six.**

Today he hands out a pre-printed card because patients don't reliably follow verbal
instructions. The card covers the whole day's schedule; the medicines are dispensed
alongside it.

So a visit records:

- **Medicines** — a list, any length. Each: name, potency, form (pills / drops), quantity
- **One card** — exactly one of the six, chosen to match the patient

The earlier draft of this spec proposed binding each medicine to a specific line of the
card (before food / after food / bedtime). **That was wrong and has been dropped** — it
modelled more structure than the clinic actually uses and would have slowed down entry
for no benefit.

**The six cards.** Reading the printed set, they are combinations of six instruction lines:

| Line | Instruction |
|---|---|
| Empty stomach | 4 pills daily, on an empty stomach |
| Before food | 4 pills, three times a day, 10 min before meals |
| After food | 4 pills, three times a day, 10 min after meals |
| 10 drops | 10 drops in ¼ glass lukewarm water, three times a day, 10 min after meals |
| 20 drops | 20 drops in ¼ glass lukewarm water, three times a day, 10 min after meals |
| Bedtime | 4 pills at bedtime, on the tongue, without water |

Every card also carries the same footer: *Store medicine in an air-tight container* /
*Keep away from sunlight*.

The six cards are stored in the database with their full English and Tamil text and are
**editable in Settings**, exactly like the message templates — wording can be corrected
without a new build. The doctor picks a card by tapping it; the card's text is shown so
he can confirm it is the right one.

### 4.4 Sharing the card to the patient

**Partial reversal of v2.** v2 dropped prescription printing on the belief that
instructions were verbal. They are not — he prints a card.

The app will **send the card's text to the patient's WhatsApp in their own language**,
alongside the physical card. Cheaper than printing more cards, and the patient still has
it at home when they forget. Uses the same `wa.me` mechanism as every other message.

Physical printing stays out of scope unless he asks for it.

### 4.5 Investigations and reports

Distinct from the existing visit photos, which are for skin/condition comparison.

A patient brings an outside scan — say a kidney USG. Three months later, another one. The
doctor wants both side by side.

```
investigation
  id, patient_id, visit_id (nullable — reports arrive between visits too),
  kind (scan | lab | xray | ecg | other),
  title            e.g. "Kidney USG"  ← the grouping key
  taken_on, note, created_by
investigation_file
  id, investigation_id, file_path, page_no
```

- Photograph or attach multiple pages per report
- Grouped by `title`, so "Kidney USG" from March and from June sit together
- **Comparison view**: pick any two dates of the same title, shown side by side — this
  reuses the photo-comparison screen that already exists
- Both roles can attach; reception often receives them at the desk

### 4.6 Message library

Six templates, each in English and Tamil, all editable in Settings. Two exist; four are new.

| Key | Status | When it is sent |
|---|---|---|
| `WELCOME` | Built | On first registration |
| `RECALL` | Built | On/after `next_visit_due` |
| `APPOINTMENT_CONFIRMATION` | New | When an appointment is booked over the phone |
| `APPOINTMENT_REMINDER` | New | Day before a booked appointment |
| `MEDICINE_DISPATCHED` | New | When medicine is posted, with the tracking ID |
| `REVIEW_REQUEST` | New | After a good consultation, to collect a Google review |

New placeholders, so the doctor's real wording works verbatim:

```
{name} {clinic} {due_date} {last_visit}          (existing)
{appointment_date} {working_hours} {clinic_address} {map_link}
{doctor_name} {doctor_qualifications} {clinic_phone}
{tracking_id} {review_link}
```

All four new templates are seeded with **his exact existing wording** — the appointment
confirmation, the post-dispatch note, the review request and the reminder. He should not
have to retype what he already sends today.

Sending stays as v2 decided: `wa.me` click-to-send, no WhatsApp Business API, no
per-message cost. That should not change until manual sending is proven to be the bottleneck.

### 4.7 Bilingual — every screen, both languages

**Confirmed: the app's own screens must be available in Tamil and English, not just the
patient messages.** This answers the question v2 left open.

- **A language switch in Settings**, per device and per login — so the receptionist can
  run the app in Tamil while the doctor runs it in English, or either can switch
- Every screen, label, button, error and empty state is translated — no half-translated screens
- Patient-facing messages continue to follow `patient.preferred_language`, which is
  **independent** of the app's own language. A Tamil-speaking receptionist still sends an
  English message to an English-preferring patient

**Technical note:** the app must set its own locale rather than following the phone's
system language, since both users share identical devices and expect different languages.
The existing Android app already ships a complete Tamil string set, so this is an
extension of work already done, not a fresh start — but **every new screen in Phases A–C
must be translated as it is built**, not retrofitted afterwards. Retrofitting translations
is how half-Tamil apps happen.

### 4.8 Appointments

**Confirmed: the app remembers the date and shows who is expected.**

This is distinct from `next_visit_due`, and the two should not be merged:

- **Next visit due** — set by the doctor at the end of a consultation ("come back in 15
  days"). Drives the recall list. A *due* date.
- **Appointment** — a patient phones and asks for a specific date; the confirmation
  message goes out. A *confirmed* date.

```
appointment
  id, patient_id, appointment_date, note,
  status (booked | attended | cancelled | no_show), created_by, created_at
```

**How it works in practice:**

1. Sabitha phones on 28 Feb asking to come on 4 March
2. Reception opens her record, picks 4 March, sends the confirmation — the date is saved
3. On 3 March, she appears in a "send reminder" list; one tap sends the reminder message
4. On 4 March, she appears in a **Booked** section on Today, above the recall list, so the
   doctor knows who is expected that evening
5. When she arrives and a visit is started, the appointment is marked attended automatically

No time slots — patients are told to come any time between 6:00 and 8:30 pm, so booking
them into 15-minute slots would add work and match nothing about how the clinic runs.

### 4.9 Clinic profile

A single settings record, so templates stop hard-coding anything:

- Clinic name, full address, landmark, Google Maps link
- Doctor name and qualifications
- Clinic mobile
- Working hours (Mon–Sat 6:00–8:30 pm, Sunday closed)
- Google review link
- **UPI ID** for the payment QR

### 4.10 UPI collection at billing

The clinic already has a Google Pay for Business QR. With the UPI ID in the clinic
profile, the billing screen can show a QR **with the amount already filled in**, so the
patient scans and the total is correct. Small feature, removes a daily source of error,
pairs with the cash/UPI payment mode that already exists.

### 4.11 Migration from MyOPD

**Confirmed: MyOPD cannot export patients. Re-entry is manual.**

This is the highest-risk item in the whole project, because it is the one place where
effort scales with the size of the existing patient base and where enthusiasm runs out
before the work does.

**The important decision: do not re-enter everyone.**

Recall runs on `next_visit_due`, which is set at the end of a consultation. A patient
typed into the new system who then doesn't visit has no due date and never appears in the
recall list — so entering them achieved nothing. Bulk-typing the entire history buys
almost no recall value for a very large amount of work.

Rough shape of the problem, worth checking against the real MyOPD count:

| Approach | Volume | Time at ~15 s each | Recall value |
|---|---|---|---|
| Everyone ever registered | thousands | tens of hours | Low — most won't return soon |
| **Only those due back soon** | ~a few hundred | **an hour or two** | **High — these are the ones who'd be lost** |
| Everyone else, as they walk in | zero upfront | none | Full, from their next visit onward |

**The plan, in three parts:**

1. **Backlog pass, before go-live.** Working from MyOPD's own follow-up list, enter only
   patients due back within roughly the next two months. These are the only people who
   would actually fall through the crack on switchover.
2. **Organic migration, after go-live.** Everyone else is entered once, at the desk, on
   their next visit. Within a few months of normal working the active patient base has
   migrated itself, at zero extra effort.
3. **Keep MyOPD readable, don't keep using it.** Old history stays there for reference
   for a year. It is not re-typed and not synced — the new system starts from the patient's
   first visit under it.

**What the app must provide to make this bearable:**

- **Quick-add mode** — a stripped-down registration form: name, phone, language, last
  visit date, next visit due. Nothing else. Full details get filled in when the patient
  actually turns up. The difference between a 90-second form and a 15-second one decides
  whether the backlog pass gets finished.
- **Carry-forward visit** — a migrated patient needs a due date for recall to work, but
  has no visit in the new system. Entering one creates a visit marked
  `source = migrated`, dated to their last MyOPD visit, carrying the next-visit-due
  across. It is a real visit that happened; it was just recorded elsewhere. The UI shows
  "recorded in MyOPD" rather than blank clinical fields. **This requires no change to the
  recall query** — the whole recall system works for migrated patients immediately.
- **Duplicate detection on phone number.** Manual entry across two people and several
  weeks *will* create duplicates, and a duplicated patient means a missed recall. On
  registration, if the phone number already exists, show the existing patient and offer to
  open it instead of creating a second record.
  **Note:** the current app has a `findByPhone` query in the database layer but nothing
  calls it — there is no duplicate check on the registration form today. This is a real
  gap and it closes in Phase A.

### 4.12 Pharmacy hand-off — Phase D, not now

A third role on a third device. Once the doctor completes a visit, the pharmacy sees the
medicine list and the card, and marks it dispensed.

This falls out of §4.3 almost for free: once medicines are a structured list rather than
one free-text line, the pharmacy view is largely a read-only screen over data that already
exists. That is a good reason to get the medicine list right now.

Deliberately last. The clinic runs today without it.

---

## 5. Revised data model

Changes marked. Unmarked tables are unchanged from v2.

```
app_user                                                          [NEW — now real]
  id, username, password_hash, display_name,
  role (doctor | reception | pharmacy), active,
  ui_language (en | ta)                                          [NEW]

patient
  id, name, dob_or_age, sex, address, phone, alternate_phone,
  referred_by, occupation, blood_group, current_medication,
  preferred_language(en|ta), created_at, archived,
  migrated_from_myopd (bool)                                     [NEW]

visit                                                             [CHANGED]
  id, patient_id, visit_date, complaint, next_visit_due, created_at,
  status (waiting | in_consultation | completed),               [NEW]
  card_id,                                                      [NEW]
  source (recorded | migrated),                                 [NEW]
  opened_by_user_id, completed_by_user_id                       [NEW]
  -- remedy_given_text / potency / advice_given replaced by
  --   visit_medicine rows + the chosen card

visit_vitals                                                      [NEW]
  visit_id, height_cm, weight_kg, bp_systolic, bp_diastolic,
  pulse (nullable), recorded_by_user_id, recorded_at

visit_medicine                                                    [NEW]
  id, visit_id, medicine_name, potency,
  form (pills | drops), quantity, sort_order
  -- any number per visit

card                                                              [NEW]
  id, code, label_en, label_ta, body_en, body_ta, sort_order, active
  -- the six printed cards, editable in Settings

investigation                                                     [NEW]
  id, patient_id, visit_id, kind, title, taken_on, note, created_by

investigation_file                                                [NEW]
  id, investigation_id, file_path, page_no

appointment                                                       [NEW]
  id, patient_id, appointment_date, note, status, created_by, created_at

photo                                                             (unchanged)
  id, visit_id, file_path, caption, taken_at

message_template                                                  [CHANGED]
  id, template_key, language(en|ta), body_text, updated_at
  -- template_key extended to six values

message_log                                                       [CHANGED]
  id, patient_id, template_key, language, sent_at, channel, status,
  sent_by_user_id                                                [NEW]

invoice                                                           (unchanged)
  id, visit_id, consultation_fee, medicine_charge, total,
  payment_mode(cash|upi), payment_status, paid_at

clinic_profile                                                    [NEW]
  single row: name, address, landmark, map_link, phone,
  doctor_name, doctor_qualifications, working_hours,
  review_link, upi_id
```

Client-generated UUIDs stay, and matter more than in v2 — two devices creating records
against one server is exactly the case they exist for.

---

## 6. Permission matrix

| | Reception | Doctor | Pharmacy (D) |
|---|---|---|---|
| Register / edit patient | ✅ | ✅ | ❌ |
| Record vitals | ✅ | ✅ | ❌ |
| Start visit (queue patient) | ✅ | ✅ | ❌ |
| Complaint, medicines, card | ❌ | ✅ | view only |
| Next visit due | ❌ | ✅ | ❌ |
| Fees and payment | ❌ | ✅ | ❌ |
| Attach investigation | ✅ | ✅ | ❌ |
| Book appointment, send messages | ✅ | ✅ | ❌ |
| Edit templates / cards / clinic settings | ❌ | ✅ | ❌ |
| Mark dispensed | ❌ | ❌ | ✅ |

Fees are doctor-only, confirmed. Worth revisiting once reception is using the app daily —
collecting money is usually a front-desk job, and this is the rule most likely to change
in practice.

---

## 7. What will be implemented, in what order

Every phase ships something usable on its own.

### Phase A — Cloud backend, auth, and the reception→doctor handoff
*Unblocks every other phase.*

- [ ] Provision the cloud VM, HTTPS, **automated daily database backups**
- [ ] FastAPI: auth with roles, patients, visits, vitals, invoices, templates, file upload
- [ ] Alembic migrations for the v3 model
- [ ] Android: login screen, role-aware navigation
- [ ] Android: server-as-truth with a local cache and an offline write queue
- [ ] Reception flow: register → vitals → start visit
- [ ] Doctor flow: **Waiting** queue on Today, open patient with vitals pre-filled
- [ ] **Language switch in Settings** — wire it up here so every later screen is built bilingual
- [ ] **Duplicate-phone check on registration** — closes an existing gap, and manual
      migration makes duplicates certain without it
- [ ] **Quick-add mode** + carry-forward visit, for the migration backlog pass (§4.11)

### Phase B — Consultation depth
- [ ] Vitals capture and display on the visit screen
- [ ] Medicine list (any number) replacing the single remedy field
- [ ] The six cards, seeded with English + Tamil text and editable in Settings
- [ ] Share the card to the patient's WhatsApp in their language
- [ ] Investigations: capture, group by title, compare two dates side by side

### Phase C — Messaging and the front desk
- [ ] Clinic profile in Settings
- [ ] Four new templates seeded with the doctor's existing wording, EN + TA
- [ ] Appointments: book, confirm, remind, show on Today, auto-mark attended
- [ ] UPI QR with the amount at billing

### Phase D — Pharmacy
- [ ] Pharmacy role and login
- [ ] Dispensing list per completed visit — medicines plus the chosen card
- [ ] Mark as dispensed

### Not scheduled
- Web/desktop app for the clinic laptop (v2 Stage 2 — still the right end state, but the
  Android app is what he is testing now, and the cloud backend makes it easy to add later)
- Reports: new vs follow-up counts, monthly revenue, recall conversion
- CSV export for the accountant
- WhatsApp Business API automation

---

## 8. Still explicitly out of scope

- Physical card printing (superseded by sharing the card on WhatsApp)
- Remedy repertory / materia medica reference
- Multi-doctor scheduling
- Appointment time slots — the clinic is walk-in between 6:00 and 8:30 pm
- Pharmacy stock and inventory — the Phase D role *dispenses*, it does not track stock
  levels. That is a much larger product and he has not asked for it.
- Rebuilding MyOPD's full feature set

---

## 9. Open items

**No open questions block Phase A.** Every architectural decision is settled.

Things to settle during Phase A, not before:

1. **How many patients are actually due back soon?** This is the size of the migration
   backlog pass (§4.11), and it is the one number that decides whether go-live is a
   two-hour job or a two-week one. Check MyOPD's follow-up list and count.
2. **Who else needs a login** — exactly one receptionist account, or do different people
   work the desk on different evenings? Separate logins make `message_log` and
   `recorded_by` genuinely useful for tracing who did what.
3. **Backup restore drill** — once backups run, restore one into a scratch database and
   confirm it actually works. An untested backup is not a backup.
4. **Go-live date and switchover** — MyOPD stays readable for reference for about a year,
   but there should be one clear date after which all new visits are recorded only in the
   new app. Running both in parallel "just for a while" is how data ends up split across
   two systems permanently.

---

## 10. What still governs everything

Unchanged from v2, and more relevant now that scope has grown:

1. **His time budget.** ~4 minutes per patient today. The reception split should *reduce*
   his typing, not add screens. If the doctor's half of the visit takes longer after this
   revision than before it, the revision has failed.
2. **Recall is still the product.** Vitals, pharmacy, appointments and investigations are
   all genuinely useful, but none of them is the reason to leave MyOPD. If recall ever
   becomes unreliable while adding them, stop and fix recall.
3. **Don't build fields he doesn't use.** The six cards exist because he already uses them
   on paper. The medicine-per-card-line idea was dropped for exactly this reason. Nothing
   should be added on speculation.
4. **Sit with him again after Phase A.** The reception→doctor handoff is the change most
   likely to be subtly wrong in real use — watch one real evening clinic before building B.
5. **Migration effort is the project's main delivery risk.** Not the code. A manual
   re-entry pass that turns out to be forty hours of typing is how this stalls three weeks
   before go-live. Keep it to the due-soon slice and let the rest migrate organically (§4.11).
