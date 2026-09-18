import uuid
from datetime import date, datetime
from decimal import Decimal
from typing import List, Optional

from pydantic import BaseModel, ConfigDict, Field, computed_field

from app.models.invoice import PaymentMode
from app.models.visit import MedicineForm, VisitSource, VisitStatus


# --- Vitals (reception) ---

class VitalsBase(BaseModel):
    height_cm: Optional[Decimal] = Field(None, ge=0, le=300)
    weight_kg: Optional[Decimal] = Field(None, ge=0, le=500)
    bp_systolic: Optional[int] = Field(None, ge=40, le=300)
    bp_diastolic: Optional[int] = Field(None, ge=20, le=200)
    pulse: Optional[int] = Field(None, ge=20, le=250)


class VitalsWrite(VitalsBase):
    pass


class VitalsResponse(VitalsBase):
    model_config = ConfigDict(from_attributes=True)

    recorded_at: datetime


# --- Medicines (doctor) ---

class MedicineBase(BaseModel):
    medicine_name: str = Field(..., min_length=1, max_length=150)
    potency: Optional[str] = Field(None, max_length=30)
    form: MedicineForm = MedicineForm.pills
    quantity: Optional[str] = Field(None, max_length=50)


class MedicineWrite(MedicineBase):
    id: Optional[uuid.UUID] = None


class MedicineResponse(MedicineBase):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    sort_order: int


# --- Billing (doctor) ---

class InvoiceWrite(BaseModel):
    consultation_fee: int = Field(0, ge=0)
    medicine_charge: int = Field(0, ge=0)
    payment_mode: PaymentMode = PaymentMode.cash
    paid: bool = True


class InvoiceResponse(InvoiceWrite):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    paid_at: Optional[datetime] = None

    @computed_field
    @property
    def total(self) -> int:
        return self.consultation_fee + self.medicine_charge


# --- Visits ---

class VisitStart(BaseModel):
    """Reception queues a patient. Clinical fields are deliberately absent."""
    id: Optional[uuid.UUID] = None      # client-generated UUID
    patient_id: uuid.UUID
    visit_date: Optional[date] = None
    vitals: Optional[VitalsWrite] = None


class VisitClinicalUpdate(BaseModel):
    """The doctor's half of the visit (spec §4.1)."""
    model_config = ConfigDict(extra="forbid")

    complaint: Optional[str] = None
    card_id: Optional[uuid.UUID] = None
    next_visit_due: Optional[date] = None
    medicines: Optional[List[MedicineWrite]] = None
    invoice: Optional[InvoiceWrite] = None
    complete: bool = False


class MigratedVisitCreate(BaseModel):
    """
    Carry-forward visit for a patient re-typed from MyOPD (spec §4.11).
    Records the last known visit and due date so recall works immediately,
    without inventing clinical detail that was never captured here.
    """
    id: Optional[uuid.UUID] = None
    patient_id: uuid.UUID
    visit_date: date
    next_visit_due: Optional[date] = None


class VisitResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    patient_id: uuid.UUID
    visit_date: date
    status: VisitStatus
    source: VisitSource
    complaint: Optional[str] = None
    card_id: Optional[uuid.UUID] = None
    next_visit_due: Optional[date] = None
    created_at: datetime

    vitals: Optional[VitalsResponse] = None
    medicines: List[MedicineResponse] = []
    invoice: Optional[InvoiceResponse] = None


class VisitReceptionResponse(BaseModel):
    """
    What reception is allowed to see (spec §6): no complaint, no medicines,
    no fees. Enforced by returning a different shape, not by hiding fields in
    the UI — the data never leaves the server.
    """

    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    patient_id: uuid.UUID
    visit_date: date
    status: VisitStatus
    source: VisitSource
    next_visit_due: Optional[date] = None
    created_at: datetime
    vitals: Optional[VitalsResponse] = None
