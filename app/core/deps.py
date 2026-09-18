import uuid
from typing import Iterable

from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.security import decode_access_token
from app.models.user import AppUser, Role

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="auth/login")

CREDENTIALS_ERROR = HTTPException(
    status_code=status.HTTP_401_UNAUTHORIZED,
    detail="Could not validate credentials",
    headers={"WWW-Authenticate": "Bearer"},
)


def get_current_user(
    token: str = Depends(oauth2_scheme),
    db: Session = Depends(get_db),
) -> AppUser:
    payload = decode_access_token(token)
    if payload is None:
        raise CREDENTIALS_ERROR

    subject = payload.get("sub")
    if not subject:
        raise CREDENTIALS_ERROR
    try:
        user_id = uuid.UUID(subject)
    except ValueError:
        raise CREDENTIALS_ERROR

    user = db.query(AppUser).filter(AppUser.id == user_id).first()
    if user is None or not user.active:
        raise CREDENTIALS_ERROR
    return user


def require_roles(*allowed: Role):
    """
    Guard for endpoints only some roles may reach — see the permission matrix
    in spec §6. Reception must not see complaints, medicines or fees.
    """

    def guard(user: AppUser = Depends(get_current_user)) -> AppUser:
        if user.role not in allowed:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"This action requires one of: "
                       f"{', '.join(r.value for r in allowed)}",
            )
        return user

    return guard


# Named guards, so routers read like the permission matrix.
doctor_only = require_roles(Role.doctor)
clinical_staff = require_roles(Role.doctor, Role.reception)
