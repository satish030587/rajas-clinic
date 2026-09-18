import uuid
from datetime import date, datetime
from typing import List, Union

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.deps import clinical_staff, doctor_only, get_current_user
from app.models.appointment import Appointment, AppointmentStatus
from app.models.invoice import Invoice
from app.models.patient import Patient
from app.models.user import AppUser, Role
from app.models.visit import (
    MedicineForm, Visit, VisitMedicine, VisitSource, VisitStatus, VisitVitals,
)
from app.schemas.visit import (
    MigratedVisitCreate, VisitClinicalUpdate, VisitReceptionResponse,
    VisitResponse, VisitStart, VitalsWrite,
)

router = APIRouter(prefix="/visits", tags=["visits"])


def _visible(visit: Visit, user: AppUser):
    """Reception never receives clinical fields or fees (spec §6)."""
    if user.role == Role.reception:
        return VisitReceptionResponse.model_validate(visit)
    return VisitResponse.model_validate(visit)


ResponseUnion = Union[VisitResponse, VisitReceptionResponse]


@router.get("/queue", response_model=List[ResponseUnion])
def waiting_queue(
    user: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    """
    Who is in the waiting room right now, in arrival order. This is what the
    doctor's Today screen opens onto (spec §4.1).
    """
    visits = (
        db.query(Visit)
        .filter(
            Visit.visit_date == date.today(),
            Visit.status.in_([VisitStatus.waiting, VisitStatus.in_consultation]),
        )
        .order_by(Visit.created_at.asc())
        .all()
    )
    return [_visible(v, user) for v in visits]


@router.get("/patient/{patient_id}", response_model=List[ResponseUnion])
def visits_for_patient(
    patient_id: uuid.UUID,
    user: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    visits = (
        db.query(Visit)
        .filter(Visit.patient_id == patient_id)
        .order_by(Visit.visit_date.desc(), Visit.created_at.desc())
        .all()
    )
    return [_visible(v, user) for v in visits]


@router.get("/{visit_id}", response_model=ResponseUnion)
def get_visit(
    visit_id: uuid.UUID,
    user: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    visit = db.get(Visit, visit_id)
    if visit is None:
        raise HTTPException(status_code=404, detail="Visit not found")
    return _visible(visit, user)


@router.post("/start", response_model=ResponseUnion, status_code=201)
def start_visit(
    data: VisitStart,
    user: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    """
    Reception queues a patient, optionally with vitals already taken.
    Clinical fields are not accepted here by design.
    """
    if db.get(Patient, data.patient_id) is None:
        raise HTTPException(status_code=404, detail="Patient not found")

    visit_date = data.visit_date or date.today()

    visit = Visit(
        patient_id=data.patient_id,
        visit_date=visit_date,
        status=VisitStatus.waiting,
        source=VisitSource.recorded,
    )
    if data.id:
        visit.id = data.id
    db.add(visit)
    db.flush()

    if data.vitals:
        db.add(
            VisitVitals(
                visit_id=visit.id,
                recorded_by_user_id=user.id,
                **data.vitals.model_dump(),
            )
        )

    # An appointment booked for today is fulfilled by the patient turning up.
    appointment = (
        db.query(Appointment)
        .filter(
            Appointment.patient_id == data.patient_id,
            Appointment.appointment_date == visit_date,
            Appointment.status == AppointmentStatus.booked,
        )
        .first()
    )
    if appointment:
        appointment.status = AppointmentStatus.attended

    db.commit()
    db.refresh(visit)
    return _visible(visit, user)


@router.put("/{visit_id}/vitals", response_model=ResponseUnion)
def set_vitals(
    visit_id: uuid.UUID,
    data: VitalsWrite,
    user: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    visit = db.get(Visit, visit_id)
    if visit is None:
        raise HTTPException(status_code=404, detail="Visit not found")

    if visit.vitals is None:
        visit.vitals = VisitVitals(visit_id=visit.id)
    for field, value in data.model_dump().items():
        setattr(visit.vitals, field, value)
    visit.vitals.recorded_by_user_id = user.id
    visit.vitals.recorded_at = datetime.utcnow()

    db.commit()
    db.refresh(visit)
    return _visible(visit, user)


@router.post("/{visit_id}/open", response_model=VisitResponse)
def open_visit(
    visit_id: uuid.UUID,
    user: AppUser = Depends(doctor_only),
    db: Session = Depends(get_db),
):
    """Doctor picks the patient up from the queue."""
    visit = db.get(Visit, visit_id)
    if visit is None:
        raise HTTPException(status_code=404, detail="Visit not found")

    if visit.status == VisitStatus.waiting:
        visit.status = VisitStatus.in_consultation
        visit.opened_by_user_id = user.id
        db.commit()
        db.refresh(visit)
    return visit


@router.patch("/{visit_id}/clinical", response_model=VisitResponse)
def update_clinical(
    visit_id: uuid.UUID,
    data: VisitClinicalUpdate,
    user: AppUser = Depends(doctor_only),
    db: Session = Depends(get_db),
):
    """
    The doctor's half: complaint, medicines, card, next visit due and fees.
    Medicines are replaced wholesale — the client always sends the full list,
    which avoids per-row add/remove/reorder bookkeeping over the wire.
    """
    visit = db.get(Visit, visit_id)
    if visit is None:
        raise HTTPException(status_code=404, detail="Visit not found")

    values = data.model_dump(exclude_unset=True)
    complete = values.pop("complete", False)
    medicines = values.pop("medicines", None)
    invoice_data = values.pop("invoice", None)

    for field, value in values.items():
        setattr(visit, field, value)

    if medicines is not None:
        visit.medicines.clear()
        db.flush()
        for order, med in enumerate(medicines):
            med.pop("id", None)
            visit.medicines.append(VisitMedicine(sort_order=order, **med))

    if invoice_data is not None:
        if visit.invoice is None:
            visit.invoice = Invoice(visit_id=visit.id)
        for field, value in invoice_data.items():
            setattr(visit.invoice, field, value)
        visit.invoice.paid_at = datetime.utcnow() if visit.invoice.paid else None

    if complete:
        visit.status = VisitStatus.completed
        visit.completed_by_user_id = user.id

    db.commit()
    db.refresh(visit)
    return visit


@router.post("/migrated", response_model=VisitResponse, status_code=201)
def create_migrated_visit(
    data: MigratedVisitCreate,
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    """
    Carry-forward visit for a patient re-typed from MyOPD (spec §4.11).
    Marked `migrated` so the UI shows "recorded in MyOPD" rather than blank
    clinical fields, and so the recall query needs no special case.
    """
    if db.get(Patient, data.patient_id) is None:
        raise HTTPException(status_code=404, detail="Patient not found")

    visit = Visit(
        patient_id=data.patient_id,
        visit_date=data.visit_date,
        next_visit_due=data.next_visit_due,
        status=VisitStatus.completed,
        source=VisitSource.migrated,
    )
    if data.id:
        visit.id = data.id
    db.add(visit)
    db.commit()
    db.refresh(visit)
    return visit
