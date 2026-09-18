"""
Shared read queries for the recall dashboard and the waiting queue.

The latest-visit-per-patient join is defined once here because it is what the
whole recall system runs on (spec §3) and it must behave identically wherever
it is used.
"""

from datetime import date
from typing import List, Optional

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.invoice import Invoice
from app.models.patient import Patient
from app.models.visit import Visit, VisitStatus
from app.schemas.patient import PatientRow


def _latest_visit_subquery():
    """The most recent visit per patient, by visit date then creation order."""
    ranked = select(
        Visit.id.label("visit_id"),
        Visit.patient_id.label("patient_id"),
        Visit.visit_date.label("visit_date"),
        Visit.next_visit_due.label("next_visit_due"),
        func.row_number()
        .over(
            partition_by=Visit.patient_id,
            order_by=(Visit.visit_date.desc(), Visit.created_at.desc()),
        )
        .label("rn"),
    ).subquery()
    return select(ranked).where(ranked.c.rn == 1).subquery()


def _visit_count_subquery():
    return (
        select(Visit.patient_id, func.count(Visit.id).label("visit_count"))
        .group_by(Visit.patient_id)
        .subquery()
    )


def _to_rows(result) -> List[PatientRow]:
    return [
        PatientRow(
            id=r.id,
            name=r.name,
            phone=r.phone,
            sex=r.sex,
            date_of_birth=r.date_of_birth,
            age_years=r.age_years,
            age_recorded_on=r.age_recorded_on,
            preferred_language=r.preferred_language,
            last_visit_date=r.last_visit_date,
            next_visit_due=r.next_visit_due,
            visit_count=r.visit_count or 0,
        )
        for r in result
    ]


def _base_select(latest, counts):
    return select(
        Patient.id,
        Patient.name,
        Patient.phone,
        Patient.sex,
        Patient.date_of_birth,
        Patient.age_years,
        Patient.age_recorded_on,
        Patient.preferred_language,
        latest.c.visit_date.label("last_visit_date"),
        latest.c.next_visit_due.label("next_visit_due"),
        counts.c.visit_count,
    )


def search_patients(
    db: Session,
    query: Optional[str] = None,
    skip: int = 0,
    limit: int = 100,
) -> List[PatientRow]:
    latest, counts = _latest_visit_subquery(), _visit_count_subquery()
    stmt = (
        _base_select(latest, counts)
        .select_from(Patient)
        .outerjoin(latest, latest.c.patient_id == Patient.id)
        .outerjoin(counts, counts.c.patient_id == Patient.id)
        .where(Patient.archived.is_(False))
    )
    if query:
        term = f"%{query.strip()}%"
        stmt = stmt.where(Patient.name.ilike(term) | Patient.phone.ilike(term))

    stmt = stmt.order_by(func.lower(Patient.name)).offset(skip).limit(limit)
    return _to_rows(db.execute(stmt).all())


def due_on_or_before(db: Session, day: date) -> List[PatientRow]:
    """Everyone whose recall date has arrived or passed, soonest first."""
    latest, counts = _latest_visit_subquery(), _visit_count_subquery()
    stmt = (
        _base_select(latest, counts)
        .select_from(Patient)
        .join(latest, latest.c.patient_id == Patient.id)
        .outerjoin(counts, counts.c.patient_id == Patient.id)
        .where(
            Patient.archived.is_(False),
            latest.c.next_visit_due.isnot(None),
            latest.c.next_visit_due <= day,
        )
        .order_by(latest.c.next_visit_due.asc())
    )
    return _to_rows(db.execute(stmt).all())


def seen_on(db: Session, day: date) -> List[PatientRow]:
    latest, counts = _latest_visit_subquery(), _visit_count_subquery()
    stmt = (
        _base_select(latest, counts)
        .select_from(Visit)
        .join(Patient, Patient.id == Visit.patient_id)
        .outerjoin(latest, latest.c.patient_id == Patient.id)
        .outerjoin(counts, counts.c.patient_id == Patient.id)
        .where(Visit.visit_date == day, Visit.status == VisitStatus.completed)
        .order_by(Visit.created_at.desc())
    )
    return _to_rows(db.execute(stmt).all())


def collection_on(db: Session, day: date, paid: bool = True) -> int:
    stmt = (
        select(func.coalesce(func.sum(Invoice.consultation_fee + Invoice.medicine_charge), 0))
        .select_from(Invoice)
        .join(Visit, Visit.id == Invoice.visit_id)
        .where(Visit.visit_date == day, Invoice.paid.is_(paid))
    )
    return int(db.execute(stmt).scalar() or 0)
