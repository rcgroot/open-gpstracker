package nl.sogeti.android.gpstracker.util;

import android.os.Environment;

public class Constants
{
   public static final String DISABLEBLANKING = "disableblanking";
   public static final String EXTRA_TRACK_ID = "nl.sogeti.android.gpstracker.intent.trackid";
   public static final String SATELLITE = "SATELLITE";
   public static final String TRAFFIC = "TRAFFIC";
   public static final String SPEED = "showspeed";
   public static final String COMPASS = "COMPASS";
   public static final String LOCATION = "LOCATION";
   public static final String TRACKCOLORING = "trackcoloring";
   public static final int UNKNOWN = -1;
   public static final int LOGGING = 1;
   public static final int PAUSED = 2;
   public static final int STOPPED = 3;
   public static final String SPEEDSANITYCHECK = "speedsanitycheck";
   public static final String PRECISION = "precision";
   public static final String LOGATSTARTUP = "logatstartup";
   public static final String SERVICENAME = "nl.sogeti.android.gpstracker.intent.action.GPSLoggerService";
   public static final String UNITS = "units";
   public static final int UNITS_DEFAULT = 0;
   public static final int UNITS_IMPERIAL = 1;
   public static final int UNITS_METRIC = 2;
   public static final String EXTERNAL_DIR = "/OpenGPSTracker/";
   public static final String TMPICTUREFILE_PATH = EXTERNAL_DIR+"media_tmp";
   
}
