import enum
import uuid
from datetime import datetime

from sqlalchemy import Column, Date, DateTime, Enum, ForeignKey, Text
from sqlalchemy.dialects.postgresql import UUID

from app.core.database import Base


class AppointmentStatus(str, enum.Enum):
    booked = "booked"
    attended = "attended"
    cancelled = "cancelled"
    no_show = "no_show"


class Appointment(Base):
    """
    A date a patient confirmed over the phone (spec §4.8).

    Deliberately separate from `visit.next_visit_due`: a recall is a date the
    patient is *due*, an appointment is a date they *said they would come*.
    No time slots — the clinic is walk-in between 6:00 and 8:30 pm.
    """

    __tablename__ = "appointments"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    patient_id = Column(
        UUID(as_uuid=True),
        ForeignKey("patients.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    appointment_date = Column(Date, nullable=False, index=True)
    note = Column(Text, nullable=True)
    status = Column(
        Enum(AppointmentStatus), nullable=False, default=AppointmentStatus.booked
    )

    created_by_user_id = Column(
        UUID(as_uuid=True), ForeignKey("app_users.id"), nullable=True
    )
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
