/*------------------------------------------------------------------------------
 **     Ident: Sogeti Smart Mobile Solutions
 **    Author: rene
 ** Copyright: (c) Apr 24, 2011 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 *
 *   This file is part of OpenGPSTracker.
 *
 *   OpenGPSTracker is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   OpenGPSTracker is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with OpenGPSTracker.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package nl.sogeti.android.gpstracker.util;

import java.io.File;

import nl.sogeti.android.gpstracker.actions.tasks.XmlCreator;
import nl.sogeti.android.gpstracker.db.GPStracking;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;


/**
 * Various application wide constants
 * 
 * @version $Id$
 * @author rene (c) Mar 22, 2009, Sogeti B.V.
 */
public class Constants
{
   public static final String DISABLEBLANKING = "disableblanking";
   public static final String SATELLITE = "SATELLITE";
   public static final String TRAFFIC = "TRAFFIC";
   public static final String SPEED = "showspeed";
   public static final String ALTITUDE = "showaltitude";
   public static final String COMPASS = "COMPASS";
   public static final String LOCATION = "LOCATION";
   public static final String MAPPROVIDER = "mapprovider";
   public static final String TRACKCOLORING = "trackcoloring";
   public static final String SPEEDSANITYCHECK = "speedsanitycheck";
   public static final String PRECISION = "precision";
   public static final String LOGATSTARTUP = "logatstartup";
   public static final String STARTUPATBOOT = "startupatboot";
   public static final String SERVICENAME = "nl.sogeti.android.gpstracker.intent.action.GPSLoggerService";
   public static final String UNITS = "units";
   public static final int UNITS_DEFAULT      = 0;
   public static final int UNITS_IMPERIAL     = 1;
   public static final int UNITS_METRIC       = 2;
   public static final int UNITS_NAUTIC       = 3;
   public static final int UNITS_METRICPACE   = 4;
   public static final int UNITS_IMPERIALPACE = 5;
   public static final String SDDIR_DIR             = "SDDIR_DIR";
   public static final String DEFAULT_EXTERNAL_DIR  = "/OpenGPSTracker/";
   public static final String TMPICTUREFILE_SUBPATH = "media_tmp.tmp";
   public static final Uri NAME_URI = Uri.parse( "content://" + GPStracking.AUTHORITY+".string" );
   public static final int GOOGLE = 0;
   public static final int OSM = 1;
   public static final String JOGRUNNER_AUTH = "JOGRUNNER_AUTH";
   public static final String EXPORT_TYPE = "SHARE_TYPE";
   public static final String EXPORT_GPXTARGET = "EXPORT_GPXTARGET";
   public static final String EXPORT_KMZTARGET = "EXPORT_KMZTARGET";
   public static final String EXPORT_TXTTARGET = "EXPORT_TXTTARGET";
   
   public static final double MIN_STATISTICS_SPEED = 1.0d;
   public static final int OSM_CLOUDMADE = 0;
   public static final int OSM_MAKNIK    = 1;
   public static final int OSM_CYCLE   = 2;
   public static final String OSMBASEOVERLAY = "OSM_BASE_OVERLAY";
   
   public static final String LOGGING_INTERVAL = "customprecisiontime";
   public static final String LOGGING_DISTANCE = "customprecisiondistance";
   public static final String STATUS_MONITOR = "gpsstatusmonitor";
   public static final String OSM_USERNAME = "OSM_USERNAME";
   public static final String OSM_PASSWORD = "OSM_PASSWORD";
   public static final String OSM_VISIBILITY = "OSM_VISIBILITY";
   public static final String DATASOURCES_KEY = "DATASOURCES";
   
   /**
    * Broadcast intent action indicating that the logger service state has
    * changed. Includes the logging state and its precision.
    *
    * @see #EXTRA_LOGGING_PRECISION
    * @see #EXTRA_LOGGING_STATE
    */

   public static final String LOGGING_STATE_CHANGED_ACTION = "nl.sogeti.android.gpstracker.LOGGING_STATE_CHANGED";
   
   /**
    * The precision the service is logging on.
    * 
    * @see #LOGGING_FINE
    * @see #LOGGING_NORMAL
    * @see #LOGGING_COARSE
    * @see #LOGGING_GLOBAL
    * @see #LOGGING_CUSTOM
    * 
    */
   public static final String EXTRA_LOGGING_PRECISION = "nl.sogeti.android.gpstracker.EXTRA_LOGGING_PRECISION";
   
   /**
    * The state the service is.
    * 
    * @see #UNKNOWN
    * @see #LOGGING
    * @see #PAUSED
    * @see #STOPPED
    */
   public static final String EXTRA_LOGGING_STATE = "nl.sogeti.android.gpstracker.EXTRA_LOGGING_STATE";

   /**
    * The state of the service is unknown
    */
   public static final int UNKNOWN = -1;
   
   /**
    * The service is activly logging, it has requested location update from the location provider.
    */
   public static final int LOGGING = 1;
   
   /**
    * The service is not active, but can be resumed to become active and store location changes as 
    * part of a new segment of the current track.
    */
   public static final int PAUSED = 2;
   
   /**
    * The service is not active and can not resume a current track but must start a new one when becoming active.
    */
   public static final int STOPPED = 3;
   
   /**
    * The precision of the GPS provider is based on the custom time interval and distance.
    */
   public static final int LOGGING_CUSTOM = 0;
   
   /**
    * The GPS location provider is asked to update every 10 seconds or every 5 meters.
    */
   public static final int LOGGING_FINE   = 1;
   
   /**
    * The GPS location provider is asked to update every 15 seconds or every 10 meters.
    */
   public static final int LOGGING_NORMAL = 2;
   
   /**
    * The GPS location provider is asked to update every 30 seconds or every 25 meters.
    */
   public static final int LOGGING_COARSE = 3;
   
   /**
    * The radio location provider is asked to update every 5 minutes or every 500 meters.
    */
   public static final int LOGGING_GLOBAL = 4;
   
   public static final String REQUEST_URL     = "http://api.gobreadcrumbs.com/oauth/request_token";
   public static final String ACCESS_URL      = "http://api.gobreadcrumbs.com/oauth/access_token";
   public static final String AUTHORIZE_URL   = "http://api.gobreadcrumbs.com/oauth/authorize";

   public static final String OSM_REQUEST_URL = "http://www.openstreetmap.org/oauth/request_token";
   public static final String OSM_ACCESS_URL = "http://www.openstreetmap.org/oauth/access_token";
   public static final String OSM_AUTHORIZE_URL = "http://www.openstreetmap.org/oauth/authorize";

   public static final String OAUTH_CALLBACK_SCHEME = "x-oauthflow-opengpstracker";
   public static final String OAUTH_CALLBACK_HOST = "callback";
   public static final String OAUTH_CALLBACK_URL = OAUTH_CALLBACK_SCHEME + "://" + OAUTH_CALLBACK_HOST;

   
   /**
    * Based on preference return the SD-Card directory in which Open GPS Tracker creates and stores files
    * shared tracks,
    * 
    * @param ctx
    * @return 
    */
   public static String getSdCardDirectory( Context ctx )
   {
      // Read preference and ensure start and end with '/' symbol
      String dir = PreferenceManager.getDefaultSharedPreferences(ctx).getString(SDDIR_DIR, DEFAULT_EXTERNAL_DIR);
      if( !dir.startsWith("/") )
      {
         dir = "/" + dir;
      }
      if( !dir.endsWith("/") )
      {
         dir = dir + "/" ;
      }
      dir = Environment.getExternalStorageDirectory().getAbsolutePath() + dir;
      
      // If neither exists or can be created fall back to default
      File dirHandle = new File(dir);
      if( !dirHandle.exists() && !dirHandle.mkdirs() )
      {
         dir = Environment.getExternalStorageDirectory().getAbsolutePath() + DEFAULT_EXTERNAL_DIR;
      }
      return dir;
   }
   
   public static String getSdCardTmpFile( Context ctx )
   {
      String dir = getSdCardDirectory( ctx ) + TMPICTUREFILE_SUBPATH;
      return dir;
   }

   public static final String BREADCRUMBS_CONNECT = "breadcrumbs_connect";

   public static final int BREADCRUMBS_CONNECT_ITEM_VIEW_TYPE  = 0;
   public static final int BREADCRUMBS_ACTIVITY_ITEM_VIEW_TYPE = 1;
   public static final int BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE   = 2;
   public static final int BREADCRUMBS_TRACK_ITEM_VIEW_TYPE    = 3;
   public static final int SECTIONED_HEADER_ITEM_VIEW_TYPE = 0;
   
}
