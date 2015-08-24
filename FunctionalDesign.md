# Introduction #

The functional design of the program is based on _client_ sketches of a GPS Tracking
application and a description of the desired functionality.

# Details #

## Concept basic functionality ##
  * An application that shows where I have been.

To register where we have been, it is necessary to log (with interval) the date+time
and the location. This means that the application must be active in the background.


The application has three modes:
  1. Logging (also possible in the background) in combination with show current position
  1. Review - Look at historical data in combination with show current position, Logging must continue
  1. Configuration - Configure preferences, Logging must continue

### Mode 1 Logging (main modes, main screen ###

  * Store date/time code with location met interval in the database
  * Show chosen interval (default = 1 minute)
  * Button for review
  * Button for configuration
  * Button for close
  * Show map with current position (on Google maps)
  * Can remain active in the background

### Mode 2 Review (sub screen) ###

  * Button for start and end date/time
  * Button with <show on map>
  * Button with close
  * Map with graphical log display of current selection and current position (on Google maps)
  * Logging must remain active

### Mode 3 Configuration (sub screen) ###

  * Entry Interval choice from 1, 5, 10 and 30 seconds or 1, 5, 10, 30 and 60 minutes
  * Button for set
  * Button for close
  * Logging must remain active

When there is no signal then no map but a question mark in mode 1 and 3.
When no data then a notice that no recorded data is available in mode 2.

####  ####
![http://open-gpstracker.googlecode.com/svn/wiki/images/functional_mockups.png](http://open-gpstracker.googlecode.com/svn/wiki/images/functional_mockups.png)

Screen mocks
####  ####