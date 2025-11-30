from sqlalchemy import (Column, Integer, String, Boolean, DateTime,
                        LargeBinary, ForeignKey, Table)
from sqlalchemy.orm import declarative_base, relationship
from datetime import datetime

Base = declarative_base()

# ─────────────────── join-tables ─────────────────────────────
roles_permissions = Table(
    "roles_permissions", Base.metadata,
    Column("role_id",      Integer, ForeignKey("roles.role_id"),      primary_key=True),
    Column("permission_id",Integer, ForeignKey("permissions.permission_id"), primary_key=True)
)

user_doorbell = Table(
    "user_doorbell", Base.metadata,
    Column("user_id",     Integer, ForeignKey("users.user_id"),       primary_key=True),
    Column("doorbell_id", Integer, ForeignKey("doorbells.doorbell_id"), primary_key=True),
    Column("role_id",     Integer, ForeignKey("roles.role_id"))
)

# ────────────────── main entities ───────────────────────────
class User(Base):
    __tablename__ = "users"

    user_id       = Column(Integer, primary_key=True)
    username      = Column(String(50),  unique=True, nullable=False)
    email         = Column(String(100), unique=True, nullable=False)
    password_hash = Column(String(255), nullable=False)
    display_name  = Column(String(100))
    created_at    = Column(DateTime, default=datetime.utcnow)

    settings  = relationship("UserSettings", back_populates="user", uselist=False)
    doorbells = relationship("Doorbell", secondary=user_doorbell, back_populates="users")
    twofa     = relationship("User2FASecret", back_populates="user", uselist=False)

class UserSettings(Base):
    __tablename__ = "user_settings"

    user_id                  = Column(Integer, ForeignKey("users.user_id"), primary_key=True)
    enable_2fa               = Column(Boolean, default=False)
    face_login_app           = Column(Boolean, default=False)
    enable_in_app_alerts     = Column(Boolean, default=True)
    enable_push_notifications= Column(Boolean, default=True)
    notify_new_device_login  = Column(Boolean, default=True)

    user = relationship("User", back_populates="settings")

class User2FASecret(Base):
    __tablename__ = "user_2fa_secret"

    user_id = Column(Integer, ForeignKey("users.user_id"), primary_key=True)
    secret  = Column(String(32), nullable=False)

    user = relationship("User", back_populates="twofa")

class Doorbell(Base):
    __tablename__ = "doorbells"

    doorbell_id               = Column(Integer, primary_key=True)
    doorbell_name             = Column(String(100), nullable=False)
    doorbell_password         = Column(String(255), nullable=False)
    owner_user_id             = Column(Integer, ForeignKey("users.user_id"), nullable=False)
    enable_facial_recognition = Column(Boolean, default=True)
    auto_delete_recordings    = Column(Boolean, default=True)
    created_at                = Column(DateTime, default=datetime.utcnow)

    users          = relationship("User", secondary=user_doorbell, back_populates="doorbells")
    face_profiles  = relationship(                     
        "FaceProfile",
        back_populates="doorbell",
        cascade="all, delete-orphan"
    )

class DoorbellTokens(Base):
    __tablename__ = "doorbell_tokens"

    doorbell_token_id   = Column(Integer, primary_key=True, autoincrement=True)
    token               = Column(String(36), nullable=False)
    doorbell_id         = Column(Integer, ForeignKey("doorbells.doorbell_id", ondelete="CASCADE"), nullable=False)


class FaceProfile(Base):                               
    __tablename__ = "face_profiles"

    face_id     = Column(Integer, primary_key=True)
    doorbell_id = Column(Integer, ForeignKey("doorbells.doorbell_id", ondelete="CASCADE"), nullable=False)
    face_data   = Column(LargeBinary)
    created_at  = Column(DateTime, default=datetime.utcnow)
    face_profile_name = Column(String(100))
    is_blocked  = Column(Boolean, default=False, nullable=False)

    doorbell = relationship("Doorbell", back_populates="face_profiles")

class Role(Base):
    __tablename__ = "roles"

    role_id   = Column(Integer, primary_key=True)
    role_name = Column(String(50), nullable=False)
    permissions = relationship("Permission", secondary=roles_permissions, back_populates="roles")

class Permission(Base):
    __tablename__ = "permissions"

    permission_id   = Column(Integer, primary_key=True)
    permission_name = Column(String(100), nullable=False)
    roles = relationship("Role", secondary=roles_permissions, back_populates="permissions")

class UserPermOverride(Base):
    __tablename__ = "user_permissions_override"

    user_id       = Column(Integer, ForeignKey("users.user_id"), primary_key=True)
    permission_id = Column(Integer, ForeignKey("permissions.permission_id"), primary_key=True)
    is_granted    = Column(Boolean, nullable=False)

class NotifTokens(Base):
    __tablename__ = "notif_tokens"

    notif_id    = Column(Integer, primary_key=True, autoincrement=True)
    fcm_token   = Column(String(255), nullable=False)
    user_id     = Column(Integer, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False)
    doorbell_id = Column(Integer, ForeignKey("doorbells.doorbell_id", ondelete="CASCADE"), nullable=False)