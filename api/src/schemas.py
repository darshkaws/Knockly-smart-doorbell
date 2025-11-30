from pydantic import BaseModel, EmailStr, Field
from typing import Optional, List

# ───────── auth ─────────
class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"

class UserCreate(BaseModel):
    username: str = Field(min_length=3, max_length=50)
    email:    EmailStr
    password: str = Field(min_length=8)
    display_name: str

class UserLogin(BaseModel):
    username_or_email: str
    password: str
    totp_code: Optional[str] = None

# ───────── user & settings ─────────
class UserOut(BaseModel):
    user_id: int
    username: str
    email: EmailStr
    display_name: Optional[str]

    class Config:
        orm_mode = True

class UserSettingsOut(BaseModel):
    enable_2fa: bool
    face_login_app: bool
    enable_in_app_alerts: bool
    enable_push_notifications: bool
    notify_new_device_login: bool

    class Config:
        orm_mode = True

# ───────── doorbells & permissions ─────────

class LinkedUserOut(BaseModel):
    user_id: int
    username: str
    display_name: Optional[str] = None
    role_id: Optional[int] = None
    role_name: Optional[str] = None

    class Config:
        orm_mode = True
        
class DoorbellOut(BaseModel):
    doorbell_id: int
    doorbell_name: str
    owner_user_id: Optional[int] = None
    linked_users: List[LinkedUserOut] = []  

    class Config:
        orm_mode = True

class DoorbellCreate(BaseModel):
    doorbell_name: str
    doorbell_password: str
    enable_facial_recognition: bool = False
    auto_delete_recordings: bool = False

# class DoorbellGetUsers(BaseModel):
#     doorbell_id: int
    
class UserDoorbellLinkCreate(BaseModel):
    user_id: int
    doorbell_id: int
    role_id: Optional[int] = None

class PermCheckResponse(BaseModel):
    permission: str
    granted: bool

class OverrideSet(BaseModel):
    permission_name: str
    is_granted: bool

# ───────── face profiles ─────────
class FaceProfileOut(BaseModel):
    face_id: int
    doorbell_id: int
    face_profile_name: Optional[str]
    is_blocked: bool

    class Config:
        orm_mode = True

# _____________ Notifications _____________
class NotificationIsSubscribed(BaseModel):
    fcm_token: str
    doorbell_id: int

class NotificationIsSubscribedResponse(BaseModel):
    is_subscribed: bool

class NotificationSubscribe(BaseModel):
    fcm_token: str
    doorbell_id: int

class NotificationUnsubscribe(BaseModel):
    fcm_token: str
    doorbell_id: int

class NotificationUpdateToken(BaseModel):
    old_token: str
    new_token: str

class NotifyMotionDetected(BaseModel):
    token: str