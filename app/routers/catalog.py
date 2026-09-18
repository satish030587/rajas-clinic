import uuid
from datetime import datetime
from typing import List

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.deps import clinical_staff, doctor_only
from app.models.card import Card
from app.models.clinic import ClinicProfile
from app.models.message import Language, MessageLog, MessageTemplate, TemplateKey
from app.models.user import AppUser
from app.schemas.catalog import (
    CardResponse, CardUpdate, ClinicProfileResponse, ClinicProfileWrite,
    MessageLogCreate, MessageLogResponse, TemplateResponse, TemplateUpdate,
)

router = APIRouter(tags=["catalog"])


# --- Cards: the six printed dosage instructions ---

@router.get("/cards", response_model=List[CardResponse])
def list_cards(
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    return (
        db.query(Card)
        .filter(Card.active.is_(True))
        .order_by(Card.sort_order)
        .all()
    )


@router.patch("/cards/{card_id}", response_model=CardResponse)
def update_card(
    card_id: uuid.UUID,
    data: CardUpdate,
    _: AppUser = Depends(doctor_only),
    db: Session = Depends(get_db),
):
    card = db.get(Card, card_id)
    if card is None:
        raise HTTPException(status_code=404, detail="Card not found")
    for field, value in data.model_dump(exclude_unset=True).items():
        setattr(card, field, value)
    card.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(card)
    return card


# --- Message templates ---

@router.get("/templates", response_model=List[TemplateResponse])
def list_templates(
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    return db.query(MessageTemplate).all()


@router.put("/templates/{key}/{language}", response_model=TemplateResponse)
def update_template(
    key: TemplateKey,
    language: Language,
    data: TemplateUpdate,
    _: AppUser = Depends(doctor_only),
    db: Session = Depends(get_db),
):
    template = (
        db.query(MessageTemplate)
        .filter(
            MessageTemplate.template_key == key,
            MessageTemplate.language == language,
        )
        .first()
    )
    if template is None:
        template = MessageTemplate(template_key=key, language=language, body_text="")
        db.add(template)
    template.body_text = data.body_text
    template.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(template)
    return template


@router.post("/messages/log", response_model=MessageLogResponse, status_code=201)
def log_message(
    data: MessageLogCreate,
    user: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    """Records that a message went out — the basis for the recall-conversion report."""
    log = MessageLog(**data.model_dump(), sent_by_user_id=user.id)
    db.add(log)
    db.commit()
    db.refresh(log)
    return log


@router.get("/messages/patient/{patient_id}", response_model=List[MessageLogResponse])
def message_history(
    patient_id: uuid.UUID,
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    return (
        db.query(MessageLog)
        .filter(MessageLog.patient_id == patient_id)
        .order_by(MessageLog.sent_at.desc())
        .all()
    )


# --- Clinic profile ---

def _profile(db: Session) -> ClinicProfile:
    profile = db.query(ClinicProfile).first()
    if profile is None:
        profile = ClinicProfile()
        db.add(profile)
        db.commit()
        db.refresh(profile)
    return profile


@router.get("/clinic", response_model=ClinicProfileResponse)
def get_clinic(
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    return _profile(db)


@router.patch("/clinic", response_model=ClinicProfileResponse)
def update_clinic(
    data: ClinicProfileWrite,
    _: AppUser = Depends(doctor_only),
    db: Session = Depends(get_db),
):
    profile = _profile(db)
    for field, value in data.model_dump(exclude_unset=True).items():
        setattr(profile, field, value)
    profile.updated_at = datetime.utcnow()
    db.commit()
    db.refresh(profile)
    return profile
