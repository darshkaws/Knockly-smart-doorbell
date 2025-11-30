# Python

## Setup

You will need to have completed the setup instructions in the [api folder](../../api/README.md), the [Arduino folder](../Arduino/README.md) and the [mobile_app folder](../../mobile_app/README.md).

## Usage

Please run these [bash scripts](<../Startup scripts>) in this sequence as to ensure every single component works:

1. ngrok.sh
2. uvicorn.sh
3. auto.sh

Please follow the usage intruction in the [Arduino folder](../Arduino/README.md) and the [mobile_app folder](../../mobile_app/README.md) as well.

## Common Problems

**Arduino not detected by system?**

It may be a hardware issue on the Arduino, you will need to buy a new Arduino, or you may have changed the setup code in the Arduino and/or the Python code.

**System displays "Device busy" error?**

You may have already have a program running the camera and will need to stop that program.

**There seems to be a file flickering in the directory when the system is running?**

That is just the system creating and deleting the command.txt file to send commands from the facial recogition or the app to the python loop.

**The system still recognizes a face even though I deleted the face video from the processed_videos folder?**

The face id encodings are stored on the database on the pi.

**The system is not training off of the video I sent from my phone?**

You may have named the video with an existing name in processed_videos.

**Notifications not sending to the app?**

You may have not connected your phone to the same network as the doorbell.

**Not getting a live feed?**

You may have not activated the ngrok.sh and uvicorn.sh scripts.