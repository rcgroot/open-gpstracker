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
package nl.sogeti.android.gpstracker.viewer;

import java.util.concurrent.Semaphore;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.ControlTracking;
import nl.sogeti.android.gpstracker.actions.InsertNote;
import nl.sogeti.android.gpstracker.actions.ShareTrack;
import nl.sogeti.android.gpstracker.actions.Statistics;
import nl.sogeti.android.gpstracker.db.GPStracking.Media;
import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.logger.GPSLoggerServiceManager;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.UnitsI18n;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Gallery;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

/**
 * Main activity showing a track and allowing logging control
 * 
 * @version $Id$
 * @author rene (c) Jan 18, 2009, Sogeti B.V.
 */
public class LoggerMap extends FragmentActivity
{
   public static final String GOOGLE_PROVIDER = "GOOGLE";

   private static final String INSTANCE_LONG = "long";
   private static final String INSTANCE_LAT = "lat";
   private static final String INSTANCE_ZOOM = "zoom";
   private static final String INSTANCE_SPEED = "averagespeed";
   private static final String INSTANCE_TRACK = "track";
   private static final int ZOOM_LEVEL = 16;
   // MENU'S
   private static final int MENU_PLAYERROR = 15;
   private static final int DIALOG_NOTRACK = 24;
   private static final int DIALOG_LAYERS = 31;
   private static final int DIALOG_URIS = 34;
   private static final int DIALOG_CONTRIB = 35;
   private static final String TAG = "OGT.LoggerMap";

   private static final int MENU_LOGGERMAP_TRACKLIST = 0;
   private static final int MENU_LOGGERMAP_ABOUT = 2;
   private static final int MENU_LOGGERMAP_TRACKING = 4;
   private static final int MENU_LOGGERMAP_SHARE = 6;
   private static final int MENU_LOGGERMAP_NOTE = 8;
   // UI's
   private CheckBox mTraffic;
   private CheckBox mSpeed;
   private CheckBox mAltitude;
   private CheckBox mDistance;
   private CheckBox mCompass;
   private CheckBox mLocation;
   private TextView[] mSpeedtexts = new TextView[0];
   private TextView mLastGPSSpeedView = null;
   private TextView mLastGPSAltitudeView = null;
   private TextView mDistanceView = null;
   private Gallery mGallery;

   private double mAverageSpeed = 33.33d / 3d;
   private long mTrackId = -1;
   private long mLastSegment = -1;
   private UnitsI18n mUnits;
   private WakeLock mWakeLock = null;
   private SharedPreferences mSharedPreferences;
   private GPSLoggerServiceManager mLoggerServiceManager;
   private BaseAdapter mMediaAdapter;

   private GoogleMap mMapView = null;
   private Handler mHandler;

   private ContentObserver mTrackSegmentsObserver;
   private ContentObserver mSegmentWaypointsObserver;
   private ContentObserver mTrackMediasObserver;
   private DialogInterface.OnClickListener mNoTrackDialogListener;
   private OnClickListener mNoteSelectDialogListener;
   private OnCheckedChangeListener mCheckedChangeListener;
   private android.widget.RadioGroup.OnCheckedChangeListener mGroupCheckedChangeListener;
   private OnSharedPreferenceChangeListener mSharedPreferenceChangeListener;
   private UnitsI18n.UnitsChangeListener mUnitsChangeListener;

   /**
    * Run after the ServiceManager completes the binding to the remote service
    */
   private Runnable mServiceConnected;
   private Runnable speedCalculator;

   private View mMapscreen;
   private TileOverlay mTrackTileOverlay;
   private TrackTileProvider mTrackTileProvider;

   /**
    * Called when the activity is first created.
    */
   @Override
   protected void onCreate(Bundle load)
   {
      super.onCreate(load);

      setContentView(R.layout.map);
      mMapView = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.myMapView)).getMap();

      mMapscreen = findViewById(R.id.mapScreen);
      mMapscreen.setDrawingCacheEnabled(true);
      mUnits = new UnitsI18n(this);
      mLoggerServiceManager = new GPSLoggerServiceManager(this);

      final Semaphore calulatorSemaphore = new Semaphore(0);
      Thread calulator = new Thread("OverlayCalculator")
         {
            @Override
            public void run()
            {
               Looper.prepare();
               mHandler = new Handler();
               calulatorSemaphore.release();
               Looper.loop();
            }
         };
      calulator.start();
      try
      {
         calulatorSemaphore.acquire();
      }
      catch (InterruptedException e)
      {
         Log.e(TAG, "Failed waiting for a semaphore", e);
      }
      mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

      TextView[] speeds = { (TextView) findViewById(R.id.speedview05), (TextView) findViewById(R.id.speedview04), (TextView) findViewById(R.id.speedview03), (TextView) findViewById(R.id.speedview02),
            (TextView) findViewById(R.id.speedview01), (TextView) findViewById(R.id.speedview00) };
      mSpeedtexts = speeds;
      mLastGPSSpeedView = (TextView) findViewById(R.id.currentSpeed);
      mLastGPSAltitudeView = (TextView) findViewById(R.id.currentAltitude);
      mDistanceView = (TextView) findViewById(R.id.currentDistance);

      createListeners();
      onRestoreInstanceState(load);
   }

   @Override
   protected void onResume()
   {
      super.onResume();
      if (ConnectionResult.SUCCESS != GooglePlayServicesUtil.isGooglePlayServicesAvailable(this))
      {
         showDialog(MENU_PLAYERROR);
      }

      mLoggerServiceManager.startup(this, mServiceConnected);

      mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
      mUnits.setUnitsChangeListener(mUnitsChangeListener);
      updateTitleBar();
      updateBlankingBehavior();

      if (mTrackId >= 0)
      {
         ContentResolver resolver = this.getContentResolver();
         Uri trackUri = Uri.withAppendedPath(Tracks.CONTENT_URI, mTrackId + "/segments");
         Uri lastSegmentUri = Uri.withAppendedPath(Tracks.CONTENT_URI, mTrackId + "/segments/" + mLastSegment + "/waypoints");
         Uri mediaUri = ContentUris.withAppendedId(Media.CONTENT_URI, mTrackId);

         resolver.unregisterContentObserver(this.mTrackSegmentsObserver);
         resolver.unregisterContentObserver(this.mSegmentWaypointsObserver);
         resolver.unregisterContentObserver(this.mTrackMediasObserver);
         resolver.registerContentObserver(trackUri, false, this.mTrackSegmentsObserver);
         resolver.registerContentObserver(lastSegmentUri, true, this.mSegmentWaypointsObserver);
         resolver.registerContentObserver(mediaUri, true, this.mTrackMediasObserver);
      }
      updateDataOverlays();

      updateSpeedColoring();
      updateSpeedDisplayVisibility();
      updateAltitudeDisplayVisibility();
      updateDistanceDisplayVisibility();
      updateLocationDisplayVisibility();
   }

   @Override
   protected void onPause()
   {
      if (this.mWakeLock != null && this.mWakeLock.isHeld())
      {
         this.mWakeLock.release();
         Log.w(TAG, "onPause(): Released lock to keep screen on!");
      }
      ContentResolver resolver = this.getContentResolver();
      resolver.unregisterContentObserver(this.mTrackSegmentsObserver);
      resolver.unregisterContentObserver(this.mSegmentWaypointsObserver);
      resolver.unregisterContentObserver(this.mTrackMediasObserver);
      mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this.mSharedPreferenceChangeListener);
      mUnits.setUnitsChangeListener(null);
      mMapView.setMyLocationEnabled(false);

      mTrackTileProvider.shutdown();
      mLoggerServiceManager.shutdown(this);

      super.onPause();
   }

   /*
    * (non-Javadoc)
    * @see com.google.android.maps.MapActivity#onPause()
    */
   @Override
   protected void onDestroy()
   {
      super.onDestroy();

      mHandler.post(new Runnable()
         {
            @Override
            public void run()
            {
               Looper.myLooper().quit();
            }
         });

      if (mWakeLock != null && mWakeLock.isHeld())
      {
         mWakeLock.release();
         Log.w(TAG, "onDestroy(): Released lock to keep screen on!");
      }
      if (mLoggerServiceManager.getLoggingState() == Constants.STOPPED)
      {
         stopService(new Intent(Constants.SERVICENAME));
      }
      mUnits = null;
   }

   /*
    * (non-Javadoc)
    * @see com.google.android.maps.MapActivity#onNewIntent(android.content.Intent)
    */
   @Override
   public void onNewIntent(Intent newIntent)
   {
      Uri data = newIntent.getData();
      if (data != null)
      {
         moveToTrack(Long.parseLong(data.getLastPathSegment()), true);
      }
   }

   @Override
   protected void onRestoreInstanceState(Bundle load)
   {
      if (load != null)
      {
         super.onRestoreInstanceState(load);
      }

      Uri data = this.getIntent().getData();
      if (load != null && load.containsKey(INSTANCE_TRACK)) // 1st method: track from a previous instance of this activity
      {
         long loadTrackId = load.getLong(INSTANCE_TRACK);
         if (load.containsKey(INSTANCE_SPEED))
         {
            mAverageSpeed = load.getDouble(INSTANCE_SPEED);
         }
         moveToTrack(loadTrackId, false);
      }
      else if (data != null) // 2nd method: track ordered to make
      {
         long loadTrackId = Long.parseLong(data.getLastPathSegment());
         mAverageSpeed = 0.0;
         moveToTrack(loadTrackId, true);
      }
      else
      // 3rd method: just try the last track
      {
         moveToLastTrack();
      }

      if (load != null && load.containsKey(INSTANCE_ZOOM))
      {
         mMapView.animateCamera(CameraUpdateFactory.zoomTo(load.getInt(INSTANCE_ZOOM)));
      }
      else
      {
         mMapView.animateCamera(CameraUpdateFactory.zoomTo(LoggerMap.ZOOM_LEVEL));
      }

      if (load != null && load.containsKey(INSTANCE_LAT) && load.containsKey(INSTANCE_LONG))
      {
         LatLng storedPoint = new LatLng(load.getDouble(INSTANCE_LAT), load.getDouble(INSTANCE_LONG));
         mMapView.animateCamera(CameraUpdateFactory.newLatLng(storedPoint));
      }
      else
      {
         LatLng lastPoint = getLastTrackPoint();
         mMapView.animateCamera(CameraUpdateFactory.newLatLng(lastPoint));
      }
   }

   @Override
   protected void onSaveInstanceState(Bundle save)
   {
      super.onSaveInstanceState(save);
      save.putLong(INSTANCE_TRACK, this.mTrackId);
      save.putDouble(INSTANCE_SPEED, mAverageSpeed);
   }

   @Override
   public boolean onKeyDown(int keyCode, KeyEvent event)
   {
      boolean propagate = true;
      switch (keyCode)
      {
         case KeyEvent.KEYCODE_T:
            mMapView.animateCamera(CameraUpdateFactory.zoomIn());
            propagate = false;
            break;
         case KeyEvent.KEYCODE_G:
            mMapView.animateCamera(CameraUpdateFactory.zoomOut());
            propagate = false;
            break;
         case KeyEvent.KEYCODE_S:
            setSatelliteOverlay(this.mMapView.getMapType() != GoogleMap.MAP_TYPE_NORMAL);
            propagate = false;
            break;
         case KeyEvent.KEYCODE_A:
            setTrafficOverlay(!this.mMapView.isTrafficEnabled());
            propagate = false;
            break;
         case KeyEvent.KEYCODE_F:
            mAverageSpeed = 0.0;
            moveToTrack(this.mTrackId - 1, true);
            propagate = false;
            break;
         case KeyEvent.KEYCODE_H:
            mAverageSpeed = 0.0;
            moveToTrack(this.mTrackId + 1, true);
            propagate = false;
            break;
         default:
            propagate = super.onKeyDown(keyCode, event);
            break;
      }
      return propagate;
   }

   private void setTrafficOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.TRAFFIC, b);
      editor.commit();
   }

   private void setSatelliteOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.SATELLITE, b);
      editor.commit();
   }

   private void setSpeedOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.SPEED, b);
      editor.commit();
   }

   private void setAltitudeOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.ALTITUDE, b);
      editor.commit();
   }

   private void setDistanceOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.DISTANCE, b);
      editor.commit();
   }

   private void setLocationOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.LOCATION, b);
      editor.commit();
   }

   private void createListeners()
   {
      /*******************************************************
       * 8 Runnable listener actions
       */
      speedCalculator = new Runnable()
         {
            @Override
            public void run()
            {
               double avgspeed = 0.0;
               ContentResolver resolver = LoggerMap.this.getContentResolver();
               Cursor waypointsCursor = null;
               try
               {
                  waypointsCursor = resolver.query(Uri.withAppendedPath(Tracks.CONTENT_URI, LoggerMap.this.mTrackId + "/waypoints"), new String[] { "avg(" + Waypoints.SPEED + ")",
                        "max(" + Waypoints.SPEED + ")" }, null, null, null);

                  if (waypointsCursor != null && waypointsCursor.moveToLast())
                  {
                     double average = waypointsCursor.getDouble(0);
                     double maxBasedAverage = waypointsCursor.getDouble(1) / 2;
                     avgspeed = Math.min(average, maxBasedAverage);
                  }
                  if (avgspeed < 2)
                  {
                     avgspeed = 5.55d / 2;
                  }
               }
               finally
               {
                  if (waypointsCursor != null)
                  {
                     waypointsCursor.close();
                  }
               }
               mAverageSpeed = avgspeed;
               runOnUiThread(new Runnable()
                  {
                     @Override
                     public void run()
                     {
                        updateSpeedColoring();
                     }
                  });
            }
         };
      mServiceConnected = new Runnable()
         {
            @Override
            public void run()
            {
               updateBlankingBehavior();
            }
         };
      /*******************************************************
       * 8 Various dialog listeners
       */
      mNoteSelectDialogListener = new DialogInterface.OnClickListener()
         {

            @Override
            public void onClick(DialogInterface dialog, int which)
            {
               Uri selected = (Uri) mGallery.getSelectedItem();
               SegmentOverlay.handleMedia(LoggerMap.this, selected);
            }
         };
      mGroupCheckedChangeListener = new android.widget.RadioGroup.OnCheckedChangeListener()
         {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
               switch (checkedId)
               {
                  case R.id.layer_google_satellite:
                     setSatelliteOverlay(true);
                     break;
                  case R.id.layer_google_regular:
                     setSatelliteOverlay(false);
                     break;
                  default:
                     break;
               }
            }
         };
      mCheckedChangeListener = new OnCheckedChangeListener()
         {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
               int checkedId;
               checkedId = buttonView.getId();
               switch (checkedId)
               {
                  case R.id.layer_traffic:
                     setTrafficOverlay(isChecked);
                     break;
                  case R.id.layer_speed:
                     setSpeedOverlay(isChecked);
                     break;
                  case R.id.layer_altitude:
                     setAltitudeOverlay(isChecked);
                     break;
                  case R.id.layer_distance:
                     setDistanceOverlay(isChecked);
                     break;
                  case R.id.layer_location:
                     setLocationOverlay(isChecked);
                     break;
                  default:
                     break;
               }
            }
         };
      mNoTrackDialogListener = new DialogInterface.OnClickListener()
         {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
               //            Log.d( TAG, "mNoTrackDialogListener" + which);
               Intent tracklistIntent = new Intent(LoggerMap.this, TrackList.class);
               tracklistIntent.putExtra(Tracks._ID, LoggerMap.this.mTrackId);
               startActivityForResult(tracklistIntent, MENU_LOGGERMAP_TRACKLIST);
            }
         };
      /**
       * Listeners to events outside this mapview
       */
      mSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener()
         {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
            {
               if (key.equals(Constants.TRACKCOLORING))
               {
                  mAverageSpeed = 0.0;
                  updateSpeedColoring();
               }
               else if (key.equals(Constants.DISABLEBLANKING) || key.equals(Constants.DISABLEDIMMING))
               {
                  updateBlankingBehavior();
               }
               else if (key.equals(Constants.SPEED))
               {
                  updateSpeedDisplayVisibility();
               }
               else if (key.equals(Constants.ALTITUDE))
               {
                  updateAltitudeDisplayVisibility();
               }
               else if (key.equals(Constants.DISTANCE))
               {
                  updateDistanceDisplayVisibility();
               }
               else if (key.equals(Constants.TRAFFIC))
               {
                  updateGoogleOverlays();
               }
               else if (key.equals(Constants.SATELLITE))
               {
                  updateGoogleOverlays();
               }
               else if (key.equals(Constants.LOCATION))
               {
                  updateLocationDisplayVisibility();
               }
            }
         };
      mTrackMediasObserver = new ContentObserver(new Handler())
         {
            @Override
            public void onChange(boolean selfUpdate)
            {
               if (!selfUpdate)
               {
                  mTrackTileProvider.calculateMediaOnLastSegment();
               }
               else
               {
                  Log.w(TAG, "mTrackMediasObserver skipping change on " + mLastSegment);
               }
            }
         };
      mTrackSegmentsObserver = new ContentObserver(new Handler())
         {
            @Override
            public void onChange(boolean selfUpdate)
            {
               if (!selfUpdate)
               {
                  LoggerMap.this.updateDataOverlays();
               }
               else
               {
                  Log.w(TAG, "mTrackSegmentsObserver skipping change on " + mLastSegment);
               }
            }
         };
      mSegmentWaypointsObserver = new ContentObserver(new Handler())
         {
            @Override
            public void onChange(boolean selfUpdate)
            {
               if (!selfUpdate)
               {
                  LoggerMap.this.updateTrackNumbers();
                  moveActiveViewWindow();
                  LoggerMap.this.updateMapProviderAdministration();
               }
               else
               {
                  Log.w(TAG, "mSegmentWaypointsObserver skipping change on " + mLastSegment);
               }
            }
         };
      mUnitsChangeListener = new UnitsI18n.UnitsChangeListener()
         {
            @Override
            public void onUnitsChange()
            {
               mAverageSpeed = 0.0;
               updateTrackNumbers();
               updateSpeedColoring();
            }
         };
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      super.onCreateOptionsMenu(menu);
      getMenuInflater().inflate(R.menu.loggermap, menu);
      return true;
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
    */
   @Override
   public boolean onPrepareOptionsMenu(Menu menu)
   {
      MenuItem noteMenu = menu.findItem(R.id.menu_loggermap_note);
      noteMenu.setEnabled(mLoggerServiceManager.isMediaPrepared());

      MenuItem shareMenu = menu.findItem(R.id.menu_loggermap_share);
      shareMenu.setEnabled(mTrackId >= 0);

      return super.onPrepareOptionsMenu(menu);
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      boolean handled = false;

      Uri trackUri;
      Intent intent;
      switch (item.getItemId())
      {
         case R.id.menu_loggermap_tracking:
            intent = new Intent(this, ControlTracking.class);
            startActivityForResult(intent, MENU_LOGGERMAP_TRACKING);
            handled = true;
            break;
         case R.id.menu_loggermap_layers:
            showDialog(DIALOG_LAYERS);
            handled = true;
            break;
         case R.id.menu_loggermap_note:
            intent = new Intent(this, InsertNote.class);
            startActivityForResult(intent, MENU_LOGGERMAP_NOTE);
            handled = true;
            break;
         case R.id.menu_loggermap_settings:
            intent = new Intent(this, ApplicationPreferenceActivity.class);
            startActivity(intent);
            handled = true;
            break;
         case R.id.menu_loggermap_tracklist:
            intent = new Intent(this, TrackList.class);
            intent.putExtra(Tracks._ID, this.mTrackId);
            startActivityForResult(intent, MENU_LOGGERMAP_TRACKLIST);
            break;
         case R.id.menu_loggermap_stats:
            if (this.mTrackId >= 0)
            {
               intent = new Intent(this, Statistics.class);
               trackUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, mTrackId);
               intent.setData(trackUri);
               startActivity(intent);
               handled = true;
               break;
            }
            else
            {
               showDialog(DIALOG_NOTRACK);
            }
            handled = true;
            break;
         case R.id.menu_loggermap_share:
            intent = new Intent(Intent.ACTION_RUN);
            trackUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, mTrackId);
            intent.setDataAndType(trackUri, Tracks.CONTENT_ITEM_TYPE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Bitmap bm = findViewById(R.id.mapScreen).getDrawingCache();
            Uri screenStreamUri = ShareTrack.storeScreenBitmap(bm);
            intent.putExtra(Intent.EXTRA_STREAM, screenStreamUri);
            startActivityForResult(Intent.createChooser(intent, getString(R.string.share_track)), MENU_LOGGERMAP_SHARE);
            handled = true;
            break;
         case R.id.menu_loggermap_contrib:
            showDialog(DIALOG_CONTRIB);
            handled = true;
            break;
         default:
            handled = super.onOptionsItemSelected(item);
            break;
      }
      return handled;
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onCreateDialog(int)
    */
   @Override
   protected Dialog onCreateDialog(int id)
   {
      Dialog dialog = null;
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_LAYERS:
            builder = new AlertDialog.Builder(this);
            factory = LayoutInflater.from(this);
            view = factory.inflate(R.layout.layerdialog, null);

            mTraffic = (CheckBox) view.findViewById(R.id.layer_traffic);
            mSpeed = (CheckBox) view.findViewById(R.id.layer_speed);
            mAltitude = (CheckBox) view.findViewById(R.id.layer_altitude);
            mDistance = (CheckBox) view.findViewById(R.id.layer_distance);
            mCompass = (CheckBox) view.findViewById(R.id.layer_compass);
            mLocation = (CheckBox) view.findViewById(R.id.layer_location);

            ((RadioGroup) view.findViewById(R.id.google_backgrounds)).setOnCheckedChangeListener(mGroupCheckedChangeListener);

            mTraffic.setOnCheckedChangeListener(mCheckedChangeListener);
            mSpeed.setOnCheckedChangeListener(mCheckedChangeListener);
            mAltitude.setOnCheckedChangeListener(mCheckedChangeListener);
            mDistance.setOnCheckedChangeListener(mCheckedChangeListener);
            mCompass.setOnCheckedChangeListener(mCheckedChangeListener);
            mLocation.setOnCheckedChangeListener(mCheckedChangeListener);

            builder.setTitle(R.string.dialog_layer_title).setIcon(android.R.drawable.ic_dialog_map).setPositiveButton(R.string.btn_okay, null).setView(view);
            dialog = builder.create();
            return dialog;
         case DIALOG_NOTRACK:
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_notrack_title).setMessage(R.string.dialog_notrack_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(R.string.btn_selecttrack, mNoTrackDialogListener).setNegativeButton(R.string.btn_cancel, null);
            dialog = builder.create();
            return dialog;
         case DIALOG_URIS:
            builder = new AlertDialog.Builder(this);
            factory = LayoutInflater.from(this);
            view = factory.inflate(R.layout.mediachooser, null);
            mGallery = (Gallery) view.findViewById(R.id.gallery);
            builder.setTitle(R.string.dialog_select_media_title).setMessage(R.string.dialog_select_media_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setNegativeButton(R.string.btn_cancel, null).setPositiveButton(R.string.btn_okay, mNoteSelectDialogListener).setView(view);
            dialog = builder.create();
            return dialog;
         case DIALOG_CONTRIB:
            builder = new AlertDialog.Builder(this);
            factory = LayoutInflater.from(this);
            view = factory.inflate(R.layout.contrib, null);
            WebView contribView = (WebView) view.findViewById(R.id.contrib_view);
            contribView.loadUrl("file:///android_asset/contrib.html");
            builder.setTitle(R.string.dialog_contrib_title).setView(view).setIcon(android.R.drawable.ic_dialog_email).setPositiveButton(R.string.btn_okay, null);
            dialog = builder.create();
            return dialog;
         case MENU_PLAYERROR:
            int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
            dialog = GooglePlayServicesUtil.getErrorDialog(result, this, MENU_PLAYERROR);
            return dialog;
         default:
            return super.onCreateDialog(id);
      }
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
    */
   @Override
   protected void onPrepareDialog(int id, Dialog dialog)
   {
      super.onPrepareDialog(id, dialog);

      RadioButton satellite;
      RadioButton regular;
      RadioButton cloudmade;
      RadioButton mapnik;
      RadioButton cycle;
      switch (id)
      {
         case DIALOG_LAYERS:
            satellite = (RadioButton) dialog.findViewById(R.id.layer_google_satellite);
            regular = (RadioButton) dialog.findViewById(R.id.layer_google_regular);
            satellite.setChecked(mSharedPreferences.getBoolean(Constants.SATELLITE, false));
            regular.setChecked(!mSharedPreferences.getBoolean(Constants.SATELLITE, false));

            mTraffic.setChecked(mSharedPreferences.getBoolean(Constants.TRAFFIC, false));
            mSpeed.setChecked(mSharedPreferences.getBoolean(Constants.SPEED, false));
            mAltitude.setChecked(mSharedPreferences.getBoolean(Constants.ALTITUDE, false));
            mDistance.setChecked(mSharedPreferences.getBoolean(Constants.DISTANCE, false));
            mLocation.setChecked(mSharedPreferences.getBoolean(Constants.LOCATION, false));
            break;
         case DIALOG_URIS:
            mGallery.setAdapter(mMediaAdapter);
            break;
         default:
            break;
      }
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
    */
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent intent)
   {
      super.onActivityResult(requestCode, resultCode, intent);
      Uri trackUri;
      long trackId;
      switch (requestCode)
      {
         case MENU_LOGGERMAP_TRACKLIST:
            if (resultCode == RESULT_OK)
            {
               trackUri = intent.getData();
               trackId = Long.parseLong(trackUri.getLastPathSegment());
               mAverageSpeed = 0.0;
               moveToTrack(trackId, true);
            }
            break;
         case MENU_LOGGERMAP_ABOUT:
            break;
         case MENU_LOGGERMAP_TRACKING:
            if (resultCode == RESULT_OK)
            {
               trackUri = intent.getData();
               if (trackUri != null)
               {
                  trackId = Long.parseLong(trackUri.getLastPathSegment());
                  mAverageSpeed = 0.0;
                  moveToTrack(trackId, true);
               }
            }
            break;
         case MENU_LOGGERMAP_SHARE:
            ShareTrack.clearScreenBitmap();
            break;
         case MENU_PLAYERROR:
            Log.i(TAG, "Play services error dialog finished");
            break;
         default:
            Log.e(TAG, "Returned form unknow activity: " + requestCode);
            break;
      }
   }

   public void showAboutInfo(View v)
   {
      dismissDialog(DIALOG_CONTRIB);
      Intent intent = new Intent(this, About.class);
      startActivityForResult(intent, MENU_LOGGERMAP_ABOUT);
   }

   private void updateTitleBar()
   {
      ContentResolver resolver = this.getContentResolver();
      Cursor trackCursor = null;
      try
      {
         trackCursor = resolver.query(ContentUris.withAppendedId(Tracks.CONTENT_URI, this.mTrackId), new String[] { Tracks.NAME }, null, null, null);
         if (trackCursor != null && trackCursor.moveToLast())
         {
            String trackName = trackCursor.getString(0);
            this.setTitle(this.getString(R.string.app_name) + ": " + trackName);
         }
      }
      finally
      {
         if (trackCursor != null)
         {
            trackCursor.close();
         }
      }
   }

   private void updateGoogleOverlays()
   {
      boolean sat = mSharedPreferences.getBoolean(Constants.SATELLITE, false);
      if (sat)
      {
         LoggerMap.this.mMapView.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
      }
      else
      {
         LoggerMap.this.mMapView.setMapType(GoogleMap.MAP_TYPE_NORMAL);
      }
      boolean traffic = mSharedPreferences.getBoolean(Constants.TRAFFIC, false);
      LoggerMap.this.mMapView.setTrafficEnabled(traffic);
   }

   protected void updateMapProviderAdministration()
   {
      if (findViewById(R.id.myMapView).getVisibility() == View.VISIBLE)
      {
         mLoggerServiceManager.storeDerivedDataSource(GOOGLE_PROVIDER);
      }
   }

   private void updateBlankingBehavior()
   {
      boolean disableblanking = mSharedPreferences.getBoolean(Constants.DISABLEBLANKING, false);
      boolean disabledimming = mSharedPreferences.getBoolean(Constants.DISABLEDIMMING, false);
      if (disableblanking)
      {
         if (mWakeLock == null)
         {
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            if (disabledimming)
            {
               mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
            }
            else
            {
               mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, TAG);
            }
         }
         if (mLoggerServiceManager.getLoggingState() == Constants.LOGGING && !mWakeLock.isHeld())
         {
            mWakeLock.acquire();
            Log.w(TAG, "Acquired lock to keep screen on!");
         }
      }
   }

   private void updateSpeedColoring()
   {
      int trackColoringMethod = Integer.valueOf(mSharedPreferences.getString(Constants.TRACKCOLORING, "3")).intValue();
      View speedbar = findViewById(R.id.speedbar);

      if (trackColoringMethod == SegmentOverlay.DRAW_MEASURED || trackColoringMethod == SegmentOverlay.DRAW_CALCULATED)
      {
         // mAverageSpeed is set to 0 if unknown or to trigger an recalculation here
         if (mAverageSpeed == 0.0)
         {
            mHandler.removeCallbacks(speedCalculator);
            mHandler.post(speedCalculator);
         }
         else
         {
            drawSpeedTexts(mAverageSpeed);

            speedbar.setVisibility(View.VISIBLE);
            for (int i = 0; i < mSpeedtexts.length; i++)
            {
               mSpeedtexts[i].setVisibility(View.VISIBLE);
            }
         }
      }
      else
      {
         speedbar.setVisibility(View.INVISIBLE);
         for (int i = 0; i < mSpeedtexts.length; i++)
         {
            mSpeedtexts[i].setVisibility(View.INVISIBLE);
         }
      }
      mTrackTileProvider.setTrackColoringMethod(trackColoringMethod, mAverageSpeed);

   }

   private void updateSpeedDisplayVisibility()
   {
      boolean showspeed = mSharedPreferences.getBoolean(Constants.SPEED, false);
      if (showspeed)
      {
         mLastGPSSpeedView.setVisibility(View.VISIBLE);
      }
      else
      {
         mLastGPSSpeedView.setVisibility(View.GONE);
      }
   }

   private void updateAltitudeDisplayVisibility()
   {
      boolean showaltitude = mSharedPreferences.getBoolean(Constants.ALTITUDE, false);
      if (showaltitude)
      {
         mLastGPSAltitudeView.setVisibility(View.VISIBLE);
      }
      else
      {
         mLastGPSAltitudeView.setVisibility(View.GONE);
      }
   }

   private void updateDistanceDisplayVisibility()
   {
      boolean showdistance = mSharedPreferences.getBoolean(Constants.DISTANCE, false);
      if (showdistance)
      {
         mDistanceView.setVisibility(View.VISIBLE);
      }
      else
      {
         mDistanceView.setVisibility(View.GONE);
      }
   }

   private void updateLocationDisplayVisibility()
   {
      boolean location = mSharedPreferences.getBoolean(Constants.LOCATION, false);
      mMapView.setMyLocationEnabled(location);
   }

   /**
    * Retrieves the numbers of the measured speed and altitude from the most recent waypoint and updates UI components with this latest bit of information.
    */
   private void updateTrackNumbers()
   {
      Location lastWaypoint = mLoggerServiceManager.getLastWaypoint();
      UnitsI18n units = mUnits;
      if (lastWaypoint != null && units != null)
      {
         // Speed number
         double speed = lastWaypoint.getSpeed();
         speed = units.conversionFromMetersPerSecond(speed);
         String speedText = units.formatSpeed(speed, false);
         mLastGPSSpeedView.setText(speedText);

         // Speed color bar and refrence numbers
         if (speed > 2 * mAverageSpeed)
         {
            mAverageSpeed = 0.0;
            updateSpeedColoring();
         }

         //Altitude number
         double altitude = lastWaypoint.getAltitude();
         altitude = units.conversionFromMeterToHeight(altitude);
         String altitudeText = String.format("%.0f %s", altitude, units.getHeightUnit());
         mLastGPSAltitudeView.setText(altitudeText);

         //Distance number
         double distance = units.conversionFromMeter(mLoggerServiceManager.getTrackedDistance());
         String distanceText = String.format("%.2f %s", distance, units.getDistanceUnit());
         mDistanceView.setText(distanceText);
      }
   }

   /**
    * For the current track identifier the route of that track is drawn by adding a OverLay for each segments in the track
    * 
    * @param trackId
    * @see SegmentOverlay
    */
   private void createDataOverlays()
   {
      mMapView.clear();

      mTrackTileProvider = new TrackTileProvider(this);
      mLastSegment = mTrackTileProvider.setTrackId(mTrackId, mAverageSpeed);
      mTrackTileOverlay = mMapView.addTileOverlay(new TileOverlayOptions().tileProvider(mTrackTileProvider).visible(true).zIndex(5F));

      Uri lastSegmentUri = Uri.withAppendedPath(Tracks.CONTENT_URI, mTrackId + "/segments/" + mLastSegment + "/waypoints");
      ContentResolver resolver = getContentResolver();
      resolver.unregisterContentObserver(this.mSegmentWaypointsObserver);
      resolver.registerContentObserver(lastSegmentUri, false, this.mSegmentWaypointsObserver);
   }

   private void updateDataOverlays()
   {
      ContentResolver resolver = this.getContentResolver();
      Uri segmentsUri = Uri.withAppendedPath(Tracks.CONTENT_URI, this.mTrackId + "/segments");
      Cursor segmentsCursor = null;
      int segmentOverlaysCount = 0;
      long segmentsTrackId = -1;
      if (mTrackTileProvider != null)
      {
         segmentsTrackId = mTrackTileProvider.getTrackId();
         segmentOverlaysCount = mTrackTileProvider.getSegmentCount();
      }
      if (segmentsTrackId == mTrackId)
      {
         try
         {
            segmentsCursor = resolver.query(segmentsUri, new String[] { Segments._ID }, null, null, null);
            if (segmentsCursor != null && segmentsCursor.getCount() == segmentOverlaysCount)
            {
               //            Log.d( TAG, "Alignment of segments" );
            }
            else
            {
               createDataOverlays();
            }
         }
         finally
         {
            if (segmentsCursor != null)
            {
               segmentsCursor.close();
            }
         }
      }
      else
      {
         createDataOverlays();
      }

   }

   /**
    * Call when an overlay has recalulated and has new information to be redrawn
    */
   public void onDateOverlayChanged()
   {
      //      this.mMapView.postInvalidate();
   }

   private void moveActiveViewWindow()
   {
      LatLng lastPoint = getLastTrackPoint();
      if (lastPoint != null && mLoggerServiceManager.getLoggingState() == Constants.LOGGING)
      {
         Point out = this.mMapView.getProjection().toScreenLocation(lastPoint);
         int height = mMapscreen.getHeight();
         int width = mMapscreen.getWidth();
         if (out.x < 0 || out.y < 0 || out.y > height || out.x > width)
         {

            this.mMapView.stopAnimation();
            mMapView.animateCamera(CameraUpdateFactory.newLatLng(lastPoint));
         }
         else if (out.x < width / 4 || out.y < height / 4 || out.x > (width / 4) * 3 || out.y > (height / 4) * 3)
         {
            this.mMapView.stopAnimation();
            mMapView.animateCamera(CameraUpdateFactory.newLatLng(lastPoint));
         }
      }
   }

   /**
    * @param avgSpeed avgSpeed in m/s
    */
   private void drawSpeedTexts(double avgSpeed)
   {
      UnitsI18n units = mUnits;
      if (units != null)
      {
         avgSpeed = units.conversionFromMetersPerSecond(avgSpeed);
         for (int i = 0; i < mSpeedtexts.length; i++)
         {
            mSpeedtexts[i].setVisibility(View.VISIBLE);
            double speed;
            if (mUnits.isUnitFlipped())
            {
               speed = ((avgSpeed * 2d) / 5d) * (mSpeedtexts.length - i - 1);
            }
            else
            {
               speed = ((avgSpeed * 2d) / 5d) * i;
            }
            String speedText = units.formatSpeed(speed, false);
            mSpeedtexts[i].setText(speedText);
         }
      }
   }

   /**
    * Alter this to set a new track as current.
    * 
    * @param trackId
    * @param center center on the end of the track
    */
   private void moveToTrack(long trackId, boolean center)
   {
      Cursor track = null;
      try
      {
         ContentResolver resolver = this.getContentResolver();
         Uri trackUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, trackId);
         track = resolver.query(trackUri, new String[] { Tracks.NAME }, null, null, null);
         if (track != null && track.moveToFirst())
         {
            this.mTrackId = trackId;
            mLastSegment = -1;
            resolver.unregisterContentObserver(this.mTrackSegmentsObserver);
            resolver.unregisterContentObserver(this.mTrackMediasObserver);
            Uri tracksegmentsUri = Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/segments");

            resolver.registerContentObserver(tracksegmentsUri, false, this.mTrackSegmentsObserver);
            resolver.registerContentObserver(Media.CONTENT_URI, true, this.mTrackMediasObserver);

            updateTitleBar();
            createDataOverlays();
            updateSpeedColoring();
            if (center)
            {
               LatLng lastPoint = getLastTrackPoint();
               mMapView.animateCamera(CameraUpdateFactory.newLatLng(lastPoint));
            }
         }
      }
      finally
      {
         if (track != null)
         {
            track.close();
         }
      }
   }

   /**
    * Get the last know position from the GPS provider and return that information wrapped in a GeoPoint to which the Map can navigate.
    * 
    * @see GeoPoint
    * @return
    */
   private LatLng getLastKnowGeopointLocation()
   {
      double latitude = 0;
      double longitude = 0;
      LocationManager locationManager = (LocationManager) this.getApplication().getSystemService(Context.LOCATION_SERVICE);
      Location locationFine = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
      if (locationFine != null)
      {
         latitude = locationFine.getLatitude();
         longitude = locationFine.getLongitude();
      }
      if (locationFine == null || latitude == 0 || longitude == 0)
      {
         Location locationCoarse = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
         if (locationCoarse != null)
         {
            latitude = locationCoarse.getLatitude();
            longitude = locationCoarse.getLongitude();
         }
         if (locationCoarse == null || latitude == 0 || longitude == 0)
         {
            latitude = 51.985105;
            longitude = 5.106132;
         }
      }
      LatLng geoPoint = new LatLng(latitude, longitude);
      return geoPoint;
   }

   /**
    * Retrieve the last point of the current track
    * 
    * @param context
    */
   private LatLng getLastTrackPoint()
   {
      Cursor waypoint = null;
      LatLng lastPoint = null;
      // First try the service which might have a cached version
      Location lastLoc = mLoggerServiceManager.getLastWaypoint();
      if (lastLoc != null)
      {
         double latitude = lastLoc.getLatitude();
         double longitude = lastLoc.getLongitude();
         lastPoint = new LatLng(latitude, longitude);
      }

      // If nothing yet, try the content resolver and query the track
      if (lastPoint == null || lastPoint.latitude == 0 || lastPoint.longitude == 0)
      {
         try
         {
            ContentResolver resolver = this.getContentResolver();
            waypoint = resolver.query(Uri.withAppendedPath(Tracks.CONTENT_URI, mTrackId + "/waypoints"), new String[] { Waypoints.LATITUDE, Waypoints.LONGITUDE,
                  "max(" + Waypoints.TABLE + "." + Waypoints._ID + ")" }, null, null, null);
            if (waypoint != null && waypoint.moveToLast())
            {
               double latitude = waypoint.getDouble(0);
               double longitude = waypoint.getDouble(1);
               lastPoint = new LatLng(latitude, longitude);
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

      // If nothing yet, try the last generally known location
      if (lastPoint == null || lastPoint.latitude == 0 || lastPoint.longitude == 0)
      {
         lastPoint = getLastKnowGeopointLocation();
      }
      return lastPoint;
   }

   private void moveToLastTrack()
   {
      int trackId = -1;
      Cursor track = null;
      try
      {
         ContentResolver resolver = this.getContentResolver();
         track = resolver.query(Tracks.CONTENT_URI, new String[] { "max(" + Tracks._ID + ")", Tracks.NAME, }, null, null, null);
         if (track != null && track.moveToLast())
         {
            trackId = track.getInt(0);
            mAverageSpeed = 0.0;
            moveToTrack(trackId, false);
         }
      }
      finally
      {
         if (track != null)
         {
            track.close();
         }
      }
   }

   /**
    * Enables a SegmentOverlay to call back to the MapActivity to show a dialog with choices of media
    * 
    * @param mediaAdapter
    */
   public void showDialog(BaseAdapter mediaAdapter)
   {
      mMediaAdapter = mediaAdapter;
      showDialog(LoggerMap.DIALOG_URIS);
   }
}