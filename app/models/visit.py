import enum
import uuid
from datetime import datetime

from sqlalchemy import (
    Column, Date, DateTime, Enum, ForeignKey, Integer, Numeric, String, Text
)
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship

from app.core.database import Base


class VisitStatus(str, enum.Enum):
    """Spec §4.1 — the reception → doctor handoff."""
    waiting = "waiting"          # reception has taken vitals, patient is in the queue
    in_consultation = "in_consultation"
    completed = "completed"


class VisitSource(str, enum.Enum):
    recorded = "recorded"        # entered in this app
    migrated = "migrated"        # carried forward from MyOPD (spec §4.11)


class MedicineForm(str, enum.Enum):
    pills = "pills"
    drops = "drops"


class Visit(Base):
    __tablename__ = "visits"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    patient_id = Column(
        UUID(as_uuid=True),
        ForeignKey("patients.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    visit_date = Column(Date, nullable=False, index=True)

    # Doctor's half
    complaint = Column(Text, nullable=True)
    card_id = Column(UUID(as_uuid=True), ForeignKey("cards.id"), nullable=True)

    # Spec §3: the single field the whole recall system runs on.
    next_visit_due = Column(Date, nullable=True, index=True)

    status = Column(
        Enum(VisitStatus), nullable=False, default=VisitStatus.waiting, index=True
    )
    source = Column(Enum(VisitSource), nullable=False, default=VisitSource.recorded)

    opened_by_user_id = Column(
        UUID(as_uuid=True), ForeignKey("app_users.id"), nullable=True
    )
    completed_by_user_id = Column(
        UUID(as_uuid=True), ForeignKey("app_users.id"), nullable=True
    )

    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    vitals = relationship(
        "VisitVitals", back_populates="visit", uselist=False,
        cascade="all, delete-orphan",
    )
    medicines = relationship(
        "VisitMedicine", back_populates="visit",
        cascade="all, delete-orphan", order_by="VisitMedicine.sort_order",
    )
    invoice = relationship(
        "Invoice", back_populates="visit", uselist=False,
        cascade="all, delete-orphan",
    )
    card = relationship("Card")


class VisitVitals(Base):
    """Recorded by reception before the doctor sees the patient (spec §4.2)."""

    __tablename__ = "visit_vitals"

    visit_id = Column(
        UUID(as_uuid=True),
        ForeignKey("visits.id", ondelete="CASCADE"),
        primary_key=True,
    )
    height_cm = Column(Numeric(5, 1), nullable=True)
    weight_kg = Column(Numeric(5, 1), nullable=True)
    bp_systolic = Column(Integer, nullable=True)
    bp_diastolic = Column(Integer, nullable=True)
    pulse = Column(Integer, nullable=True)  # optional, confirmed by the doctor

    recorded_by_user_id = Column(
        UUID(as_uuid=True), ForeignKey("app_users.id"), nullable=True
    )
    recorded_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    visit = relationship("Visit", back_populates="vitals")


class VisitMedicine(Base):
    """
    Any number per visit (spec §4.3). Medicines are not bound to individual card
    lines — the one card covers the whole day's schedule.
    """

    __tablename__ = "visit_medicines"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    visit_id = Column(
        UUID(as_uuid=True),
        ForeignKey("visits.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    medicine_name = Column(String(150), nullable=False)
    potency = Column(String(30), nullable=True)
    form = Column(Enum(MedicineForm), nullable=False, default=MedicineForm.pills)
    quantity = Column(String(50), nullable=True)
    sort_order = Column(Integer, nullable=False, default=0)

    visit = relationship("Visit", back_populates="medicines")
