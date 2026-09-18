import uuid
from pathlib import Path
from typing import List

from fastapi import (
    APIRouter, Depends, File, HTTPException, UploadFile, status,
)
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.database import get_db
from app.core.deps import clinical_staff
from app.models.investigation import Investigation, InvestigationFile
from app.models.patient import Patient
from app.models.user import AppUser
from app.schemas.catalog import InvestigationCreate, InvestigationResponse

router = APIRouter(prefix="/investigations", tags=["investigations"])

ALLOWED_TYPES = {"image/jpeg", "image/png", "image/webp", "application/pdf"}
EXTENSIONS = {
    "image/jpeg": ".jpg",
    "image/png": ".png",
    "image/webp": ".webp",
    "application/pdf": ".pdf",
}


def _upload_root() -> Path:
    root = Path(settings.upload_dir) / "investigations"
    root.mkdir(parents=True, exist_ok=True)
    return root


@router.get("/patient/{patient_id}", response_model=List[InvestigationResponse])
def for_patient(
    patient_id: uuid.UUID,
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    """
    Oldest first, so the comparison view can default to earliest vs latest —
    which is the comparison that actually matters (spec §4.5).
    """
    return (
        db.query(Investigation)
        .filter(Investigation.patient_id == patient_id)
        .order_by(Investigation.title.asc(), Investigation.taken_on.asc())
        .all()
    )


@router.post("", response_model=InvestigationResponse, status_code=201)
def create(
    data: InvestigationCreate,
    user: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    if db.get(Patient, data.patient_id) is None:
        raise HTTPException(status_code=404, detail="Patient not found")

    values = data.model_dump()
    supplied_id = values.pop("id", None)
    investigation = Investigation(**values, created_by_user_id=user.id)
    if supplied_id:
        investigation.id = supplied_id
    db.add(investigation)
    db.commit()
    db.refresh(investigation)
    return investigation


@router.post("/{investigation_id}/files", response_model=InvestigationResponse)
async def upload_file(
    investigation_id: uuid.UUID,
    file: UploadFile = File(...),
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    investigation = db.get(Investigation, investigation_id)
    if investigation is None:
        raise HTTPException(status_code=404, detail="Investigation not found")

    if file.content_type not in ALLOWED_TYPES:
        raise HTTPException(
            status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
            detail=f"Unsupported file type: {file.content_type}",
        )

    payload = await file.read()
    if len(payload) > settings.max_upload_bytes:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail="File is too large",
        )

    name = f"{uuid.uuid4()}{EXTENSIONS[file.content_type]}"
    (_upload_root() / name).write_bytes(payload)

    next_page = len(investigation.files) + 1
    db.add(
        InvestigationFile(
            investigation_id=investigation.id,
            file_path=f"investigations/{name}",
            page_no=next_page,
        )
    )
    db.commit()
    db.refresh(investigation)
    return investigation


@router.get("/files/{file_id}")
def download_file(
    file_id: uuid.UUID,
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    record = db.get(InvestigationFile, file_id)
    if record is None:
        raise HTTPException(status_code=404, detail="File not found")

    path = Path(settings.upload_dir) / record.file_path
    if not path.exists():
        raise HTTPException(status_code=404, detail="File is missing from storage")
    return FileResponse(path)


@router.delete("/{investigation_id}", status_code=204)
def delete(
    investigation_id: uuid.UUID,
    _: AppUser = Depends(clinical_staff),
    db: Session = Depends(get_db),
):
    investigation = db.get(Investigation, investigation_id)
    if investigation is None:
        raise HTTPException(status_code=404, detail="Investigation not found")

    for record in investigation.files:
        (Path(settings.upload_dir) / record.file_path).unlink(missing_ok=True)
    db.delete(investigation)
    db.commit()
