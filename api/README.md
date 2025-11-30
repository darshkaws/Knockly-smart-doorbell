# Knockly API v2

A compact FastAPI service that runs directly on a Raspberry Pi.  
One `uvicorn` worker talks to the local MariaDB instance—no extra services required.

---

## 1. Start-up

```bash
cd api                   # project root on the Pi
python3 -m venv venv && source venv/bin/activate

pip install -r requirements.txt

cd src

# one-time: create a signing key for JWTs
export JWT_SECRET=$(openssl rand -hex 32)

ngrok http --url=mutual-osprey-actually.ngrok-free.app 8000

uvicorn main:app --host 0.0.0.0 --port 8000
```

API Base Url: `https://mutual-osprey-actually.ngrok-free.app`

Interactive docs available at `https://mutual-osprey-actually.ngrok-free.app/docs`.

---

## 2. Core Endpoints

| Method   | Path                                                  | Purpose                                                             |
|----------|-------------------------------------------------------|---------------------------------------------------------------------|
| **POST** | `/auth/register`                                      | Create account and obtain JWT                                       |
| **POST** | `/auth/login`                                         | Login and obtain JWT (`totp_code` optional)                         |
| **POST** | `/auth/2fa/setup`                                     | Enable TOTP, returns QR and URI                                     |
| **POST** | `/auth/2fa/disable`                                   | Disable TOTP                                                        |
| **GET**  | `/users/me`                                           | Retrieve user profile                                               |
| **GET**  | `/users/me/settings`                                  | Retrieve user toggle settings                                       |
| **GET**  | `/doorbells`                                          | List doorbells linked to the current user (with linked users & roles) |
| **POST** | `/doorbells`                                          | Create a new doorbell and auto-link to creator                      |
| **POST** | `/doorbells/link-user`                                | Link another user to a doorbell with optional role assignment       |
| **GET**  | `/doorbells/{id}/permissions`                         | List permissions with granted status                                |
| **PATCH**| `/doorbells/{id}/permissions/override`                | Add or remove a permission override                                 |
| **GET**  | `/doorbells/{id}/faces`                               | List face profiles for a doorbell                                   |
| **PATCH**| ``/faces/{face_id}/block?block=true\|false``          | Block or unblock a face profile                                     |
| **POST** | `/notifications/isSubscribed`                         | Check if user is subscribed to notifications for a given doorbell   |
| **POST** | `/notifications/subscribe`                            | Subscribe user to notifications for a given doorbell                |
| **POST** | `/notifications/unsubscribe`                          | Unsubscribe user from notifications for a given doorbell            |
| **POST** | `/notifications/notify/motionDetected`                | Push motion-detected notification to all devices from doorbell      |

_All routes except `/auth/*` require `Authorization: Bearer <token>`._

_`/notifications/notify/*` does not require an authorisation token but does require a UUID stored on the doorbell to identify it_

---

## 3. Permission Logic

```text
(role permissions) ± (user overrides) → effective set
```

- Role is assigned via `user_doorbell`.
- Overrides are stored in `user_permissions_override`.
- CRUD helpers compute the resolved permission set, allowing routes to simply verify access:

```python
if "open_door" not in perms:
    raise HTTPException(status_code=403)
```

---

## 4. Directory Structure

```text
main.py        HTTP routes and auth dependency
models.py      SQLAlchemy ORM mappings (users, doorbells, faces, roles, permissions)
crud.py        Database helpers (auth, permissions, faces, doorbell linking)
security.py    bcrypt, JWT, TOTP helpers
database.py    Async connection pool
schemas.py     Pydantic request/response models (doorbells include linked users and roles)
config.py      Database credentials and JWT settings (dotenv loaded)
requirements.txt
```

---

## 5. Possible Future Improvements

- Extend CRUD for doorbell logs, sensor data, and motion events.
- Deploy behind a reverse proxy (Caddy or Nginx) if accessible from outside LAN.
- Add unit tests using `pytest` and FastAPI `TestClient`.
- Use Alembic for database migrations.
- Implement role-based access control enforcement in frontend applications.
- Optimize database queries for scaling to larger numbers of users and doorbells.

---

# Summary of Changes

| Previous                                    | Updated                                                      |
|---------------------------------------------|--------------------------------------------------------------|
| Basic auth and doorbell listing             | Full doorbell creation, user linking, and role visibility   |
| JWT manual setup                            | Environment-based JWT secret loading with dotenv            |
| Limited doorbell info                       | Now includes linked users and their roles in responses      |
---

