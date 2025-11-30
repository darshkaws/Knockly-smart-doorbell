from fastapi import HTTPException
from sqlalchemy.future import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload                       
from typing import Optional, List, Set
import models, security, schemas

# ---------- user helpers ----------
async def _user_query():
    """Reusable SELECT that eagerly loads the settings relationship."""
    return select(models.User).options(selectinload(models.User.settings))

async def get_user_by_username(db: AsyncSession, username: str):
    stmt = (await _user_query()).where(models.User.username == username)
    res = await db.execute(stmt)
    return res.scalars().first()

async def get_user_by_email(db: AsyncSession, email: str):
    stmt = (await _user_query()).where(models.User.email == email)
    res = await db.execute(stmt)
    return res.scalars().first()

async def create_user(db: AsyncSession, *, username, email, password, display_name):
    user = models.User(
        username=username,
        email=email,
        password_hash=security.hash_password(password),
        display_name=display_name,
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)

    # insert default settings row
    db.add(models.UserSettings(user_id=user.user_id))
    await db.commit()
    return user

async def authenticate(db: AsyncSession, identifier: str, password: str):
    # single query for either username OR email, with settings pre‑loaded
    stmt = (await _user_query()).where(
        (models.User.username == identifier) | (models.User.email == identifier)
    )
    user = (await db.execute(stmt)).scalars().first()
    if user and security.verify_password(password, user.password_hash):
        return user
    return None

# ---------- doorbell helpers ----------
async def list_user_doorbells(db: AsyncSession, user_id: int):
    q = (
        select(models.Doorbell)
        .options(selectinload(models.Doorbell.users))  # preloading users
        .join(models.user_doorbell, models.user_doorbell.c.doorbell_id == models.Doorbell.doorbell_id)
        .where(models.user_doorbell.c.user_id == user_id)
    )
    res = await db.execute(q)
    doorbells = res.scalars().all()

    # Now enrich linked_users manually
    enriched_doorbells = []
    for doorbell in doorbells:
        linked_users = []
        for user in doorbell.users:
            # Fetch role_id manually from user_doorbell join table
            result = await db.execute(
                select(models.user_doorbell.c.role_id)
                .where(
                    (models.user_doorbell.c.user_id == user.user_id) &
                    (models.user_doorbell.c.doorbell_id == doorbell.doorbell_id)
                )
            )
            role_id = result.scalar()

            # Optionally get role_name too
            role_name = None
            if role_id:
                role_res = await db.execute(
                    select(models.Role.role_name).where(models.Role.role_id == role_id)
                )
                role_name = role_res.scalar()

            linked_users.append(schemas.LinkedUserOut(
                user_id=user.user_id,
                username=user.username,
                role_id=role_id,
                role_name=role_name
            ))

        doorbell.linked_users = linked_users
        enriched_doorbells.append(doorbell)

    return enriched_doorbells

async def get_doorbell_by_id(db: AsyncSession, doorbell_id: int):
    stmt = select(models.Doorbell).where(models.Doorbell.doorbell_id == doorbell_id)
    result = await db.execute(stmt)
    return result.scalars().first()

async def get_doorbell_by_token(db:AsyncSession, token: str):
    stmt = select(models.DoorbellTokens.doorbell_id).where(models.DoorbellTokens.token == token)
    result = await db.execute(stmt)
    return result.scalars().first()

async def get_users_from_doorbell(db: AsyncSession, doorbell_id: int):
    stmt = select(models.User, models.Role).select_from(models.user_doorbell).join(models.User, models.user_doorbell.c.user_id == models.User.user_id).outerjoin(models.Role, models.user_doorbell.c.role_id == models.Role.role_id).where(models.user_doorbell.c.doorbell_id == doorbell_id)
    results = await db.execute(stmt)
    
    linked_users = [
        schemas.LinkedUserOut(
            user_id = user.user_id,
            username = user.username,
            display_name = user.display_name,
            role_id = role.role_id if role else None,
            role_name = role.role_name if role else None
        )
        for user, role in results.all()
    ]

    return linked_users

# ---------- permission maths ----------
async def _role_permission_ids(db: AsyncSession, role_id: int) -> Set[int]:
    q = select(models.roles_permissions).where(
        models.roles_permissions.c.role_id == role_id
    )
    res = await db.execute(q)
    return {row.permission_id for row in res.fetchall()}

async def effective_permissions(
    db: AsyncSession, *, user_id: int, doorbell_id: int
) -> Set[str]:
    # 1) role attached in user_doorbell
    q = select(models.user_doorbell).where(
        (models.user_doorbell.c.user_id == user_id)
        & (models.user_doorbell.c.doorbell_id == doorbell_id)
    )
    row = (await db.execute(q)).first()
    if not row:
        return set()
    role_id = row.role_id

    # 2) permissions from role
    ids = await _role_permission_ids(db, role_id)

    # 3) apply overrides
    ovr_q = select(models.UserPermOverride).where(
        models.UserPermOverride.user_id == user_id
    )
    for o in (await db.execute(ovr_q)).scalars():
        if o.is_granted:
            ids.add(o.permission_id)
        else:
            ids.discard(o.permission_id)

    # 4) translate IDs ➜ names
    if not ids:
        return set()
    names_q = select(models.Permission.permission_name).where(
        models.Permission.permission_id.in_(ids)
    )
    res = await db.execute(names_q)
    return {n for (n,) in res.all()}

async def set_override(
    db: AsyncSession, *, user_id: int, perm_name: str, is_granted: bool
):
    perm_row = (
        await db.execute(
            select(models.Permission).where(
                models.Permission.permission_name == perm_name
            )
        )
    ).scalar_one_or_none()
    if not perm_row:
        return False

    override = await db.get(
        models.UserPermOverride, (user_id, perm_row.permission_id)
    )
    if override:
        override.is_granted = is_granted
    else:
        override = models.UserPermOverride(
            user_id=user_id,
            permission_id=perm_row.permission_id,
            is_granted=is_granted,
        )
        db.add(override)
    await db.commit()
    return True

# ---------- face profile helpers ----------
async def list_faces(db: AsyncSession, *, doorbell_id: int) -> List[models.FaceProfile]:
    stmt = select(models.FaceProfile).where(models.FaceProfile.doorbell_id == doorbell_id)
    res = await db.execute(stmt)
    return res.scalars().all()

async def add_face(db: AsyncSession, *, doorbell_id: int,
                   face_data: bytes, name: str | None):
    fp = models.FaceProfile(
        doorbell_id=doorbell_id,
        face_data=face_data,
        face_profile_name=name,
    )
    db.add(fp)
    await db.commit()
    await db.refresh(fp)
    return fp

async def set_face_blocked(db: AsyncSession, *, face_id: int, blocked: bool):
    fp = await db.get(models.FaceProfile, face_id)
    if not fp:
        return None
    fp.is_blocked = blocked
    await db.commit()
    return fp

#------ Notification Helpers -----
async def get_fcm_token(db: AsyncSession, fcm_token: str, doorbell_id: int, user_id: int):
    stmt = select(models.NotifTokens).where(models.NotifTokens.fcm_token == fcm_token, 
                                            models.NotifTokens.doorbell_id == doorbell_id, 
                                            models.NotifTokens.user_id == user_id)
    result = await db.execute(stmt)
    return result.scalars().first()

async def get_fcm_tokens_by_doorbell(db: AsyncSession, doorbell_id: int):
    stmt = select(models.NotifTokens.fcm_token).where(models.NotifTokens.doorbell_id == doorbell_id)
    result = await db.execute(stmt)
    return result.scalars().all()

async def create_fcm_token(db: AsyncSession, fcm_token: str, user_id: int, doorbell_id: int):
    token = models.NotifTokens(
        fcm_token = fcm_token,
        user_id = user_id,
        doorbell_id = doorbell_id
    )

    db.add(token)
    await db.commit()
    await db.refresh(token)
    return token

async def delete_fcm_token(db: AsyncSession, fcm_token: str, doorbell_id: int, user_id: int):
    token = await get_fcm_token(db, fcm_token, doorbell_id, user_id)

    if not token:
        raise HTTPException(400, "Token linked to user and doorbell not found")
    
    await db.delete(token)
    await db.commit()
    return {"message": "Token deleted"}

async def get_doorbells_by_fcm_token(db: AsyncSession, fcm_token: str):
    stmt = select(models.NotifTokens.doorbell_id).where(models.NotifTokens.fcm_token == fcm_token)
    doorbells = await db.execute(stmt)

    return doorbells.scalars()

    