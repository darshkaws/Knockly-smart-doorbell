# Knockly - Smart IoT Doorbell System

Knockly is a smart IoT doorbell system developed as a university group project (8 members).  
It integrates an edge device (Raspberry Pi + Arduino), a FastAPI backend service, and an Android mobile application to provide a fully functional smart doorbell system with face recognition, door control, live video streaming, alarms, and user management.

This repository contains all main components: backend API, edge device scripts, database setup, and the mobile application.

---

## 1. System Overview

Knockly consists of three primary components:

### 1.1 Edge Device (Raspberry Pi + Arduino)
The Raspberry Pi manages:
- Face recognition using Python and OpenCV.
- Camera capture and live feed output.
- Communication with the backend API.
- Sending hardware commands to the Arduino.

The Arduino manages:
- Servo motor for door lock actuation.
- Buzzer for doorbell and alarm.
- Ultrasonic distance sensor.
- Doorbell button and manual override switch.

### 1.2 Backend API (FastAPI + MariaDB)
The backend provides:
- User authentication and authorisation.
- Storage of face encodings and metadata.
- Door control logic.
- Communication bridge between the app and the edge device.
- Database management and device state tracking.

### 1.3 Android Mobile Application
The app allows the user to:
- Log in and authenticate.
- View a live feed from the doorbell camera.
- Open and close the door.
- Upload videos or images to register new faces.
- Adjust notification and motion detection settings.
- View device status and alerts.

---

## 2. Repository Structure
```text
api/                     Backend API implemented in FastAPI
  src/
  requirements.txt
  README.md

edge_device/
  Arduino/               Arduino sketches for sensors, servo, and buzzer
  Python/                Raspberry Pi scripts for face recognition and control loop
  Databases/             SQL schema and initialization files
  Live_Feed/             Live camera feed functionality
  Startup_Scripts/       Bash scripts for starting services and tunneling

mobile_app/              Android application source code
docs/                    Documentation, reports, diagrams

README.md                This file
```

Folder names may vary slightly based on final repo organisation.

---

## 3. My Contribution

Although this was an 8-person group project, I was responsible for the majority of the core architecture, backend logic, and edge-device integration. My contributions included:

### Backend Development (FastAPI)

* Designed and implemented most backend routes and models.
* Developed user authentication and authorisation flows.
* Created endpoints for door control, face upload, and device management.
* Structured the backend logic connecting app, database, and edge device.

### Database Design and Integration

* Designed the MariaDB schema (users, devices, faces, settings).
* Wrote SQL schema files and integrated database access into the backend.
* Managed linking of face encodings to users and devices.

### Face Recognition on Raspberry Pi

* Implemented the face recognition pipeline using Python and OpenCV.
* Integrated face matching results into the door control process.
* Connected the recognition loop to API updates, logs, and notifications.

### Android App to API Integration

* Implemented communication between the mobile app and the FastAPI backend.
* Developed login flow, door control functions, and face upload interface.
* Assisted in designing the app's structure and API contracts.

### System Architecture

* Defined how the mobile app, backend API, Raspberry Pi, and Arduino communicate.
* Helped structure the overall project layout.
* Conducted integration testing and debugging across all components.
* Ensured final system cohesion for demonstration.

AI tools were used to speed up development, but all core decisions, debugging, architectural linking, and system integration required manual understanding and implementation.

---

## 4. Tech Stack

### Languages

* Python
* Java / Kotlin
* SQL
* Arduino C/C++

### Backend and Database

* FastAPI
* Uvicorn
* SQLAlchemy (if applicable)
* MariaDB / MySQL

### Edge Device

* Raspberry Pi OS
* Python (OpenCV, face_recognition)
* Arduino microcontroller

### Mobile

* Android Studio
* HTTP client libraries (e.g., Retrofit or similar)

### Additional Tools

* Bash scripting
* Ngrok for external connectivity
* Git, GitLab, GitHub

---

## 5. Running the System

### 5.1 Start the Backend API
```bash
cd api
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn src.main:app --host 0.0.0.0 --port 8000
```

### 5.2 Start the Edge Device Scripts
```bash
cd edge_device/Python
python3 main.py
```

Requirements:

* Raspberry Pi must detect the Arduino over serial.
* API URL must point to the Pi or ngrok tunnel.
* Database must be initialised.

### 5.3 Run the Android Application

* Open `mobile_app/` in Android Studio.
* Update API base URL in config.
* Run on a physical Android device on the same network.

---

## 6. Implemented Features

* Face recognition-based unlock.
* Manual open/close via mobile app.
* Live video streaming from doorbell.
* Upload and registration of new faces.
* Motion and distance-based alarms (ultrasonic sensor).
* Buzzer/doorbell functionality.
* Device status sync between app, backend, and edge device.
* Ngrok tunneling for off-LAN testing.

---

## 7. Potential Enhancements

* Replace polling with WebSockets for real-time control.
* Improve accuracy and performance of face recognition.
* Add secure environment variable management.
* Harden backend authentication.
* Introduce event history and cloud storage.
* Add multiple roles and granular permissions.
* Redesign API for public deployment.

---

## 8. Contact

For questions or technical clarifications:

**Darsh Kanjani**  
GitHub: [https://github.com/darshkaws](https://github.com/darshkaws)
