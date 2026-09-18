import uuid
import enum
from datetime import datetime, date

from sqlalchemy import Column, String, Integer, Date, DateTime, Boolean, Enum, Text
from sqlalchemy.dialects.postgresql import UUID

from app.core.database import Base


class Sex(str, enum.Enum):
    male = "male"
    female = "female"
    other = "other"


class Language(str, enum.Enum):
    en = "en"
    ta = "ta"


class Patient(Base):
    __tablename__ = "patients"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    name = Column(String(150), nullable=False, index=True)
    phone = Column(String(20), nullable=False, index=True)
    alternate_phone = Column(String(20), nullable=True)

    sex = Column(Enum(Sex), nullable=False)
    date_of_birth = Column(Date, nullable=True)
    age_years = Column(Integer, nullable=True)
    age_recorded_on = Column(Date, nullable=True)

    address = Column(Text, nullable=True)
    occupation = Column(String(100), nullable=True)
    blood_group = Column(String(10), nullable=True)
    referred_by = Column(String(150), nullable=True)
    current_medication = Column(Text, nullable=True)

    preferred_language = Column(
        Enum(Language), nullable=False, default=Language.en
    )

    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    archived = Column(Boolean, nullable=False, default=False)