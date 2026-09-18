import uuid
from datetime import datetime

from sqlalchemy import Column, DateTime, ForeignKey, String, Text
from sqlalchemy.dialects.postgresql import UUID

from app.core.database import Base


class Photo(Base):
    """Clinical/condition photos taken during a visit — distinct from
    investigations, which are reports the patient brings from outside."""

    __tablename__ = "visit_photos"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    visit_id = Column(
        UUID(as_uuid=True),
        ForeignKey("visits.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    file_path = Column(String(500), nullable=False)
    caption = Column(Text, nullable=True)
    taken_at = Column(DateTime, nullable=False, default=datetime.utcnow)
