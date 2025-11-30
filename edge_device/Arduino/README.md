# Arduino

## Setup

To be able to ultilize this code, you will require these equipment:

1. Arduino Base Shield
2. DF Robot Expansion Shield (Arduino Leonardo)
3. USB to Micro-USB
4. LED Button x 2
5. Servo Motor
6. Ultrasonic Ranger
7. Buzzer
8. Short axle

You will need to install Arduino IDE (https://www.arduino.cc/en/software/) to be able to send the code into the Arduino.

You will need to install the module in these ports on the Arduino to run the code without any editing:

1. D2 - Ultrasonic ranger
2. D4 - LED Button
3. D6 - Servo Motor
4. D7 - Buzzer
5. D8 - LED Button

It is best to install the short axle section of the motor aligned parallel to the motor and extended section along the bulk of the motor for the intended outcome.

Please remove this block of code in the setup() if you are planning to run the Arduino by itself.

'''

  while(!Serial) {
    ;
  }

'''

## Usage

While the system is in use, the ranger must be placed sensor down so that the system will not trigger the alarm function (or change the disableAlarm flag to true).

The message outputs will be sent to the Serial Monitor in the Arduino IDE if you want to see them.

The button on D4 is the button that triggers the open and close cycle of the door.

The button on D8 is the button that triggers the doorbell ring.

## Common Problems

**Arduino not detected by system?**

It may be a hardware issue on the Arduino, you will need to buy a new Arduino.

**Buzzer ringing 10 times again and again with a short delay in between?**

You may have have left the ranger with a large amount of distance between the sensors and the nearest object in front of it.

**Setup() messages not popping up?**

You may not have removed this code segment as mentioned above.

'''

  while(!Serial) {
    ;
  }

'''

**System not working as expected?**

You may have not installed the modules in the right ports.