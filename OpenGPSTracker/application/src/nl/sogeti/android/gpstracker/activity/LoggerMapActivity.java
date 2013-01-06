/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Nov 4, 2012 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 */
package nl.sogeti.android.gpstracker.activity;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.InsertNote;
import nl.sogeti.android.gpstracker.actions.ShareTrack;
import nl.sogeti.android.gpstracker.actions.Statistics;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.logger.GPSLoggerServiceManager;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.viewer.ApplicationPreferenceActivity;
import nl.sogeti.android.gpstracker.viewer.TrackList;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Gallery;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * ????
 * 
 * @version $Id:$
 * @author rene (c) Nov 4, 2012, Sogeti B.V.
 */
public class LoggerMapActivity extends Activity
{
   private static final String TAG = "LoggerMapActivity";

   private static final int MENU_LOGGERMAP_TRACKLIST = 0;
   private static final int MENU_LOGGERMAP_ABOUT = 2;
   private static final int MENU_LOGGERMAP_TRACKING = 4;
   private static final int MENU_LOGGERMAP_SHARE = 6;
   private static final int MENU_LOGGERMAP_NOTE = 8;
   private static final int DIALOG_NOTRACK = 24;
   private static final int DIALOG_INSTALL_ABOUT = 29;
   private static final int DIALOG_LAYERS = 31;
   private static final int DIALOG_URIS = 34;
   private static final int DIALOG_CONTRIB = 35;
   private static final int DIALOG_PLAYERROR = 36;

   private long mTrackId = -1;
   private GPSLoggerServiceManager mLoggerServiceManager;
   private WakeLock mWakeLock;
   private SharedPreferences mSharedPreferences;

   private BaseAdapter mMediaAdapter;

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_map);

      mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
      mLoggerServiceManager = new GPSLoggerServiceManager(this);
   }

   @Override
   protected void onResume()
   {
      super.onResume();
      if (ConnectionResult.SUCCESS != GooglePlayServicesUtil.isGooglePlayServicesAvailable(this))
      {
         showDialog(DIALOG_PLAYERROR);
      }

      if (mWakeLock == null)
      {
         boolean disabledimming = mSharedPreferences.getBoolean(Constants.DISABLEDIMMING, false);
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
      mLoggerServiceManager.startup(this, new ServiceConnected());
   }

   @Override
   protected void onPause()
   {
      if (mWakeLock != null && mWakeLock.isHeld())
      {
         mWakeLock.release();
         mWakeLock = null;

         Log.i(TAG, "onPause(): Released lock to keep screen on!");
      }
      this.mLoggerServiceManager.shutdown(this);

      super.onPause();
   }

   @Override
   protected void onDestroy()
   {
      if (mLoggerServiceManager.getLoggingState() == Constants.STOPPED)
      {
         stopService(new Intent(Constants.SERVICENAME));
      }
      super.onDestroy();
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
                  moveToTrack(trackId, true);
               }
            }
            break;
         case MENU_LOGGERMAP_SHARE:
            ShareTrack.clearScreenBitmap();
            break;
         case DIALOG_PLAYERROR:
            Log.i(TAG, "Play services error dialog finished");
            break;
         default:
            Log.e(TAG, "Returned form unknow activity: " + requestCode);
            break;
      }
   }

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
            intent = new Intent(this, ControlTrackingActivity.class);
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
            if (mTrackId >= 0)
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
         case R.id.menu_loggermap_about:
            intent = new Intent("org.openintents.action.SHOW_ABOUT_DIALOG");
            try
            {
               startActivityForResult(intent, MENU_LOGGERMAP_ABOUT);
            }
            catch (ActivityNotFoundException e)
            {
               showDialog(DIALOG_INSTALL_ABOUT);
            }
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
   protected Dialog onCreateDialog(int id, Bundle bundle)
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

            CheckBox traffic = (CheckBox) view.findViewById(R.id.layer_traffic);
            CheckBox speed = (CheckBox) view.findViewById(R.id.layer_speed);
            CheckBox altitude = (CheckBox) view.findViewById(R.id.layer_altitude);
            CheckBox distance = (CheckBox) view.findViewById(R.id.layer_distance);
            CheckBox location = (CheckBox) view.findViewById(R.id.layer_location);

            ((RadioGroup) view.findViewById(R.id.google_backgrounds)).setOnCheckedChangeListener(new BaseLayerChangeListener());
            OnCheckedChangeListener checkedChangeListener = new LayerCheckedChangeListener();
            traffic.setOnCheckedChangeListener(checkedChangeListener);
            speed.setOnCheckedChangeListener(checkedChangeListener);
            altitude.setOnCheckedChangeListener(checkedChangeListener);
            distance.setOnCheckedChangeListener(checkedChangeListener);
            location.setOnCheckedChangeListener(checkedChangeListener);

            builder.setTitle(R.string.dialog_layer_title).setIcon(android.R.drawable.ic_dialog_map).setPositiveButton(R.string.btn_okay, null).setView(view);
            dialog = builder.create();
            return dialog;
         case DIALOG_NOTRACK:
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_notrack_title).setMessage(R.string.dialog_notrack_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(R.string.btn_selecttrack, new NoTrackDialogListener()).setNegativeButton(R.string.btn_cancel, null);
            dialog = builder.create();
            return dialog;
         case DIALOG_URIS:
            builder = new AlertDialog.Builder(this);
            factory = LayoutInflater.from(this);

            view = factory.inflate(R.layout.mediachooser, null);
            Gallery gallery = (Gallery) view.findViewById(R.id.gallery);
            builder.setTitle(R.string.dialog_select_media_title).setMessage(R.string.dialog_select_media_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setNegativeButton(R.string.btn_cancel, null).setPositiveButton(R.string.btn_okay, new NoteSelectDialogListener(gallery)).setView(view);
            dialog = builder.create();
            return dialog;
         case DIALOG_CONTRIB:
            builder = new AlertDialog.Builder(this);
            factory = LayoutInflater.from(this);
            view = factory.inflate(R.layout.contrib, null);
            TextView contribView = (TextView) view.findViewById(R.id.contrib_view);
            contribView.setText(R.string.dialog_contrib_message);
            builder.setTitle(R.string.dialog_contrib_title).setView(view).setIcon(android.R.drawable.ic_dialog_email).setPositiveButton(R.string.btn_okay, null);
            dialog = builder.create();
            return dialog;
         case DIALOG_PLAYERROR:
            int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
            dialog = GooglePlayServicesUtil.getErrorDialog(result, this, DIALOG_PLAYERROR);
            return dialog;
         default:
            return super.onCreateDialog(id, bundle);
      }
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
    */
   @Override
   protected void onPrepareDialog(int id, Dialog dialog, Bundle bundle)
   {
      RadioButton satellite;
      RadioButton regular;
      switch (id)
      {
         case DIALOG_LAYERS:
            satellite = (RadioButton) dialog.findViewById(R.id.layer_google_satellite);
            satellite.setChecked(mSharedPreferences.getBoolean(Constants.SATELLITE, false));
            regular = (RadioButton) dialog.findViewById(R.id.layer_google_regular);
            regular.setChecked(!mSharedPreferences.getBoolean(Constants.SATELLITE, false));

            CheckBox traffic = (CheckBox) dialog.findViewById(R.id.layer_traffic);
            traffic.setChecked(mSharedPreferences.getBoolean(Constants.TRAFFIC, false));
            CheckBox speed = (CheckBox) dialog.findViewById(R.id.layer_speed);
            ;
            speed.setChecked(mSharedPreferences.getBoolean(Constants.SPEED, false));
            CheckBox altitude = (CheckBox) dialog.findViewById(R.id.layer_altitude);
            altitude.setChecked(mSharedPreferences.getBoolean(Constants.ALTITUDE, false));
            CheckBox distance = (CheckBox) dialog.findViewById(R.id.layer_distance);
            distance.setChecked(mSharedPreferences.getBoolean(Constants.DISTANCE, false));
            CheckBox location = (CheckBox) dialog.findViewById(R.id.layer_location);
            location.setChecked(mSharedPreferences.getBoolean(Constants.LOCATION, false));
            break;
         case DIALOG_URIS:
            Gallery gallery = (Gallery) dialog.findViewById(R.id.gallery);
            gallery.setAdapter(mMediaAdapter);
            break;
         default:
            break;
      }
      super.onPrepareDialog(id, dialog, bundle);
   }

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
            mTrackId = trackId;
            Uri tracksegmentsUri = Uri.withAppendedPath(Tracks.CONTENT_URI, trackId + "/segments");

            updateTitleBar();
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

   private void updateBlankingBehavior()
   {
      boolean disableblanking = mSharedPreferences.getBoolean(Constants.DISABLEBLANKING, false);
      if (disableblanking && mWakeLock != null)
      {
         if (mLoggerServiceManager.getLoggingState() == Constants.LOGGING && !mWakeLock.isHeld())
         {
            mWakeLock.acquire();
            Log.w(TAG, "Acquired lock to keep screen on!");
         }
      }
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

   class ServiceConnected implements Runnable
   {
      @Override
      public void run()
      {
         updateBlankingBehavior();
      }
   };

   class BaseLayerChangeListener implements android.widget.RadioGroup.OnCheckedChangeListener
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

   class LayerCheckedChangeListener implements OnCheckedChangeListener
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

   class NoTrackDialogListener implements DialogInterface.OnClickListener
   {
      @Override
      public void onClick(DialogInterface dialog, int which)
      {
         Intent tracklistIntent = new Intent(LoggerMapActivity.this, TrackList.class);
         tracklistIntent.putExtra(Tracks._ID, LoggerMapActivity.this.mTrackId);
         startActivityForResult(tracklistIntent, MENU_LOGGERMAP_TRACKLIST);
      }
   };

   class NoteSelectDialogListener implements DialogInterface.OnClickListener
   {
      private AdapterView<SpinnerAdapter> mGallery;

      public NoteSelectDialogListener(Gallery gallery)
      {
         mGallery = gallery;
      }

      @Override
      public void onClick(DialogInterface dialog, int which)
      {
         Uri selected = (Uri) mGallery.getSelectedItem();
         // TODO not implemented
         //SegmentOverlay.handleMedia(LoggerMap.this, selected);
         Log.e(TAG, "Not implemented");
      }
   };
}
