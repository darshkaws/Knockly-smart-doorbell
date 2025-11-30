// Library imports
#include <Servo.h>
#include <Ultrasonic.h>
#include <avr/wdt.h>

// Pin variables initialization (Add one to port number)
const int manualButtonPin = 5;
const int doorbellButtonPin = 9;

// Port variables initialization
Ultrasonic doorSensor(2);
const int motorPort = 6;
const int buzzerPort = 7;

// State variables initialization
bool manualButtonState;
bool doorbellButtonState;

// Flag variables initialization
bool wasIdle = false;
bool disableAlarm = false;

// Integer variables initialization
int doorDistance;
int overrideCommand;

// Object creation
Servo door;

void setup() {
    
  // Buttons

  // Button pin modes
  pinMode(manualButtonPin, INPUT);
  pinMode(doorbellButtonPin, INPUT);

  // Motor

  // Attaching motor to port
  door.attach(motorPort);
  // Sets the door at a "closed" position
  door.write(90);

  // Buzzer

  // Buzzer pin mode
  pinMode(buzzerPort, OUTPUT);
  // No sound on activation
  noTone(buzzerPort);

  // Python <-> Arduino

  // Setting serial baudrate
	Serial.begin(9600); 
	// Setting max serial data wait time 
	Serial.setTimeout(1);

  while(!Serial) {
    ;
  }

  // Prints message to log
  Serial.println("Arduino startup complete.");
}

void loop() {
  // Reading data from modules

  // Getting state of manual override button  
  manualButtonState = digitalRead(manualButtonPin);
  // Getting state of doorbell button 
  doorbellButtonState = digitalRead(doorbellButtonPin);
  // Getting distance measured by doorSensor
  doorDistance = doorSensor.read(CM);

  // Bootleg method to avoid fully covered ranger getting max distance when both transmitter and reciever are covered
  if (doorDistance == 357) { // Max distance
    // Sets as no distance
    doorDistance = 0;
  }

  // External command check

  // Detects if there is data in Serial sent by Python code
  if (Serial.available() > 0) { // Data detected
    // Runs function
    override();

    // Changing flag for logging and preventing spam
    wasIdle = false;
  }

  // Manual override button check

  // Detects if the manual override button is pressed
  if (manualButtonState == LOW) { // Button pressed
    // Prints log to Serial
    Serial.println("Manual open button pressed.");

    // Runs function
    open_close();

    // Adds delay so door can close without triggering an alarm
    delay(1000);

    // Changing flag for logging and preventing spam
    wasIdle = false;

    
  }

  // Doorbell button check

  // Detects if the doorbell button is pressed
  if (doorbellButtonState == LOW) { // Button pressed
    // Runs function
    doorbell();
    
    // Changing flag for logging and preventing spam
    wasIdle = false;
  }

  // Door sensor check

  // Detects if door is open and alarm is not disabled
  if (doorDistance >= 6 && disableAlarm == false) { // Open door and alarm not disabled
    // Runs function
    alarm();
    
    // Changing flag for logging and preventing spam
    wasIdle = false;
  }

  // Idle state

  // Detects if last action was idle()
  if (wasIdle == false) { // Last action is not idle()
    // Runs function
    idle();
    
    // Changing flag back to avoid spam
    wasIdle = true;
  }
  
}

// Function for opening and closing door after a period of time
void open_close() {

  // Runs function
  open();

  // Waits 5 seconds for user to enter
  delay(5000);

  // Runs function
  close();
}

// Function that rings the doorbell
void doorbell() {

  // Prints log to Serial
  Serial.println("Doorbell rung.");
  
  // Doorbell segment
  for (int count = 0; count < 3; count++) { // Loops chord 3 times
    // Plays high note for 0.5 seconds
    tone(buzzerPort, 100);
    delay(500);

    // Plays low note for 0.5 seconds
    tone(buzzerPort, 50);
    delay(500);

    // Plays no note for 0.5 seconds
    noTone(buzzerPort);
    delay(500);
  }

  // Stops buzzer from ringing all the time
  noTone(buzzerPort);
}

// Function that rings the alarm
void alarm(){

  // Prints log to Serial
  Serial.println("Alarm rung.");

  // Alarm segment
  for (int count = 0; count < 10; count++) { // Loops chord 10 times
    // Plays a high note for 0.1 second
    tone(buzzerPort, 100);
    delay(100);

    // Plays no sound for 0.1 second
    noTone(buzzerPort);
    delay(100);
  }
}  

// Function that sets the system at an idle state
void idle() {

  // Prints log to Serial
  Serial.println("Door idle."); 
}

// Function to reset the arduino
void reset() {
  // Initiates (almost) instant restart
  wdt_enable(WDTO_15MS);

  // Prevents unwanted code executions
  while(1);
}

// Function that opens the door
void open() {
  // Opens the door
  door.write(0);

  // Prints log to Serial
  Serial.println("Door opened.");
}

// Function that closes the door
void close() {

  // Closes the door
  door.write(90);

  // Prints log to Serial
  Serial.println("Door closed.");
}

// Function that reads commands from python to execute actions
void override() {
  // Translates the serial data as integers
  overrideCommand = Serial.readString().toInt();

  // Checks which command is sent
  if (overrideCommand == 0) { // System startup
    // Runs function
    reset();
  }
  else if (overrideCommand == 1) { // Open command
    // Runs function
    open();

    // Disables alarm
    disableAlarm = true;
  } else if (overrideCommand == 2){ // Close command
    // Runs function
    close();

    // Adds delay so door can close without triggering an alarm
    delay(1000);

    // Enables alarm
    disableAlarm = false;
  } else if (overrideCommand == 3){ // Allowed face scanned

    // Runs function
    open_close();

    // Adds delay so door can close without triggering an alarm
    delay(1000);
    
  }

  // Clears Serial for next command
  Serial.read();
}







