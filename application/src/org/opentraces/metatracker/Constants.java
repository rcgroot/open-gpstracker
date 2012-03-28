/*
 * Copyright (C) 2010  Just Objects B.V.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opentraces.metatracker;

import android.net.Uri;

/**
 * Various application wide constants
 *
 * @author Just van den Broecke
 * @version $Id:$
 */
public class Constants
{
	/**
	 * The authority of the track data provider
	 */
	public static final String AUTHORITY = "nl.sogeti.android.gpstracker";

	/**
	 * The content:// style URL for the track data provider
	 */
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

	/**
	 * Logging states, slightly adapted from GPSService states
	 */
	public static final int DOWN = 0;
	public static final int LOGGING = 1;
	public static final int PAUSED = 2;
	public static final int STOPPED = 3;
	public static final int UNKNOWN = 4;

	public static String[] LOGGING_STATES = {"down", "logging", "paused", "stopped", "unknown"};

	public static final String DISABLEBLANKING = "disableblanking";
	public static final String PREF_ENABLE_SOUND = "pref_enablesound";
	
	public static final String PREF_SERVER_URL = "pref_server_url";
	public static final String PREF_SERVER_USER = "pref_server_user";
	public static final String PREF_SERVER_PASSWORD = "pref_server_password";

	public static final String SERVICE_GPS_LOGGING = "nl.sogeti.android.gpstracker.intent.action.GPSLoggerService";
	public static final String EXTERNAL_DIR = "/OpenGPSTracker/";
	public static final Uri NAME_URI = Uri.parse("content://" + AUTHORITY + ".string");

	/**
	 * Activity Action: Pick a file through the file manager, or let user
	 * specify a custom file name.
	 * Data is the current file name or file name suggestion.
	 * Returns a new file name as file URI in data.
	 * <p/>
	 * <p>Constant Value: "org.openintents.action.PICK_FILE"</p>
	 */
	public static final String ACTION_PICK_FILE = "org.openintents.action.PICK_FILE";
	public static final int REQUEST_CODE_PICK_FILE_OR_DIRECTORY = 1;

	/**
	 * Activity Action: Show an about dialog to display
	 * information about the application.
	 * <p/>
	 * The application information is retrieved from the
	 * application's manifest. In order to send the package
	 * you have to launch this activity through
	 * startActivityForResult().
	 * <p/>
	 * Alternatively, you can specify the package name
	 * manually through the extra EXTRA_PACKAGE.
	 * <p/>
	 * All data can be replaced using optional intent extras.
	 * <p/>
	 * <p>
	 * Constant Value: "org.openintents.action.SHOW_ABOUT_DIALOG"
	 * </p>
	 */
	public static final String OI_ACTION_SHOW_ABOUT_DIALOG =
			"org.openintents.action.SHOW_ABOUT_DIALOG";

	/**
	 * Definitions for tracks.
	 *
	 */
	public static final class Tracks implements android.provider.BaseColumns
	{
		/**
		 * The name of this table
		 */
		static final String TABLE = "tracks";

		/**
		 * The end time
		 */
		public static final String NAME = "name";
		public static final String CREATION_TIME = "creationtime";

		/**
		 * The MIME type of a CONTENT_URI subdirectory of a single track.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nl.sogeti.android.track";

		/**
		 * The content:// style URL for this provider
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE);

	}


	/**
	 * Definitions for segments.
	 */
	public static final class Segments implements android.provider.BaseColumns
	{
		/**
		 * The name of this table
		 */
		static final String TABLE = "segments";

		/**
		 * The track _id to which this segment belongs
		 */
		public static final String TRACK = "track";
		static final String TRACK_TYPE = "INTEGER NOT NULL";
		static final String _ID_TYPE = "INTEGER PRIMARY KEY AUTOINCREMENT";

		/**
		 * The MIME type of a CONTENT_URI subdirectory of a single segment.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.nl.sogeti.android.segment";
		/**
		 * The MIME type of CONTENT_URI providing a directory of segments.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.nl.sogeti.android.segment";

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE);
	}

	/**
	 * Definitions for media URI's.
	 *
	 */
	public static final class Media implements android.provider.BaseColumns
	{
		/**
		 * The track _id to which this segment belongs
		 */
		public static final String TRACK = "track";
		public static final String SEGMENT = "segment";
		public static final String WAYPOINT = "waypoint";
		public static final String URI = "uri";
		/**
		 * The name of this table
		 */
		public static final String TABLE = "media";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE);
	}


	/**
	 * Definitions for waypoints.
	 *
	 */
	public static final class Waypoints implements android.provider.BaseColumns
	{
		/**
		 * The name of this table
		 */
		public static final String TABLE = "waypoints";

		/**
		 * The latitude
		 */
		public static final String LATITUDE = "latitude";
		/**
		 * The longitude
		 */
		public static final String LONGITUDE = "longitude";
		/**
		 * The recorded time
		 */
		public static final String TIME = "time";
		/**
		 * The speed in meters per second
		 */
		public static final String SPEED = "speed";
		/**
		 * The segment _id to which this segment belongs
		 */
		public static final String SEGMENT = "tracksegment";
		/**
		 * The accuracy of the fix
		 */
		public static final String ACCURACY = "accuracy";
		/**
		 * The altitude
		 */
		public static final String ALTITUDE = "altitude";
		/**
		 * the bearing of the fix
		 */
		public static final String BEARING = "bearing";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE);
	}
}
