"""
Seeds the reference content the clinic can't work without: the six dosage cards
and the six message templates, in English and Tamil.

Wording is the doctor's own, taken from what he already sends and prints today —
he should not have to retype it. All of it is editable in the app afterwards
(spec §4.3, §4.6), so this is a starting point, not a fixed list.

Run:  python -m app.seed
"""

import sys

from sqlalchemy.orm import Session

from app.core.database import SessionLocal
from app.core.security import hash_password
from app.models.card import Card
from app.models.clinic import ClinicProfile
from app.models.message import Language, MessageTemplate, TemplateKey
from app.models.user import AppUser, Role

FOOTER_EN = (
    "Store medicine in an air-tight container.\n"
    "Keep away from sunlight."
)
FOOTER_TA = (
    "மருந்தை காற்று புகாதவாறு மூடி வைக்கவும்.\n"
    "சூரிய ஒளி படாத இடத்தில் வைக்கவும்."
)

# The instruction lines the six printed cards are built from.
LINES = {
    "empty_stomach": (
        "Take 4 pills daily, on an empty stomach. Place the pills in the cap "
        "without touching them. Put the pills on your tongue and let them "
        "dissolve without drinking water.",
        "காலையில் வெறும் வயிற்றில் தண்ணீர் குடிக்காமல் நான்கு மாத்திரைகளைத் "
        "தொடாமல் மூடியில் எடுத்து நாக்கில் வைத்து உமிழ்நீரால் கரைய விடவும்.",
    ),
    "before_food": (
        "Take 4 pills daily, three times a day, 10 min before meals. Place the "
        "pills in the cap without touching them. Put the pills on your tongue "
        "and let them dissolve without drinking water.",
        "உணவுக்கு 10 நிமிடம் முன், தினமும் மூன்று வேளை, 4 மாத்திரைகள் எடுத்துக் "
        "கொள்ளவும். மாத்திரைகளை தொடாமல் மூடியில் வைக்கவும். நாக்கில் வைத்து "
        "தண்ணீர் குடிக்காமல் கரைய விடவும்.",
    ),
    "after_food": (
        "Take 4 pills daily, three times a day, 10 min after meals. Place the "
        "pills in the cap without touching them. Put the pills on your tongue "
        "and let them dissolve without drinking water.",
        "உணவுக்கு 10 நிமிடத்திற்கு பின், தினமும் மூன்று வேளை, 4 மாத்திரைகள் "
        "எடுத்துக் கொள்ளவும். மாத்திரைகளை தொடாமல் மூடியில் வைக்கவும். நாக்கில் "
        "வைத்து தண்ணீர் குடிக்காமல் கரைய விடவும்.",
    ),
    "drops_10": (
        "Take 10 drops of medicine in 1/4 glass of lukewarm water, three times "
        "a day, 10 min after meals.",
        "உணவுக்கு 10 நிமிடத்திற்கு பின் தினமும் மூன்று வேளை ஒரு கால் கிளாஸ் "
        "வெதுவெதுப்பான தண்ணீரில் 10 துளிகள் மருந்தை எடுத்துக் கொள்ளவும்.",
    ),
    "drops_20": (
        "Take 20 drops of medicine in 1/4 glass of lukewarm water, three times "
        "a day, 10 min after meals.",
        "உணவுக்கு 20 நிமிடத்திற்கு பின் தினமும் மூன்று வேளை ஒரு கால் கிளாஸ் "
        "வெதுவெதுப்பான தண்ணீரில் 10 துளிகள் மருந்தை எடுத்துக் கொள்ளவும்.",
    ),
    "bedtime": (
        "Take 4 pills daily before going to sleep at night by putting them in "
        "the cap without touching them, and without drinking water keep them on "
        "the tongue until they dissolve.",
        "தூங்குவதற்கு முன், தினமும் 4 மாத்திரைகள் எடுத்துக் கொள்ளவும். "
        "மாத்திரைகளை தொடாமல் மூடியில் எடுத்து நாக்கில் வைத்து தண்ணீர் "
        "குடிக்காமல் கரைய விடவும்.",
    ),
}

# The six cards, as combinations of those lines.
CARDS = [
    ("card_1", "Card 1", "அட்டை 1", ["empty_stomach", "before_food", "drops_10"]),
    ("card_2", "Card 2", "அட்டை 2", ["empty_stomach", "before_food", "after_food"]),
    ("card_3", "Card 3", "அட்டை 3", ["before_food", "drops_10", "bedtime"]),
    ("card_4", "Card 4", "அட்டை 4", ["empty_stomach", "before_food", "after_food", "drops_10"]),
    ("card_5", "Card 5", "அட்டை 5",
     ["empty_stomach", "before_food", "drops_10", "bedtime"]),
    ("card_6", "Card 6", "அட்டை 6", ["empty_stomach", "before_food", "drops_20"]),
]

SIGNATURE_EN = (
    "\n\nProf. Dr. M. Ilayaraja\n"
    "B.H.M.S., M.D.(Homoeo)., Ph.D., M.B.A.(Hosp. Mang).,\n"
    "{clinic}"
)
SIGNATURE_TA = (
    "\n\nபேரா. டாக்டர் M. இளையராஜா\n"
    "B.H.M.S., M.D.(Homoeo)., Ph.D., M.B.A.(Hosp. Mang).,\n"
    "{clinic}"
)

TEMPLATES = {
    TemplateKey.welcome: (
        "Dear {name}, welcome to {clinic}. Thank you for visiting us today. "
        "Save this number for any questions about your treatment, and we will "
        "remind you when your next visit is due." + SIGNATURE_EN,
        "அன்புள்ள {name}, {clinic} நிலையத்திற்கு வரவேற்கிறோம். இன்று எங்களை "
        "நாடியதற்கு நன்றி. சிகிச்சை குறித்த சந்தேகங்களுக்கு இந்த எண்ணைச் "
        "சேமித்து வைக்கவும். அடுத்த வருகை நேரத்தில் நினைவூட்டுவோம்."
        + SIGNATURE_TA,
    ),
    TemplateKey.recall: (
        "Dear {name}, thanks for contacting us. This is a reminder that your "
        "next visit was due on {due_date}. Please visit our clinic any time "
        "between {working_hours}. Wishing you a speedy recovery.\n\n{map_link}"
        + SIGNATURE_EN,
        "அன்புள்ள {name}, {clinic} நிலையத்திலிருந்து நினைவூட்டல். உங்கள் அடுத்த "
        "வருகை {due_date} அன்று வர வேண்டியிருந்தது. {working_hours} நேரத்தில் "
        "வருகை தரவும். விரைவில் குணமடைய வாழ்த்துகள்.\n\n{map_link}"
        + SIGNATURE_TA,
    ),
    TemplateKey.appointment_confirmation: (
        "Hi {name},\n\nThis is a confirmation for your upcoming appointment "
        "with Prof. Dr. M. ILAYARAJA scheduled on {appointment_date} between "
        "{working_hours}.\n\nIf you need to reschedule or have any questions, "
        "please feel free to chat with us. We look forward to seeing you soon!"
        "\n\n{clinic_address}\nMobile: {clinic_phone}\n\n"
        "Click the link for location:\n{map_link}" + SIGNATURE_EN,
        "வணக்கம் {name},\n\nபேரா. டாக்டர் M. இளையராஜா அவர்களுடன் {appointment_date} "
        "அன்று {working_hours} நேரத்தில் உங்கள் சந்திப்பு உறுதி செய்யப்பட்டுள்ளது."
        "\n\nமாற்றம் தேவைப்பட்டால் எங்களைத் தொடர்பு கொள்ளவும். உங்களைச் சந்திக்க "
        "காத்திருக்கிறோம்!\n\n{clinic_address}\nதொலைபேசி: {clinic_phone}\n\n"
        "இடம் அறிய:\n{map_link}" + SIGNATURE_TA,
    ),
    TemplateKey.appointment_reminder: (
        "Dear {name},\n\nThanks for contacting us. This is a reminder that your "
        "appointment is scheduled on {appointment_date}. Please visit our clinic "
        "any time between {working_hours}. Wishing you a speedy recovery.\n\n"
        "{map_link}" + SIGNATURE_EN,
        "அன்புள்ள {name},\n\nஉங்கள் சந்திப்பு {appointment_date} அன்று "
        "திட்டமிடப்பட்டுள்ளது. {working_hours} நேரத்தில் வருகை தரவும். விரைவில் "
        "குணமடைய வாழ்த்துகள்.\n\n{map_link}" + SIGNATURE_TA,
    ),
    TemplateKey.medicine_dispatched: (
        "Good morning,\n\nYour medicine has been dispatched. For updates, please "
        "check your SMS (Message) for the tracking ID.\n\nCopy and paste the "
        "message into the Speed Post WhatsApp number to get updates on the "
        "tracking status.\n\nTracking ID: {tracking_id}\n\nWith Thanks & Regards"
        + SIGNATURE_EN,
        "காலை வணக்கம்,\n\nஉங்கள் மருந்து அனுப்பப்பட்டுள்ளது. தகவலுக்கு உங்கள் SMS-ல் "
        "உள்ள ட்ராக்கிங் ஐடியைப் பார்க்கவும்.\n\nஅந்தச் செய்தியை Speed Post WhatsApp "
        "எண்ணுக்கு அனுப்பி நிலையை அறியலாம்.\n\nட்ராக்கிங் ஐடி: {tracking_id}\n\n"
        "நன்றி" + SIGNATURE_TA,
    ),
    TemplateKey.review_request: (
        "{clinic} on Google Pay for Business is requesting you to review their "
        "business. Share your review on {review_link}",
        "{clinic} உங்கள் கருத்தைப் பகிரக் கேட்டுக்கொள்கிறது. உங்கள் விமர்சனத்தை "
        "இங்கே பதிவு செய்யவும்: {review_link}",
    ),
}


def seed_cards(db: Session) -> int:
    created = 0
    for order, (code, label_en, label_ta, line_keys) in enumerate(CARDS):
        if db.query(Card).filter(Card.code == code).first():
            continue
        body_en = "\n\n".join(LINES[k][0] for k in line_keys) + "\n\n" + FOOTER_EN
        body_ta = "\n\n".join(LINES[k][1] for k in line_keys) + "\n\n" + FOOTER_TA
        db.add(
            Card(
                code=code,
                label_en=label_en,
                label_ta=label_ta,
                body_en=body_en,
                body_ta=body_ta,
                sort_order=order,
            )
        )
        created += 1
    return created


def seed_templates(db: Session) -> int:
    created = 0
    for key, (body_en, body_ta) in TEMPLATES.items():
        for language, body in ((Language.en, body_en), (Language.ta, body_ta)):
            exists = (
                db.query(MessageTemplate)
                .filter(
                    MessageTemplate.template_key == key,
                    MessageTemplate.language == language,
                )
                .first()
            )
            if exists:
                continue
            db.add(
                MessageTemplate(
                    template_key=key, language=language, body_text=body
                )
            )
            created += 1
    return created


def seed_clinic(db: Session) -> bool:
    if db.query(ClinicProfile).first():
        return False
    # Real values are filled in from the app's Settings screen, not committed here.
    db.add(
        ClinicProfile(
            name="Rajas Homoeo Care",
            doctor_name="Prof. Dr. M. Ilayaraja",
            doctor_qualifications=(
                "B.H.M.S., M.D.(Homoeo)., Ph.D., M.B.A.(Hosp. Mang)."
            ),
            working_hours="6:00 pm to 8:30 pm",
        )
    )
    return True


def seed_doctor_account(db: Session, username: str, password: str) -> bool:
    if db.query(AppUser).filter(AppUser.username == username).first():
        return False
    db.add(
        AppUser(
            username=username,
            password_hash=hash_password(password),
            display_name="Dr M Ilayaraja",
            role=Role.doctor,
        )
    )
    return True


def main() -> None:
    db = SessionLocal()
    try:
        cards = seed_cards(db)
        templates = seed_templates(db)
        clinic = seed_clinic(db)

        doctor = False
        if len(sys.argv) == 3:
            doctor = seed_doctor_account(db, sys.argv[1], sys.argv[2])

        db.commit()
        print(f"cards seeded:     {cards}")
        print(f"templates seeded: {templates}")
        print(f"clinic profile:   {'created' if clinic else 'already present'}")
        if len(sys.argv) == 3:
            print(f"doctor account:   {'created' if doctor else 'already present'}")
        else:
            print("doctor account:   skipped "
                  "(run: python -m app.seed <username> <password>)")
    finally:
        db.close()


if __name__ == "__main__":
    main()
