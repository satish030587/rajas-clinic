import enum
import uuid
from datetime import datetime

from sqlalchemy import Boolean, Column, DateTime, Enum, ForeignKey, Integer
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship

from app.core.database import Base


class PaymentMode(str, enum.Enum):
    cash = "cash"
    upi = "upi"


class Invoice(Base):
    __tablename__ = "invoices"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    visit_id = Column(
        UUID(as_uuid=True),
        ForeignKey("visits.id", ondelete="CASCADE"),
        nullable=False,
        unique=True,
    )
    consultation_fee = Column(Integer, nullable=False, default=0)
    medicine_charge = Column(Integer, nullable=False, default=0)
    payment_mode = Column(Enum(PaymentMode), nullable=False, default=PaymentMode.cash)
    paid = Column(Boolean, nullable=False, default=True)
    paid_at = Column(DateTime, nullable=True)

    visit = relationship("Visit", back_populates="invoice")

    @property
    def total(self) -> int:
        return (self.consultation_fee or 0) + (self.medicine_charge or 0)
