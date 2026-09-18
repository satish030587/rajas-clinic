import uuid
from datetime import date
from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.deps import clinical_staff
from app.models.appointment import Appointment
from app.models.patient import Patient
from app.models.user import AppUser
from app.schemas.catalog import (
    AppointmentCreate, AppointmentResponse, AppointmentUpdate,
)

router = APIRouter(prefix="/appointments", tags=["appointments"])


@router.get("", response_model=List[AppointmentResponse])
def list_appointments(
    from_date: Optional[date] = Query(None),
    to_date: Optional[date] = Query(None),
    patient_id: Optional[uuid.UUID] = Query(None),
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    q = db.query(Appointment)
    if from_date:
        q = q.filter(Appointment.appointment_date >= from_date)
    if to_date:
        q = q.filter(Appointment.appointment_date <= to_date)
    if patient_id:
        q = q.filter(Appointment.patient_id == patient_id)
    return q.order_by(Appointment.appointment_date.asc()).all()


@router.post("", response_model=AppointmentResponse, status_code=201)
def book(
    data: AppointmentCreate,
    user: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    if db.get(Patient, data.patient_id) is None:
        raise HTTPException(status_code=404, detail="Patient not found")

    values = data.model_dump()
    supplied_id = values.pop("id", None)
    appointment = Appointment(**values, created_by_user_id=user.id)
    if supplied_id:
        appointment.id = supplied_id
    db.add(appointment)
    db.commit()
    db.refresh(appointment)
    return appointment


@router.patch("/{appointment_id}", response_model=AppointmentResponse)
def update(
    appointment_id: uuid.UUID,
    data: AppointmentUpdate,
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    appointment = db.get(Appointment, appointment_id)
    if appointment is None:
        raise HTTPException(status_code=404, detail="Appointment not found")
    for field, value in data.model_dump(exclude_unset=True).items():
        setattr(appointment, field, value)
    db.commit()
    db.refresh(appointment)
    return appointment
