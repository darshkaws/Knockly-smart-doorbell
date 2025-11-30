import os, cv2, pickle, shutil, threading, time, datetime, serial, mysql.connector
import serial.tools.list_ports
import face_recognition
import numpy as np
from pathlib import Path
from picamera2 import Picamera2
from pyftpdlib.authorizers import DummyAuthorizer
from pyftpdlib.handlers import FTPHandler
from pyftpdlib.servers import FTPServer
from libcamera import Transform

import sendMotionNotif

# ------------------------- Configuration -------------------------

def find_arduino_port():
    """
    Scan available serial ports and return the one that looks like
    an Arduino (by description or device name). Returns None if not found.
    """
    ports = serial.tools.list_ports.comports()
    for port in ports:
        if ("Arduino" in port.description
            or "ttyACM" in port.device
            or "ttyUSB" in port.device):
            return port.device
    return None

# Serial parameters for Arduino connection
pi_port   = find_arduino_port()
baud_rate = 9600
timeout_s = 0.1

# FTP server credentials
FTP_USER     = "pi"
FTP_PASSWORD = "raspberry"

# Directory and file names
FTP_DIR       = "ftp_videos"
PROCESSED_DIR = "processed_videos"
COMMAND_FILE  = "command.txt"

# MySQL database connection info
DB_CONFIG = {
    "host":     "localhost",
    "user":     "door_user",
    "password": "door_user_door_user",
    "database": "smart_door_db",
}

# ID of this doorbell in the database
DOORBELL_ID = 1

# Constraints for video validation
MIN_VIDEO_DURATION   = 15   # seconds
BRIGHTNESS_THRESHOLD = 32   # minimum average brightness

# Cooldown between door-open commands on same face
OPEN_DOOR_COOLDOWN = 5      # seconds

# ------------------------- Paths & Files -------------------------

base_dir      = Path(__file__).parent
log_file      = base_dir / "log.txt"
ftp_dir       = base_dir / FTP_DIR
processed_dir = base_dir / PROCESSED_DIR
command_file  = base_dir / COMMAND_FILE

# Ensure required directories exist
ftp_dir.mkdir(exist_ok=True)
processed_dir.mkdir(exist_ok=True)

# ------------------------- Hardware Setup -------------------------

# Initialize serial connection to Arduino
arduino = serial.Serial(pi_port, baudrate=baud_rate, timeout=timeout_s)

# Initialize and configure Pi Camera
cam = Picamera2()
cam.configure(
    cam.create_video_configuration(
        main={"format": "XRGB8888", "size": (640, 480)},
        transform=Transform(rotation=180)
    )
)

# ------------------------- Global Locks -------------------------

# Lock to serialize calls into dlib/face_recognition
DLIB_LOCK = threading.Lock()

# ------------------------- Utility Functions -------------------------

def log(msg):
    """
    Write a timestamped log message to console and append it to log_file.
    """
    now  = datetime.datetime.now().strftime("%Y-%m-%d %I:%M:%S %p")
    line = f"{now} | {msg}"
    print(line)
    with log_file.open("a") as f:
        f.write(line + "\n")
        f.flush()

def send_command(cmd):
    """
    Send a one-character command to the Arduino over serial,
    then log a descriptive message for that command.
    """
    try:
        arduino.write(bytes(cmd, 'utf-8'))
    except Exception as e:
        log(f"[ERROR] Sending command {cmd}: {e}")
    time.sleep(0.05)

    # Map command codes to log messages
    if   cmd == "0":
        log("Middleman startup complete.")
    elif cmd == "1":
        log("Phone has requested to open the door.")
    elif cmd == "2":
        log("Phone has requested to close the door.")
    elif cmd == "3":
        log("Known face detected.")
    elif cmd == "4":
        log("Blocked face detected.")

def read():
    """
    Read any available line from Arduino serial,
    log it, and if it's a doorbell ring, send a notification.
    """
    try:
        line = arduino.readline().decode("utf-8", errors="ignore").strip()
        if line:
            log(f"Arduino: {line}")
            if line == "Doorbell rung.":
                sendMotionNotif.personAtDoorNotif()
    except Exception as e:
        log(f"[ERROR] Reading Arduino: {e}")

def check_command():
    """
    If COMMAND_FILE exists, read its contents as a command,
    delete the file, and return the command string.
    """
    if command_file.is_file():
        with command_file.open('r') as f:
            command = f.read().strip()
        command_file.unlink()
        return command
    return None

def python_loop():
    """
    Loop that checks for commands from the app (via file),
    forwards them to Arduino, and logs incoming serial messages.
    """
    while True:
        cmd = check_command()
        if cmd:
            send_command(cmd)
        read()
        time.sleep(0.1)

# ------------------------- dlib-safe wrappers -------------------------

def dlib_face_locations(img):
    """Thread-safe wrapper around face_recognition.face_locations."""
    with DLIB_LOCK:
        return face_recognition.face_locations(img)

def dlib_face_encodings(img, locs=None):
    """Thread-safe wrapper around face_recognition.face_encodings."""
    with DLIB_LOCK:
        return face_recognition.face_encodings(img, locs)

# ------------------------- Orientation Helpers -------------------------

ROTATE_CODE = {
    0:   None,
    90:  cv2.ROTATE_90_CLOCKWISE,
    180: cv2.ROTATE_180,
    270: cv2.ROTATE_90_COUNTERCLOCKWISE,
}

def rotate_image(img, angle):
    """Rotate an image by 0/90/180/270° using OpenCV."""
    code = ROTATE_CODE[angle]
    return img if code is None else cv2.rotate(img, code)

def find_faces_multi_orientation(img_rgb):
    """
    Attempt face detection/encoding on 0°, 90°, 180°, and 270° rotations,
    returning the encodings from the first orientation where faces appear.
    """
    for angle in (0, 90, 180, 270):
        test = rotate_image(img_rgb, angle) if angle else img_rgb
        test = np.ascontiguousarray(test)
        locs = dlib_face_locations(test)
        if locs:
            return dlib_face_encodings(test, locs)
    return []

# ------------------------- File-upload race guard -------------------------

def is_file_complete(path: Path, wait_s: float = 1.0) -> bool:
    """
    Check if a file is finished uploading by verifying its size
    remains constant over wait_s seconds.
    """
    s1 = path.stat().st_size
    time.sleep(wait_s)
    return s1 == path.stat().st_size

# ------------------------- DB Helpers -------------------------

def store_face_encoding(doorbell_id, profile_name, encoding):
    """
    Insert a single face encoding blob into the face_profiles table.
    """
    conn = cur = None
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cur  = conn.cursor()
        cur.execute(
            "INSERT INTO face_profiles (doorbell_id, face_profile_name, face_data) "
            "VALUES (%s, %s, %s)",
            (doorbell_id, profile_name, pickle.dumps(encoding)),
        )
        conn.commit()
        log(f"[DEBUG] Stored encoding for {profile_name}")
    except Exception as e:
        log(f"[DB ERROR] {e}")
    finally:
        if cur:  cur.close()
        if conn: conn.close()

def fetch_all_encodings():
    """
    Retrieve all stored face encodings and their names from the database.
    """
    conn = mysql.connector.connect(**DB_CONFIG)
    cur  = conn.cursor()
    cur.execute("SELECT face_profile_name, face_data FROM face_profiles")
    encs, names = [], []
    for name, blob in cur.fetchall():
        encs.append(pickle.loads(blob))
        names.append(name)
    cur.close()
    conn.close()
    return encs, names

# ------------------------- Video Processing -------------------------

def extract_encodings_from_video(video_path: str, doorbell_id: int):
    """
    Validate a video file for minimum duration and brightness,
    then sample frames to detect faces and store up to 20 encodings.
    """
    log(f"[INFO] Validating video: {video_path}")
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        log("[ERROR] Could not open video file.")
        return

    fps   = cap.get(cv2.CAP_PROP_FPS)
    total = cap.get(cv2.CAP_PROP_FRAME_COUNT)
    dur   = total / fps if fps else 0
    if dur < MIN_VIDEO_DURATION:
        log(f"[ERROR] Video too short ({dur:.1f}s).")
        cap.release()
        return

    ok, first = cap.read()
    if not ok:
        log("[ERROR] Could not read first frame.")
        cap.release()
        return
    if cv2.cvtColor(first, cv2.COLOR_BGR2GRAY).mean() < BRIGHTNESS_THRESHOLD:
        log("[ERROR] Too dark.")
        cap.release()
        return

    cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
    log(f"[INFO] Video OK ({dur:.1f}s)")

    step  = max(int(total) // 20, 1)
    saved = 0
    name  = Path(video_path).stem

    for i in range(0, int(total), step):
        cap.set(cv2.CAP_PROP_POS_FRAMES, i)
        ok, frame = cap.read()
        if not ok:
            log(f"[WARN] Bad frame at {i}")
            continue
        rgb  = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        encs = find_faces_multi_orientation(rgb)
        for enc in encs:
            store_face_encoding(doorbell_id, name, enc)
            saved += 1
        if saved >= 20:
            break

    cap.release()
    log(f"[INFO] Stored {saved} encodings for {name}")

# ------------------------- Auto-processing Thread -------------------------

def has_been_processed(path: Path) -> bool:
    """Return True if this file has already been moved to processed_dir."""
    return (processed_dir / path.name).exists()

def process_latest_video(doorbell_id):
    """
    Scan ftp_dir for new .mp4 files, validate completeness,
    skip already-processed, then extract encodings and move file.
    """
    for mp4 in ftp_dir.glob("*.mp4"):
        if not is_file_complete(mp4):
            continue
        if has_been_processed(mp4):
            continue
        try:
            extract_encodings_from_video(str(mp4), doorbell_id)
            shutil.move(str(mp4), str(processed_dir / mp4.name))
        except Exception as e:
            log(f"[ERROR] Auto-process {mp4.name}: {e}")

def auto_process_loop():
    """Run process_latest_video every 5 seconds in its own thread."""
    while True:
        try:
            process_latest_video(DOORBELL_ID)
        except Exception as e:
            log(f"[ERROR] auto_process_loop: {e}")
        time.sleep(5)

# ------------------------- Live Recognition (MAIN THREAD) -------------------------

def run_live_recognition():
    """
    Continuously capture camera frames, detect faces,
    and open the door on recognized faces with a cooldown.
    """
    known_encs, known_names = fetch_all_encodings()
    log("[INFO] Running live facial recognition…")
    last_open_time = 0.0

    while True:
        frame = cam.capture_array().copy()
        small = cv2.resize(frame, (0, 0), fx=0.25, fy=0.25)
        rgb   = cv2.cvtColor(small, cv2.COLOR_BGR2RGB)
        rgb   = np.ascontiguousarray(rgb)

        try:
            locs = dlib_face_locations(rgb)
            encs = dlib_face_encodings(rgb, locs)
        except Exception as e:
            log(f"[ERROR] Recognition failed: {e}")
            continue

        for (t, r, b, l), enc in zip(locs, encs):
            matches = face_recognition.compare_faces(known_encs, enc)
            name    = "Unknown"
            if known_encs:
                dists = face_recognition.face_distance(known_encs, enc)
                idx   = np.argmin(dists)
                if matches[idx]:
                    name = known_names[idx]

            # Draw bounding box and name label
            color = (0,255,0) if name!="Unknown" else (0,0,255)
            t, r, b, l = [x*4 for x in (t,r,b,l)]
            cv2.rectangle(frame, (l,t), (r,b), color, 2)
            cv2.putText(frame, name, (l+6, t-6),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)

            now = time.time()
            if name!="Unknown" and (now-last_open_time)>=OPEN_DOOR_COOLDOWN:
                send_command("3")
                last_open_time = now
            elif name=="Unknown" and (now-last_open_time)>=OPEN_DOOR_COOLDOWN:
                threading.Thread(
                    target=sendMotionNotif.personAtDoorNotif,
                    daemon=True
                ).start()
                last_open_time = now

        cv2.imshow("Live Feed", frame)
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

# ------------------------- FTP Server Thread -------------------------

def start_ftp_server():
    """Launch an FTP server on port 2121 to accept video uploads."""
    authorizer = DummyAuthorizer()
    authorizer.add_user(FTP_USER, FTP_PASSWORD, str(ftp_dir), perm="elradfmw")
    handler = FTPHandler; handler.authorizer = authorizer
    server  = FTPServer(("0.0.0.0", 2121), handler)
    log("[INFO] FTP Server started on port 2121.")
    server.serve_forever()

# ------------------------- Startup & Main loop -------------------------

def startup():
    """Bring up camera, reset Arduino, and re-detect its port."""
    global arduino
    cam.start()
    send_command("0")
    time.sleep(2)
    port    = find_arduino_port()
    arduino = serial.Serial(port, baudrate=baud_rate, timeout=timeout_s)
    print("Listening for log messages...")

def main():
    startup()
    threading.Thread(target=start_ftp_server,    daemon=True).start()
    threading.Thread(target=auto_process_loop,   daemon=True).start()
    threading.Thread(target=python_loop,         daemon=True).start()
    try:
        run_live_recognition()
    except KeyboardInterrupt:
        log("[INFO] KeyboardInterrupt – shutting down …")
    finally:
        cam.stop()
        cv2.destroyAllWindows()
        log("[INFO] Clean shutdown complete.")

if __name__ == "__main__":
    main()
