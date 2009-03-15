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
import nl.sogeti.android.gpstracker.logger.GPSLoggerService;
import nl.sogeti.android.gpstracker.logger.GPSLoggerServiceManager;
import nl.sogeti.android.gpstracker.logger.SettingsDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

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
   private static final int ZOOM_LEVEL = 9;
   private static final int MENU_SETTINGS = 0;
   private static final int MENU_TOGGLE   = 1;
   private static final int MENU_TRACKLIST = 5;
   private static final int TRACK_TITLE_ID = 0;


   private long mTrackId;
   private MapView mMapView = null;
   private MapController mMapController = null;
   private GPSLoggerServiceManager loggerServiceManager;

   private final ContentObserver mTrackObserver = new ContentObserver(new Handler()) 
   {
      @Override
      public void onChange(boolean selfUpdate) 
      {
         GeoPoint lastPoint = GPSLoggerService.getLastTrackPoint(LoggerMap.this) ;
         if( lastPoint != null )
         {
            LoggerMap.this.mMapView.getController().animateTo( lastPoint );
         }
         LoggerMap.this.drawTrackingData();
      }
   };

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
      menu.add(0, MENU_SETTINGS, 0, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences).setAlphabeticShortcut( 's' );
      menu.add(0, MENU_TOGGLE, 0, R.string.menu_toggle_on).setIcon(android.R.drawable.ic_menu_mapmode).setAlphabeticShortcut( 't' );
      menu.add(0, MENU_TRACKLIST, 0, R.string.tracklist).setIcon(android.R.drawable.ic_menu_gallery).setAlphabeticShortcut( 'l' );
      return result;
   }

   @Override
   public boolean onPrepareOptionsMenu(Menu menu) {
      super.onPrepareOptionsMenu(menu);
      if( this.loggerServiceManager.isLogging() ) 
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
            if( this.loggerServiceManager.isLogging() ) 
            {
               this.loggerServiceManager.stopGPSLoggerService();
               item.setTitle( R.string.menu_toggle_on );
            }
            else 
            {
               this.mTrackId = this.loggerServiceManager.startGPSLoggerService(null);
               attempToMoveToTrack( this.mTrackId );
               showDialog(TRACK_TITLE_ID);
               item.setTitle( R.string.menu_toggle_off );
            }
            handled = true;
            break;
         case MENU_SETTINGS:
            Dialog d = new SettingsDialog( this );
            d.show();
            handled = true;
            break;
         case MENU_TRACKLIST:
            Intent i = new Intent(this, TrackList.class);
            i.putExtra( Tracks._ID, this.mTrackId );
            startActivityForResult(i, MENU_TRACKLIST);
            break;
         default:
            handled = super.onOptionsItemSelected(item);
      }
      return handled;
   }

   /**
    * (non-Javadoc)
    * @see android.app.Activity#onCreateDialog(int)
    */
   @Override
   protected Dialog onCreateDialog( int id )
   {
      Dialog dialog = null ; 
      switch (id) {
         case TRACK_TITLE_ID:
            dialog = createTrackTitleDialog();          
            break;
         default:
            dialog = null;
         break;
      }
      return dialog;
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
      this.loggerServiceManager = new GPSLoggerServiceManager( (Context)this );
      this.loggerServiceManager.connectToGPSLoggerService();

      setContentView(R.layout.map);
      this.mMapView = (MapView) findViewById( R.id.myMapView );
      this.mMapView.setClickable( true );
      this.mMapView.setStreetView( false );
      this.mMapView.setSatellite( false );

      /* Collect the zoomcontrols and place them */
      View zoomView = this.mMapView.getZoomControls();
      LinearLayout layout_zoom =  (LinearLayout) findViewById(R.id.layout_zoom);
      layout_zoom.addView( zoomView );
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
            this.mMapController.setZoom( 1 );
         }
      }
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
      try 
      {
         segments = resolver.query( 
               Uri.withAppendedPath( Tracks.CONTENT_URI, this.mTrackId+"/segments" ), 
               new String[] { Segments._ID }, 
               null, null, null );
         if(segments.moveToFirst())
         {
            do 
            {
               Uri segmentUri = Uri.withAppendedPath( Segments.CONTENT_URI, ""+segments.getInt( 0 )+"/waypoints" );
               TrackingOverlay segmentOverlay = new TrackingOverlay((Context)this, resolver, segmentUri);
               overlays.add( segmentOverlay );
               if( segments.isFirst() ) 
               {
                  segmentOverlay.setPlace( TrackingOverlay.FIRST );
               } 
               if( segments.isLast() )
               {
                  segmentOverlay.setPlace( TrackingOverlay.LAST );
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
            Uri trackUri = Uri.withAppendedPath( Tracks.CONTENT_URI, ""+trackId ) ;
            track = resolver.query( 
                  trackUri, 
                  new String[] { Tracks.NAME }, null, null, null );
            exists = track.moveToFirst();
            if( exists )
            {
               this.mTrackId = trackId ;
               setTitleToTrackName(track.getString( 0 ));
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
   private void setTitleToTrackName(String trackName) 
   {
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

   private Dialog createTrackTitleDialog() 
   {
      final Dialog dialog = new Dialog(this);
      dialog.setTitle( R.string.dialog_routename_title );
      dialog.setContentView( R.layout.namedialog );  


      final EditText text = (EditText) dialog.findViewById( R.id.nameField);
      final Button done = (Button) dialog.findViewById( R.id.doneButton );
      done.setOnClickListener(
            new View.OnClickListener() {
               public void onClick( View v )
               {
                  dialog.dismiss();
                  String trackName = text.getText().toString();
                  setTitleToTrackName( trackName );
                  ContentValues values = new ContentValues();
                  values.put( Tracks.NAME, trackName);
                  getContentResolver().update(
                        Uri.withAppendedPath( Tracks.CONTENT_URI, ""+LoggerMap.this.mTrackId ), 
                        values, 
                        null, null );
               }
            } 
      );
      return dialog;
   }
}