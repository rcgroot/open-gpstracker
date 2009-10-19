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
import nl.sogeti.android.gpstracker.actions.ExportGPX;
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
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

/**
 *
 * @version $Id$
 * @author rene (c) Jan 18, 2009, Sogeti B.V.
 */
public class LoggerMap extends MapActivity
{   
   private static final int ZOOM_LEVEL = 10;
   
   // MENU'S
   private static final int MENU_SETTINGS = 0;
   private static final int MENU_TOGGLE   = 1;
   private static final int MENU_TRACKLIST = 5;
   private static final int MENU_VIEW = 7;
   
   private static final String TAG = LoggerMap.class.getName();

   protected static final String DISABLEBLANKING = "disableblanking";

   private long mTrackId = -1;
   private MapView mMapView = null;
   private MapController mMapController = null;
   private GPSLoggerServiceManager mLoggerServiceManager;
   private String mTrackName;
   private EditText mFileNameView;
   private EditText mTrackNameView;
   private WakeLock mWakeLock = null;

   private TextView[] mSpeedtexts = null;

   private OnSharedPreferenceChangeListener mSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener()
   {
      public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
      {
         if( key.equals( TrackingOverlay.TRACKCOLORING ) )
         {
            int trackColoringMethod = new Integer( sharedPreferences.getString( TrackingOverlay.TRACKCOLORING, "3" ) ).intValue();
            updateSpeedbarVisibility( trackColoringMethod );
            List<Overlay> overlays = LoggerMap.this.mMapView.getOverlays();
            for( Overlay overlay : overlays )
            {
               if( overlay instanceof TrackingOverlay )
               {
                  ( (TrackingOverlay) overlay ).setTrackColoringMethod( trackColoringMethod );
                  
               }
            }
         }
         else if( key.equals( LoggerMap.DISABLEBLANKING ) )
         {
            updateBlankingBehavior();
         }
      }
   };

   private final ContentObserver mTrackObserver = new ContentObserver(new Handler()) 
   {
      @Override
      public void onChange(boolean selfUpdate) 
      {
         LoggerMap.this.drawTrackingData();
      }
   };
   
   private final DialogInterface.OnClickListener mNoTrackDialogListener = new DialogInterface.OnClickListener()
   {
      public void onClick(DialogInterface dialog, int which)
      {
            Intent tracklistIntent = new Intent(LoggerMap.this, TrackList.class);
            tracklistIntent.putExtra( Tracks._ID, LoggerMap.this.mTrackId );
            startActivityForResult(tracklistIntent, MENU_TRACKLIST);
      }
   } ;

   private final DialogInterface.OnClickListener mFileNameDialogListener = new DialogInterface.OnClickListener()
   {
      public void onClick(DialogInterface dialog, int which)
      {
         String filename = mFileNameView.getText().toString();
         
         Uri uri = ContentUris.withAppendedId( Tracks.CONTENT_URI, LoggerMap.this.mTrackId );
         Intent actionIntent = new Intent(Intent.ACTION_SEND, uri );         
         actionIntent.putExtra( ExportGPX.FILENAME, filename );
         LoggerMap.this.sendBroadcast( actionIntent, android.Manifest.permission.ACCESS_FINE_LOCATION );
         mFileNameView = null;
      }
   } ;
   
   DialogInterface.OnClickListener mTrackNameDialogListener = new DialogInterface.OnClickListener() {
      public void onClick( DialogInterface dialog, int which )
      {
         String trackName = mTrackNameView.getText().toString();
         ContentValues values = new ContentValues();
         values.put( Tracks.NAME, trackName);
         getContentResolver().update(
               ContentUris.withAppendedId( Tracks.CONTENT_URI, LoggerMap.this.mTrackId ), 
               values, null, null );
         mTrackNameView = null;
      }
   } ;

   @Override
   public boolean onKeyDown( int keyCode, KeyEvent event )
   {
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
            propagate = false;
            break;
         case KeyEvent.KEYCODE_F:
            attempToMoveToTrack(this.mTrackId-1);
            propagate = false;
            break;
         case KeyEvent.KEYCODE_H:
            attempToMoveToTrack(this.mTrackId+1);
            propagate = false;
            break;
         default :
            propagate =  super.onKeyDown( keyCode, event );
         break;
      }
      return propagate;
   }

   @Override
   public boolean onCreateOptionsMenu( Menu menu )
   {
      boolean result =  super.onCreateOptionsMenu(menu);

      menu.add(0, MENU_TOGGLE, 0, R.string.menu_toggle_on).setIcon(android.R.drawable.ic_menu_mapmode).setAlphabeticShortcut( 't' );
      menu.add(0, MENU_TRACKLIST, 0, R.string.menu_tracklist).setIcon(android.R.drawable.ic_menu_gallery).setAlphabeticShortcut( 'l' );
      menu.add(0, MENU_VIEW, 0, R.string.menu_showTrack).setIcon(android.R.drawable.ic_menu_view).setAlphabeticShortcut( 'e' );
      menu.add(0, MENU_SETTINGS, 0, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences).setAlphabeticShortcut( 's' );
      return result;
   }

   @Override
   public boolean onPrepareOptionsMenu(Menu menu) {
      super.onPrepareOptionsMenu(menu);
      if( this.mLoggerServiceManager.isLogging() ) 
      {
         menu.findItem( MENU_TOGGLE ).setTitle( R.string.menu_toggle_off );
      }
      else 
      {
         menu.findItem( MENU_TOGGLE ).setTitle( R.string.menu_toggle_on );
      }
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      boolean handled = false ;
      switch ( item.getItemId() ) {
         case MENU_TOGGLE:
            if( this.mLoggerServiceManager.isLogging() ) 
            {
               this.mLoggerServiceManager.stopGPSLoggerService();
               item.setTitle( R.string.menu_toggle_on );
            }
            else 
            {
               this.mTrackId = this.mLoggerServiceManager.startGPSLoggerService(null);
               attempToMoveToTrack( this.mTrackId );
               item.setTitle( R.string.menu_toggle_off );
               
               LayoutInflater factory = LayoutInflater.from( this );
               View view = factory.inflate( R.layout.namedialog, null );
               mTrackNameView = (EditText) view.findViewById( R.id.nameField );
               createTrackTitleDialog( this, view, mTrackNameDialogListener ).show();
            }
            updateBlankingBehavior();
            handled = true;
            break;
         case MENU_SETTINGS:
            Intent i = new Intent( this, SettingsDialog.class );
            startActivity( i );
            handled = true;
            break;
         case MENU_TRACKLIST:
            Intent tracklistIntent = new Intent(this, TrackList.class);
            tracklistIntent.putExtra( Tracks._ID, this.mTrackId );
            startActivityForResult(tracklistIntent, MENU_TRACKLIST);
            break;
         case MENU_VIEW:
            if( this.mTrackId >= 0 )
            {
               Uri uri = ContentUris.withAppendedId( Tracks.CONTENT_URI, this.mTrackId );
               Intent actionIntent = new Intent(Intent.ACTION_VIEW, uri );
               startActivity( actionIntent );
               handled = true;
               break;
            }
            else
            {
               createAlertNoTrack( this, mNoTrackDialogListener, null ).show();
            }
            handled = true;
            break;
         default:
            handled = super.onOptionsItemSelected(item);
      }
      return handled;
   }

   /** 
    * Called when the activity is first created. 
    *
    */
   @Override
   protected void onCreate( Bundle load )
   {
      super.onCreate( load );

      this.startService( new Intent( GPSLoggerService.SERVICENAME ) );
      this.mLoggerServiceManager = new GPSLoggerServiceManager( (Context)this );
      this.mLoggerServiceManager.connectToGPSLoggerService();

      PreferenceManager.getDefaultSharedPreferences( this ).registerOnSharedPreferenceChangeListener( mSharedPreferenceChangeListener );
      updateBlankingBehavior();
      
      setContentView(R.layout.map);
      this.mMapView = (MapView) findViewById( R.id.myMapView );
      this.mMapView.setClickable( true );
      this.mMapView.setStreetView( false );
      this.mMapView.setSatellite( false );
      TextView[] speeds = 
      { (TextView) findViewById( R.id.speedview05)
      , (TextView) findViewById( R.id.speedview04)
      , (TextView) findViewById( R.id.speedview03)
      , (TextView) findViewById( R.id.speedview02)
      , (TextView) findViewById( R.id.speedview01)
      , (TextView) findViewById( R.id.speedview00)
      } ;
      mSpeedtexts = speeds;

      
      /* Collect the zoomcontrols and place them */
      this.mMapView.setBuiltInZoomControls( true );
      this.mMapController = this.mMapView.getController();

      /* Initial display: Last logged track drawn and zoomed to current location */
      if( load==null || !load.containsKey("track") )
      {
         moveToLastTrack();
      }
      if( load==null || !load.containsKey("zoom") )
      {
         this.mMapController.setZoom( LoggerMap.ZOOM_LEVEL );
      }
      if( load==null || !load.containsKey("e6lat") || !load.containsKey("e6long") )
      {
         GeoPoint point = getLastKnowGeopointLocation();
         if( point.getLatitudeE6() != 0 && point.getLongitudeE6() != 0 )
         {
             this.mMapView.getController().animateTo( point );
         }
         else 
         {
            this.mMapController.setZoom( LoggerMap.ZOOM_LEVEL );
         }
      }
   }

   /*
    * (non-Javadoc)
    * @see com.google.android.maps.MapActivity#onPause()
    */
   @Override
   protected void onDestroy()
   {
      super.onDestroy();
      resumeBlanking();
      PreferenceManager.getDefaultSharedPreferences( this ).unregisterOnSharedPreferenceChangeListener( this.mSharedPreferenceChangeListener );
   }

   @Override
   public void onRestoreInstanceState(Bundle load) 
   {
      if( load!=null && load.containsKey("track") )
      {
         this.mTrackId = load.getLong( "track" );
         attempToMoveToTrack( this.mTrackId );
      }
      if( load!=null && load.containsKey("zoom") )
      {
         this.mMapController.setZoom(  load.getInt("zoom") );
      }
      if( load!=null && load.containsKey("e6lat") && load.containsKey("e6long") )
      {
         GeoPoint lastPoint = new GeoPoint( load.getInt("e6lat"),  load.getInt("e6long") );
         this.mMapView.getController().animateTo( lastPoint );
      }
   }

   @Override 
   public void onSaveInstanceState(Bundle save) 
   {
      GeoPoint point = this.mMapView.getMapCenter();
      save.putInt("e6lat", point.getLatitudeE6() );
      save.putInt("e6long", point.getLongitudeE6() );
      save.putInt("zoom", this.mMapView.getZoomLevel() );
      save.putLong("track", this.mTrackId );
      super.onSaveInstanceState(save); 
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
    */
   @Override
   protected void onActivityResult( int requestCode, int resultCode, Intent data )
   {
      super.onActivityResult(requestCode, resultCode, data);
      if( resultCode != RESULT_CANCELED )
      {
         Bundle extras = data.getExtras();
         switch(requestCode) {
            case MENU_TRACKLIST:
               long trackId = extras.getLong(Tracks._ID);
               attempToMoveToTrack( trackId );
               break;
         }
      }
   }

   /**
    * 
    * (non-Javadoc)
    * @see com.google.android.maps.MapActivity#isRouteDisplayed()
    */
   @Override
   protected boolean isRouteDisplayed()
   {
      return true;
   }

   private void resumeBlanking()
   {
      if( mWakeLock != null )
      {
         mWakeLock.release();
         this.mWakeLock = null ;
      }
   }

   private void updateBlankingBehavior()
   {
      boolean disableblanking = PreferenceManager.getDefaultSharedPreferences( this ).getBoolean( LoggerMap.DISABLEBLANKING, false );
      if( disableblanking && this.mLoggerServiceManager.isLogging() )
      {
         PowerManager pm = (PowerManager) this.getSystemService( Context.POWER_SERVICE );
         this.mWakeLock = pm.newWakeLock( PowerManager.SCREEN_DIM_WAKE_LOCK, TAG );
         this.mWakeLock.acquire();
      }
      else 
      {
         resumeBlanking();
      }
   }
   private void updateSpeedbarVisibility( int trackColoringMethod )
   {
      View speedbar = findViewById( R.id.speedbar );     
      if( trackColoringMethod == TrackingOverlay.DRAW_MEASURED || trackColoringMethod == TrackingOverlay.DRAW_CALCULATED )
      {
         speedbar.setVisibility( View.VISIBLE );
         for( int i=0 ; i<mSpeedtexts.length ; i++ )
         {
            mSpeedtexts[i].setVisibility( View.VISIBLE );
         }
      }
      else 
      {
         speedbar.setVisibility( View.INVISIBLE );
         for( int i=0 ; i<5 ; i++ )
         {
            mSpeedtexts[i].setVisibility( View.INVISIBLE );
         }
      }
   }
   
   /**
    * For the current track identifier the route of that track is drawn 
    * by adding a OverLay for each segments in the track
    * 
    * @param trackId
    * @see TrackingOverlay
    */
   private void drawTrackingData()
   {
      List<Overlay> overlays = this.mMapView.getOverlays();
      overlays.clear();

      ContentResolver resolver = this.getApplicationContext().getContentResolver();
      Cursor segments = null ;
      int trackColoringMethod = new Integer( PreferenceManager.getDefaultSharedPreferences( this ).getString( TrackingOverlay.TRACKCOLORING, "3" ) ).intValue();

      Cursor waypointsCursor = null ;
      double avgSpeed = 33.33d/2d;
      try
      {
         waypointsCursor = resolver.query
               ( Uri.withAppendedPath( Tracks.CONTENT_URI, this.mTrackId+"/waypoints" )
               , new String[] { "avg("+Waypoints.SPEED+")" }
               , null
               , null
               , null );
         if( waypointsCursor.moveToLast() )
         {
            avgSpeed = waypointsCursor.getDouble( 0 );
            if( avgSpeed == 0 )
            {
               avgSpeed = 33.33d/2d;
            }
            drawSpeedTexts( avgSpeed );
         }
      }
      finally
      {
         if( waypointsCursor != null )
         {
            waypointsCursor.close();
         }
      }
      
      Cursor trackCursor = null ;
      try
      {
         trackCursor = resolver.query
               ( ContentUris.withAppendedId( Tracks.CONTENT_URI, this.mTrackId )
               , new String[] { Tracks.NAME }
               , null
               , null
               , null );
         if( trackCursor.moveToLast() )
         {
            String name = trackCursor.getString( 0 );
            drawToTrackName( name );
         }
      }
      finally
      {
         if( trackCursor != null )
         {
            trackCursor.close();
         }
      }
      
      
      try 
      {
         Uri segmentsUri = Uri.withAppendedPath( Tracks.CONTENT_URI, this.mTrackId+"/segments" );
         segments = resolver.query( 
               segmentsUri, 
               new String[] { Segments._ID }, 
               null, null, null );
         if(segments.moveToFirst())
         {
            do 
            {
               long segmentsId = segments.getLong( 0 );
               Uri segmentUri = Uri.withAppendedPath( segmentsUri, segmentsId+"/waypoints" );
               TrackingOverlay segmentOverlay = new TrackingOverlay( (Context)this, resolver, segmentUri, trackColoringMethod, avgSpeed );

               
               updateSpeedbarVisibility( trackColoringMethod );
               
               overlays.add( segmentOverlay );
               if( segments.isFirst() ) 
               {
                  segmentOverlay.setPlacement( TrackingOverlay.FIRST );
               }
               if( segments.isLast() )
               {
                  segmentOverlay.setPlacement( TrackingOverlay.LAST );
                  GeoPoint lastPoint = getLastTrackPoint( this.mTrackId, segmentsId );
                  if( lastPoint != null )
                  {
                     Point out = new Point();
                     this.mMapView.getProjection().toPixels( lastPoint, out );
                     if( out.x < this.mMapView.getWidth()/4 
                           || out.y < this.mMapView.getHeight()/4  
                           || out.x > (this.mMapView.getWidth()/4)*3 
                           || out.y > (this.mMapView.getHeight()/4)*3 ) 
                     {
//                         Log.d( TAG, "Animating to "+out );
                         this.mMapView.getController().animateTo( lastPoint );
                     }
                  }
               }
            }
            while( segments.moveToNext());
         }
      }
      finally
      {
         if( segments != null )
         {
            segments.close();
         }
      }
      this.mMapView.postInvalidate();
   }
   /**
    * 
    * @param avgSpeed  avgSpeed in m/s
    */
   private void drawSpeedTexts( double avgSpeed )
   {
      TypedValue outValue = new TypedValue();
      this.getResources().getValue( R.raw.conversion_from_mps, outValue, false ) ;
      float conversion_from_mps =  outValue.getFloat();
      String unit = this.getResources().getString( R.string.speed_unitname );
      
      avgSpeed = avgSpeed *  conversion_from_mps;
      for( int i=0 ; i<mSpeedtexts.length ; i++ )
      {
         mSpeedtexts[i].setVisibility( View.VISIBLE );
         int speed = (int) ((avgSpeed*2d)/5d)*i;
         mSpeedtexts[i].setText( speed+unit );
      }
   }

   /**
    * Retrieve the last point of the current track 
    * 
    *  @param context 
    */
   private GeoPoint getLastTrackPoint( long trackId, long segmentId )
   {
      Cursor waypoint = null;
      GeoPoint lastPoint = null;
      try
      {
         ContentResolver resolver = this.getContentResolver();
         waypoint = resolver.query( 
               Uri.withAppendedPath( Tracks.CONTENT_URI, trackId+"/segments/"+segmentId+"/waypoints" ), 
               new String[] { Waypoints.LATITUDE, Waypoints.LONGITUDE,  "max("+Waypoints._ID+")"  }, null, null, null );
         boolean exists = waypoint.moveToLast();
         if( exists )
         {
            int microLatitude = (int) ( waypoint.getDouble( 0 ) * 1E6d );
            int microLongitude = (int) ( waypoint.getDouble( 1 ) * 1E6d );
            lastPoint = new GeoPoint(microLatitude, microLongitude);
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
   
   /**
    * 
    * Alter this to set a new track as current.
    * 
    * @param trackId 
    * @return if the switch to the new track succeeded
    */
   private boolean attempToMoveToTrack( long trackId )
   {
      boolean exists;
      if( trackId >= 0 )
      {
         Cursor track = null;
         try{
            ContentResolver resolver = this.getApplicationContext().getContentResolver();
            Uri trackUri = ContentUris.withAppendedId( Tracks.CONTENT_URI, trackId ) ;
            track = resolver.query( 
                  trackUri, 
                  new String[] { Tracks.NAME }, null, null, null );
            exists = track.moveToFirst();
            if( exists )
            {
               this.mTrackId = trackId ;
               drawToTrackName(track.getString( 0 ));
               resolver.unregisterContentObserver( this.mTrackObserver );
               resolver.registerContentObserver( trackUri, false, this.mTrackObserver );
               drawTrackingData();
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
      else 
      {
         exists = false;
      }
      return exists;
   }

   /**
    * 
    * Alter the view to display the title with track information 
    */
   private void drawToTrackName(String trackName) 
   {
      this.mTrackName = trackName;
      this.setTitle( this.getString( R.string.app_name ) + ": " + trackName); 
   }

   /**
    * Get the last know position from the GPS provider and
    * return that information wrapped in a GeoPoint 
    * to which the Map can navigate.
    * 
    * @see GeoPoint
    * 
    * @return
    */
   private GeoPoint getLastKnowGeopointLocation()
   {
      LocationManager locationManager =  (LocationManager) this.getApplication().getSystemService(Context.LOCATION_SERVICE) ;
      Location location = locationManager.getLastKnownLocation( LocationManager.GPS_PROVIDER );
      int microLatitude = 0 ;
      int microLongitude = 0 ;
      if( location != null )
      {
         microLatitude = (int) (location.getLatitude() * 1E6d);
         microLongitude = (int) (location.getLongitude() * 1E6d);
      }
      GeoPoint geoPoint = new GeoPoint(microLatitude, microLongitude);
      return geoPoint;
   }

   private int moveToLastTrack()
   {
      int trackId = 0;
      Cursor track = null;
      try 
      {
         ContentResolver resolver = this.getApplicationContext().getContentResolver();
         track = resolver.query( 
               Tracks.CONTENT_URI, 
               new String[] {  "max("+Tracks._ID+")", Tracks.NAME,  }, 
               null, null, null );
         if(track.moveToLast())
         {
            attempToMoveToTrack( this.mTrackId );
         }
      }
      finally 
      {
         if( track != null )
         {
            track.close();
         }
      }
      return trackId;
   }

   public static Dialog createTrackTitleDialog( Context ctx, View view, DialogInterface.OnClickListener positiveListener) 
   {           

      Builder builder = new AlertDialog.Builder( ctx )
      .setTitle( R.string.dialog_routename_title )
      .setMessage( R.string.dialog_routename_message )
      .setIcon( android.R.drawable.ic_dialog_alert )
      .setView( view )
      .setPositiveButton(R.string.btn_okay, positiveListener);
      
      Dialog dialog = builder.create();
      return dialog;
   }

   private static Dialog createAlertNoTrack( Context ctx, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener ) 
   {
      Builder builder = new AlertDialog.Builder( ctx )
      .setTitle( R.string.dialog_notrack_title )
      .setMessage(R.string.dialog_notrack_message )
      .setIcon( android.R.drawable.ic_dialog_alert )
      .setPositiveButton( R.string.btn_selecttrack, positiveListener )
      .setNegativeButton( R.string.btn_cancel, negativeListener );
      
      Dialog dialog = builder.create();
      return dialog;
   }
}