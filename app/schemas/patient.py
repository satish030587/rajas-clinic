import uuid
import re
from datetime import date, datetime
from typing import Optional

from pydantic import (
    BaseModel, ConfigDict, Field, field_validator,
    model_validator, computed_field
)

from app.models.patient import Sex, Language


PHONE_RE = re.compile(r"^[6-9]\d{9}$")


def normalize_phone(value: Optional[str]) -> Optional[str]:
    """Strip formatting and the +91 country code, keep 10 digits."""
    if value is None:
        return None
    digits = re.sub(r"\D", "", value)
    if digits.startswith("91") and len(digits) == 12:
        digits = digits[2:]
    if digits.startswith("0") and len(digits) == 11:
        digits = digits[1:]
    if not PHONE_RE.match(digits):
        raise ValueError(
            "Phone must be a valid 10-digit Indian mobile number"
        )
    return digits


class PatientBase(BaseModel):
    name: str = Field(..., min_length=1, max_length=150)
    phone: str
    alternate_phone: Optional[str] = None
    sex: Sex
    date_of_birth: Optional[date] = None
    age_years: Optional[int] = Field(None, ge=0, le=120)
    address: Optional[str] = None
    occupation: Optional[str] = None
    blood_group: Optional[str] = None
    referred_by: Optional[str] = None
    current_medication: Optional[str] = None
    preferred_language: Language = Language.en

    @field_validator("name")
    @classmethod
    def clean_name(cls, v: str) -> str:
        cleaned = " ".join(v.split())
        if not cleaned:
            raise ValueError("Name cannot be blank")
        return cleaned

    @field_validator("phone")
    @classmethod
    def validate_phone(cls, v: str) -> str:
        return normalize_phone(v)

    @field_validator("alternate_phone")
    @classmethod
    def validate_alt_phone(cls, v: Optional[str]) -> Optional[str]:
        if v in (None, ""):
            return None
        return normalize_phone(v)


class PatientCreate(PatientBase):
    @model_validator(mode="after")
    def require_age_or_dob(self):
        if self.date_of_birth is None and self.age_years is None:
            raise ValueError(
                "Provide either date_of_birth or age_years"
            )
        return self


class PatientUpdate(BaseModel):
    """All fields optional — PATCH semantics."""
    model_config = ConfigDict(extra="forbid")

    name: Optional[str] = Field(None, min_length=1, max_length=150)
    phone: Optional[str] = None
    alternate_phone: Optional[str] = None
    sex: Optional[Sex] = None
    date_of_birth: Optional[date] = None
    age_years: Optional[int] = Field(None, ge=0, le=120)
    address: Optional[str] = None
    occupation: Optional[str] = None
    blood_group: Optional[str] = None
    referred_by: Optional[str] = None
    current_medication: Optional[str] = None
    preferred_language: Optional[Language] = None

    @field_validator("phone", "alternate_phone")
    @classmethod
    def validate_phones(cls, v: Optional[str]) -> Optional[str]:
        if v in (None, ""):
            return None
        return normalize_phone(v)


class PatientResponse(PatientBase):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    age_recorded_on: Optional[date] = None
    created_at: datetime
    archived: bool

    @computed_field
    @property
    def current_age(self) -> Optional[int]:
        today = date.today()
        if self.date_of_birth:
            dob = self.date_of_birth
            return today.year - dob.year - (
                (today.month, today.day) < (dob.month, dob.day)
            )
        if self.age_years is not None and self.age_recorded_on is not None:
            elapsed = today.year - self.age_recorded_on.year - (
                (today.month, today.day)
                < (self.age_recorded_on.month, self.age_recorded_on.day)
            )
            return self.age_years + elapsed
        return None