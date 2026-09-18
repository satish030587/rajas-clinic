import uuid
from datetime import date
from typing import Optional, List

from sqlalchemy import or_, func
from sqlalchemy.orm import Session

from app.models.patient import Patient
from app.schemas.patient import PatientCreate, PatientUpdate


def create_patient(db: Session, data: PatientCreate) -> Patient:
    values = data.model_dump()

    # If age was given instead of DOB, stamp when it was recorded.
    if values.get("age_years") is not None and values.get("date_of_birth") is None:
        values["age_recorded_on"] = date.today()

    patient = Patient(**values)
    db.add(patient)
    db.commit()
    db.refresh(patient)
    return patient


def get_patient(db: Session, patient_id: uuid.UUID) -> Optional[Patient]:
    return db.query(Patient).filter(Patient.id == patient_id).first()


def find_by_phone(db: Session, phone: str) -> Optional[Patient]:
    return (
        db.query(Patient)
        .filter(Patient.phone == phone, Patient.archived.is_(False))
        .first()
    )


def list_patients(
    db: Session,
    search: Optional[str] = None,
    include_archived: bool = False,
    skip: int = 0,
    limit: int = 50,
) -> List[Patient]:
    query = db.query(Patient)

    if not include_archived:
        query = query.filter(Patient.archived.is_(False))

    if search:
        term = f"%{search.strip()}%"
        query = query.filter(
            or_(
                Patient.name.ilike(term),
                Patient.phone.ilike(term),
            )
        )

    return (
        query.order_by(func.lower(Patient.name))
        .offset(skip)
        .limit(limit)
        .all()
    )


def update_patient(
    db: Session, patient: Patient, data: PatientUpdate
) -> Patient:
    values = data.model_dump(exclude_unset=True)

    # Age and DOB are mutually exclusive — keep them consistent.
    if "age_years" in values and values["age_years"] is not None:
        values["age_recorded_on"] = date.today()
        values["date_of_birth"] = None
    if "date_of_birth" in values and values["date_of_birth"] is not None:
        values["age_years"] = None
        values["age_recorded_on"] = None

    for field, value in values.items():
        setattr(patient, field, value)

    db.commit()
    db.refresh(patient)
    return patient


def archive_patient(db: Session, patient: Patient) -> Patient:
    patient.archived = True
    db.commit()
    db.refresh(patient)
    return patient