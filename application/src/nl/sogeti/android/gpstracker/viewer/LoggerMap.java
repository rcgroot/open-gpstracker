/*------------------------------------------------------------------------------
 **     Ident: Innovation en Inspiration > Google Android 
 **    Author: rene
 ** Copyright: (c) Jan 22, 2009 Sogeti Nederland B.V. All Rights Reserved.
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

import java.util.List;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.logger.GPSLoggerService;
import nl.sogeti.android.gpstracker.logger.GPSLoggerServiceManager;
import nl.sogeti.android.gpstracker.logger.SettingsDialog;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

/**
 * @version $Id$
 * @author rene (c) Jan 18, 2009, Sogeti B.V.
 */
public class LoggerMap extends MapActivity
{
   private static final int ZOOM_LEVEL = 14;
   public static final String EXTRA_TRACK_ID = "nl.sogeti.android.gpstracker.intent.trackid";

   // MENU'S
   private static final int MENU_SETTINGS = 0;
   private static final int MENU_TRACKING = 1;
   private static final int MENU_TRACKLIST = 5;
   private static final int MENU_VIEW = 7;
   private static final String TAG = LoggerMap.class.getName();
   protected static final String DISABLEBLANKING = "disableblanking";
   protected static final String SHOWSPEED = "showspeed";

   private static final int DIALOG_TRACKNAME = 23;
   private static final int DIALOG_NOTRACK = 24;
   private static final int DIALOG_LOGCONTROL = 26;

   private MapView mMapView = null;
   private MapController mMapController = null;
   private GPSLoggerServiceManager mLoggerServiceManager;
   private EditText mTrackNameView;
   private WakeLock mWakeLock = null;
   private double mAverageSpeed = 33.33d / 2d;
   private TextView[] mSpeedtexts = null;
   private TextView mLastGPSSpeedText = null;
   
   private long mTrackId = -1;
   private long mLastSegment = -1 ;
   private long mLastWaypoint = -1;

   private OnSharedPreferenceChangeListener mSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener()
      {
         public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
         {
            if( key.equals( TrackingOverlay.TRACKCOLORING ) )
            {
               int trackColoringMethod = new Integer( sharedPreferences.getString( TrackingOverlay.TRACKCOLORING, "3" ) ).intValue();
               updateSpeedbarVisibility();
               List<Overlay> overlays = LoggerMap.this.mMapView.getOverlays();
               for (Overlay overlay : overlays)
               {
                  if( overlay instanceof TrackingOverlay )
                  {
                     ( (TrackingOverlay) overlay ).setTrackColoringMethod( trackColoringMethod, mAverageSpeed );
                  }
               }
            }
            else if( key.equals( LoggerMap.DISABLEBLANKING ) )
            {
               updateBlankingBehavior();
            }
            else if( key.equals( SHOWSPEED ) )
            {
               updateSpeedDisplayVisibility();
            }
         }
      };

   private final ContentObserver mTrackObserver = new ContentObserver( new Handler() )
      {
         @Override
         public void onChange( boolean selfUpdate )
         {
            if( !selfUpdate )
            {
               Log.d( TAG, "Have drawn to segment "+mLastSegment+" with waypoint "+mLastWaypoint );
               LoggerMap.this.createTrackingDataOverlays();
               LoggerMap.this.createSpeedDisplayNumbers();
            }
            else
            {
               Log.d( TAG, "Skipping caused by self" );
            }
         }
      };

   private final DialogInterface.OnClickListener mNoTrackDialogListener = new DialogInterface.OnClickListener()
      {
         public void onClick( DialogInterface dialog, int which )
         {
            Intent tracklistIntent = new Intent( LoggerMap.this, TrackList.class );
            tracklistIntent.putExtra( Tracks._ID, LoggerMap.this.mTrackId );
            startActivityForResult( tracklistIntent, MENU_TRACKLIST );
         }
      };

   DialogInterface.OnClickListener mTrackNameDialogListener = new DialogInterface.OnClickListener()
      {
         public void onClick( DialogInterface dialog, int which )
         {
            String trackName = mTrackNameView.getText().toString();
            ContentValues values = new ContentValues();
            values.put( Tracks.NAME, trackName );
            getContentResolver().update( ContentUris.withAppendedId( Tracks.CONTENT_URI, LoggerMap.this.mTrackId ), values, null, null );
            updateTitleBar();
         }
      };

   @Override
   public boolean onKeyDown( int keyCode, KeyEvent event )
   {
      Toast toast;
      boolean propagate = true;
      switch (keyCode)
      {
         case KeyEvent.KEYCODE_T:
            propagate = this.mMapView.getController().zoomIn();
            break;
         case KeyEvent.KEYCODE_G:
            propagate = this.mMapView.getController().zoomOut();
            break;
         case KeyEvent.KEYCODE_S:
            this.mMapView.setSatellite( !this.mMapView.isSatellite() );
            toast = Toast.makeText( this.getApplicationContext(), "Satellite: " + this.mMapView.isSatellite(), Toast.LENGTH_SHORT );
            toast.show();
            propagate = false;
            break;
         case KeyEvent.KEYCODE_A:
            this.mMapView.setTraffic( !this.mMapView.isTraffic() );
            toast = Toast.makeText( this.getApplicationContext(), "Traffic: " + this.mMapView.isTraffic(), Toast.LENGTH_SHORT );
            toast.show();
            propagate = false;
            break;
         case KeyEvent.KEYCODE_F:
            moveToTrack( this.mTrackId - 1 );
            propagate = false;
            break;
         case KeyEvent.KEYCODE_H:
            moveToTrack( this.mTrackId + 1 );
            propagate = false;
            break;
         default:
            propagate = super.onKeyDown( keyCode, event );
            break;
      }
      return propagate;
   }

   @Override
   public boolean onCreateOptionsMenu( Menu menu )
   {
      boolean result = super.onCreateOptionsMenu( menu );

      menu.add( 0, MENU_TRACKING, 0, R.string.menu_tracking ).setIcon( android.R.drawable.ic_menu_mapmode ).setAlphabeticShortcut( 't' );
      menu.add( 0, MENU_TRACKLIST, 0, R.string.menu_tracklist ).setIcon( android.R.drawable.ic_menu_gallery ).setAlphabeticShortcut( 'l' );
      menu.add( 0, MENU_VIEW, 0, R.string.menu_showTrack ).setIcon( android.R.drawable.ic_menu_view ).setAlphabeticShortcut( 'e' );
      menu.add( 0, MENU_SETTINGS, 0, R.string.menu_settings ).setIcon( android.R.drawable.ic_menu_preferences ).setAlphabeticShortcut( 's' );
      return result;
   }

   @Override
   public boolean onOptionsItemSelected( MenuItem item )
   {
      boolean handled = false;
      switch (item.getItemId())
      {
         case MENU_TRACKING:
            showDialog( DIALOG_LOGCONTROL );
//            if( this.mLoggerServiceManager.isLogging() )
//            {
//               this.mLoggerServiceManager.stopGPSLogging();
//               updateBlankingBehavior();
//               item.setTitle( R.string.menu_toggle_on );
//            }
//            else
//            {
//               long loggerTrackId = this.mLoggerServiceManager.startGPSLogging( null );
//               moveToTrack( loggerTrackId );
//               updateBlankingBehavior();
//               item.setTitle( R.string.menu_toggle_off );
//               showDialog( DIALOG_TRACKNAME );
//            }
            updateBlankingBehavior();
            handled = true;
            break;
         case MENU_SETTINGS:
            Intent i = new Intent( this, SettingsDialog.class );
            startActivity( i );
            handled = true;
            break;
         case MENU_TRACKLIST:
            Intent tracklistIntent = new Intent( this, TrackList.class );
            tracklistIntent.putExtra( Tracks._ID, this.mTrackId );
            startActivityForResult( tracklistIntent, MENU_TRACKLIST );
            break;
         case MENU_VIEW:
            if( this.mTrackId >= 0 )
            {
               Uri uri = ContentUris.withAppendedId( Tracks.CONTENT_URI, this.mTrackId );
               Intent actionIntent = new Intent( Intent.ACTION_VIEW, uri );
               startActivity( actionIntent );
               handled = true;
               break;
            }
            else
            {
               showDialog( DIALOG_NOTRACK );
            }
            handled = true;
            break;
         default:
            handled = super.onOptionsItemSelected( item );
      }
      return handled;
   }

   /**
    * Called when the activity is first created.
    */
   @Override
   protected void onCreate( Bundle load )
   {
      super.onCreate( load );
      this.startService( new Intent( GPSLoggerService.SERVICENAME ) );

      Object previousInstanceData = getLastNonConfigurationInstance();
      if( previousInstanceData != null && previousInstanceData instanceof GPSLoggerServiceManager )
      {
         this.mLoggerServiceManager = (GPSLoggerServiceManager) previousInstanceData;
      }
      else
      {
         this.mLoggerServiceManager = new GPSLoggerServiceManager( (Context) this );
      }
      this.mLoggerServiceManager.startup();

      PreferenceManager.getDefaultSharedPreferences( this ).registerOnSharedPreferenceChangeListener( mSharedPreferenceChangeListener );

      setContentView( R.layout.map );
      this.mMapView = (MapView) findViewById( R.id.myMapView );
      this.mMapView.setClickable( true );
      this.mMapView.setStreetView( false );
      this.mMapView.setSatellite( false );
      TextView[] speeds = { (TextView) findViewById( R.id.speedview05 ), (TextView) findViewById( R.id.speedview04 ), (TextView) findViewById( R.id.speedview03 ),
            (TextView) findViewById( R.id.speedview02 ), (TextView) findViewById( R.id.speedview01 ), (TextView) findViewById( R.id.speedview00 ) };
      mSpeedtexts = speeds;
      mLastGPSSpeedText = (TextView) findViewById( R.id.currentSpeed );

      /* Collect the zoomcontrols and place them */
      this.mMapView.setBuiltInZoomControls( true );
      this.mMapController = this.mMapView.getController();

      onRestoreInstanceState( load );
   }

   protected void onPause()
   {
      super.onPause();
      if( this.mWakeLock != null && this.mWakeLock.isHeld() )
      {
         this.mWakeLock.release();
         Log.w( TAG, "onPause(): Released lock to keep screen on!" );
      }
   }

   protected void onResume()
   {
      super.onResume();
      updateTitleBar();
      updateBlankingBehavior();
      updateSpeedbarVisibility();
      updateSpeedDisplayVisibility();
   }

   /*
    * (non-Javadoc)
    * @see com.google.android.maps.MapActivity#onPause()
    */
   @Override
   protected void onDestroy()
   {
      if( this.mWakeLock != null && this.mWakeLock.isHeld() )
      {
         this.mWakeLock.release();
         Log.w( TAG, "onDestroy(): Released lock to keep screen on!" );
      }
      this.mLoggerServiceManager.shutdown();
      PreferenceManager.getDefaultSharedPreferences( this ).unregisterOnSharedPreferenceChangeListener( this.mSharedPreferenceChangeListener );
      super.onDestroy();
   }

   /*
    * (non-Javadoc)
    * @see com.google.android.maps.MapActivity#onNewIntent(android.content.Intent)
    */
   @Override
   public void onNewIntent( Intent newIntent )
   {
      if( newIntent.getExtras() != null )
      {
         long intentTrackId = newIntent.getExtras().getLong( EXTRA_TRACK_ID, -1 );
         if( intentTrackId >= 0 )
         {
            moveToTrack( intentTrackId );
         }
      }
   }

   @Override
   public void onRestoreInstanceState( Bundle load )
   {
      long intentTrackId = this.getIntent().getLongExtra( EXTRA_TRACK_ID, -1 );
      if( intentTrackId >= 0 )
      {
         moveToTrack( intentTrackId );
      }
      else if( load != null && load.containsKey( "track" ) )
      {
         long loadTrackId = load.getLong( "track" );
         moveToTrack( loadTrackId );
      }
      else 
      {
         moveToLastTrack();
      }

      if( load != null && load.containsKey( "zoom" ) )
      {
         this.mMapController.setZoom( load.getInt( "zoom" ) );
      }
      else
      {
         this.mMapController.setZoom( LoggerMap.ZOOM_LEVEL );
      }

      if( load != null && load.containsKey( "e6lat" ) && load.containsKey( "e6long" ) )
      {
         GeoPoint storedPoint = new GeoPoint( load.getInt( "e6lat" ), load.getInt( "e6long" ) );
         this.mMapView.getController().animateTo( storedPoint );
      }
      else
      {
         GeoPoint lastPoint = getLastTrackPoint();
         if( lastPoint.getLatitudeE6() != 0 && lastPoint.getLongitudeE6() != 0 )
         {
            this.mMapView.getController().animateTo( lastPoint );
         }
         else
         {
            lastPoint = getLastKnowGeopointLocation();
            if( lastPoint.getLatitudeE6() != 0 && lastPoint.getLongitudeE6() != 0 )
            {
               this.mMapView.getController().animateTo( lastPoint );
            }
            else
            {
               GeoPoint startPoint = new GeoPoint( 51985105, 5106132 );
               this.mMapView.getController().animateTo( startPoint );
            }
         }
         

      }
   }

   @Override
   public void onSaveInstanceState( Bundle save )
   {
      save.putLong( "track", this.mTrackId );
      save.putInt( "zoom", this.mMapView.getZoomLevel() );
      GeoPoint point = this.mMapView.getMapCenter();
      save.putInt( "e6lat", point.getLatitudeE6() );
      save.putInt( "e6long", point.getLongitudeE6() );
      super.onSaveInstanceState( save );
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onRetainNonConfigurationInstance()
    */
   @Override
   public Object onRetainNonConfigurationInstance()
   {
      Object nonConfigurationInstance = this.mLoggerServiceManager;
      return nonConfigurationInstance;
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onCreateDialog(int)
    */
   @Override
   protected Dialog onCreateDialog( int id )
   {
      Dialog dialog = null;
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_TRACKNAME:
            builder = new AlertDialog.Builder( this );
            factory = LayoutInflater.from( this );
            view = factory.inflate( R.layout.namedialog, null );
            mTrackNameView = (EditText) view.findViewById( R.id.nameField );
            builder.setTitle( R.string.dialog_routename_title )
            .setMessage( R.string.dialog_routename_message )
            .setIcon( android.R.drawable.ic_dialog_alert )
            .setPositiveButton( R.string.btn_okay, mTrackNameDialogListener )
            .setView( view );
            dialog = builder.create();
            return dialog;
         case DIALOG_NOTRACK:
            builder = new AlertDialog.Builder( this );
            builder.setTitle( R.string.dialog_notrack_title )
            .setMessage( R.string.dialog_notrack_message )
            .setIcon( android.R.drawable.ic_dialog_alert )
            .setPositiveButton( R.string.btn_selecttrack, mNoTrackDialogListener )
            .setNegativeButton( R.string.btn_cancel, null );
            dialog = builder.create();
            return dialog;
         case DIALOG_LOGCONTROL:
            builder = new AlertDialog.Builder( this );
            factory = LayoutInflater.from( this );
            view = factory.inflate( R.layout.logcontrol, null );
            builder.setTitle( R.string.dialog_tracking_title )
            .setIcon( android.R.drawable.ic_dialog_alert )
            .setNegativeButton( R.string.btn_cancel, null )
            .setView( view );
            dialog = builder.create();
            return dialog;
         default:
            return super.onCreateDialog( id );
      }
   }
   
   /*
    * (non-Javadoc)
    * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
    */
   @Override
   protected void onPrepareDialog( int id, Dialog dialog )
   {
      int state = this.mLoggerServiceManager.getLoggingState();
      switch (id)
      {
         case DIALOG_LOGCONTROL:
            Button start = (Button) dialog.findViewById( R.id.logcontrol_start );
            Button pause = (Button) dialog.findViewById( R.id.logcontrol_pause );
            Button resume = (Button) dialog.findViewById( R.id.logcontrol_resume );
            Button stop = (Button) dialog.findViewById( R.id.logcontrol_stop );
            switch( state )
            {
               case GPSLoggerService.STOPPED:
                  start.setEnabled( true );
                  pause.setEnabled( false );
                  resume.setEnabled( false );
                  stop.setEnabled( false );
                  break;
               case GPSLoggerService.RUNNING:
                  start.setEnabled( false );
                  pause.setEnabled( true );
                  resume.setEnabled( false );
                  stop.setEnabled( true );
                  break;
               case GPSLoggerService.PAUSED:
                  start.setEnabled( false );
                  pause.setEnabled( false );
                  resume.setEnabled( true );
                  stop.setEnabled( true );
                  break;
               default:
                  start.setEnabled( false );
                  pause.setEnabled( false );
                  resume.setEnabled( false );
                  stop.setEnabled( false );
            }
      }
      super.onPrepareDialog( id, dialog );
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
    */
   @Override
   protected void onActivityResult( int requestCode, int resultCode, Intent data )
   {
      super.onActivityResult( requestCode, resultCode, data );
      if( resultCode != RESULT_CANCELED )
      {
         Bundle extras = data.getExtras();
         switch (requestCode)
         {
            case MENU_TRACKLIST:
               long trackId = extras.getLong( Tracks._ID );
               moveToTrack( trackId );
               break;
         }
      }
   }

   /**
    * (non-Javadoc)
    * 
    * @see com.google.android.maps.MapActivity#isRouteDisplayed()
    */
   @Override
   protected boolean isRouteDisplayed()
   {
      return true;
   }

   private void updateTitleBar()
   {
      ContentResolver resolver = this.getApplicationContext().getContentResolver();
      Cursor trackCursor = null;
      try
      {
         trackCursor = resolver.query( ContentUris.withAppendedId( Tracks.CONTENT_URI, this.mTrackId ), new String[] { Tracks.NAME }, null, null, null );
         if( trackCursor.moveToLast() )
         {
            String trackName = trackCursor.getString( 0 );
            this.setTitle( this.getString( R.string.app_name ) + ": " + trackName );
         }
      }
      finally
      {
         if( trackCursor != null )
         {
            trackCursor.close();
         }
      }
   }

   private void updateBlankingBehavior()
   {
      boolean disableblanking = PreferenceManager.getDefaultSharedPreferences( this ).getBoolean( LoggerMap.DISABLEBLANKING, false );
      if( disableblanking )
      {
         if( this.mWakeLock == null )
         {
            PowerManager pm = (PowerManager) this.getSystemService( Context.POWER_SERVICE );
            this.mWakeLock = pm.newWakeLock( PowerManager.SCREEN_DIM_WAKE_LOCK, TAG );
         }
         if( this.mLoggerServiceManager.getLoggingState() == GPSLoggerService.RUNNING && !this.mWakeLock.isHeld() )
         {
            this.mWakeLock.acquire();
            Log.w( TAG, "Acquired lock to keep screen on!" );
         }
      }
   }

   private void updateSpeedbarVisibility()
   {
      int trackColoringMethod = new Integer( PreferenceManager.getDefaultSharedPreferences( this ).getString( TrackingOverlay.TRACKCOLORING, "3" ) ).intValue();
      ContentResolver resolver = this.getApplicationContext().getContentResolver();
      Cursor waypointsCursor = null;
      try
      {
         waypointsCursor = resolver.query( Uri.withAppendedPath( Tracks.CONTENT_URI, this.mTrackId + "/waypoints" ), new String[] { "avg(" + Waypoints.SPEED + ")" }, null, null, null );
         if( waypointsCursor != null && waypointsCursor.moveToLast() )
         {
            mAverageSpeed = waypointsCursor.getDouble( 0 );
            if( mAverageSpeed == 0 )
            {
               mAverageSpeed = 33.33d / 2d;
            }
         }
      }
      finally
      {
         if( waypointsCursor != null )
         {
            waypointsCursor.close();
         }
      }
      View speedbar = findViewById( R.id.speedbar );
      if( trackColoringMethod == TrackingOverlay.DRAW_MEASURED || trackColoringMethod == TrackingOverlay.DRAW_CALCULATED )
      {
         drawSpeedTexts( mAverageSpeed );
         speedbar.setVisibility( View.VISIBLE );
         for (int i = 0; i < mSpeedtexts.length; i++)
         {
            mSpeedtexts[i].setVisibility( View.VISIBLE );
         }
      }
      else
      {
         speedbar.setVisibility( View.INVISIBLE );
         for (int i = 0; i < mSpeedtexts.length; i++)
         {
            mSpeedtexts[i].setVisibility( View.INVISIBLE );
         }
      }
   }

   private void updateSpeedDisplayVisibility()
   {
      boolean showspeed = PreferenceManager.getDefaultSharedPreferences( this ).getBoolean( LoggerMap.SHOWSPEED, false );
      if( showspeed )
      {
         mLastGPSSpeedText.setVisibility( View.VISIBLE );
      }
      else
      {
         mLastGPSSpeedText.setVisibility( View.INVISIBLE );
      }
   }

   protected void createSpeedDisplayNumbers()
   {
      ContentResolver resolver = this.getApplicationContext().getContentResolver();
      Cursor waypointsCursor = null;
      try
      {
         Uri lastSegmentUri = Uri.withAppendedPath( Tracks.CONTENT_URI, this.mTrackId + "/segments/"+mLastSegment+"/waypoints" );
         waypointsCursor = resolver.query( lastSegmentUri, new String[] { Waypoints.SPEED }, null, null, null );
         if( waypointsCursor.moveToLast() )
         {
            String speed_unit = this.getResources().getString( R.string.speed_unitname );
            TypedValue outValue = new TypedValue();
            this.getResources().getValue( R.raw.conversion_from_mps, outValue, false );
            float conversion_from_mps = outValue.getFloat();
            double speed = waypointsCursor.getDouble( 0 );
            speed = speed * conversion_from_mps;
            String speedText = String.format( "%.0f", speed ) + " " + speed_unit;
            mLastGPSSpeedText.setText( speedText );
         }
      }
      finally
      {
         if( waypointsCursor != null )
         {
            waypointsCursor.close();
         }
      }
   }

   /**
    * For the current track identifier the route of that track is drawn by adding a OverLay for each segments in the track
    * 
    * @param trackId
    * @see TrackingOverlay
    */
   private void createTrackingDataOverlays()
   {
      List<Overlay> overlays = this.mMapView.getOverlays();
      overlays.clear();

      ContentResolver resolver = this.getApplicationContext().getContentResolver();
      Cursor segments = null;
      int trackColoringMethod = new Integer( PreferenceManager.getDefaultSharedPreferences( this ).getString( TrackingOverlay.TRACKCOLORING, "3" ) ).intValue();

      GeoPoint lastPoint = null;
      try
      {
         Uri segmentsUri = Uri.withAppendedPath( Tracks.CONTENT_URI, this.mTrackId + "/segments" );
         segments = resolver.query( segmentsUri, new String[] { Segments._ID }, null, null, null );
         if( segments.moveToFirst() )
         {
            do
            {
               long segmentsId = segments.getLong( 0 );
               Uri segmentUri = Uri.withAppendedPath( segmentsUri, segmentsId + "/waypoints" );
               TrackingOverlay segmentOverlay = new TrackingOverlay( (Context) this, segmentUri, trackColoringMethod, mAverageSpeed, this.mMapView );
               overlays.add( segmentOverlay );
               if( segments.isFirst() )
               {
                  segmentOverlay.addPlacement( TrackingOverlay.FIRST_SEGMENT );
               }
               if( segments.isLast() )
               {
                  segmentOverlay.addPlacement( TrackingOverlay.LAST_SEGMENT );
                  lastPoint = getLastTrackPoint();
               }
               mLastSegment = segmentsId;
            }
            while (segments.moveToNext());
         }
      }
      finally
      {
         if( segments != null )
         {
            segments.close();
         }
      }
      if( lastPoint != null )
      {
         Point out = new Point();
         this.mMapView.getProjection().toPixels( lastPoint, out );
         int height = this.mMapView.getHeight();
         int width = this.mMapView.getWidth();
         if( out.x < 0 || out.y < 0 || out.y > height || out.x > width )
         {
            this.mMapView.clearAnimation();
            this.mMapView.getController().setCenter( lastPoint );
         }
         else if( out.x < width / 4 || out.y < height / 4 || out.x > ( width / 4 ) * 3 || out.y > ( height / 4 ) * 3 )
         {
            this.mMapView.clearAnimation();
            this.mMapView.getController().animateTo( lastPoint );
         }
         
      }
      this.mMapView.postInvalidate();
   }

   /**
    * @param avgSpeed avgSpeed in m/s
    */
   private void drawSpeedTexts( double avgSpeed )
   {
      TypedValue outValue = new TypedValue();
      this.getResources().getValue( R.raw.conversion_from_mps, outValue, false );
      float conversion_from_mps = outValue.getFloat();
      String unit = this.getResources().getString( R.string.speed_unitname );

      avgSpeed = avgSpeed * conversion_from_mps;
      for (int i = 0; i < mSpeedtexts.length; i++)
      {
         mSpeedtexts[i].setVisibility( View.VISIBLE );
         int speed = (int) ( ( avgSpeed * 2d ) / 5d ) * i;
         mSpeedtexts[i].setText( speed + unit );
      }
   }

   /**
    * Alter this to set a new track as current.
    * 
    * @param trackId
    */
   private void moveToTrack( long trackId )
   {
      Cursor track = null;
      try
      {
         ContentResolver resolver = this.getApplicationContext().getContentResolver();
         Uri trackUri = ContentUris.withAppendedId( Tracks.CONTENT_URI, trackId );
         track = resolver.query( trackUri, new String[] { Tracks.NAME }, null, null, null );
         if( track.moveToFirst() )
         {
            this.mTrackId = trackId;
            mLastSegment = -1;
            mLastWaypoint = -1;
            resolver.unregisterContentObserver( this.mTrackObserver );
            resolver.registerContentObserver( trackUri, false, this.mTrackObserver );

            updateTitleBar();
            createTrackingDataOverlays();
            updateSpeedbarVisibility();
         }
      }
      finally
      {
         if( track != null )
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
   private GeoPoint getLastKnowGeopointLocation()
   {
      LocationManager locationManager = (LocationManager) this.getApplication().getSystemService( Context.LOCATION_SERVICE );
      Location location = locationManager.getLastKnownLocation( LocationManager.GPS_PROVIDER );
      int microLatitude = 0;
      int microLongitude = 0;
      if( location != null )
      {
         microLatitude = (int) ( location.getLatitude() * 1E6d );
         microLongitude = (int) ( location.getLongitude() * 1E6d );
      }
      GeoPoint geoPoint = new GeoPoint( microLatitude, microLongitude );
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
      try
      {
         ContentResolver resolver = this.getContentResolver();
         waypoint = resolver.query( Uri.withAppendedPath( Tracks.CONTENT_URI, mTrackId + "/waypoints" ), new String[] { Waypoints.LATITUDE, Waypoints.LONGITUDE,
               "max(" + Waypoints.TABLE+"."+Waypoints._ID + ")" }, null, null, null );
         if( waypoint == null )
         {
            lastPoint = new GeoPoint( 0, 0 );
         }
         else
         {
            boolean exists = waypoint.moveToLast();
            if( exists )
            {
               int microLatitude = (int) ( waypoint.getDouble( 0 ) * 1E6d );
               int microLongitude = (int) ( waypoint.getDouble( 1 ) * 1E6d );
               lastPoint = new GeoPoint( microLatitude, microLongitude );
               mLastWaypoint = waypoint.getLong( 2 );
            }
            else 
            {
               Log.e(TAG, "There is NO waypoint for this given track id "+mTrackId);
               lastPoint = new GeoPoint( 51985105, 5106132 );
            }
         }
      }
      finally
      {
         if( waypoint != null )
         {
            waypoint.close();
         }
      }
      return lastPoint;
   }

   private void moveToLastTrack()
   {
      int trackId = -1;
      Cursor track = null;
      try
      {
         ContentResolver resolver = this.getApplicationContext().getContentResolver();
         track = resolver.query( Tracks.CONTENT_URI, new String[] { "max(" + Tracks._ID + ")", Tracks.NAME, }, null, null, null );
         if( track.moveToLast() )
         {
            trackId = track.getInt( 0 );
            moveToTrack( trackId );
         }
      }
      finally
      {
         if( track != null )
         {
            track.close();
         }
      }
   }
}