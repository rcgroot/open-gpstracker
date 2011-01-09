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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.ControlTracking;
import nl.sogeti.android.gpstracker.actions.NameTrack;
import nl.sogeti.android.gpstracker.actions.Statistics;
import nl.sogeti.android.gpstracker.db.GPStracking.Media;
import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.logger.GPSLoggerServiceManager;
import nl.sogeti.android.gpstracker.logger.SettingsDialog;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.UnitsI18n;
import nl.sogeti.android.gpstracker.viewer.proxy.MapViewProxy;
import nl.sogeti.android.gpstracker.viewer.proxy.MyLocationOverlayProxy;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
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
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;

/**
 * Main activity showing a track and allowing logging control
 * 
 * @version $Id$
 * @author rene (c) Jan 18, 2009, Sogeti B.V.
 */
public class LoggerMap extends MapActivity
{
   private static final int ZOOM_LEVEL = 16;
   // MENU'S
   private static final int MENU_SETTINGS = 1;
   private static final int MENU_TRACKING = 2;
   private static final int MENU_TRACKLIST = 3;
   private static final int MENU_STATS = 4;
   private static final int MENU_ABOUT = 5;
   private static final int MENU_LAYERS = 6;
   private static final int MENU_NOTE = 7;
   private static final int MENU_NAME = 8;
   private static final int MENU_PICTURE = 9;
   private static final int MENU_TEXT = 10;
   private static final int MENU_VOICE = 11;
   private static final int MENU_VIDEO = 12;
   private static final int MENU_SHARE = 13;
   private static final int MENU_CONTRIB = 14;
   private static final int DIALOG_NOTRACK = 24;
   private static final int DIALOG_INSTALL_ABOUT = 29;
   private static final int DIALOG_LAYERS = 31;
   private static final int DIALOG_TEXT = 32;
   private static final int DIALOG_NAME = 33;
   private static final int DIALOG_URIS = 34;
   private static final int DIALOG_CONTRIB = 35;
   private static final String TAG = "OGT.LoggerMap";
   private CheckBox mTraffic;
   private CheckBox mSpeed;
   private CheckBox mAltitude;
   private CheckBox mCompass;
   private CheckBox mLocation;
   private TextView[] mSpeedtexts = null;
   private TextView mLastGPSSpeedView = null;
   private TextView mLastGPSAltitudeView = null;
   private EditText mNoteNameView;
   private EditText mNoteTextView;

   private double mAverageSpeed = 33.33d / 3d;
   private long mTrackId = -1;
   private long mLastSegment = -1;
   @SuppressWarnings("unused")
   private long mLastWaypoint = -1;
   private UnitsI18n mUnits;
   private WakeLock mWakeLock = null;
   private SharedPreferences mSharedPreferences;
   private GPSLoggerServiceManager mLoggerServiceManager;
   private SegmentOverlay mLastSegmentOverlay;
   private BaseAdapter mMediaAdapter;
   private Gallery mGallery;
   
   private MapViewProxy mMapView = null;
   private MyLocationOverlayProxy mMylocation;
   
   private final ContentObserver mTrackSegmentsObserver = new ContentObserver( new Handler() )
      {
         @Override
         public void onChange( boolean selfUpdate )
         {
            if( !selfUpdate )
            {
//               Log.d( TAG, "mTrackSegmentsObserver "+ mTrackId );
               LoggerMap.this.updateDataOverlays();
            }
            else
            {
               Log.w( TAG, "mTrackSegmentsObserver skipping change on "+ mLastSegment );
            }
         }
      };
   private final ContentObserver mSegmentWaypointsObserver = new ContentObserver( new Handler() )
      {
         @Override
         public void onChange( boolean selfUpdate )
         {
            if( !selfUpdate  )
            {
//               Log.d( TAG, "mSegmentWaypointsObserver "+ mLastSegment );
               LoggerMap.this.updateDisplayedSpeedViews();
               if( mLastSegmentOverlay != null )
               {
                  moveActiveViewWindow();
                  mLastSegmentOverlay.calculateTrack();
                  mMapView.postInvalidate();
               }
            }
            else
            {
               Log.w( TAG, "mSegmentWaypointsObserver skipping change on "+ mLastSegment );
            }
         }
      };
   private final ContentObserver mTrackMediasObserver = new ContentObserver( new Handler() )
      {
         @Override
         public void onChange( boolean selfUpdate )
         {
//            Log.d( TAG, "mTrackMediasObserver received onChange()" );
            if( !selfUpdate )
            {
               if( mLastSegmentOverlay != null )
               {
                  mLastSegmentOverlay.calculateMedia();
                  mMapView.postInvalidate();
               }
            }
            else
            {
               Log.w( TAG, "mTrackMediasObserver skipping change on "+ mLastSegment );
            }
         }
      };
   private final DialogInterface.OnClickListener mNoTrackDialogListener = new DialogInterface.OnClickListener()
      {
         public void onClick( DialogInterface dialog, int which )
         {
            //            Log.d( TAG, "mNoTrackDialogListener" + which);
            Intent tracklistIntent = new Intent( LoggerMap.this, TrackList.class );
            tracklistIntent.putExtra( Tracks._ID, LoggerMap.this.mTrackId );
            startActivityForResult( tracklistIntent, MENU_TRACKLIST );
         }
      };

   private final DialogInterface.OnClickListener mOiAboutDialogListener = new DialogInterface.OnClickListener()
      {
         public void onClick( DialogInterface dialog, int which )
         {
            Uri oiDownload = Uri.parse( "market://details?id=org.openintents.about" );
            Intent oiAboutIntent = new Intent( Intent.ACTION_VIEW, oiDownload );
            try
            {
               startActivity( oiAboutIntent );
            }
            catch (ActivityNotFoundException e)
            {
               oiDownload = Uri.parse( "http://openintents.googlecode.com/files/AboutApp-1.0.0.apk" );
               oiAboutIntent = new Intent( Intent.ACTION_VIEW, oiDownload );
               startActivity( oiAboutIntent );
            }
         }
      };

   private final OnCheckedChangeListener mCheckedChangeListener = new OnCheckedChangeListener()
      {
         public void onCheckedChanged( CompoundButton buttonView, boolean isChecked )
         {
            int checkedId;
            checkedId = buttonView.getId();
            Log.d( TAG, "Checked button id "+checkedId);
            switch (checkedId)
            {
               case R.id.layer_traffic:
                  setTrafficOverlay( isChecked );
                  break;
               case R.id.layer_speed:
                  setSpeedOverlay( isChecked );
                  break;
               case R.id.layer_altitude:
                  setAltitudeOverlay( isChecked );
                  break;
               case R.id.layer_compass:
                  setCompassOverlay( isChecked );
                  break;
               case R.id.layer_location:
                  setLocationOverlay( isChecked );
                  break;
               default:
                  break;
            }
         }
      };

   private final android.widget.RadioGroup.OnCheckedChangeListener mGroupCheckedChangeListener = new android.widget.RadioGroup.OnCheckedChangeListener()
      {
         public void onCheckedChanged( RadioGroup group, int checkedId )
         {
            Log.d( TAG, "Checked group "+group+" on id "+checkedId);
            switch (checkedId)
            {
               case R.id.layer_google_satellite:
                  setSatelliteOverlay( true );
                  break;
               case R.id.layer_google_regular:
                  setSatelliteOverlay( false );
                  break;
               case R.id.layer_osm_cloudmade:
                  setOsmBaseOverlay( Constants.OSM_CLOUDMADE );
                  break;
               case R.id.layer_osm_maknik:
                  setOsmBaseOverlay( Constants.OSM_MAKNIK );
                  break;
               case R.id.layer_osm_bicycle:
                  setOsmBaseOverlay( Constants.OSM_CYCLE );
                  break;
               default:
                  break;
            }
         }
      };
   private final OnSharedPreferenceChangeListener mSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener()
      {
         public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
         {
            if( key.equals( Constants.TRACKCOLORING ) )
            {
               updateSpeedColoring();
            }
            else if( key.equals( Constants.DISABLEBLANKING ) )
            {
               updateBlankingBehavior();
            }
            else if( key.equals( Constants.SPEED ) )
            {
               updateSpeedDisplayVisibility();
            }
            else if( key.equals( Constants.ALTITUDE ) )
            {
               updateAltitudeDisplayVisibility();
            }
            else if( key.equals( Constants.COMPASS ) )
            {
               updateCompassDisplayVisibility();
            }
            else if( key.equals( Constants.TRAFFIC ) )
            {
               updateGoogleOverlays();
            }
            else if( key.equals( Constants.SATELLITE ) )
            {
               updateGoogleOverlays();
            }
            else if( key.equals( Constants.LOCATION ) )
            {
               updateLocationDisplayVisibility();
            }
            else if( key.equals( Constants.MAPPROVIDER ) ) 
            {
               updateMapProvider();
            }
            else if( key.equals( Constants.OSMBASEOVERLAY ) )
            {
               updateOsmBaseOverlay();
            }
         }
      };
   private final UnitsI18n.UnitsChangeListener mUnitsChangeListener = new UnitsI18n.UnitsChangeListener()
      {
         public void onUnitsChange()
         {
            updateDisplayedSpeedViews();
            updateSpeedColoring();
         }
      };
   private final OnClickListener mNoteTextDialogListener = new DialogInterface.OnClickListener()
   {

      public void onClick( DialogInterface dialog, int which )
      {
         String noteText = mNoteTextView.getText().toString();
         Calendar c = Calendar.getInstance();
         String newName = String.format( "Textnote_%tY-%tm-%td_%tH%tM%tS.txt", c, c, c, c, c, c );
         String sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
         File file = new File( sdcard + Constants.EXTERNAL_DIR + newName );
         FileWriter filewriter = null;
         try
         {
            file.getParentFile().mkdirs();
            file.createNewFile();
            filewriter = new FileWriter( file );
            filewriter.append( noteText );
            filewriter.flush();
         }
         catch (IOException e)
         {
            Log.e( TAG, "Note storing failed", e );
            CharSequence text = e.getLocalizedMessage();
            Toast toast = Toast.makeText( LoggerMap.this, text, Toast.LENGTH_LONG );
            toast.show();
         }
         finally
         {
            if( filewriter!=null){ try { filewriter.close(); } catch (IOException e){ /* */ } }
         }

         
         LoggerMap.this.mLoggerServiceManager.storeMediaUri( Uri.fromFile( file ) );
      }
      
   };
   private final OnClickListener mNoteNameDialogListener = new DialogInterface.OnClickListener()
   {

      public void onClick( DialogInterface dialog, int which )
      {
         String name = mNoteNameView.getText().toString();
         Uri media = Uri.withAppendedPath( Constants.NAME_URI, Uri.encode( name ) );
         LoggerMap.this.mLoggerServiceManager.storeMediaUri( media );
      }
      
   };
   private final OnClickListener mNoteSelectDialogListener = new DialogInterface.OnClickListener()
   {

      public void onClick( DialogInterface dialog, int which )
      {
         Uri selected = (Uri) mGallery.getSelectedItem();
         SegmentOverlay.handleMedia( LoggerMap.this, selected );
      }
      
   };
 
   /**
    * Called when the activity is first created.
    */
   @Override
   protected void onCreate( Bundle load )
   {
      super.onCreate( load );
      this.startService( new Intent( Constants.SERVICENAME ) );

      Object previousInstanceData = getLastNonConfigurationInstance();
      if( previousInstanceData != null && previousInstanceData instanceof GPSLoggerServiceManager )
      {
         mLoggerServiceManager = (GPSLoggerServiceManager) previousInstanceData;
      }
      else
      {
         mLoggerServiceManager = new GPSLoggerServiceManager( (Context) this );
      }
      mLoggerServiceManager.startup();

      mUnits = new UnitsI18n( this, mUnitsChangeListener );

      mSharedPreferences = PreferenceManager.getDefaultSharedPreferences( this );
      mSharedPreferences.registerOnSharedPreferenceChangeListener( mSharedPreferenceChangeListener );

      setContentView( R.layout.map );

      mMapView = new MapViewProxy();
      updateMapProvider();

      mMylocation = new MyLocationOverlayProxy( this, mMapView ); 
      mMapView.setBuiltInZoomControls( true );
      mMapView.setClickable( true );

      TextView[] speeds = { (TextView) findViewById( R.id.speedview05 ), (TextView) findViewById( R.id.speedview04 ), (TextView) findViewById( R.id.speedview03 ),
            (TextView) findViewById( R.id.speedview02 ), (TextView) findViewById( R.id.speedview01 ), (TextView) findViewById( R.id.speedview00 ) };
      mSpeedtexts = speeds;
      mLastGPSSpeedView = (TextView) findViewById( R.id.currentSpeed );
      mLastGPSAltitudeView = (TextView) findViewById( R.id.currentAltitude );

      onRestoreInstanceState( load );
   }

   protected void onPause()
   {
      if( this.mWakeLock != null && this.mWakeLock.isHeld() )
      {
         this.mWakeLock.release();
         Log.w( TAG, "onPause(): Released lock to keep screen on!" );
      }
      if( mTrackId > 0 )
      {
         ContentResolver resolver = this.getContentResolver();
         resolver.unregisterContentObserver( this.mTrackSegmentsObserver );
         resolver.unregisterContentObserver( this.mSegmentWaypointsObserver );
         resolver.unregisterContentObserver( this.mTrackMediasObserver );
      }
      mMylocation.disableMyLocation();
      mMylocation.disableCompass();
      
      super.onPause();
   }

   protected void onResume()
   {
      super.onResume();
      updateTitleBar();
      updateBlankingBehavior();

      updateMapProvider();

      if( mTrackId >= 0 )
      {
         ContentResolver resolver = this.getContentResolver();
         Uri trackUri = Uri.withAppendedPath( Tracks.CONTENT_URI, mTrackId+"/segments" );
         Uri lastSegmentUri = Uri.withAppendedPath( Tracks.CONTENT_URI, mTrackId+"/segments/"+mLastSegment+"/waypoints" );
         Uri mediaUri = ContentUris.withAppendedId( Media.CONTENT_URI, mTrackId );

         resolver.unregisterContentObserver( this.mTrackSegmentsObserver );
         resolver.unregisterContentObserver( this.mSegmentWaypointsObserver );
         resolver.unregisterContentObserver( this.mTrackMediasObserver );
         resolver.registerContentObserver( trackUri,       false, this.mTrackSegmentsObserver );
         resolver.registerContentObserver( lastSegmentUri, true, this.mSegmentWaypointsObserver );
         resolver.registerContentObserver( mediaUri,       true, this.mTrackMediasObserver );
      }
      updateDataOverlays();
      
      updateSpeedColoring();
      updateSpeedDisplayVisibility();
      updateAltitudeDisplayVisibility();
      updateCompassDisplayVisibility();
      updateLocationDisplayVisibility();
      
      mMapView.executePostponedActions();
   }
   
   

   /*
    * (non-Javadoc)
    * @see com.google.android.maps.MapActivity#onPause()
    */
   @Override
   protected void onDestroy()
   {
      super.onDestroy();
      this.mLoggerServiceManager.shutdown();
      if( mWakeLock != null && mWakeLock.isHeld() )
      {
         mWakeLock.release();
         Log.w( TAG, "onDestroy(): Released lock to keep screen on!" );
      }
      mSharedPreferences.unregisterOnSharedPreferenceChangeListener( this.mSharedPreferenceChangeListener );
      if( mLoggerServiceManager.getLoggingState() == Constants.STOPPED )
      {
         stopService( new Intent( Constants.SERVICENAME ) );
      }
   }

   /*
    * (non-Javadoc)
    * @see com.google.android.maps.MapActivity#onNewIntent(android.content.Intent)
    */
   @Override
   public void onNewIntent( Intent newIntent )
   {
      Uri data = newIntent.getData();
      if( data != null )
      {
         moveToTrack( Long.parseLong( data.getLastPathSegment() ), true );
      }
   }

   @Override
   protected void onRestoreInstanceState( Bundle load )
   {
      if( load != null )
      {
         super.onRestoreInstanceState( load );
      }    
      
      Uri data = this.getIntent().getData();
      if( load != null && load.containsKey( "track" ) ) // 1st track from a previous instance of this activity
      {
         long loadTrackId = load.getLong( "track" );
         moveToTrack( loadTrackId, false );
      }
      else if( data != null )                           // 2nd track ordered to make
      {
         long loadTrackId = Long.parseLong( data.getLastPathSegment() );
         moveToTrack( loadTrackId, true );
      }
      else 
      {
         moveToLastTrack();                             // 3rd just try the last track
      }

      if( load != null && load.containsKey( "zoom" ) )
      {
         mMapView.getController().setZoom( load.getInt( "zoom" ) );
      }
      else
      {
         mMapView.getController().setZoom( LoggerMap.ZOOM_LEVEL );
      }

      if( load != null && load.containsKey( "e6lat" ) && load.containsKey( "e6long" ) )
      {
         GeoPoint storedPoint = new GeoPoint( load.getInt( "e6lat" ), load.getInt( "e6long" ) );
         this.mMapView.getController().animateTo( storedPoint );
      }
      else
      {
         GeoPoint lastPoint = getLastTrackPoint();
         this.mMapView.getController().animateTo( lastPoint );
      }
   }
   
   private void restoreMapState( Bundle load )
   {

   }

   @Override
   protected void onSaveInstanceState( Bundle save )
   {
      super.onSaveInstanceState( save );
      save.putLong( "track", this.mTrackId );
      save.putInt( "zoom", this.mMapView.getZoomLevel() );
      GeoPoint point = this.mMapView.getMapCenter();
      save.putInt( "e6lat", point.getLatitudeE6() );
      save.putInt( "e6long", point.getLongitudeE6() );
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
            setSatelliteOverlay( !this.mMapView.isSatellite() );
            propagate = false;
            break;
         case KeyEvent.KEYCODE_A:
            setTrafficOverlay( !this.mMapView.isTraffic() );
            propagate = false;
            break;
         case KeyEvent.KEYCODE_F:
            moveToTrack( this.mTrackId - 1, true );
            propagate = false;
            break;
         case KeyEvent.KEYCODE_H:
            moveToTrack( this.mTrackId + 1, true );
            propagate = false;
            break;
         default:
            propagate = super.onKeyDown( keyCode, event );
            break;
      }
      return propagate;
   }

   private void setTrafficOverlay( boolean b )
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean( Constants.TRAFFIC, b );
      editor.commit();
   }

   private void setSatelliteOverlay( boolean b )
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean( Constants.SATELLITE, b );
      editor.commit();
   }

   private void setSpeedOverlay( boolean b )
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean( Constants.SPEED, b );
      editor.commit();
   }

   private void setAltitudeOverlay( boolean b )
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean( Constants.ALTITUDE, b );
      editor.commit();
   }
   
   private void setCompassOverlay( boolean b )
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean( Constants.COMPASS, b );
      editor.commit();
   }

   private void setLocationOverlay( boolean b )
   {
      Editor editor = mSharedPreferences.edit();
      editor.putBoolean( Constants.LOCATION, b );
      editor.commit();
   }

   private void setOsmBaseOverlay( int b )
   {
      Editor editor = mSharedPreferences.edit();
      editor.putInt( Constants.OSMBASEOVERLAY, b );
      editor.commit();
   }

   @Override
   public boolean onCreateOptionsMenu( Menu menu )
   {
      boolean result = super.onCreateOptionsMenu( menu );

      menu.add( ContextMenu.NONE, MENU_TRACKING, ContextMenu.NONE, R.string.menu_tracking ).setIcon( R.drawable.ic_menu_movie ).setAlphabeticShortcut( 'T' );
      menu.add( ContextMenu.NONE, MENU_LAYERS, ContextMenu.NONE, R.string.menu_showLayers ).setIcon( R.drawable.ic_menu_mapmode ).setAlphabeticShortcut( 'L' );
      SubMenu notemenu = menu.addSubMenu( ContextMenu.NONE, MENU_NOTE, ContextMenu.NONE, R.string.menu_insertnote ).setIcon( R.drawable.ic_menu_myplaces );
      
      menu.add( ContextMenu.NONE, MENU_STATS, ContextMenu.NONE, R.string.menu_statistics ).setIcon( R.drawable.ic_menu_picture ).setAlphabeticShortcut( 'S' );
      menu.add( ContextMenu.NONE, MENU_SHARE, ContextMenu.NONE, R.string.menu_shareTrack ).setIcon( R.drawable.ic_menu_share ).setAlphabeticShortcut('I');
      // More

      menu.add( ContextMenu.NONE, MENU_TRACKLIST, ContextMenu.NONE, R.string.menu_tracklist ).setIcon( R.drawable.ic_menu_show_list ).setAlphabeticShortcut( 'P' );
      menu.add( ContextMenu.NONE, MENU_SETTINGS, ContextMenu.NONE, R.string.menu_settings ).setIcon( R.drawable.ic_menu_preferences ).setAlphabeticShortcut( 'C' );
      menu.add( ContextMenu.NONE, MENU_ABOUT, ContextMenu.NONE, R.string.menu_about ).setIcon( R.drawable.ic_menu_info_details ).setAlphabeticShortcut( 'A' );
      menu.add( ContextMenu.NONE, MENU_CONTRIB, ContextMenu.NONE, R.string.menu_contrib ).setIcon( R.drawable.ic_menu_allfriends );
      
      notemenu.add( ContextMenu.NONE, MENU_NAME, ContextMenu.NONE, R.string.menu_notename );
      notemenu.add( ContextMenu.NONE, MENU_TEXT, ContextMenu.NONE, R.string.menu_notetext );
      notemenu.add( ContextMenu.NONE, MENU_VOICE, ContextMenu.NONE, R.string.menu_notespeech );
      notemenu.add( ContextMenu.NONE, MENU_PICTURE, ContextMenu.NONE, R.string.menu_notepicture );
      notemenu.add( ContextMenu.NONE, MENU_VIDEO, ContextMenu.NONE, R.string.menu_notevideo );
      
      return result;
   }
   
   /*
    * (non-Javadoc)
    * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
    */
   @Override
   public boolean onPrepareOptionsMenu( Menu menu )
   {
      MenuItem notemenu = menu.findItem( MENU_NOTE );
      notemenu.setEnabled( mLoggerServiceManager.isMediaPrepared() );
      MenuItem sharemenu = menu.findItem( MENU_SHARE );
      sharemenu.setEnabled( mTrackId >= 0 );
      return super.onPrepareOptionsMenu( menu );
   }

   @Override
   public boolean onOptionsItemSelected( MenuItem item )
   {
      boolean handled = false;

      Uri trackUri;
      switch (item.getItemId())
      {
         case MENU_TRACKING:
            Intent controlIntent = new Intent( this, ControlTracking.class );
            startActivityForResult( controlIntent, MENU_TRACKING );
            handled = true;
            break;
         case MENU_LAYERS:
            showDialog( DIALOG_LAYERS );
            handled = true;
            break;
         case MENU_SETTINGS:
            Intent settingsIntent = new Intent( this, SettingsDialog.class );
            startActivity( settingsIntent );
            handled = true;
            break;
         case MENU_TRACKLIST:
            Intent tracklistIntent = new Intent( this, TrackList.class );
            tracklistIntent.putExtra( Tracks._ID, this.mTrackId );
            startActivityForResult( tracklistIntent, MENU_TRACKLIST );
            break;
         case MENU_STATS:
            if( this.mTrackId >= 0 )
            {
               Intent actionIntent = new Intent( this, Statistics.class );
               trackUri = ContentUris.withAppendedId( Tracks.CONTENT_URI, mTrackId );
               actionIntent.setData( trackUri );
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
         case MENU_ABOUT:
            Intent intent = new Intent( "org.openintents.action.SHOW_ABOUT_DIALOG" );
            try
            {
               startActivityForResult( intent, MENU_ABOUT );
            }
            catch (ActivityNotFoundException e)
            {
               showDialog( DIALOG_INSTALL_ABOUT );
            }
            break;
         case MENU_SHARE:
            Intent actionIntent = new Intent( Intent.ACTION_RUN );
            trackUri = ContentUris.withAppendedId( Tracks.CONTENT_URI, mTrackId );
            actionIntent.setDataAndType( trackUri, Tracks.CONTENT_ITEM_TYPE );
            actionIntent.addFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION );
            startActivity( Intent.createChooser( actionIntent, getString( R.string.share_track ) ) );
            handled = true;
            break;
         case MENU_PICTURE:
            addPicture();
            handled = true;
            break;
         case MENU_VIDEO:
            addVideo();
            handled = true;
            break;
         case MENU_VOICE:
            addVoice();
            handled = true;
            break;
         case MENU_TEXT:
            showDialog( DIALOG_TEXT );
            handled = true;
            break;
         case MENU_NAME:
            showDialog( DIALOG_NAME );
            handled = true;
            break;
         case MENU_CONTRIB:
            showDialog( DIALOG_CONTRIB );
         default:
            handled = super.onOptionsItemSelected( item );
            break;
      }
      return handled;
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
         case DIALOG_LAYERS:
            builder = new AlertDialog.Builder( this );
            factory = LayoutInflater.from( this );
            view = factory.inflate( R.layout.layerdialog, null );
                       
            mTraffic   = (CheckBox) view.findViewById( R.id.layer_traffic );
            mSpeed     = (CheckBox) view.findViewById( R.id.layer_speed );
            mAltitude  = (CheckBox) view.findViewById( R.id.layer_altitude );
            mCompass   = (CheckBox) view.findViewById( R.id.layer_compass );
            mLocation  = (CheckBox) view.findViewById( R.id.layer_location );
            
            ( (RadioGroup) view.findViewById( R.id.google_backgrounds ) ).setOnCheckedChangeListener( mGroupCheckedChangeListener );
            ( (RadioGroup) view.findViewById( R.id.osm_backgrounds    ) ).setOnCheckedChangeListener( mGroupCheckedChangeListener );

            mTraffic.setOnCheckedChangeListener( mCheckedChangeListener );
            mSpeed.setOnCheckedChangeListener( mCheckedChangeListener );
            mAltitude.setOnCheckedChangeListener( mCheckedChangeListener );
            mCompass.setOnCheckedChangeListener( mCheckedChangeListener );
            mLocation.setOnCheckedChangeListener( mCheckedChangeListener );

            builder
               .setTitle( R.string.dialog_layer_title )
               .setIcon( android.R.drawable.ic_dialog_map )
               .setPositiveButton( R.string.btn_okay, null )
               .setView( view );
            dialog = builder.create();
            return dialog;
         case DIALOG_NOTRACK:
            builder = new AlertDialog.Builder( this );
            builder
               .setTitle( R.string.dialog_notrack_title )
               .setMessage( R.string.dialog_notrack_message )
               .setIcon( android.R.drawable.ic_dialog_alert )
               .setPositiveButton( R.string.btn_selecttrack, mNoTrackDialogListener )
               .setNegativeButton( R.string.btn_cancel, null );
            dialog = builder.create();
            return dialog;
         case DIALOG_INSTALL_ABOUT:
            builder = new AlertDialog.Builder( this );
            builder
               .setTitle( R.string.dialog_nooiabout )
               .setMessage( R.string.dialog_nooiabout_message )
               .setIcon( android.R.drawable.ic_dialog_alert )
               .setPositiveButton( R.string.btn_install, mOiAboutDialogListener )
               .setNegativeButton( R.string.btn_cancel, null );
            dialog = builder.create();
            return dialog;
         case DIALOG_TEXT:
            builder = new AlertDialog.Builder( this );
            factory = LayoutInflater.from( this );
            view = factory.inflate( R.layout.notetextdialog, null );
            mNoteTextView = (EditText) view.findViewById( R.id.notetext );
            builder
               .setTitle( R.string.dialog_notetext_title )
               .setMessage( R.string.dialog_notetext_message )
               .setIcon( android.R.drawable.ic_dialog_map )
               .setPositiveButton( R.string.btn_okay, mNoteTextDialogListener )
               .setNegativeButton( R.string.btn_cancel, null )
               .setView( view );
            dialog = builder.create();
            return dialog;
         case DIALOG_NAME:
            builder = new AlertDialog.Builder( this );
            factory = LayoutInflater.from( this );
            view = factory.inflate( R.layout.notenamedialog, null );
            mNoteNameView = (EditText) view.findViewById( R.id.notename );
            builder
               .setTitle( R.string.dialog_notename_title )
               .setMessage( R.string.dialog_notename_message )
               .setIcon( android.R.drawable.ic_dialog_map )
               .setPositiveButton( R.string.btn_okay, mNoteNameDialogListener )
               .setNegativeButton( R.string.btn_cancel, null )
               .setView( view );
            dialog = builder.create();
            return dialog;
         case DIALOG_URIS:
            builder = new AlertDialog.Builder( this );
            factory = LayoutInflater.from( this );
            view = factory.inflate( R.layout.mediachooser, null );
            mGallery = (Gallery) view.findViewById( R.id.gallery );
            builder
               .setTitle( R.string.dialog_select_media_title )
               .setMessage( R.string.dialog_select_media_message )
               .setIcon( android.R.drawable.ic_dialog_alert )
               .setNegativeButton( R.string.btn_cancel, null )
               .setPositiveButton( R.string.btn_okay, mNoteSelectDialogListener )
               .setView( view );
            dialog = builder.create();
            return dialog;
         case DIALOG_CONTRIB:
            builder = new AlertDialog.Builder( this );
            factory = LayoutInflater.from( this );
            view = factory.inflate( R.layout.contrib, null );
            TextView contribView = (TextView) view.findViewById( R.id.contrib_view );
            contribView.setText ( R.string.dialog_contrib_message );
            builder
            .setTitle( R.string.dialog_contrib_title )
            .setView( view )
            .setIcon( android.R.drawable.ic_dialog_email )
            .setPositiveButton( R.string.btn_okay, null );
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
      RadioButton satellite;
      RadioButton regular;
      RadioButton cloudmade;
      RadioButton mapnik;
      RadioButton cycle;
      switch (id)
      {
         case DIALOG_LAYERS:
            satellite = (RadioButton) dialog.findViewById( R.id.layer_google_satellite );
            regular   = (RadioButton) dialog.findViewById( R.id.layer_google_regular );
            satellite.setChecked(  mSharedPreferences.getBoolean( Constants.SATELLITE, false ) );
            regular.setChecked(   !mSharedPreferences.getBoolean( Constants.SATELLITE, false ) );
            
            int osmbase = mSharedPreferences.getInt( Constants.OSMBASEOVERLAY, 0 );
            cloudmade = (RadioButton) dialog.findViewById( R.id.layer_osm_cloudmade );
            mapnik    = (RadioButton) dialog.findViewById( R.id.layer_osm_maknik );
            cycle     = (RadioButton) dialog.findViewById( R.id.layer_osm_bicycle);
            cloudmade.setChecked( osmbase == Constants.OSM_CLOUDMADE );
            mapnik.setChecked(    osmbase == Constants.OSM_MAKNIK );
            cycle.setChecked(     osmbase == Constants.OSM_CYCLE );
            
            mTraffic.setChecked( mSharedPreferences.getBoolean( Constants.TRAFFIC, false ) );
            mSpeed.setChecked( mSharedPreferences.getBoolean( Constants.SPEED, false ) );
            mAltitude.setChecked( mSharedPreferences.getBoolean( Constants.ALTITUDE, false ) );
            mCompass.setChecked( mSharedPreferences.getBoolean( Constants.COMPASS, false ) );
            mLocation.setChecked( mSharedPreferences.getBoolean( Constants.LOCATION, false ) );
            int provider = new Integer( mSharedPreferences.getString( Constants.MAPPROVIDER, ""+Constants.GOOGLE ) ).intValue();
            switch( provider )
            {
               case Constants.GOOGLE:
                  dialog.findViewById( R.id.google_backgrounds ).setVisibility( View.VISIBLE );
                  dialog.findViewById( R.id.osm_backgrounds ).setVisibility( View.GONE );
                  dialog.findViewById( R.id.shared_layers ).setVisibility( View.VISIBLE );
                  dialog.findViewById( R.id.google_overlays ).setVisibility( View.VISIBLE );
                  break;
               case Constants.OSM:
                  dialog.findViewById( R.id.osm_backgrounds ).setVisibility( View.VISIBLE );
                  dialog.findViewById( R.id.google_backgrounds ).setVisibility( View.GONE );
                  dialog.findViewById( R.id.shared_layers ).setVisibility( View.VISIBLE );
                  dialog.findViewById( R.id.google_overlays ).setVisibility( View.GONE );
                  break;
               default:
                  Log.e( TAG, "Fault in value "+provider+" as MapProvider." );
                  break;
            }
            break;
         case DIALOG_URIS:
            mGallery.setAdapter( mMediaAdapter );  
         default:
            break;
      }
      super.onPrepareDialog( id, dialog );
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
    */
   @Override
   protected void onActivityResult( int requestCode, int resultCode, Intent intent )
   {
      super.onActivityResult( requestCode, resultCode, intent );
      if( resultCode != RESULT_CANCELED )
      {
         String sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
         File file;
         Uri uri;
         File newFile;
         String newName;
         Uri fileUri;
         android.net.Uri.Builder builder;
         Uri trackUri;
         long trackId;
         switch (requestCode)
         {
            case MENU_TRACKLIST:
               trackUri = intent.getData();
               trackId = Long.parseLong( trackUri.getLastPathSegment() );
               moveToTrack( trackId, true );
               break;
            case MENU_ABOUT:
               break;
            case MENU_PICTURE:
               file = new File( sdcard + Constants.TMPICTUREFILE_PATH );
               Calendar c = Calendar.getInstance();
               newName =  String.format( "Picture_%tY-%tm-%td_%tH%tM%tS.jpg", c, c, c, c, c, c );
               newFile = new File( sdcard + Constants.EXTERNAL_DIR + newName );
               file.getParentFile().mkdirs();
               boolean isRenamed = file.renameTo( newFile );
               if( isRenamed )
               {
                  Bitmap bm = BitmapFactory.decodeFile( newFile.getAbsolutePath() );
                  if( bm != null )
                  {
                     String height = Integer.toString( bm.getHeight() );
                     String width = Integer.toString( bm.getWidth() );
                     bm.recycle();
                     bm = null;
                     builder = new Uri.Builder();
                     fileUri = builder.scheme( "file").
                        appendEncodedPath( "/" ).
                        appendEncodedPath( newFile.getAbsolutePath() ).
                        appendQueryParameter( "width", width ).
                        appendQueryParameter( "height", height )
                        .build();
                     this.mLoggerServiceManager.storeMediaUri( fileUri );
                     mLastSegmentOverlay.calculateMedia();
                     mMapView.postInvalidate();
                  }
                  else
                  {
                     Log.e( TAG, "Failed to read image from: " + newFile.getAbsolutePath() );
                  }
               }
               else
               {
                  Log.e( TAG, "Failed to rename image: " + file.getAbsolutePath() );
               }
               break;
            case MENU_VIDEO:
               file = new File( sdcard + Constants.TMPICTUREFILE_PATH );               
               c = Calendar.getInstance();
               newName =  String.format( "Video_%tY%tm%td_%tH%tM%tS.3gp", c, c, c, c, c, c );
               newFile = new File( sdcard + Constants.EXTERNAL_DIR + newName );
               file.getParentFile().mkdirs();
               file.renameTo( newFile );
               builder = new Uri.Builder();
               fileUri = builder.scheme( "file").
                  appendPath( newFile.getAbsolutePath() ).build();
               this.mLoggerServiceManager.storeMediaUri( fileUri );
               mLastSegmentOverlay.calculateMedia();
               mMapView.postInvalidate();
               break;
            case MENU_VOICE:
               uri = Uri.parse( intent.getDataString() );
               this.mLoggerServiceManager.storeMediaUri(  uri );
               mLastSegmentOverlay.calculateMedia();
               mMapView.postInvalidate();
               break;
            case MENU_TRACKING:
               trackUri = intent.getData();
               if( trackUri != null )
               {
                  trackId = Long.parseLong( trackUri.getLastPathSegment() );
                  moveToTrack( trackId, true );
                  Intent namingIntent = new Intent( this, NameTrack.class );
                  namingIntent.setData( ContentUris.withAppendedId( Tracks.CONTENT_URI, trackId ) );
                  startActivity( namingIntent );
               }
               break;
            default:
               Log.e( TAG, "Returned form unknow activity: " + requestCode );
               break;
         }
      }
      else
      {
         Log.w( TAG, "Received unexpected resultcode "+resultCode);
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
      ContentResolver resolver = this.getContentResolver();
      Cursor trackCursor = null;
      try
      {
         trackCursor = resolver.query( ContentUris.withAppendedId( Tracks.CONTENT_URI, this.mTrackId ), new String[] { Tracks.NAME }, null, null, null );
         if( trackCursor != null && trackCursor.moveToLast() )
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

   private void updateMapProvider()
   {
      int provider = new Integer( mSharedPreferences.getString( Constants.MAPPROVIDER, ""+Constants.GOOGLE ) ).intValue();
      switch( provider )
      {
         case Constants.GOOGLE:
            findViewById( R.id.myOsmMapView ).setVisibility( View.INVISIBLE );
            findViewById( R.id.myMapView ).setVisibility( View.VISIBLE );
            mMapView.setMap( findViewById( R.id.myMapView ) );
            updateGoogleOverlays();
            break;
         case Constants.OSM:
            findViewById( R.id.myMapView ).setVisibility( View.INVISIBLE );
            findViewById( R.id.myOsmMapView ).setVisibility( View.VISIBLE );
            mMapView.setMap( findViewById( R.id.myOsmMapView ) );
            updateOsmBaseOverlay();
            break;
         default:
            Log.e( TAG, "Fault in value "+provider+" as MapProvider." );
            break;
      }
   }
   
   private void updateGoogleOverlays()
   {
      LoggerMap.this.mMapView.setSatellite( mSharedPreferences.getBoolean( Constants.SATELLITE, false ) );
      LoggerMap.this.mMapView.setTraffic( mSharedPreferences.getBoolean( Constants.TRAFFIC, false ) );
   }
   
   private void updateOsmBaseOverlay()
   {
      int baselayer = mSharedPreferences.getInt( Constants.OSMBASEOVERLAY, 0 );
      mMapView.setOSMType( baselayer );
   }

   private void updateBlankingBehavior()
   {
      boolean disableblanking = mSharedPreferences.getBoolean( Constants.DISABLEBLANKING, false );
      if( disableblanking )
      {
         if( mWakeLock == null )
         {
            PowerManager pm = (PowerManager) this.getSystemService( Context.POWER_SERVICE );
            mWakeLock = pm.newWakeLock( PowerManager.SCREEN_DIM_WAKE_LOCK, TAG );
         }
         if( mLoggerServiceManager.getLoggingState() == Constants.LOGGING && !mWakeLock.isHeld() )
         {
            mWakeLock.acquire();
            Log.w( TAG, "Acquired lock to keep screen on!" );
         }
      }
   }

   private void updateSpeedColoring()
   {
      int trackColoringMethod = new Integer( mSharedPreferences.getString( Constants.TRACKCOLORING, "3" ) ).intValue();
      ContentResolver resolver = this.getContentResolver();
      Cursor waypointsCursor = null;
      try
      {
         waypointsCursor = resolver.query( Uri.withAppendedPath( Tracks.CONTENT_URI, this.mTrackId + "/waypoints" )
                                          , new String[] { "avg(" + Waypoints.SPEED + ")", "max(" + Waypoints.SPEED + ")" }
                                          , null
                                          , null
                                          , null );

         if( waypointsCursor != null && waypointsCursor.moveToLast() )
         {
            double average = waypointsCursor.getDouble( 0 );
            double maxBasedAverage = waypointsCursor.getDouble( 1 ) / 2;
            mAverageSpeed = Math.min( average, maxBasedAverage) ;
         }
         if( mAverageSpeed < 2 )
         {
            mAverageSpeed = 5.55d / 2;
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
      if( trackColoringMethod == SegmentOverlay.DRAW_MEASURED || trackColoringMethod == SegmentOverlay.DRAW_CALCULATED )
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
      List< ? > overlays = mMapView.getOverlays();
      for (Object overlay : overlays)
      {
         if( overlay instanceof SegmentOverlay )
         {
            ( (SegmentOverlay) overlay ).setTrackColoringMethod( trackColoringMethod, mAverageSpeed );
         }
      }
   }

   private void updateSpeedDisplayVisibility()
   {
      boolean showspeed = mSharedPreferences.getBoolean( Constants.SPEED, false );
      if( showspeed )
      {
         mLastGPSSpeedView.setVisibility( View.VISIBLE );
         mLastGPSSpeedView.setText( "" );
      }
      else
      {
         mLastGPSSpeedView.setVisibility( View.INVISIBLE );
      }
   }

   private void updateAltitudeDisplayVisibility()
   {
      boolean showaltitude = mSharedPreferences.getBoolean( Constants.ALTITUDE, false );
      if( showaltitude )
      {
         mLastGPSAltitudeView.setVisibility( View.VISIBLE );
         mLastGPSAltitudeView.setText( "" );
      }
      else
      {
         mLastGPSAltitudeView.setVisibility( View.INVISIBLE );
      }
   }
   
   private void updateCompassDisplayVisibility()
   {
      boolean compass = mSharedPreferences.getBoolean( Constants.COMPASS, false );
      if( compass )
      {
         mMylocation.enableCompass();
      }
      else
      {
         mMylocation.disableCompass();
      }
   }

   private void updateLocationDisplayVisibility()
   {
      boolean location = mSharedPreferences.getBoolean( Constants.LOCATION, false );
      if( location )
      {
         mMylocation.enableMyLocation();
      }
      else
      {
         mMylocation.disableMyLocation();
      }
   }

   /**
    * Retrieves the numbers of the measured speed and altitude 
    * from the most recent waypoint and 
    * updates UI components with this latest bit of information. 
    * 
    */
   private void updateDisplayedSpeedViews()
   {
      ContentResolver resolver = this.getContentResolver();
      Cursor waypointsCursor = null;
      try
      {
         Uri lastSegmentUri = Uri.withAppendedPath( Tracks.CONTENT_URI, this.mTrackId + "/segments/" + mLastSegment + "/waypoints" );
         waypointsCursor = resolver.query( lastSegmentUri, new String[] { Waypoints.SPEED, Waypoints.ALTITUDE }, null, null, null );
         if( waypointsCursor != null && waypointsCursor.moveToLast() )
         {
            // Speed number
            double speed = waypointsCursor.getDouble( 0 );
            speed = mUnits.conversionFromMetersPerSecond( speed );
            String speedText = String.format( "%.0f %s", speed, mUnits.getSpeedUnit() );
            mLastGPSSpeedView.setText( speedText );
            
            // Speed color bar
            if( speed > 2*mAverageSpeed )
            {
               updateSpeedColoring();
               mMapView.postInvalidate();
            }
            
            //Altitude number
            double altitude = waypointsCursor.getDouble( 1 );
            altitude = mUnits.conversionFromMeterToHeight( altitude );
            String altitudeText = String.format( "%.0f %s", altitude, mUnits.getHeightUnit() );
            mLastGPSAltitudeView.setText( altitudeText );
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
    * @see SegmentOverlay
    */
   private void createDataOverlays()
   {
      mLastSegmentOverlay = null;
      mMapView.clearOverlays();
      mMapView.addOverlay( mMylocation );

      ContentResolver resolver = this.getContentResolver();
      Cursor segments = null;
      int trackColoringMethod = new Integer( mSharedPreferences.getString( Constants.TRACKCOLORING, "2" ) ).intValue();

      try
      {
         Uri segmentsUri = Uri.withAppendedPath( Tracks.CONTENT_URI, this.mTrackId + "/segments" );
         segments = resolver.query( segmentsUri, new String[] { Segments._ID }, null, null, null );
         if( segments != null && segments.moveToFirst() )
         {
            do
            {
               long segmentsId = segments.getLong( 0 );
               Uri segmentUri = ContentUris.withAppendedId( segmentsUri, segmentsId );
               SegmentOverlay segmentOverlay = new SegmentOverlay( this, segmentUri, trackColoringMethod, mAverageSpeed, this.mMapView );
               mMapView.addOverlay( segmentOverlay );
               mLastSegmentOverlay = segmentOverlay;
               if( segments.isFirst() )
               {
                  segmentOverlay.addPlacement( SegmentOverlay.FIRST_SEGMENT );
               }
               if( segments.isLast() )
               {
                  segmentOverlay.addPlacement( SegmentOverlay.LAST_SEGMENT );
                  getLastTrackPoint();
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
      
      moveActiveViewWindow();

      Uri lastSegmentUri = Uri.withAppendedPath( Tracks.CONTENT_URI, mTrackId+"/segments/"+mLastSegment+"/waypoints" );
      resolver.unregisterContentObserver( this.mSegmentWaypointsObserver );
      resolver.registerContentObserver( lastSegmentUri, false, this.mSegmentWaypointsObserver );
   }
   
   private void updateDataOverlays()
   {
      ContentResolver resolver = this.getContentResolver();
      Uri segmentsUri = Uri.withAppendedPath( Tracks.CONTENT_URI, this.mTrackId + "/segments" );
      Cursor segmentsCursor = null;
      List<?> overlays = this.mMapView.getOverlays();
      int segmentOverlaysCount = 0;
      
      for( Object overlay : overlays )
      {
         if( overlay instanceof SegmentOverlay )
         {
            segmentOverlaysCount++;
         }
      }
      try
      {
         segmentsCursor = resolver.query( segmentsUri, new String[] { Segments._ID }, null, null, null );
         if( segmentsCursor != null && segmentsCursor.getCount() == segmentOverlaysCount )
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
         if( segmentsCursor != null ) { segmentsCursor.close(); }
      }
      moveActiveViewWindow();
   }

   private void moveActiveViewWindow()
   {
      GeoPoint lastPoint =  getLastTrackPoint();
      if( lastPoint != null && mLoggerServiceManager.getLoggingState() == Constants.LOGGING )
      {
         Point out = new Point();
         this.mMapView.getProjection().toPixels( lastPoint, out );
         int height = this.mMapView.getHeight();
         int width = this.mMapView.getWidth();
         if( out.x < 0 || out.y < 0 || out.y > height || out.x > width )
         {
            
            this.mMapView.clearAnimation();
            this.mMapView.getController().setCenter( lastPoint );
//            Log.d( TAG, "mMapView.setCenter()" );
         }
         else if( out.x < width / 4 || out.y < height / 4 || out.x > ( width / 4 ) * 3 || out.y > ( height / 4 ) * 3 )
         {
            this.mMapView.clearAnimation();
            this.mMapView.getController().animateTo( lastPoint );
//            Log.d( TAG, "mMapView.animateTo()" );
         }
      }
   }

   /**
    * @param avgSpeed avgSpeed in m/s
    */
   private void drawSpeedTexts( double avgSpeed )
   {
      avgSpeed = mUnits.conversionFromMetersPerSecond( avgSpeed );
      for (int i = 0; i < mSpeedtexts.length; i++)
      {
         mSpeedtexts[i].setVisibility( View.VISIBLE );
         double speed = ( ( avgSpeed * 2d ) / 5d ) * i;
         String speedText = String.format( "%.0f %s", speed, mUnits.getSpeedUnit() );
         mSpeedtexts[i].setText( speedText );
      }
   }

   /**
    * Alter this to set a new track as current.
    * 
    * @param trackId
    * @param center center on the end of the track
    */
   private void moveToTrack( long trackId, boolean center )
   {
      Cursor track = null;
      try
      {
         ContentResolver resolver = this.getContentResolver();
         Uri trackUri = ContentUris.withAppendedId( Tracks.CONTENT_URI, trackId );
         track = resolver.query( trackUri, new String[] { Tracks.NAME }, null, null, null );
         if( track != null && track.moveToFirst() )
         {
            this.mTrackId = trackId;
            mLastSegment = -1;
            mLastWaypoint = -1;
            resolver.unregisterContentObserver( this.mTrackSegmentsObserver );
            resolver.unregisterContentObserver( this.mTrackMediasObserver );
            Uri tracksegmentsUri = Uri.withAppendedPath( Tracks.CONTENT_URI, trackId+"/segments" );
            
            resolver.registerContentObserver( tracksegmentsUri, false, this.mTrackSegmentsObserver );
            resolver.registerContentObserver( Media.CONTENT_URI, true, this.mTrackMediasObserver );
            
            this.mMapView.clearOverlays();

            updateTitleBar();
            updateDataOverlays();
            updateSpeedColoring();

            if( center )
            {
               GeoPoint lastPoint = getLastTrackPoint();
               this.mMapView.getController().animateTo( lastPoint );
            }
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
      int microLatitude = 0;
      int microLongitude = 0;
      LocationManager locationManager = (LocationManager) this.getApplication().getSystemService( Context.LOCATION_SERVICE );
      Location locationFine = locationManager.getLastKnownLocation( LocationManager.GPS_PROVIDER );
      if( locationFine != null )
      {
         microLatitude = (int) ( locationFine.getLatitude() * 1E6d );
         microLongitude = (int) ( locationFine.getLongitude() * 1E6d );
      }
      if( locationFine == null || microLatitude == 0 || microLongitude == 0 )
      {
         Location locationCoarse = locationManager.getLastKnownLocation( LocationManager.NETWORK_PROVIDER );
         if( locationCoarse != null )
         {
            microLatitude = (int) ( locationCoarse.getLatitude() * 1E6d );
            microLongitude = (int) ( locationCoarse.getLongitude() * 1E6d );
         }
         if( locationCoarse == null || microLatitude == 0 || microLongitude == 0 )
         {
            microLatitude = 51985105;
            microLongitude = 5106132;
         }
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
               "max(" + Waypoints.TABLE + "." + Waypoints._ID + ")" }, null, null, null );
         if( waypoint != null && waypoint.moveToLast() )
         {
            int microLatitude = (int) ( waypoint.getDouble( 0 ) * 1E6d );
            int microLongitude = (int) ( waypoint.getDouble( 1 ) * 1E6d );
            lastPoint = new GeoPoint( microLatitude, microLongitude );
         }
         if( lastPoint == null || lastPoint.getLatitudeE6() == 0 || lastPoint.getLongitudeE6() == 0 )
         {
            lastPoint = getLastKnowGeopointLocation();
         }
         else
         {
            mLastWaypoint = waypoint.getLong( 2 );
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
         ContentResolver resolver = this.getContentResolver();
         track = resolver.query( Tracks.CONTENT_URI, new String[] { "max(" + Tracks._ID + ")", Tracks.NAME, }, null, null, null );
         if( track != null && track.moveToLast() )
         {
            trackId = track.getInt( 0 );
            moveToTrack( trackId, true );
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

   /***
    * Collecting additional data
    */
   private void addPicture()
   {
      Intent i = new Intent( android.provider.MediaStore.ACTION_IMAGE_CAPTURE );
      File file = new File( Environment.getExternalStorageDirectory().getAbsolutePath() + Constants.TMPICTUREFILE_PATH );
//      Log.d( TAG, "Picture requested at: " + file );
      i.putExtra( android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile( file ) );
      i.putExtra( android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 1 );
      startActivityForResult( i, MENU_PICTURE );
   }

   /***
    * Collecting additional data
    */
   private void addVideo()
   {
      Intent i = new Intent( android.provider.MediaStore.ACTION_VIDEO_CAPTURE );
      File file = new File( Environment.getExternalStorageDirectory().getAbsolutePath() + Constants.TMPICTUREFILE_PATH );
//      Log.d( TAG, "Video requested at: " + file );
      i.putExtra( android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile( file ) );
      i.putExtra( android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 1 );
      try
      {
         startActivityForResult( i, MENU_VIDEO );
      }
      catch (ActivityNotFoundException e) 
      {
         Log.e(  TAG, "Unable to start Activity to record video", e );
      }
   }

   private void addVoice()
   {
      Intent intent = new Intent( android.provider.MediaStore.Audio.Media.RECORD_SOUND_ACTION );
      try
      {
         startActivityForResult( intent, MENU_VOICE );
      }
      catch (ActivityNotFoundException e) 
      {
         Log.e(  TAG, "Unable to start Activity to record audio", e );
      }
   }

   /**
    * Enables a SegmentOverlay to call back to the MapActivity to show a dialog with choices of media
    * 
    * @param mediaAdapter
    */
   public void showDialog( BaseAdapter mediaAdapter )
   {
      mMediaAdapter = mediaAdapter;
      showDialog( LoggerMap.DIALOG_URIS );
   }
}