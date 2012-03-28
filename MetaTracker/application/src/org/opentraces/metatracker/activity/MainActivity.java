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

package org.opentraces.metatracker.activity;

import android.app.*;
import android.content.*;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.opentraces.metatracker.*;
import org.opentraces.metatracker.logger.GPSLoggerServiceManager;

import java.text.DecimalFormat;

public class MainActivity extends Activity
{

	// MENU'S
	private static final int MENU_SETTINGS = 1;
	private static final int MENU_TRACKING = 2;
	private static final int MENU_UPLOAD = 3;
	private static final int MENU_HELP = 4;
	private static final int MENU_ABOUT = 5;
	private static final int DIALOG_INSTALL_OPENGPSTRACKER = 1;
	private static final int DIALOG_INSTALL_ABOUT = 2;
	private static DecimalFormat REAL_FORMATTER1 = new DecimalFormat("0.#");
	private static DecimalFormat REAL_FORMATTER2 = new DecimalFormat("0.##");
	private static final String TAG = "MetaTracker.Main";
	private TextView waypointCountView, timeView, distanceView, speedView, roadRatingView, accuracyView;
	private GPSLoggerServiceManager loggerServiceManager;
	private TrackingState trackingState;
	private MediaPlayer mediaPlayer;

//	private PowerManager.WakeLock wakeLock = null;

	private static final int COLOR_WHITE = 0xFFFFFFFF;
	private static final int[] ROAD_RATING_COLORS = {0xFF808080, 0xFFCC0099, 0xFFEE0000, 0xFFFF6600, 0xFFFFCC00, 0xFF33CC33};
	private SharedPreferences sharedPreferences;
	private Button[] roadRatingButtons = new Button[6];

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

		trackingState = new TrackingState(sharedPreferences);
		trackingState.load();

		setContentView(R.layout.main);
		
		getLastTrackingState();

		getApplicationContext().getContentResolver().registerContentObserver(Constants.Tracks.CONTENT_URI, true, trackingObserver);

		// mUnits = new UnitsI18n(this, mUnitsChangeListener);

		speedView = (TextView) findViewById(R.id.currentSpeed);
		timeView = (TextView) findViewById(R.id.currentTime);
		distanceView = (TextView) findViewById(R.id.totalDist);
		waypointCountView = (TextView) findViewById(R.id.waypointCount);
		accuracyView = (TextView) findViewById(R.id.currentAccuracy);
		roadRatingView = (TextView) findViewById(R.id.currentRoadRating);

		// Capture our button from layout
		roadRatingButtons[0] = (Button) findViewById(R.id.road_qual_none);
		roadRatingButtons[1] = (Button) findViewById(R.id.road_qual_nogo);
		roadRatingButtons[2] = (Button) findViewById(R.id.road_qual_bad);
		roadRatingButtons[3] = (Button) findViewById(R.id.road_qual_poor);
		roadRatingButtons[4] = (Button) findViewById(R.id.road_qual_good);
		roadRatingButtons[5] = (Button) findViewById(R.id.road_qual_best);

		for (Button button : roadRatingButtons)
		{
			button.setOnClickListener(roadRatingButtonListener);
		}

		bindGPSLoggingService();
	}


	/**
	 * Called when the activity is started.
	 */
	@Override
	public void onStart()
	{
		Log.d(TAG, "onStart()");
		super.onStart();
	}

	/**
	 * Called when the activity is started.
	 */
	@Override
	public void onRestart()
	{
		Log.d(TAG, "onRestart()");
		super.onRestart();
	}

	/**
	 * Called when the activity is resumed.
	 */
	@Override
	public void onResume()
	{
		Log.d(TAG, "onResume()");

		getLastTrackingState();

		// updateBlankingBehavior();

		drawScreen();
		super.onResume();
	}

	/**
	 * Called when the activity is paused.
	 */
	@Override
	public void onPause()
	{
		trackingState.save();
/*		if (this.wakeLock != null && this.wakeLock.isHeld())
		{
			this.wakeLock.release();
			Log.w(TAG, "onPause(): Released lock to keep screen on!");
		} */
		Log.d(TAG, "onPause()");
		super.onPause();
	}



	@Override
	protected void onDestroy()
	{
		Log.d(TAG, "onDestroy()");
		trackingState.save();

/*		if (wakeLock != null && wakeLock.isHeld())
		{
			wakeLock.release();
			Log.w(TAG, "onDestroy(): Released lock to keep screen on!");
		}   */

		getApplicationContext().getContentResolver().unregisterContentObserver(trackingObserver);
		sharedPreferences.unregisterOnSharedPreferenceChangeListener(this.sharedPreferenceChangeListener);

		unbindGPSLoggingService();

		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		boolean result = super.onCreateOptionsMenu(menu);

		menu.add(ContextMenu.NONE, MENU_TRACKING, ContextMenu.NONE, R.string.menu_tracking).setIcon(R.drawable.ic_menu_movie).setAlphabeticShortcut('T');
		menu.add(ContextMenu.NONE, MENU_UPLOAD, ContextMenu.NONE, R.string.menu_upload).setIcon(R.drawable.ic_menu_upload).setAlphabeticShortcut('I');
		menu.add(ContextMenu.NONE, MENU_SETTINGS, ContextMenu.NONE, R.string.menu_settings).setIcon(R.drawable.ic_menu_preferences).setAlphabeticShortcut('C');
		menu.add(ContextMenu.NONE, MENU_HELP, ContextMenu.NONE, R.string.menu_help).setIcon(R.drawable.ic_menu_help).setAlphabeticShortcut('I');
		menu.add(ContextMenu.NONE, MENU_ABOUT, ContextMenu.NONE, R.string.menu_about).setIcon(R.drawable.ic_menu_info_details).setAlphabeticShortcut('A');

		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case MENU_TRACKING:
				startOpenGPSTrackerActivity();
				break;

			case MENU_UPLOAD:
				startActivityForClass("org.opentraces.metatracker", "org.opentraces.metatracker.activity.UploadTrackActivity");
				break;

			case MENU_SETTINGS:
				startActivity(new Intent(this, SettingsActivity.class));
				break;

			case MENU_ABOUT:
				try
				{
					startActivityForResult(new Intent(Constants.OI_ACTION_SHOW_ABOUT_DIALOG), MENU_ABOUT);
				}
				catch (ActivityNotFoundException e)
				{
					showDialog(DIALOG_INSTALL_ABOUT);
				}
				break;

			default:
				showAlert(R.string.menu_message_unsupported);
				break;
		}
		return true;
	}

	/*
		* (non-Javadoc)
		* @see android.app.Activity#onCreateDialog(int)
		*/

	@Override
	protected Dialog onCreateDialog(int id)
	{
		Dialog dialog = null;
		AlertDialog.Builder builder = null;
		switch (id)
		{
			case DIALOG_INSTALL_OPENGPSTRACKER:
				builder = new AlertDialog.Builder(this);
				builder
						.setTitle(R.string.dialog_noopengpstracker)
						.setMessage(R.string.dialog_noopengpstracker_message)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setPositiveButton(R.string.btn_install, mOpenGPSTrackerDownloadDialogListener)
						.setNegativeButton(R.string.btn_cancel, null);
				dialog = builder.create();
				break;

			case DIALOG_INSTALL_ABOUT:
				builder = new AlertDialog.Builder(this);
				builder
						.setTitle(R.string.dialog_nooiabout)
						.setMessage(R.string.dialog_nooiabout_message)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setPositiveButton(R.string.btn_install, mOiAboutDialogListener)
						.setNegativeButton(R.string.btn_cancel, null);
				dialog = builder.create();
				return dialog;

			default:
				dialog = super.onCreateDialog(id);
				break;
		}
		return dialog;
	}

	private void drawTitleBar(String s)
	{
		this.setTitle(s);
	}

	/**
	 * Called on any update to Tracks table.
	 */
	private void onTrackingUpdate()
	{
		getLastTrackingState();
		if (trackingState.waypoint.count == 1)
		{
			sendRoadRating();
		}
		drawScreen();
	}

	/**
	 * Called on any update to Tracks table.
	 */
	private synchronized void playPingSound()
	{
		if (!sharedPreferences.getBoolean(Constants.PREF_ENABLE_SOUND, false)) {
			return;
		}

		try
		{
			if (mediaPlayer == null)
			{
				mediaPlayer = MediaPlayer.create(this, R.raw.ping_short);
			} else
			{
				mediaPlayer.stop();
				mediaPlayer.prepare();
			}
			
			mediaPlayer.start();
		} catch (Throwable t)
		{
			Log.e(TAG, "Error playing sound", t);
		}
	}


	/**
	 * Retrieve the last point of the current track
	 */
	private void getLastWaypoint()
	{
		Cursor waypoint = null;
		try
		{
			ContentResolver resolver = this.getContentResolver();
			waypoint = resolver.query(Uri.withAppendedPath(Constants.Tracks.CONTENT_URI, trackingState.track.id + "/" + Constants.Waypoints.TABLE),
					new String[]{"max(" + Constants.Waypoints.TABLE + "." + Constants.Waypoints._ID + ")", Constants.Waypoints.LONGITUDE, Constants.Waypoints.LATITUDE, Constants.Waypoints.SPEED, Constants.Waypoints.ACCURACY, Constants.Waypoints.SEGMENT
					}, null, null, null);
			if (waypoint != null && waypoint.moveToLast())
			{
				// New point: increase pointcount
				int waypointId = waypoint.getInt(0);
				if (waypointId > 0 && trackingState.waypoint.id != waypointId)
				{
					trackingState.waypoint.count++;

					trackingState.waypoint.id = waypoint.getInt(0);

					// Increase total distance
					Location newLocation = new Location(this.getClass().getName());
					newLocation.setLongitude(waypoint.getDouble(1));
					newLocation.setLatitude(waypoint.getDouble(2));

					if (trackingState.waypoint.location != null)
					{
						float delta = trackingState.waypoint.location.distanceTo(newLocation);
						// Log.d(TAG, "trackingState.distance=" + trackingState.distance + " delta=" + delta + " ll=" + waypoint.getDouble(1) + ", " +waypoint.getDouble(2));
						trackingState.distance += delta;
					}

					trackingState.waypoint.location = newLocation;
					trackingState.waypoint.speed = waypoint.getFloat(3);
					trackingState.waypoint.accuracy = waypoint.getFloat(4);
					trackingState.segment.id = waypoint.getInt(5);

					playPingSound();					
				}
			}
		}
		finally
		{
			if (waypoint != null)
			{
				waypoint.close();
			}
		}
	}

	private void getLastTrack()
	{
		Cursor cursor = null;
		try
		{
			ContentResolver resolver = this.getApplicationContext().getContentResolver();
			cursor = resolver.query(Constants.Tracks.CONTENT_URI, new String[]{"max(" + Constants.Tracks._ID + ")", Constants.Tracks.NAME, Constants.Tracks.CREATION_TIME}, null, null, null);
			if (cursor != null && cursor.moveToLast())
			{
				int trackId = cursor.getInt(0);

				// Check if new track created
				if (trackId != trackingState.track.id)
				{
					trackingState.reset();
					trackingState.save();
				}

				trackingState.track.id = trackId;
				trackingState.track.name = cursor.getString(1);
				trackingState.track.creationTime = cursor.getLong(2);
			}
		}
		finally
		{
			if (cursor != null)
			{
				cursor.close();
			}
		}
	}

	private void getLastTrackingState()
	{
		getLastTrack();
		getLoggingState();
		getLastWaypoint();
	}

	private void sendRoadRating()
	{
		if (trackingState.roadRating > 0 && trackingState.newRoadRating && loggerServiceManager != null)
		{
			Uri media = Uri.withAppendedPath(Constants.NAME_URI, Uri.encode(trackingState.roadRating + ""));
			loggerServiceManager.storeMediaUri(media);
			trackingState.newRoadRating = false;
		}
	}

	private void startOpenGPSTrackerActivity()
	{
		try
		{
			startActivityForClass("nl.sogeti.android.gpstracker", "nl.sogeti.android.gpstracker.viewer.LoggerMap");
		}
		catch (ActivityNotFoundException e)
		{
			Log.i(TAG, "Cannot find activity for open-gpstracker");
			// No compatible file manager was found: install openintents filemanager.
			showDialog(DIALOG_INSTALL_OPENGPSTRACKER);
		}

		sendRoadRating();
	}

	private void startActivityForClass(String aPackageName, String aClassName) throws ActivityNotFoundException
	{
		Intent intent = new Intent();
		intent.setClassName(aPackageName, aClassName);

		startActivity(intent);
	}

	private void bindGPSLoggingService()
	{
		unbindGPSLoggingService();

		ServiceConnection serviceConnection = new ServiceConnection()
		{
			public void onServiceConnected(ComponentName className, IBinder service)
			{
				getLoggingState();
				drawTitleBar();
			}

			public void onServiceDisconnected(ComponentName className)
			{
				trackingState.loggingState = Constants.DOWN;
			}
		};

		loggerServiceManager = new GPSLoggerServiceManager(this);
		loggerServiceManager.startup(serviceConnection);
	}

	private void unbindGPSLoggingService()
	{

		if (loggerServiceManager == null)
		{
			return;
		}

		try
		{
			loggerServiceManager.shutdown();
		} finally
		{
			loggerServiceManager = null;
			trackingState.loggingState  = Constants.DOWN;
		}
	}

	private void getLoggingState()
	{
		// Get state from logger if bound
		trackingState.loggingState = loggerServiceManager == null ? Constants.DOWN : loggerServiceManager.getLoggingState();

		// protect for values outside array bounds (set to unknown)
		if (trackingState.loggingState  < 0 || trackingState.loggingState  > Constants.LOGGING_STATES.length - 1)
		{
			trackingState.loggingState  = Constants.UNKNOWN;
		}
	}

	private String getLoggingStateStr()
	{
		return Constants.LOGGING_STATES[trackingState.loggingState];
	}


	private void drawTitleBar()
	{
		drawTitleBar("MT : " + trackingState.track.name + " : " + getLoggingStateStr());
	}

	private void drawScreen()
	{
		drawTitleBar();
		drawTripStats();
		drawRoadRating();
	}

	/**
	 * Retrieves the numbers of the measured speed and altitude
	 * from the most recent waypoint and
	 * updates UI components with this latest bit of information.
	 */
	private void drawTripStats()
	{
		try
		{
			waypointCountView.setText(trackingState.waypoint.count + "");

			long secsDelta = 0L;
			long hours = 0L;
			long mins = 0L;
			long secs = 0L;

			if (trackingState.track.creationTime != 0)
			{
				secsDelta = (System.currentTimeMillis() - trackingState.track.creationTime) / 1000;
				hours = secsDelta / 3600L;
				mins = (secsDelta % 3600L) / 60L;
				secs = ((secsDelta % 3600L) % 60);
			}

			timeView.setText(formatTimeNum(hours) + ":" + formatTimeNum(mins) + ":" + formatTimeNum(secs));
			speedView.setText(REAL_FORMATTER1.format(3.6f * trackingState.waypoint.speed));
			accuracyView.setText(REAL_FORMATTER1.format(trackingState.waypoint.accuracy));
			distanceView.setText(REAL_FORMATTER2.format(trackingState.distance / 1000f));
		}
		finally
		{
		}
	}

	private String formatTimeNum(long n)
	{
		return n < 10 ? ("0" + n) : (n + "");
	}

	private void drawRoadRating()
	{
		for (int i = 0; i < roadRatingButtons.length; i++)
		{
			roadRatingButtons[i].setBackgroundColor(COLOR_WHITE);
		}

		if (trackingState.roadRating >= 0)
		{
			roadRatingButtons[trackingState.roadRating].setBackgroundColor(ROAD_RATING_COLORS[trackingState.roadRating]);
		}
		String roadRatingStr = trackingState.roadRating + "";
		roadRatingView.setText(roadRatingStr);
	}

	private void showAlert(int aMessage)
	{
		new AlertDialog.Builder(this)
				.setMessage(aMessage)
				.setPositiveButton("Ok", null)
				.show();
	}

	private void updateBlankingBehavior()
	{
		boolean disableblanking = sharedPreferences.getBoolean(Constants.DISABLEBLANKING, false);
		if (disableblanking)
		{
			/*		if (wakeLock == null)
						{
							PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
							wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
						}
						wakeLock.acquire();
						Log.w(TAG, "Acquired lock to keep screen on!");   */
		}
	}

	private final DialogInterface.OnClickListener mOiAboutDialogListener = new DialogInterface.OnClickListener()
	{
		public void onClick(DialogInterface dialog, int which)
		{
			Uri oiDownload = Uri.parse("market://details?id=org.openintents.about");
			Intent oiAboutIntent = new Intent(Intent.ACTION_VIEW, oiDownload);
			try
			{
				startActivity(oiAboutIntent);
			}
			catch (ActivityNotFoundException e)
			{
				oiDownload = Uri.parse("http://openintents.googlecode.com/files/AboutApp-1.0.0.apk");
				oiAboutIntent = new Intent(Intent.ACTION_VIEW, oiDownload);
				startActivity(oiAboutIntent);
			}
		}
	};

	private final DialogInterface.OnClickListener mOpenGPSTrackerDownloadDialogListener = new DialogInterface.OnClickListener()
	{
		public void onClick(DialogInterface dialog, int which)

		{
			Uri marketUri = Uri.parse("market://details?id=nl.sogeti.android.gpstracker");
			Intent downloadIntent = new Intent(Intent.ACTION_VIEW, marketUri);
			try
			{
				startActivity(downloadIntent);
			}
			catch (ActivityNotFoundException e)
			{
				showAlert(R.string.dialog_failinstallopengpstracker_message);
			}
		}
	};

	// Create an anonymous implementation of OnClickListener
	private View.OnClickListener roadRatingButtonListener = new View.OnClickListener()
	{
		public void onClick(View v)
		{

			for (int i = 0; i < roadRatingButtons.length; i++)
			{
				if (v.getId() == roadRatingButtons[i].getId())
				{
					trackingState.roadRating = i;
				}
			}
			trackingState.newRoadRating = true;
			drawRoadRating();
			sendRoadRating();
		}
	};
	private final ContentObserver trackingObserver = new ContentObserver(new Handler())
	{
		@Override
		public void onChange(boolean selfUpdate)
		{
			if (!selfUpdate)
			{
				onTrackingUpdate();
				Log.d(TAG, "trackingObserver onTrackingUpdate lastWaypointId=" + trackingState.waypoint.id);
			} else
			{
				Log.w(TAG, "trackingObserver skipping change");
			}
		}
	};

	private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener()
	{
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
		{
			if (key.equals(Constants.DISABLEBLANKING))
			{
				updateBlankingBehavior();
			}
		}
	};

}
