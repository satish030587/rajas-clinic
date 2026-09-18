"""Every model is imported here so Alembic autogenerate sees the full metadata."""

from app.models.patient import Patient, Sex, Language
from app.models.user import AppUser, Role, UiLanguage
from app.models.card import Card
from app.models.visit import (
    Visit, VisitStatus, VisitSource, VisitVitals, VisitMedicine, MedicineForm,
)
from app.models.invoice import Invoice, PaymentMode
from app.models.photo import Photo
from app.models.investigation import (
    Investigation, InvestigationFile, InvestigationKind,
)
from app.models.appointment import Appointment, AppointmentStatus
from app.models.message import (
    MessageTemplate, MessageLog, TemplateKey, MessageChannel,
)
from app.models.clinic import ClinicProfile

__all__ = [
    "Patient", "Sex", "Language",
    "AppUser", "Role", "UiLanguage",
    "Card",
    "Visit", "VisitStatus", "VisitSource", "VisitVitals", "VisitMedicine",
    "MedicineForm",
    "Invoice", "PaymentMode",
    "Photo",
    "Investigation", "InvestigationFile", "InvestigationKind",
    "Appointment", "AppointmentStatus",
    "MessageTemplate", "MessageLog", "TemplateKey", "MessageChannel",
    "ClinicProfile",
]
