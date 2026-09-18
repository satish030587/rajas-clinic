import uuid
from datetime import date
from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.deps import clinical_staff
from app.crud import queries
from app.models.patient import Patient
from app.models.user import AppUser
from app.models.visit import Visit, VisitSource, VisitStatus
from app.schemas.patient import (
    PatientCreate, PatientQuickAdd, PatientResponse, PatientRow, PatientUpdate,
)

router = APIRouter(prefix="/patients", tags=["patients"])


@router.get("", response_model=List[PatientRow])
def list_patients(
    q: Optional[str] = Query(None, description="Name or phone"),
    skip: int = 0,
    limit: int = Query(100, le=500),
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    return queries.search_patients(db, q, skip, limit)


@router.get("/by-phone/{phone}", response_model=Optional[PatientRow])
def find_by_phone(
    phone: str,
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    """
    Duplicate guard for registration (spec §4.11). Returns null when the number
    is free, so the client can register without a second round trip.
    """
    rows = queries.search_patients(db, phone, 0, 1)
    exact = [r for r in rows if r.phone == phone]
    return exact[0] if exact else None


@router.get("/{patient_id}", response_model=PatientResponse)
def get_patient(
    patient_id: uuid.UUID,
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    patient = db.query(Patient).filter(Patient.id == patient_id).first()
    if patient is None:
        raise HTTPException(status_code=404, detail="Patient not found")
    return patient


@router.post("", response_model=PatientResponse, status_code=201)
def create_patient(
    data: PatientCreate,
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    existing = (
        db.query(Patient)
        .filter(Patient.phone == data.phone, Patient.archived.is_(False))
        .first()
    )
    if existing:
        raise HTTPException(
            status_code=409,
            detail=f"A patient with this phone already exists: {existing.name}",
        )

    values = data.model_dump()
    supplied_id = values.pop("id", None)
    if values.get("age_years") is not None and values.get("date_of_birth") is None:
        values["age_recorded_on"] = date.today()

    patient = Patient(**values)
    if supplied_id:
        patient.id = supplied_id
    db.add(patient)
    db.commit()
    db.refresh(patient)
    return patient


@router.post("/quick-add", response_model=PatientResponse, status_code=201)
def quick_add(
    data: PatientQuickAdd,
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    """
    The migration backlog pass (spec §4.11): name, phone, language and the dates
    recall needs. A carry-forward visit is created so the patient appears in the
    recall list immediately — without it, typing them in achieves nothing.
    """
    existing = (
        db.query(Patient)
        .filter(Patient.phone == data.phone, Patient.archived.is_(False))
        .first()
    )
    if existing:
        raise HTTPException(
            status_code=409,
            detail=f"A patient with this phone already exists: {existing.name}",
        )

    patient = Patient(
        name=data.name,
        phone=data.phone,
        sex="other",
        preferred_language=data.preferred_language,
        migrated_from_myopd=True,
    )
    if data.id:
        patient.id = data.id
    db.add(patient)
    db.flush()

    if data.last_visit_date or data.next_visit_due:
        db.add(
            Visit(
                patient_id=patient.id,
                visit_date=data.last_visit_date or date.today(),
                next_visit_due=data.next_visit_due,
                status=VisitStatus.completed,
                source=VisitSource.migrated,
            )
        )

    db.commit()
    db.refresh(patient)
    return patient


@router.patch("/{patient_id}", response_model=PatientResponse)
def update_patient(
    patient_id: uuid.UUID,
    data: PatientUpdate,
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    patient = db.query(Patient).filter(Patient.id == patient_id).first()
    if patient is None:
        raise HTTPException(status_code=404, detail="Patient not found")

    values = data.model_dump(exclude_unset=True)

    # Age and DOB are mutually exclusive — keep them consistent.
    if values.get("age_years") is not None:
        values["age_recorded_on"] = date.today()
        values["date_of_birth"] = None
    if values.get("date_of_birth") is not None:
        values["age_years"] = None
        values["age_recorded_on"] = None

    for field, value in values.items():
        setattr(patient, field, value)
    db.commit()
    db.refresh(patient)
    return patient


@router.delete("/{patient_id}", status_code=204)
def archive_patient(
    patient_id: uuid.UUID,
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    patient = db.query(Patient).filter(Patient.id == patient_id).first()
    if patient is None:
        raise HTTPException(status_code=404, detail="Patient not found")
    patient.archived = True
    db.commit()
