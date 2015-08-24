

### How do see the exact longitude latitude from a waypoint? ###

Export the GPX and load this in an (desktop) app and query the details of a part of the track.

### What are sane speed and sanity check? ###
These checks filter out noise locations generated with the GPS of some devices.

These are checks that allow a maximum speed of about 648 kilometer per hour or 402 mile per hour. This filters about seemingly _teleportation_ points on a track. More then half that speed is allowed but the speed is stripped from the measurement. This filters some speed spikes that randomly occur.

The altitude is considered sane if this differs less then 200 meters from the average of the last 3 points. This filters some altitude spikes that randomly occur.

### How do I disable the alarm that keeps sounding? ###
The alarm is there to warn that GPS logging is currently ineffective. This might be because your Android device is blocked from the GPS signal or the signal quality is really poor.

This alarm can be avoided by improving the GPS reception, lowering the `Logging precision` or disable the `Monitor GPS Status` in the `Settings`.

### How do I copy my recorded track to another Android phone? ###

There are two means of sharing complete tracks, by KMZ and by GPX. KMZ files are meant for Google Earth on the desktop. They can not be opened by Open GPS Tracker on other phones.

The GPX files can be opened by Open GPS Tracker. To transfer the track to another phone start sharing a track, choose GPX as the format and then send with an email application to the email address accessible from the other phone. On the other phone have Open GPS Tracker installed and open the GPX email attachment in the email application. A option should appear for Open GPS Tracker.

[Sharing screen](http://open-gpstracker.googlecode.com/svn/wiki/images/share_wizard.png)

### How do I get my Atlas maps to my HTC phone? ###

HTC provides some excellent Q&A for this, e.g. the Dutch [howto](http://www.htc.com/nl/howto.aspx?id=10297&type=1&p_id=324) or US [Q and A](http://www.htc.com/us/support/desire-uscellular/help/synchronization)

### Is there any possibility to support offline Google Maps? ###

The Google Maps are made possible by Android libraries provided by Google. Those libraries or the terms of service (INAL) do not support this feature, hence also not in Open GPS Tracker.

### How can I exit Open GPS Tracker? ###

First make sure to stop the stop the tracking, this is when the blue dot disappears from the top bar. After that use the BACK button to exit the program. All CPU tasks are then stopped. By using the back button your phone will know it can clear Open GPS Tracker data from memory when more space is needed.

### How do I get maps without a data connection? ###

Follow the instructions on the OfflineMaps page.

### How can I upload my GPX files to the app? ###

First step is to send the track as GPX file to your phone. Options are means such as your SD-card or [Dropbox](https://www.dropbox.com/referrals/NTE0MzA4MjE1OQ?src=global0). Open the file from the respective app, a file manager for SD-card or the Dropbox app for Dropbox. The Open GPS Tracker will start with the question if you wish to import the track. If you select yes then the track will be added to the track list stored in the Open GPS Tracker app.

### How can I upload a track to the internet? ###
To send a track to someone else through the Internet use **Share track** option. This option can be found when:
  * Clicking the menu button in Map screen. [Menu screenshot](http://open-gpstracker.googlecode.com/svn/wiki/images/menu.png)
  * Clicking the menu button in the Statistics screen. [Statistics screenshot](http://open-gpstracker.googlecode.com/svn/wiki/images/graph_ng.png)
  * Long-pressing a track in the Track list screen. [Track list screenshot](http://open-gpstracker.googlecode.com/svn/wiki/images/track_options.png)

A fast way to show someone a trip is to send an KMZ file by email and let the receiver open this KMZ file with Google Earth. The Google Earth program is available for free at http://earth.google.com/

### How do I rename a track? ###
From map activity access the menu and open the track list. Long-press a row from the listed tracks. In the context menu that appears select the **Rename track** option.

### How do I change the units for speed and distance? ###
From map activity access the menu and open settings. The default is to follow the language default. Imperial means miles per hour and metric means kilometers
per hour.

### Where are the tracks stored and how much space do they take up? ###
Private GPS data and settings are stored on the application partition. With many long tracks with high precision take up a fair amount of space. Please consult
the Open GPS Tracker application info in the Settings->Manage applications to check the space used. Pictures, video and audio are stored on the SD card.

### Why does the traffic information not show anything? ###
The Google Maps API allows the toggle to show traffic information. The actual traffic information content is dependent on Google providing this. Traffic
information is not available for all parts of the world.

### Why does my part of the world not show up on the map? ###
Much like the traffic information the maps information is dependent on Google providing this.

### How can I contribute to this application? ###
  * Sent code with bug fixes, features or idea's.
  * Report problems, issue's or your idea's. Add your feature or bug report to the [issue list](http://code.google.com/p/open-gpstracker/issues/list) or sent me email with your [idea](mailto:rcgroot@gmail.com).
  * Let me know you enjoy the program.

### How can I delete a track? ###
From map activity access the menu and open the track list. Long-press a row from the listed tracks. In the context menu that appears select the delete option.

### What are the exact interval for the logging precision? ###
  * Fine   : GPS updates every second after at least 5 meters movement, alarm after 2 minutes of no updates.
  * Normal : GPS updates every 15 seconds after at least 10 meters movement, alarm after 3 minutes of no updates.
  * Coarse : GPS updates every 30 seconds after at least 25 meters movement, alarm after 6 minutes of no updates.
  * Global : Use GPS and WiFi signals to triangulate every 5 minutes after at least 500 meter movement, alarm after 1 hour of no updates.

### Why does this application use so much battery power and how can I make it use less? ###
  * Big source of power drain is the screen, turn of the screen during logging.
  * Next big source of power drain is actual logging, increase the interval to course or other custom large interval
  * Next big source of power drain is a hard working GPS system, make it fast and easy to get a GPS fix by exposing the device directly to the sky in many directions.

### Why are the photo notes on the G1 so low resolution? ###

### How can I open this application in Eclipse for development? ###

  1. Setup an Android SDK and install and update the SDK
  1. Eclipse with ADT plugin and Google API 4, 15 and 17
  1. git clone https://code.google.com/p/open-gpstracker/
  1. git checkout release\_1.3.2\_osmupdate
  1. File->Import->General->Existing Projects into Workspace->next
  1. Select the directory of the Git clone as root directory
  1. Mark all 4 projects
  1. Disable the mark at "Copy projects into workspace"
  1. Substitute you own Google Maps API key in /OpenGPSTracker/res/layout/map.xml
    * See http://code.google.com/android/add-ons/google-apis/mapkey.html
  1. Substitute you own private OAuth keys by overriding value/donottranslate\_private.xml with your own keys.
    * See osm keys: http://wiki.openstreetmap.org/wiki/OAuth
    * See breadcrumbs keys: http://www.gobreadcrumbs.com/en/developers
  1. Clean, Build and Run

**This app needs a device or emulator with Google Services. Also the V1 version of the map key has been deprecated. The current app with V1 maps can't render Google Maps for new developers.**


### Can I use XXX maps with Open GPS Tracker? ###

The map rendering done in OGT is completely done by third party libraries. For the Google Maps the standaard Google-extention on Android is used. For the OpenStreetMap angle the Osmdroid library is used.

Features, like e.g. calibrated maps, need first be supported by those libraries and in most cases these need to be activated in OGT.