import enum
import uuid
from datetime import datetime

from sqlalchemy import Column, DateTime, Enum, ForeignKey, String, Text
from sqlalchemy.dialects.postgresql import UUID

from app.core.database import Base
from app.models.patient import Language


class TemplateKey(str, enum.Enum):
    """Spec §4.6 — the six messages in real daily use."""
    welcome = "welcome"
    recall = "recall"
    appointment_confirmation = "appointment_confirmation"
    appointment_reminder = "appointment_reminder"
    medicine_dispatched = "medicine_dispatched"
    review_request = "review_request"


class MessageChannel(str, enum.Enum):
    whatsapp_manual = "whatsapp_manual"
    whatsapp_api = "whatsapp_api"
    sms = "sms"


class MessageTemplate(Base):
    __tablename__ = "message_templates"

    template_key = Column(Enum(TemplateKey), primary_key=True)
    language = Column(Enum(Language), primary_key=True)
    body_text = Column(Text, nullable=False)
    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow)


class MessageLog(Base):
    """Records that a message was sent — without this the Phase 2 recall
    conversion report is impossible to build later."""

    __tablename__ = "message_log"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    patient_id = Column(
        UUID(as_uuid=True),
        ForeignKey("patients.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    template_key = Column(Enum(TemplateKey), nullable=False)
    language = Column(Enum(Language), nullable=False)
    channel = Column(
        Enum(MessageChannel), nullable=False, default=MessageChannel.whatsapp_manual
    )
    status = Column(String(30), nullable=False, default="sent")
    sent_by_user_id = Column(
        UUID(as_uuid=True), ForeignKey("app_users.id"), nullable=True
    )
    sent_at = Column(DateTime, nullable=False, default=datetime.utcnow, index=True)
