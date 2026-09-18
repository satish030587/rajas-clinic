"""Cards, message templates and the clinic profile — the editable content
that lets wording change without a redeploy."""

import uuid
from datetime import date, datetime
from typing import List, Optional

from pydantic import BaseModel, ConfigDict, Field

from app.models.appointment import AppointmentStatus
from app.models.investigation import InvestigationKind
from app.models.message import MessageChannel, TemplateKey
from app.models.patient import Language


# --- Cards (the six printed dosage instructions) ---

class CardBase(BaseModel):
    label_en: str = Field(..., max_length=120)
    label_ta: str = Field(..., max_length=120)
    body_en: str
    body_ta: str
    sort_order: int = 0
    active: bool = True


class CardUpdate(BaseModel):
    model_config = ConfigDict(extra="forbid")

    label_en: Optional[str] = Field(None, max_length=120)
    label_ta: Optional[str] = Field(None, max_length=120)
    body_en: Optional[str] = None
    body_ta: Optional[str] = None
    sort_order: Optional[int] = None
    active: Optional[bool] = None


class CardResponse(CardBase):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    code: str


# --- Message templates ---

class TemplateUpdate(BaseModel):
    body_text: str = Field(..., min_length=1)


class TemplateResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    template_key: TemplateKey
    language: Language
    body_text: str
    updated_at: datetime


class MessageLogCreate(BaseModel):
    patient_id: uuid.UUID
    template_key: TemplateKey
    language: Language
    channel: MessageChannel = MessageChannel.whatsapp_manual


class MessageLogResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    patient_id: uuid.UUID
    template_key: TemplateKey
    language: Language
    channel: MessageChannel
    sent_at: datetime


# --- Clinic profile ---

class ClinicProfileWrite(BaseModel):
    model_config = ConfigDict(extra="forbid")

    name: Optional[str] = Field(None, max_length=150)
    doctor_name: Optional[str] = Field(None, max_length=150)
    doctor_qualifications: Optional[str] = Field(None, max_length=255)
    phone: Optional[str] = Field(None, max_length=20)
    address: Optional[str] = None
    landmark: Optional[str] = None
    map_link: Optional[str] = None
    working_hours: Optional[str] = None
    review_link: Optional[str] = None
    upi_id: Optional[str] = Field(None, max_length=100)
    default_consultation_fee: Optional[int] = Field(None, ge=0)


class ClinicProfileResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    name: str
    doctor_name: str
    doctor_qualifications: str
    phone: str
    address: str
    landmark: str
    map_link: str
    working_hours: str
    review_link: str
    upi_id: str
    default_consultation_fee: int


# --- Investigations ---

class InvestigationCreate(BaseModel):
    id: Optional[uuid.UUID] = None
    patient_id: uuid.UUID
    visit_id: Optional[uuid.UUID] = None
    kind: InvestigationKind = InvestigationKind.scan
    title: str = Field(..., min_length=1, max_length=150)
    taken_on: date
    note: Optional[str] = None


class InvestigationFileResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    file_path: str
    page_no: int


class InvestigationResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    patient_id: uuid.UUID
    visit_id: Optional[uuid.UUID] = None
    kind: InvestigationKind
    title: str
    taken_on: date
    note: Optional[str] = None
    created_at: datetime
    files: List[InvestigationFileResponse] = []


# --- Appointments ---

class AppointmentCreate(BaseModel):
    id: Optional[uuid.UUID] = None
    patient_id: uuid.UUID
    appointment_date: date
    note: Optional[str] = None


class AppointmentUpdate(BaseModel):
    model_config = ConfigDict(extra="forbid")

    appointment_date: Optional[date] = None
    note: Optional[str] = None
    status: Optional[AppointmentStatus] = None


class AppointmentResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    patient_id: uuid.UUID
    appointment_date: date
    note: Optional[str] = None
    status: AppointmentStatus
    created_at: datetime
