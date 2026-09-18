import enum
import uuid
from datetime import datetime

from sqlalchemy import Boolean, Column, DateTime, Enum, String
from sqlalchemy.dialects.postgresql import UUID

from app.core.database import Base


class Role(str, enum.Enum):
    doctor = "doctor"
    reception = "reception"
    pharmacy = "pharmacy"


class UiLanguage(str, enum.Enum):
    en = "en"
    ta = "ta"


class AppUser(Base):
    __tablename__ = "app_users"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    username = Column(String(50), nullable=False, unique=True, index=True)
    password_hash = Column(String(255), nullable=False)
    display_name = Column(String(100), nullable=False)
    role = Column(Enum(Role), nullable=False)

    # Spec §4.7: the app's own language is per user, and is independent of the
    # language a patient's messages go out in.
    ui_language = Column(Enum(UiLanguage), nullable=False, default=UiLanguage.en)

    active = Column(Boolean, nullable=False, default=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
