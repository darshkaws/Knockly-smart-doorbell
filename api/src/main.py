from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from fastapi.middleware.cors import CORSMiddleware
from jose import jwt, JWTError
from sqlalchemy.ext.asyncio import AsyncSession
import pyotp, qrcode, io, base64                     # for QR URL
from sqlalchemy.future import select
from sqlalchemy.orm import selectinload
from sqlalchemy import insert
from httpx import AsyncClient

import database, crud, schemas, security, models
from config import JWT_SECRET, JWT_ALGORITHM, FIRBEASE_URL
from security import get_fcm_access_token

app = FastAPI(title="Knockly API v2")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],   # LAN only 
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

oauth2 = OAuth2PasswordBearer(tokenUrl="/auth/login")

# ------------ helpers -------------
async def current_user(token: str = Depends(oauth2),
                       db: database.AsyncSession = Depends(database.get_session)) -> models.User:
    if not token:
        print("[DEBUG] No token provided.")
        raise HTTPException(status_code=401, detail="No token provided", headers={"WWW-Authenticate": "Bearer"})

    try:
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM])
        print("[DEBUG] Decoded token payload:", payload)
        uid = payload.get("sub")
        if uid is None:
            print("[DEBUG] Token missing 'sub'. Payload:", payload)
            raise HTTPException(status_code=401, detail="Invalid token: no 'sub' field", headers={"WWW-Authenticate": "Bearer"})
    except JWTError as e:
        print("[DEBUG] JWT Error while decoding:", str(e))
        raise HTTPException(status_code=401, detail="Invalid token: decode failed", headers={"WWW-Authenticate": "Bearer"})

    stmt = select(models.User).options(selectinload(models.User.settings)).where(models.User.user_id == uid)
    res = await db.execute(stmt)
    user = res.scalars().first()

    if not user:
        print(f"[DEBUG] No user found with user_id={uid}")
        raise HTTPException(status_code=404, detail=f"User ID {uid} not found", headers={"WWW-Authenticate": "Bearer"})

    if not user.settings:
        print(f"[DEBUG] User {user.username} has no settings loaded!")
        raise HTTPException(status_code=500, detail="User settings not found", headers={"WWW-Authenticate": "Bearer"})

    return user

async def send_fcm_notification(fcm_token: str, title: str, body: str):
    access_token = get_fcm_access_token()

    headers = {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json; UTF-8"
    }

    message = { "message": {
        "token": fcm_token,
        "notification": {
            "title": title,
            "body": body
        }
    }}

    async with AsyncClient() as client:
        response = await client.post(FIRBEASE_URL, json=message, headers=headers)
        response.raise_for_status()
        print("Fcm Response: ", response.status_code, response.text)
        return response.json()

# -------- AUTH ------------------------------------------------
@app.post("/auth/register", response_model=schemas.Token, status_code=201)
async def register(inp: schemas.UserCreate,
                   db: AsyncSession = Depends(database.get_session)):
    if await crud.get_user_by_username(db, inp.username):
        raise HTTPException(400, "Username taken")
    if await crud.get_user_by_email(db, inp.email):
        raise HTTPException(400, "Email already used")
    user = await crud.create_user(db, username=inp.username,
                                       email=inp.email,
                                       password=inp.password,
                                       display_name=inp.display_name)
    token = security.create_token({"sub": user.user_id})
    return {"access_token": token}

@app.post("/auth/login", response_model=schemas.Token)
async def login(inp: schemas.UserLogin,
                db: AsyncSession = Depends(database.get_session)):
    user = await crud.authenticate(db, inp.username_or_email, inp.password)
    if not user:
        raise HTTPException(401, "Bad username/email or password")
    # if 2FA enabled verify TOTP
    if user.settings.enable_2fa:
        if not inp.totp_code or not user.twofa:
            raise HTTPException(401, "TOTP required")
        if not security.verify_totp(user.twofa.secret, inp.totp_code):
            raise HTTPException(401, "Invalid TOTP")
    token = security.create_token({"sub": user.user_id})
    return {"access_token": token}

# ---------- 2‑FA mgmt (opt‑in) ---------------- 
@app.post("/auth/2fa/setup")
async def setup_2fa(me: models.User = Depends(current_user),
                    db: AsyncSession = Depends(database.get_session)):
    if me.twofa:
        raise HTTPException(400, "2FA already set")
    secret = security.new_totp_secret()
    db.add(models.User2FASecret(user_id=me.user_id, secret=secret))
    me.settings.enable_2fa = True
    await db.commit()
    # return otpauth:// URL + QR in base64 (easier for apps)
    uri = pyotp.totp.TOTP(secret).provisioning_uri(name=me.email, issuer_name="SmartDoor")
    img = qrcode.make(uri)
    buf = io.BytesIO(); img.save(buf, format="PNG")
    b64 = base64.b64encode(buf.getvalue()).decode()
    return {"otpauth_uri": uri, "qr_base64png": b64}

@app.post("/auth/2fa/disable")
async def disable_2fa(code: str,
                      me: models.User = Depends(current_user),
                      db: AsyncSession = Depends(database.get_session)):
    if not me.twofa or not me.settings.enable_2fa:
        raise HTTPException(400, "2FA not enabled")
    if not security.verify_totp(me.twofa.secret, code):
        raise HTTPException(401, "Bad TOTP")
    await db.delete(me.twofa)
    me.settings.enable_2fa = False
    await db.commit()
    return {"disabled": True}

# ---------- USERS -------------------------------------------
@app.get("/users/me", response_model=schemas.UserOut)
async def read_me(me: models.User = Depends(current_user)):
    return me

@app.get("/users/me/settings", response_model=schemas.UserSettingsOut)
async def read_settings(me: models.User = Depends(current_user)):
    return me.settings

# ---------- DOORBELLS ---------------------------------------
@app.get("/doorbells", response_model=list[schemas.DoorbellOut])
async def my_bells(me: models.User = Depends(current_user),
                   db: AsyncSession = Depends(database.get_session)):
    return await crud.list_user_doorbells(db, me.user_id)

@app.post("/doorbells", response_model=schemas.DoorbellOut, status_code=201)
async def create_bell(body: schemas.DoorbellCreate,
                      me: models.User = Depends(current_user),
                      db: AsyncSession = Depends(database.get_session)):
    bell = models.Doorbell(
        doorbell_name=body.doorbell_name,
        doorbell_password=body.doorbell_password,
        owner_user_id=me.user_id,
        enable_facial_recognition=body.enable_facial_recognition,
        auto_delete_recordings=body.auto_delete_recordings,
    )
    db.add(bell)
    await db.flush()  # get bell.doorbell_id populated

    # link creator to doorbell
    await db.execute(
        insert(models.user_doorbell).values(
            user_id=me.user_id,
            doorbell_id=bell.doorbell_id,
            role_id=1   # or assign a default role here if needed
        )
    )
    await db.commit()
    await db.refresh(bell)
    return bell

@app.post("/doorbells/link-user", status_code=201)
async def link_user(body: schemas.UserDoorbellLinkCreate,
                    me: models.User = Depends(current_user),
                    db: AsyncSession = Depends(database.get_session)):
    # validate that role_id exists
    if body.role_id:
        role_q = await db.execute(select(models.Role).where(models.Role.role_id == body.role_id))
        if not role_q.scalar():
            raise HTTPException(400, "Invalid role_id")

    # prevent duplicate linking
    existing = await db.execute(
        select(models.user_doorbell)
        .where((models.user_doorbell.c.user_id == body.user_id) &
               (models.user_doorbell.c.doorbell_id == body.doorbell_id))
    )
    if existing.first():
        raise HTTPException(400, "User already linked to doorbell")

    await db.execute(
        insert(models.user_doorbell).values(
            user_id=body.user_id,
            doorbell_id=body.doorbell_id,
            role_id=body.role_id
        )
    )
    await db.commit()
    return {"detail": "User linked to doorbell"}

@app.get("/doorbells/{doorbell_id}/getLinkedUsers", response_model=list[schemas.LinkedUserOut])
async def get_doorbell_users(
    doorbell_id: int,
    me: models.User = Depends(current_user),
    db: AsyncSession = Depends(database.get_session)):

    linked_users = await crud.get_users_from_doorbell(db, doorbell_id)

    return linked_users


# ---------- PERMISSIONS (read + override) ------------------- 
@app.get("/doorbells/{doorbell_id}/permissions",
         response_model=list[schemas.PermCheckResponse])
async def my_perms(doorbell_id: int,
                   me: models.User = Depends(current_user),
                   db: AsyncSession = Depends(database.get_session)):
    names = await crud.effective_permissions(db, user_id=me.user_id, doorbell_id=doorbell_id)
    all_perms = (await db.execute(select(models.Permission))).scalars().all()
    return [schemas.PermCheckResponse(permission=p.permission_name,
                                      granted=p.permission_name in names)
            for p in all_perms]

@app.patch("/doorbells/{doorbell_id}/permissions/override")
async def set_override(doorbell_id: int,
                       body: schemas.OverrideSet,
                       me: models.User = Depends(current_user),
                       db: AsyncSession = Depends(database.get_session)):
    # only users with 'manage_roles_and_perms' may override others
    eff = await crud.effective_permissions(db, user_id=me.user_id, doorbell_id=doorbell_id)
    if "manage_roles_and_perms" not in eff:
        raise HTTPException(403, "Need 'manage_roles_and_perms'")
    # find target user id in query param ?user_id=xxx else self
    target_id = me.user_id
    success = await crud.set_override(db, user_id=target_id,
                                           perm_name=body.permission_name,
                                           is_granted=body.is_granted)
    if not success:
        raise HTTPException(404, "Unknown permission")
    return {"ok": True}

# ---------- FACE PROFILES -------------------------
@app.get("/doorbells/{bell_id}/faces", response_model=list[schemas.FaceProfileOut])
async def bell_faces(bell_id: int,
                     me: models.User = Depends(current_user),
                     db: AsyncSession = Depends(database.get_session)):
    return await crud.list_faces(db, doorbell_id=bell_id)

@app.patch("/faces/{face_id}/block")
async def block_face(face_id: int, block: bool = True,
                     me: models.User = Depends(current_user),
                     db: AsyncSession = Depends(database.get_session)):
    fp = await crud.set_face_blocked(db, face_id=face_id, blocked=block)
    if not fp:
        raise HTTPException(404, "face not found")
    return {"face_id": face_id, "is_blocked": fp.is_blocked}

# ---------NOTIFICATIONS ---------------
@app.post("/notifications/isSubscribed", response_model=schemas.NotificationIsSubscribedResponse)
async def check_subscribed(
    body: schemas.NotificationIsSubscribed,
    db: AsyncSession = Depends(database.get_session),
    me: models.User = Depends(current_user)):

    token = await crud.get_fcm_token(db, body.fcm_token, body.doorbell_id, me.user_id)

    if token:
        response = {"is_subscribed": True}
    else:
        response = {"is_subscribed": False}
    
    return response


@app.post("/notifications/subscribe", status_code=201)
async def subscribe_user(
    body: schemas.NotificationSubscribe, 
    db: AsyncSession = Depends(database.get_session), 
    me: models.User = Depends(current_user)):

    # Check doorbell exists
    doorbell = await crud.get_doorbell_by_id(db ,body.doorbell_id)
    if not doorbell:
        raise HTTPException(400, "Doorbell not found")
    
    # Check fcm token hasn't already been added
    existing_token = await crud.get_fcm_token(db, body.fcm_token, body.doorbell_id, me.user_id)
    if existing_token:
        raise HTTPException(400, "FCM token already exists")

    token = await crud.create_fcm_token(db, body.fcm_token, me.user_id, body.doorbell_id)

    if not token:
        raise HTTPException(400, "Failed to subscribe user")
    
    return {"message": "Subscription successful"}
    
@app.post("/notifications/unsubscribe")
async def unsubscribe_user(
    body: schemas.NotificationUnsubscribe,
    db: AsyncSession = Depends(database.get_session),
    me: models.User = Depends(current_user)):

    # Check doorbell exists
    doorbell = await crud.get_doorbell_by_id(db, body.doorbell_id)
    if not doorbell:
        raise HTTPException(400, "Doorbell not found")
    
    # Check that fcm token exists
    existing_token = await crud.get_fcm_token(db, body.fcm_token, body.doorbell_id, me.user_id)
    if not existing_token:
        raise HTTPException(400, "Token could not be found")
    
    await crud.delete_fcm_token(db, body.fcm_token, body.doorbell_id, me.user_id)

    return {"message": "Unsubscribed successfully"}

@app.post("/notifications/updateFCMToken")
async def update_FCM_token(body: schemas.NotificationUpdateToken, db: AsyncClient = Depends(database.get_session), me: models.User = Depends(current_user)):
    old_token = body.old_token
    new_token = body.new_token

    if old_token == new_token:
        return {"message": "Token hasn't changed, no update necessary"}

    doorbells = await crud.get_doorbells_by_fcm_token(db, old_token)

    for doorbell in doorbells:
        # Remove old notification token
        await crud.delete_fcm_token(db, old_token, doorbell, me.user_id)

        # Replace with new token
        await crud.create_fcm_token(db, new_token, me.user_id, doorbell)
    
    return {"message": "FCM token updated successfully"}



@app.post("/notifications/notify/motionDetected")
async def notify_motion_detected(body: schemas.NotifyMotionDetected, db:AsyncSession = Depends(database.get_session)):
    # Find doorbell from token given
    doorbell_id = await crud.get_doorbell_by_token(db, body.token)

    if not doorbell_id:
        raise HTTPException(400, "Doorbell token invalid, doorbell not found")
    
    tokens = await crud.get_fcm_tokens_by_doorbell(db, doorbell_id)
    doorbell = await crud.get_doorbell_by_id(db, doorbell_id)

    if not doorbell:
        raise HTTPException(400, "Doorbell provided is invalid, cannot be attached to a doorbell record")
    
    doorbell_name = doorbell.doorbell_name

    for token in tokens:
        try:
            await send_fcm_notification(token, "Motion detected", f"{doorbell_name}: Someone has been detected at this doorbell")
        except Exception as err:
            print(f"Error sending notification to token {token}: {err}")

    return {"message": "Notification sent"}
