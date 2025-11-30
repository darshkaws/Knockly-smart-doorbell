# Knockly Android App

Knockly has an android app that can be used to connect to the doorbell, add faces, control it and more.

## Installation
To install the app on a device you must first install Android Studio. 

Then open the project on Android Studio in the **mobile_app/android** folder (if you do not open it in the correct folder it will not build correctly and you won't be able to run it).

Once opened a Gradle build should run to compile the app.

Then use the built in device manager to either create a virtual device to run the app on, or connect it to a physical device (either by WiFi or USB).

Finally click the 'Run' button at the top of the screen and the app should download onto your chosen device. Once downloaded this device can be disconnected from Android Studio and the app can be used.

## Usage
In order for the app to work both the doorbell and the API must be set up and running (see corresponding Read Me's).

To log into the app a testing account with a doorbell added has been set up, you can use the details:

```
username: johnDoe25
email: johnDoe@example.com
password: password
```
*Note:* This login will only work if using the database sql file provided on the GitLab

All options to interact with each doorbell can be found by clicking the button for the corresponding doorbell on the home page. Here you can see the live feed, open and close the door remotely as well as manage any users and faces connected to the doorbell as well as change the doorbell settings itself.

*Note:* Due to limited equipment there is currently no way to add a doorbell through the app. To connect a new doorbell to a user account this must be done directly through adding a record in the database to the table 'User_Doorbell' with the corresponding foreign keys.

## Common Problems
**Code not compiling?**

Please make sure you have opened the project in Android Studio in the *main_app/android* folder.

**Unable to connect to doorbell?**

Please make sure the device the app is running on is connected to the same WiFi network as the doorbell.

**Unable to see the doorbell live feed?**

Please make sure the device the app is running on is connected to the same WiFi network as the doorbell.

**Unable to open/close the door from the app?**

Please make sure the device the app is running on is connected to the same WiFi network as the doorbell.

**Biometric Login not working?**

Please make sure that a form of biometric ID is set up on the device (e.g. fingerprint login or face ID).

Please make sure you are currently logged into an account on the app, if not no biometric login will be available. 
You can see which account is currently logged in based on the username that is pre-filled in the login form.

**Not receiving notifications from your doorbell?**

Please make sure that this setting is turned on in your doorbell settings by going to your doorbell, selecting 'Manage Doorbell' and then selecting 'Yes' on the option 'Receive Notifications'. 
This setting is turned off by default.
*Note:* This setting is per device (that the app is running on) per doorbell so must be turned on on each device and doorbell you wish to receive notifications for.

**Add New Face video not being accepted?**

Please ensure the video meets all the requirements, it should be 15-30s and in adequate lighting (not too bright or dim).
A message should appear on the screen if the video provided is rejected, giving details on the reason.