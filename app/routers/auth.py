from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.deps import doctor_only, get_current_user
from app.core.security import create_access_token, hash_password, verify_password
from app.models.user import AppUser
from app.schemas.auth import Token, UserCreate, UserResponse, UserUpdate

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/login", response_model=Token)
def login(
    form: OAuth2PasswordRequestForm = Depends(),
    db: Session = Depends(get_db),
):
    user = db.query(AppUser).filter(AppUser.username == form.username).first()
    if user is None or not verify_password(form.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    if not user.active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN, detail="This account is disabled"
        )

    return Token(
        access_token=create_access_token(user.id, user.role.value),
        user=UserResponse.model_validate(user),
    )


@router.get("/me", response_model=UserResponse)
def me(user: AppUser = Depends(get_current_user)):
    return user


@router.patch("/me", response_model=UserResponse)
def update_me(
    data: UserUpdate,
    user: AppUser = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Lets any user change their own display name, UI language or password."""
    values = data.model_dump(exclude_unset=True)
    values.pop("active", None)  # nobody disables their own account
    if "password" in values:
        user.password_hash = hash_password(values.pop("password"))
    for field, value in values.items():
        setattr(user, field, value)
    db.commit()
    db.refresh(user)
    return user


@router.get("/users", response_model=list[UserResponse])
def list_users(
    _: AppUser = Depends(doctor_only),
    db: Session = Depends(get_db),
):
    return db.query(AppUser).order_by(AppUser.display_name).all()


@router.post("/users", response_model=UserResponse, status_code=201)
def create_user(
    data: UserCreate,
    _: AppUser = Depends(doctor_only),
    db: Session = Depends(get_db),
):
    if db.query(AppUser).filter(AppUser.username == data.username).first():
        raise HTTPException(status_code=409, detail="That username is already taken")

    user = AppUser(
        username=data.username,
        password_hash=hash_password(data.password),
        display_name=data.display_name,
        role=data.role,
        ui_language=data.ui_language,
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return user
