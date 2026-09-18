from datetime import date, timedelta
from typing import List

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.deps import clinical_staff, doctor_only
from app.crud import queries
from app.models.appointment import Appointment, AppointmentStatus
from app.models.user import AppUser
from app.schemas.catalog import AppointmentResponse
from app.schemas.patient import PatientRow

router = APIRouter(prefix="/recall", tags=["recall"])


class RecallItem(BaseModel):
    patient: PatientRow
    due_date: date
    days_overdue: int


class TodaySummary(BaseModel):
    due_today: List[RecallItem]
    overdue: List[RecallItem]
    seen_today: List[PatientRow]
    booked_today: List[AppointmentResponse]
    collection: int
    outstanding: int


def _items(rows: List[PatientRow], today: date) -> List[RecallItem]:
    out = []
    for row in rows:
        if row.next_visit_due is None:
            continue
        out.append(
            RecallItem(
                patient=row,
                due_date=row.next_visit_due,
                days_overdue=(today - row.next_visit_due).days,
            )
        )
    return out


@router.get("/today", response_model=TodaySummary)
def today(
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    day = date.today()
    all_due = _items(queries.due_on_or_before(db, day), day)

    booked = (
        db.query(Appointment)
        .filter(
            Appointment.appointment_date == day,
            Appointment.status == AppointmentStatus.booked,
        )
        .order_by(Appointment.created_at.asc())
        .all()
    )

    return TodaySummary(
        due_today=[i for i in all_due if i.days_overdue <= 0],
        overdue=[i for i in all_due if i.days_overdue > 0],
        seen_today=queries.seen_on(db, day),
        booked_today=booked,
        collection=queries.collection_on(db, day, paid=True),
        outstanding=queries.collection_on(db, day, paid=False),
    )


@router.get("/overdue", response_model=List[RecallItem])
def overdue(
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    """Overdue patients, most overdue first — the ones at risk of dropping out."""
    day = date.today()
    items = [i for i in _items(queries.due_on_or_before(db, day), day) if i.days_overdue > 0]
    return sorted(items, key=lambda i: i.days_overdue, reverse=True)


@router.get("/reminders-due", response_model=List[AppointmentResponse])
def reminders_due(
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    """Appointments booked for tomorrow — the day-before reminder list (spec §4.8)."""
    return (
        db.query(Appointment)
        .filter(
            Appointment.appointment_date == date.today() + timedelta(days=1),
            Appointment.status == AppointmentStatus.booked,
        )
        .order_by(Appointment.created_at.asc())
        .all()
    )


@router.get("/collection", response_model=dict)
def collection(
    day: date | None = None,
    _: AppUser = Depends(doctor_only),
    db: Session = Depends(get_db),
):
    target = day or date.today()
    return {
        "date": target,
        "collected": queries.collection_on(db, target, paid=True),
        "outstanding": queries.collection_on(db, target, paid=False),
    }
