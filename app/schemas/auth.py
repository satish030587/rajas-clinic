import uuid
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field

from app.models.user import Role, UiLanguage


class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: "UserResponse"


class UserBase(BaseModel):
    username: str = Field(..., min_length=3, max_length=50)
    display_name: str = Field(..., min_length=1, max_length=100)
    role: Role
    ui_language: UiLanguage = UiLanguage.en


class UserCreate(UserBase):
    password: str = Field(..., min_length=6, max_length=128)


class UserUpdate(BaseModel):
    model_config = ConfigDict(extra="forbid")

    display_name: Optional[str] = Field(None, min_length=1, max_length=100)
    ui_language: Optional[UiLanguage] = None
    active: Optional[bool] = None
    password: Optional[str] = Field(None, min_length=6, max_length=128)


class UserResponse(UserBase):
    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    active: bool


Token.model_rebuild()
