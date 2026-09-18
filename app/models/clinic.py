import uuid
from datetime import datetime

from sqlalchemy import Column, DateTime, Integer, String, Text
from sqlalchemy.dialects.postgresql import UUID

from app.core.database import Base


class ClinicProfile(Base):
    """
    Single row (spec §4.9). Exists so message templates stop hard-coding the
    clinic's address, hours and links — and so none of it lives in source.
    """

    __tablename__ = "clinic_profile"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    name = Column(String(150), nullable=False, default="Rajas Homoeo Care")
    doctor_name = Column(String(150), nullable=False, default="")
    doctor_qualifications = Column(String(255), nullable=False, default="")
    phone = Column(String(20), nullable=False, default="")
    address = Column(Text, nullable=False, default="")
    landmark = Column(Text, nullable=False, default="")
    map_link = Column(Text, nullable=False, default="")
    working_hours = Column(Text, nullable=False, default="")
    review_link = Column(Text, nullable=False, default="")
    upi_id = Column(String(100), nullable=False, default="")
    default_consultation_fee = Column(Integer, nullable=False, default=200)

    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow)
