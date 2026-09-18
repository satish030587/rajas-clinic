import uuid
from datetime import datetime

from sqlalchemy import Boolean, Column, DateTime, Integer, String, Text
from sqlalchemy.dialects.postgresql import UUID

from app.core.database import Base


class Card(Base):
    """
    One of the six printed dosage cards (spec §4.3).

    The doctor hands a patient any number of medicines but exactly one card, so
    the card lives on the visit, not on the medicine.
    """

    __tablename__ = "cards"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    code = Column(String(30), nullable=False, unique=True)

    label_en = Column(String(120), nullable=False)
    label_ta = Column(String(120), nullable=False)
    body_en = Column(Text, nullable=False)
    body_ta = Column(Text, nullable=False)

    sort_order = Column(Integer, nullable=False, default=0)
    active = Column(Boolean, nullable=False, default=True)
    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow)
