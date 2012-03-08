/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Feb 26, 2012 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 */
package nl.sogeti.android.gpstracker.viewer.map;

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
import nl.sogeti.android.gpstracker.viewer.About;
import nl.sogeti.android.gpstracker.viewer.ApplicationPreferenceActivity;
import nl.sogeti.android.gpstracker.viewer.TrackList;
import android.app.Activity;
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
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Gallery;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;

/**
 * ????
 * 
 * @version $Id:$
 * @author rene (c) Feb 26, 2012, Sogeti B.V.
 */
public class LoggerMapHelper
{

   public static final String OSM_PROVIDER = "OSM";
   public static final String GOOGLE_PROVIDER = "GOOGLE";
   public static final String MAPQUEST_PROVIDER = "MAPQUEST";

   private static final String INSTANCE_E6LONG = "e6long";
   private static final String INSTANCE_E6LAT = "e6lat";
   private static final String INSTANCE_ZOOM = "zoom";
   private static final String INSTANCE_SPEED = "averagespeed";
   private static final String INSTANCE_TRACK = "track";
   private static final int ZOOM_LEVEL = 16;
   // MENU'S
   private static final int MENU_SETTINGS = 1;
   private static final int MENU_TRACKING = 2;
   private static final int MENU_TRACKLIST = 3;
   private static final int MENU_STATS = 4;
   private static final int MENU_ABOUT = 5;
   private static final int MENU_LAYERS = 6;
   private static final int MENU_NOTE = 7;
   private static final int MENU_SHARE = 13;
   private static final int MENU_CONTRIB = 14;
   private static final int DIALOG_NOTRACK = 24;
   private static final int DIALOG_LAYERS = 31;
   private static final int DIALOG_URIS = 34;
   private static final int DIALOG_CONTRIB = 35;
   private static final String TAG = "OGT.LoggerMap";

   private double mAverageSpeed = 33.33d / 3d;
   private long mTrackId = -1;
   private long mLastSegment = -1;
   private UnitsI18n mUnits;
   private WakeLock mWakeLock = null;
   private SharedPreferences mSharedPreferences;
   private GPSLoggerServiceManager mLoggerServiceManager;
   private SegmentRendering mLastSegmentOverlay;
   private BaseAdapter mMediaAdapter;

   private Handler mHandler;

   private ContentObserver mTrackSegmentsObserver;
   private ContentObserver mSegmentWaypointsObserver;
   private ContentObserver mTrackMediasObserver;
   private DialogInterface.OnClickListener mNoTrackDialogListener;
   private OnItemSelectedListener mGalerySelectListener;
   private Uri mSelected;
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

   private LoggerMap mLoggerMap;
   private BitmapSegmentsOverlay mBitmapSegmentsOverlay;

   public LoggerMapHelper(LoggerMap loggerMap)
   {
      mLoggerMap = loggerMap;
   }

   /**
    * Called when the activity is first created.
    */
   protected void onCreate(Bundle load)
   {
      mLoggerMap.setDrawingCacheEnabled(true);
      mUnits = new UnitsI18n(mLoggerMap.getActivity());
      mLoggerServiceManager = new GPSLoggerServiceManager(mLoggerMap.getActivity());

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
      mBitmapSegmentsOverlay = new BitmapSegmentsOverlay(mLoggerMap, mHandler);
      mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mLoggerMap.getActivity());
      createListeners();
      onRestoreInstanceState(load);
      mLoggerMap.updateOverlays();
   }

   protected void onResume()
   {
      updateMapProvider();

      mLoggerServiceManager.startup(mLoggerMap.getActivity(), mServiceConnected);

      mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
      mUnits.setUnitsChangeListener(mUnitsChangeListener);
      updateTitleBar();
      updateBlankingBehavior();

      if (mTrackId >= 0)
      {
         ContentResolver resolver = mLoggerMap.getActivity().getContentResolver();
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
      updateCompassDisplayVisibility();
      updateLocationDisplayVisibility();

      mLoggerMap.executePostponedActions();
   }

   protected void onPause()
   {
      if (this.mWakeLock != null && this.mWakeLock.isHeld())
      {
         this.mWakeLock.release();
         Log.w(TAG, "onPause(): Released lock to keep screen on!");
      }
      mBitmapSegmentsOverlay.clearSegments();
      ContentResolver resolver = mLoggerMap.getActivity().getContentResolver();
      resolver.unregisterContentObserver(this.mTrackSegmentsObserver);
      resolver.unregisterContentObserver(this.mSegmentWaypointsObserver);
      resolver.unregisterContentObserver(this.mTrackMediasObserver);
      mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this.mSharedPreferenceChangeListener);
      mUnits.setUnitsChangeListener(null);
      mLoggerMap.disableMyLocation();
      mLoggerMap.disableCompass();
      this.mLoggerServiceManager.shutdown(mLoggerMap.getActivity());
   }

   protected void onDestroy()
   {
      mLastSegmentOverlay = null;
      mLoggerMap.clearOverlays();
      mBitmapSegmentsOverlay.clearSegments();
      mHandler.post(new Runnable()
      {
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
         mLoggerMap.getActivity().stopService(new Intent(Constants.SERVICENAME));
      }
      mUnits = null;
   }

   public void onNewIntent(Intent newIntent)
   {
      Uri data = newIntent.getData();
      if (data != null)
      {
         moveToTrack(Long.parseLong(data.getLastPathSegment()), true);
      }
   }

   protected void onRestoreInstanceState(Bundle load)
   {
      Uri data = mLoggerMap.getActivity().getIntent().getData();
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
         mLoggerMap.setZoom(load.getInt(INSTANCE_ZOOM));
      }
      else
      {
         mLoggerMap.setZoom(ZOOM_LEVEL);
      }

      if (load != null && load.containsKey(INSTANCE_E6LAT) && load.containsKey(INSTANCE_E6LONG))
      {
         GeoPoint storedPoint = new GeoPoint(load.getInt(INSTANCE_E6LAT), load.getInt(INSTANCE_E6LONG));
         mLoggerMap.animateTo(storedPoint);
      }
      else
      {
         GeoPoint lastPoint = getLastTrackPoint();
         mLoggerMap.animateTo(lastPoint);
      }
   }

   protected void onSaveInstanceState(Bundle save)
   {
      save.putLong(INSTANCE_TRACK, this.mTrackId);
      save.putDouble(INSTANCE_SPEED, mAverageSpeed);
      save.putInt(INSTANCE_ZOOM, mLoggerMap.getZoomLevel());
      GeoPoint point = mLoggerMap.getMapCenter();
      save.putInt(INSTANCE_E6LAT, point.getLatitudeE6());
      save.putInt(INSTANCE_E6LONG, point.getLongitudeE6());
   }

   public boolean onKeyDown(int keyCode, KeyEvent event)
   {
      boolean propagate = true;
      switch (keyCode)
      {
         case KeyEvent.KEYCODE_T:
            propagate = mLoggerMap.zoomIn();
            propagate = false;
            break;
         case KeyEvent.KEYCODE_G:
            propagate = mLoggerMap.zoomOut();
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
      }
      return propagate;
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

   private void setCompassOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.COMPASS, b);
      editor.commit();
   }

   private void setLocationOverlay(boolean b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean(Constants.LOCATION, b);
      editor.commit();
   }

   private void setOsmBaseOverlay(int b)
   {
      Editor editor = mSharedPreferences.edit();
      editor.putInt(Constants.OSMBASEOVERLAY, b);
      editor.commit();
   }

   private void createListeners()
   {
      /*******************************************************
       * 8 Runnable listener actions
       */
      speedCalculator = new Runnable()
      {
         public void run()
         {
            double avgspeed = 0.0;
            ContentResolver resolver = mLoggerMap.getActivity().getContentResolver();
            Cursor waypointsCursor = null;
            try
            {
               waypointsCursor = resolver.query(Uri.withAppendedPath(Tracks.CONTENT_URI, LoggerMapHelper.this.mTrackId + "/waypoints"), new String[] {
                     "avg(" + Waypoints.SPEED + ")", "max(" + Waypoints.SPEED + ")" }, null, null, null);

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
            mLoggerMap.getActivity().runOnUiThread(new Runnable()
            {
               public void run()
               {
                  updateSpeedColoring();
               }
            });
         }
      };
      mServiceConnected = new Runnable()
      {
         public void run()
         {
            updateBlankingBehavior();
         }
      };
      /*******************************************************
       * 8 Various dialog listeners
       */
      mGalerySelectListener = new AdapterView.OnItemSelectedListener()
      {
         public void onItemSelected(AdapterView< ? > parent, View view, int pos, long id)
         {
            mSelected = (Uri) parent.getSelectedItem();
         }

         public void onNothingSelected(AdapterView< ? > arg0)
         {
            mSelected = null;
         }
      };
      mNoteSelectDialogListener = new DialogInterface.OnClickListener()
      {

         public void onClick(DialogInterface dialog, int which)
         {
            SegmentRendering.handleMedia(mLoggerMap.getActivity(), mSelected);
            mSelected = null;
         }
      };
      mGroupCheckedChangeListener = new android.widget.RadioGroup.OnCheckedChangeListener()
      {
         public void onCheckedChanged(RadioGroup group, int checkedId)
         {
            switch (checkedId)
            {
               case R.id.layer_osm_cloudmade:
                  setOsmBaseOverlay(Constants.OSM_CLOUDMADE);
                  break;
               case R.id.layer_osm_maknik:
                  setOsmBaseOverlay(Constants.OSM_MAKNIK);
                  break;
               case R.id.layer_osm_bicycle:
                  setOsmBaseOverlay(Constants.OSM_CYCLE);
                  break;
               default:
                  mLoggerMap.onLayerCheckedChanged(checkedId, true);
                  break;
            }
         }
      };
      mCheckedChangeListener = new OnCheckedChangeListener()
      {
         public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
         {
            int checkedId;
            checkedId = buttonView.getId();
            switch (checkedId)
            {
               case R.id.layer_speed:
                  setSpeedOverlay(isChecked);
                  break;
               case R.id.layer_altitude:
                  setAltitudeOverlay(isChecked);
                  break;
               case R.id.layer_distance:
                  setDistanceOverlay(isChecked);
                  break;
               case R.id.layer_compass:
                  setCompassOverlay(isChecked);
                  break;
               case R.id.layer_location:
                  setLocationOverlay(isChecked);
                  break;
               default:
                  mLoggerMap.onLayerCheckedChanged(checkedId, isChecked);
                  break;
            }
         }
      };
      mNoTrackDialogListener = new DialogInterface.OnClickListener()
      {
         public void onClick(DialogInterface dialog, int which)
         {
            //            Log.d( TAG, "mNoTrackDialogListener" + which);
            Intent tracklistIntent = new Intent(mLoggerMap.getActivity(), TrackList.class);
            tracklistIntent.putExtra(Tracks._ID, mTrackId);
            mLoggerMap.getActivity().startActivityForResult(tracklistIntent, MENU_TRACKLIST);
         }
      };
      /**
       * Listeners to events outside this mapview
       */
      mSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener()
      {
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
            else if (key.equals(Constants.COMPASS))
            {
               updateCompassDisplayVisibility();
            }
            else if (key.equals(Constants.LOCATION))
            {
               updateLocationDisplayVisibility();
            }
            else if (key.equals(Constants.MAPPROVIDER))
            {
               updateMapProvider();
            }
            else if (key.equals(Constants.OSMBASEOVERLAY))
            {
               mLoggerMap.updateOverlays();
            }
            else
            {
               mLoggerMap.onSharedPreferenceChanged(sharedPreferences, key);
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
               if (mLastSegmentOverlay != null)
               {
                  mLastSegmentOverlay.calculateMedia();
                  mLoggerMap.postInvalidate();
               }
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
               updateDataOverlays();
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
               updateTrackNumbers();
               if (mLastSegmentOverlay != null)
               {
                  moveActiveViewWindow();
                  updateMapProviderAdministration(mLoggerMap.getDataSourceId());
               }
               else
               {
                  Log.e(TAG, "Error the last segment changed but it is not on screen! " + mLastSegment);
               }
            }
            else
            {
               Log.w(TAG, "mSegmentWaypointsObserver skipping change on " + mLastSegment);
            }
         }
      };
      mUnitsChangeListener = new UnitsI18n.UnitsChangeListener()
      {
         public void onUnitsChange()
         {
            mAverageSpeed = 0.0;
            updateTrackNumbers();
            updateSpeedColoring();
         }
      };
   }

   public void onCreateOptionsMenu(Menu menu)
   {
      menu.add(ContextMenu.NONE, MENU_TRACKING, ContextMenu.NONE, R.string.menu_tracking).setIcon(R.drawable.ic_menu_movie).setAlphabeticShortcut('T');
      menu.add(ContextMenu.NONE, MENU_LAYERS, ContextMenu.NONE, R.string.menu_showLayers).setIcon(R.drawable.ic_menu_mapmode).setAlphabeticShortcut('L');
      menu.add(ContextMenu.NONE, MENU_NOTE, ContextMenu.NONE, R.string.menu_insertnote).setIcon(R.drawable.ic_menu_myplaces);

      menu.add(ContextMenu.NONE, MENU_STATS, ContextMenu.NONE, R.string.menu_statistics).setIcon(R.drawable.ic_menu_picture).setAlphabeticShortcut('S');
      menu.add(ContextMenu.NONE, MENU_SHARE, ContextMenu.NONE, R.string.menu_shareTrack).setIcon(R.drawable.ic_menu_share).setAlphabeticShortcut('I');
      // More

      menu.add(ContextMenu.NONE, MENU_TRACKLIST, ContextMenu.NONE, R.string.menu_tracklist).setIcon(R.drawable.ic_menu_show_list).setAlphabeticShortcut('P');
      menu.add(ContextMenu.NONE, MENU_SETTINGS, ContextMenu.NONE, R.string.menu_settings).setIcon(R.drawable.ic_menu_preferences).setAlphabeticShortcut('C');
      menu.add(ContextMenu.NONE, MENU_ABOUT, ContextMenu.NONE, R.string.menu_about).setIcon(R.drawable.ic_menu_info_details).setAlphabeticShortcut('A');
      menu.add(ContextMenu.NONE, MENU_CONTRIB, ContextMenu.NONE, R.string.menu_contrib).setIcon(R.drawable.ic_menu_allfriends);
   }

   public void onPrepareOptionsMenu(Menu menu)
   {
      MenuItem noteMenu = menu.findItem(MENU_NOTE);
      noteMenu.setEnabled(mLoggerServiceManager.isMediaPrepared());

      MenuItem shareMenu = menu.findItem(MENU_SHARE);
      shareMenu.setEnabled(mTrackId >= 0);
   }

   public boolean onOptionsItemSelected(MenuItem item)
   {
      boolean handled = false;

      Uri trackUri;
      Intent intent;
      switch (item.getItemId())
      {
         case MENU_TRACKING:
            intent = new Intent(mLoggerMap.getActivity(), ControlTracking.class);
            mLoggerMap.getActivity().startActivityForResult(intent, MENU_TRACKING);
            handled = true;
            break;
         case MENU_LAYERS:
            mLoggerMap.getActivity().showDialog(DIALOG_LAYERS);
            handled = true;
            break;
         case MENU_NOTE:
            intent = new Intent(mLoggerMap.getActivity(), InsertNote.class);
            mLoggerMap.getActivity().startActivityForResult(intent, MENU_NOTE);
            handled = true;
            break;
         case MENU_SETTINGS:
            intent = new Intent(mLoggerMap.getActivity(), ApplicationPreferenceActivity.class);
            mLoggerMap.getActivity().startActivity(intent);
            handled = true;
            break;
         case MENU_TRACKLIST:
            intent = new Intent(mLoggerMap.getActivity(), TrackList.class);
            intent.putExtra(Tracks._ID, this.mTrackId);
            mLoggerMap.getActivity().startActivityForResult(intent, MENU_TRACKLIST);
            handled = true;
            break;
         case MENU_STATS:
            if (this.mTrackId >= 0)
            {
               intent = new Intent(mLoggerMap.getActivity(), Statistics.class);
               trackUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, mTrackId);
               intent.setData(trackUri);
               mLoggerMap.getActivity().startActivity(intent);
               break;
            }
            else
            {
               mLoggerMap.getActivity().showDialog(DIALOG_NOTRACK);
            }
            handled = true;
            break;
         case MENU_ABOUT:
            intent = new Intent(mLoggerMap.getActivity(), About.class);
            mLoggerMap.getActivity().startActivity(intent);
            break;
         case MENU_SHARE:
            intent = new Intent(Intent.ACTION_RUN);
            trackUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, mTrackId);
            intent.setDataAndType(trackUri, Tracks.CONTENT_ITEM_TYPE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Bitmap bm = mLoggerMap.getDrawingCache();
            Uri screenStreamUri = ShareTrack.storeScreenBitmap(bm);
            intent.putExtra(Intent.EXTRA_STREAM, screenStreamUri);
            mLoggerMap.getActivity().startActivityForResult(Intent.createChooser(intent, mLoggerMap.getActivity().getString(R.string.share_track)), MENU_SHARE);
            handled = true;
            break;
         case MENU_CONTRIB:
            mLoggerMap.getActivity().showDialog(DIALOG_CONTRIB);
         default:
            handled = false;
            break;
      }
      return handled;
   }

   protected Dialog onCreateDialog(int id)
   {
      Dialog dialog = null;
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_LAYERS:
            builder = new AlertDialog.Builder(mLoggerMap.getActivity());
            factory = LayoutInflater.from(mLoggerMap.getActivity());
            view = factory.inflate(R.layout.layerdialog, null);

            CheckBox traffic = (CheckBox) view.findViewById(R.id.layer_traffic);
            CheckBox speed = (CheckBox) view.findViewById(R.id.layer_speed);
            CheckBox altitude = (CheckBox) view.findViewById(R.id.layer_altitude);
            CheckBox distance = (CheckBox) view.findViewById(R.id.layer_distance);
            CheckBox compass = (CheckBox) view.findViewById(R.id.layer_compass);
            CheckBox location = (CheckBox) view.findViewById(R.id.layer_location);

            ((RadioGroup) view.findViewById(R.id.google_backgrounds)).setOnCheckedChangeListener(mGroupCheckedChangeListener);
            ((RadioGroup) view.findViewById(R.id.osm_backgrounds)).setOnCheckedChangeListener(mGroupCheckedChangeListener);

            traffic.setOnCheckedChangeListener(mCheckedChangeListener);
            speed.setOnCheckedChangeListener(mCheckedChangeListener);
            altitude.setOnCheckedChangeListener(mCheckedChangeListener);
            distance.setOnCheckedChangeListener(mCheckedChangeListener);
            compass.setOnCheckedChangeListener(mCheckedChangeListener);
            location.setOnCheckedChangeListener(mCheckedChangeListener);

            builder.setTitle(R.string.dialog_layer_title).setIcon(android.R.drawable.ic_dialog_map).setPositiveButton(R.string.btn_okay, null).setView(view);
            dialog = builder.create();
            return dialog;
         case DIALOG_NOTRACK:
            builder = new AlertDialog.Builder(mLoggerMap.getActivity());
            builder.setTitle(R.string.dialog_notrack_title).setMessage(R.string.dialog_notrack_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(R.string.btn_selecttrack, mNoTrackDialogListener).setNegativeButton(R.string.btn_cancel, null);
            dialog = builder.create();
            return dialog;
         case DIALOG_URIS:
            builder = new AlertDialog.Builder(mLoggerMap.getActivity());
            factory = LayoutInflater.from(mLoggerMap.getActivity());
            view = factory.inflate(R.layout.mediachooser, null);
            builder.setTitle(R.string.dialog_select_media_title).setMessage(R.string.dialog_select_media_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setNegativeButton(R.string.btn_cancel, null).setPositiveButton(R.string.btn_okay, mNoteSelectDialogListener).setView(view);
            dialog = builder.create();
            return dialog;
         case DIALOG_CONTRIB:
            builder = new AlertDialog.Builder(mLoggerMap.getActivity());
            factory = LayoutInflater.from(mLoggerMap.getActivity());
            view = factory.inflate(R.layout.contrib, null);
            TextView contribView = (TextView) view.findViewById(R.id.contrib_view);
            contribView.setText(R.string.dialog_contrib_message);
            builder.setTitle(R.string.dialog_contrib_title).setView(view).setIcon(android.R.drawable.ic_dialog_email)
                  .setPositiveButton(R.string.btn_okay, null);
            dialog = builder.create();
            return dialog;
         default:
            return null;
      }
   }

   protected void onPrepareDialog(int id, Dialog dialog)
   {
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

            int osmbase = mSharedPreferences.getInt(Constants.OSMBASEOVERLAY, 0);
            cloudmade = (RadioButton) dialog.findViewById(R.id.layer_osm_cloudmade);
            mapnik = (RadioButton) dialog.findViewById(R.id.layer_osm_maknik);
            cycle = (RadioButton) dialog.findViewById(R.id.layer_osm_bicycle);
            cloudmade.setChecked(osmbase == Constants.OSM_CLOUDMADE);
            mapnik.setChecked(osmbase == Constants.OSM_MAKNIK);
            cycle.setChecked(osmbase == Constants.OSM_CYCLE);

            ((CheckBox) dialog.findViewById(R.id.layer_traffic)).setChecked(mSharedPreferences.getBoolean(Constants.TRAFFIC, false));
            ((CheckBox) dialog.findViewById(R.id.layer_speed)).setChecked(mSharedPreferences.getBoolean(Constants.SPEED, false));
            ((CheckBox) dialog.findViewById(R.id.layer_altitude)).setChecked(mSharedPreferences.getBoolean(Constants.ALTITUDE, false));
            ((CheckBox) dialog.findViewById(R.id.layer_distance)).setChecked(mSharedPreferences.getBoolean(Constants.DISTANCE, false));
            ((CheckBox) dialog.findViewById(R.id.layer_compass)).setChecked(mSharedPreferences.getBoolean(Constants.COMPASS, false));
            ((CheckBox) dialog.findViewById(R.id.layer_location)).setChecked(mSharedPreferences.getBoolean(Constants.LOCATION, false));
            int provider = new Integer(mSharedPreferences.getString(Constants.MAPPROVIDER, "" + Constants.GOOGLE)).intValue();
            switch (provider)
            {
               case Constants.GOOGLE:
                  dialog.findViewById(R.id.google_backgrounds).setVisibility(View.VISIBLE);
                  dialog.findViewById(R.id.osm_backgrounds).setVisibility(View.GONE);
                  dialog.findViewById(R.id.shared_layers).setVisibility(View.VISIBLE);
                  dialog.findViewById(R.id.google_overlays).setVisibility(View.VISIBLE);
                  break;
               case Constants.OSM:
                  dialog.findViewById(R.id.osm_backgrounds).setVisibility(View.VISIBLE);
                  dialog.findViewById(R.id.google_backgrounds).setVisibility(View.GONE);
                  dialog.findViewById(R.id.shared_layers).setVisibility(View.VISIBLE);
                  dialog.findViewById(R.id.google_overlays).setVisibility(View.GONE);
                  break;
               default:
                  dialog.findViewById(R.id.osm_backgrounds).setVisibility(View.GONE);
                  dialog.findViewById(R.id.google_backgrounds).setVisibility(View.GONE);
                  dialog.findViewById(R.id.shared_layers).setVisibility(View.VISIBLE);
                  dialog.findViewById(R.id.google_overlays).setVisibility(View.GONE);
                  break;
            }
            break;
         case DIALOG_URIS:
            Gallery gallery = (Gallery) dialog.findViewById(R.id.gallery);
            gallery.setAdapter(mMediaAdapter);
            gallery.setOnItemSelectedListener(mGalerySelectListener);
         default:
            break;
      }
   }

   protected void onActivityResult(int requestCode, int resultCode, Intent intent)
   {
      Uri trackUri;
      long trackId;
      switch (requestCode)
      {
         case MENU_TRACKLIST:
            if (resultCode == Activity.RESULT_OK)
            {
               trackUri = intent.getData();
               trackId = Long.parseLong(trackUri.getLastPathSegment());
               mAverageSpeed = 0.0;
               moveToTrack(trackId, true);
            }
            break;
         case MENU_TRACKING:
            if (resultCode == Activity.RESULT_OK)
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
         case MENU_SHARE:
            ShareTrack.clearScreenBitmap();
            break;
         default:
            Log.e(TAG, "Returned form unknow activity: " + requestCode);
            break;
      }
   }

   private void updateTitleBar()
   {
      ContentResolver resolver = mLoggerMap.getActivity().getContentResolver();
      Cursor trackCursor = null;
      try
      {
         trackCursor = resolver.query(ContentUris.withAppendedId(Tracks.CONTENT_URI, this.mTrackId), new String[] { Tracks.NAME }, null, null, null);
         if (trackCursor != null && trackCursor.moveToLast())
         {
            String trackName = trackCursor.getString(0);
            mLoggerMap.getActivity().setTitle(mLoggerMap.getActivity().getString(R.string.app_name) + ": " + trackName);
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

   private void updateMapProvider()
   {
      Class< ? > mapClass = null;
      int provider = new Integer(mSharedPreferences.getString(Constants.MAPPROVIDER, "" + Constants.GOOGLE)).intValue();
      switch (provider)
      {
         case Constants.GOOGLE:
            mapClass = GoogleLoggerMap.class;
            break;
         case Constants.OSM:
            mapClass = OsmLoggerMap.class;
            break;
         case Constants.MAPQUEST:
            mapClass = MapQuestLoggerMap.class;
            break;
         default:
            mapClass = GoogleLoggerMap.class;
            Log.e(TAG, "Fault in value " + provider + " as MapProvider, defaulting to Google Maps.");
            break;
      }
      if (mapClass != mLoggerMap.getActivity().getClass())
      {
         Intent myIntent = mLoggerMap.getActivity().getIntent();
         Intent realIntent;
         if (myIntent != null)
         {
            realIntent = new Intent(myIntent.getAction(), myIntent.getData(), mLoggerMap.getActivity(), mapClass);
            realIntent.putExtras(myIntent);
         }
         else
         {
            realIntent = new Intent(mLoggerMap.getActivity(), mapClass);
            realIntent.putExtras(myIntent);
         }
         mLoggerMap.getActivity().startActivity(realIntent);
         mLoggerMap.getActivity().finish();
      }
   }

   protected void updateMapProviderAdministration(String provider)
   {
      mLoggerServiceManager.storeDerivedDataSource(provider);
   }

   private void updateBlankingBehavior()
   {
      boolean disableblanking = mSharedPreferences.getBoolean(Constants.DISABLEBLANKING, false);
      boolean disabledimming = mSharedPreferences.getBoolean(Constants.DISABLEDIMMING, false);
      if (disableblanking)
      {
         if (mWakeLock == null)
         {
            PowerManager pm = (PowerManager) mLoggerMap.getActivity().getSystemService(Context.POWER_SERVICE);
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
      int trackColoringMethod = new Integer(mSharedPreferences.getString(Constants.TRACKCOLORING, "3")).intValue();
      View speedbar = mLoggerMap.getActivity().findViewById(R.id.speedbar);

      TextView[] speedtexts = mLoggerMap.getSpeedTextViews();
      ;
      if (trackColoringMethod == SegmentRendering.DRAW_MEASURED || trackColoringMethod == SegmentRendering.DRAW_CALCULATED)
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
            speedtexts = mLoggerMap.getSpeedTextViews();
            speedbar.setVisibility(View.VISIBLE);
            for (int i = 0; i < speedtexts.length; i++)
            {
               speedtexts[i].setVisibility(View.VISIBLE);
            }
         }
      }
      else
      {
         speedbar.setVisibility(View.INVISIBLE);
         for (int i = 0; i < speedtexts.length; i++)
         {
            speedtexts[i].setVisibility(View.INVISIBLE);
         }
      }
      mBitmapSegmentsOverlay.setTrackColoringMethod(trackColoringMethod, mAverageSpeed);
   }

   private void updateSpeedDisplayVisibility()
   {
      boolean showspeed = mSharedPreferences.getBoolean(Constants.SPEED, false);
      TextView lastGPSSpeedView = mLoggerMap.getSpeedTextView();
      if (showspeed)
      {
         lastGPSSpeedView.setVisibility(View.VISIBLE);
      }
      else
      {
         lastGPSSpeedView.setVisibility(View.GONE);
      }
   }

   private void updateAltitudeDisplayVisibility()
   {
      boolean showaltitude = mSharedPreferences.getBoolean(Constants.ALTITUDE, false);
      TextView lastGPSAltitudeView = mLoggerMap.getAltitideTextView();
      if (showaltitude)
      {
         lastGPSAltitudeView.setVisibility(View.VISIBLE);
      }
      else
      {
         lastGPSAltitudeView.setVisibility(View.GONE);
      }
   }

   private void updateDistanceDisplayVisibility()
   {
      boolean showdistance = mSharedPreferences.getBoolean(Constants.DISTANCE, false);
      TextView distanceView = mLoggerMap.getDistanceTextView();
      if (showdistance)
      {
         distanceView.setVisibility(View.VISIBLE);
      }
      else
      {
         distanceView.setVisibility(View.GONE);
      }
   }

   private void updateCompassDisplayVisibility()
   {
      boolean compass = mSharedPreferences.getBoolean(Constants.COMPASS, false);
      if (compass)
      {
         mLoggerMap.enableCompass();
      }
      else
      {
         mLoggerMap.disableCompass();
      }
   }

   private void updateLocationDisplayVisibility()
   {
      boolean location = mSharedPreferences.getBoolean(Constants.LOCATION, false);
      if (location)
      {
         mLoggerMap.enableMyLocation();
      }
      else
      {
         mLoggerMap.disableMyLocation();
      }
   }

   /**
    * Retrieves the numbers of the measured speed and altitude from the most
    * recent waypoint and updates UI components with this latest bit of
    * information.
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
         TextView lastGPSSpeedView = mLoggerMap.getSpeedTextView();
         lastGPSSpeedView.setText(speedText);

         // Speed color bar and refrence numbers
         if (speed > 2 * mAverageSpeed)
         {
            mAverageSpeed = 0.0;
            updateSpeedColoring();
            mLoggerMap.postInvalidate();
         }

         //Altitude number
         double altitude = lastWaypoint.getAltitude();
         altitude = units.conversionFromMeterToHeight(altitude);
         String altitudeText = String.format("%.0f %s", altitude, units.getHeightUnit());
         TextView mLastGPSAltitudeView = mLoggerMap.getAltitideTextView();
         mLastGPSAltitudeView.setText(altitudeText);

         //Distance number
         double distance = units.conversionFromMeter(mLoggerServiceManager.getTrackedDistance());
         String distanceText = String.format("%.2f %s", distance, units.getDistanceUnit());
         TextView mDistanceView = mLoggerMap.getDistanceTextView();
         mDistanceView.setText(distanceText);
      }
   }

   /**
    * For the current track identifier the route of that track is drawn by
    * adding a OverLay for each segments in the track
    * 
    * @param trackId
    * @see SegmentRendering
    */
   private void createDataOverlays()
   {
      mLastSegmentOverlay = null;
      mBitmapSegmentsOverlay.clearSegments();
      mLoggerMap.clearOverlays();
      mLoggerMap.addOverlay(mBitmapSegmentsOverlay);

      ContentResolver resolver = mLoggerMap.getActivity().getContentResolver();
      Cursor segments = null;
      int trackColoringMethod = new Integer(mSharedPreferences.getString(Constants.TRACKCOLORING, "2")).intValue();

      try
      {
         Uri segmentsUri = Uri.withAppendedPath(Tracks.CONTENT_URI, this.mTrackId + "/segments");
         segments = resolver.query(segmentsUri, new String[] { Segments._ID }, null, null, null);
         if (segments != null && segments.moveToFirst())
         {
            do
            {
               long segmentsId = segments.getLong(0);
               Uri segmentUri = ContentUris.withAppendedId(segmentsUri, segmentsId);
               SegmentRendering segmentOverlay = new SegmentRendering(mLoggerMap, segmentUri, trackColoringMethod, mAverageSpeed, mHandler);
               mBitmapSegmentsOverlay.addSegment(segmentOverlay);
               mLastSegmentOverlay = segmentOverlay;
               if (segments.isFirst())
               {
                  segmentOverlay.addPlacement(SegmentRendering.FIRST_SEGMENT);
               }
               if (segments.isLast())
               {
                  segmentOverlay.addPlacement(SegmentRendering.LAST_SEGMENT);
               }
               mLastSegment = segmentsId;
            }
            while (segments.moveToNext());
         }
      }
      finally
      {
         if (segments != null)
         {
            segments.close();
         }
      }

      Uri lastSegmentUri = Uri.withAppendedPath(Tracks.CONTENT_URI, mTrackId + "/segments/" + mLastSegment + "/waypoints");
      resolver.unregisterContentObserver(this.mSegmentWaypointsObserver);
      resolver.registerContentObserver(lastSegmentUri, false, this.mSegmentWaypointsObserver);
   }

   private void updateDataOverlays()
   {
      ContentResolver resolver = mLoggerMap.getActivity().getContentResolver();
      Uri segmentsUri = Uri.withAppendedPath(Tracks.CONTENT_URI, this.mTrackId + "/segments");
      Cursor segmentsCursor = null;
      int segmentOverlaysCount = mBitmapSegmentsOverlay.size();
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

   /**
    * Call when an overlay has recalulated and has new information to be redrawn
    */

   private void moveActiveViewWindow()
   {
      GeoPoint lastPoint = getLastTrackPoint();
      if (lastPoint != null && mLoggerServiceManager.getLoggingState() == Constants.LOGGING)
      {
         if (mLoggerMap.isOutsideScreen(lastPoint))
         {
            mLoggerMap.clearAnimation();
            mLoggerMap.setCenter(lastPoint);
         }
         else if (mLoggerMap.isNearScreenEdge(lastPoint))
         {
            mLoggerMap.clearAnimation();
            mLoggerMap.animateTo(lastPoint);
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
         TextView[] mSpeedtexts = mLoggerMap.getSpeedTextViews();
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
      if( trackId == mTrackId )
      {
         return;
      }
      Cursor track = null;
      try
      {
         ContentResolver resolver = mLoggerMap.getActivity().getContentResolver();
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

            mLoggerMap.clearOverlays();
            mBitmapSegmentsOverlay.clearSegments();

            updateTitleBar();
            updateDataOverlays();
            updateSpeedColoring();
            if (center)
            {
               GeoPoint lastPoint = getLastTrackPoint();
               mLoggerMap.animateTo(lastPoint);
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
    * Get the last know position from the GPS provider and return that
    * information wrapped in a GeoPoint to which the Map can navigate.
    * 
    * @see GeoPoint
    * @return
    */
   private GeoPoint getLastKnowGeopointLocation()
   {
      int microLatitude = 0;
      int microLongitude = 0;
      LocationManager locationManager = (LocationManager) mLoggerMap.getActivity().getApplication().getSystemService(Context.LOCATION_SERVICE);
      Location locationFine = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
      if (locationFine != null)
      {
         microLatitude = (int) (locationFine.getLatitude() * 1E6d);
         microLongitude = (int) (locationFine.getLongitude() * 1E6d);
      }
      if (locationFine == null || microLatitude == 0 || microLongitude == 0)
      {
         Location locationCoarse = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
         if (locationCoarse != null)
         {
            microLatitude = (int) (locationCoarse.getLatitude() * 1E6d);
            microLongitude = (int) (locationCoarse.getLongitude() * 1E6d);
         }
         if (locationCoarse == null || microLatitude == 0 || microLongitude == 0)
         {
            microLatitude = 51985105;
            microLongitude = 5106132;
         }
      }
      GeoPoint geoPoint = new GeoPoint(microLatitude, microLongitude);
      return geoPoint;
   }

   /**
    * Retrieve the last point of the current track
    * 
    * @param context
    */
   private GeoPoint getLastTrackPoint()
   {
      Cursor waypoint = null;
      GeoPoint lastPoint = null;
      // First try the service which might have a cached version
      Location lastLoc = mLoggerServiceManager.getLastWaypoint();
      if (lastLoc != null)
      {
         int microLatitude = (int) (lastLoc.getLatitude() * 1E6d);
         int microLongitude = (int) (lastLoc.getLongitude() * 1E6d);
         lastPoint = new GeoPoint(microLatitude, microLongitude);
      }

      // If nothing yet, try the content resolver and query the track
      if (lastPoint == null || lastPoint.getLatitudeE6() == 0 || lastPoint.getLongitudeE6() == 0)
      {
         try
         {
            ContentResolver resolver = mLoggerMap.getActivity().getContentResolver();
            waypoint = resolver.query(Uri.withAppendedPath(Tracks.CONTENT_URI, mTrackId + "/waypoints"), new String[] { Waypoints.LATITUDE,
                  Waypoints.LONGITUDE, "max(" + Waypoints.TABLE + "." + Waypoints._ID + ")" }, null, null, null);
            if (waypoint != null && waypoint.moveToLast())
            {
               int microLatitude = (int) (waypoint.getDouble(0) * 1E6d);
               int microLongitude = (int) (waypoint.getDouble(1) * 1E6d);
               lastPoint = new GeoPoint(microLatitude, microLongitude);
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
      if (lastPoint == null || lastPoint.getLatitudeE6() == 0 || lastPoint.getLongitudeE6() == 0)
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
         ContentResolver resolver = mLoggerMap.getActivity().getContentResolver();
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
    * Enables a SegmentOverlay to call back to the MapActivity to show a dialog
    * with choices of media
    * 
    * @param mediaAdapter
    */
   public void showMediaDialog(BaseAdapter mediaAdapter)
   {
      mMediaAdapter = mediaAdapter;
      mLoggerMap.getActivity().showDialog(DIALOG_URIS);
   }

   public SharedPreferences getPreferences()
   {
      return mSharedPreferences;
   }

   public boolean isLogging()
   {
      return mLoggerServiceManager.getLoggingState() == Constants.LOGGING;
   }

}
