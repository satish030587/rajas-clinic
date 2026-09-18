import enum
import uuid
from datetime import datetime

from sqlalchemy import (
    Column, Date, DateTime, Enum, ForeignKey, Integer, String, Text
)
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship

from app.core.database import Base


class InvestigationKind(str, enum.Enum):
    scan = "scan"
    lab = "lab"
    xray = "xray"
    ecg = "ecg"
    other = "other"


class Investigation(Base):
    """
    An outside report the patient brings in (spec §4.5). Grouped by `title` so
    the same study across months — "Kidney USG" in March and in June — can be
    compared side by side.
    """

    __tablename__ = "investigations"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    patient_id = Column(
        UUID(as_uuid=True),
        ForeignKey("patients.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    # Reports also arrive between visits, so this is optional.
    visit_id = Column(
        UUID(as_uuid=True), ForeignKey("visits.id", ondelete="SET NULL"), nullable=True
    )

    kind = Column(
        Enum(InvestigationKind), nullable=False, default=InvestigationKind.scan
    )
    title = Column(String(150), nullable=False, index=True)
    taken_on = Column(Date, nullable=False)
    note = Column(Text, nullable=True)

    created_by_user_id = Column(
        UUID(as_uuid=True), ForeignKey("app_users.id"), nullable=True
    )
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)

    files = relationship(
        "InvestigationFile", back_populates="investigation",
        cascade="all, delete-orphan", order_by="InvestigationFile.page_no",
    )


class InvestigationFile(Base):
    __tablename__ = "investigation_files"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    investigation_id = Column(
        UUID(as_uuid=True),
        ForeignKey("investigations.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    file_path = Column(String(500), nullable=False)
    page_no = Column(Integer, nullable=False, default=1)

    investigation = relationship("Investigation", back_populates="files")
