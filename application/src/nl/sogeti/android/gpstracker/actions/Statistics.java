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
package nl.sogeti.android.gpstracker.actions;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.utils.GraphCanvas;
import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.util.UnitsI18n;
import nl.sogeti.android.gpstracker.viewer.TrackList;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewFlipper;

/**
 * Display some calulations based on a track
 * 
 * @version $Id$
 * @author rene (c) Oct 19, 2009, Sogeti B.V.
 */
public class Statistics extends Activity
{

   private static final int DIALOG_GRAPHTYPE = 3;
   private static final int MENU_GRAPHTYPE = 11;
   private static final int MENU_TRACKLIST = 12;
   private static final int MENU_SHARE = 41;
   private static final String TRACKURI = "TRACKURI";
   @SuppressWarnings("unused")
   private static final String TAG = "Statistics";

   private final ContentObserver mTrackObserver = new ContentObserver( new Handler() )
      {

         @Override
         public void onChange( boolean selfUpdate )
         {
            if( !calculating )
            {
               Statistics.this.drawTrackingStatistics();
            }
         }
      };
   private static final int SWIPE_MIN_DISTANCE = 120;
   private static final int SWIPE_MAX_OFF_PATH = 250;
   private static final int SWIPE_THRESHOLD_VELOCITY = 200;

   class MyGestureDetector extends SimpleOnGestureListener
   {
      @Override
      public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY )
      {
         if( Math.abs( e1.getY() - e2.getY() ) > SWIPE_MAX_OFF_PATH )
            return false;
         // right to left swipe
         if( e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs( velocityX ) > SWIPE_THRESHOLD_VELOCITY )
         {
            mViewFlipper.setInAnimation( mSlideLeftIn );
            mViewFlipper.setOutAnimation( mSlideLeftOut );
            mViewFlipper.showNext();
         }
         else if( e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs( velocityX ) > SWIPE_THRESHOLD_VELOCITY )
         {
            mViewFlipper.setInAnimation( mSlideRightIn );
            mViewFlipper.setOutAnimation( mSlideRightOut );
            mViewFlipper.showPrevious();
         }
         return false;
      }
   }

   private Uri mTrackUri = null;
   private boolean calculating;
   private TextView overallavgSpeedView;
   private TextView avgSpeedView;
   private TextView distanceView;
   private TextView endtimeView;
   private TextView starttimeView;
   private TextView maxSpeedView;
   private TextView waypointsView;
   private TextView minAltitudeView;
   private TextView maxAltitudeView;

   private UnitsI18n mUnits;
   private GraphCanvas mGraphView1;

   private OnClickListener mGraphControlListener = new View.OnClickListener()
      {
         public void onClick( View v )
         {
            int id = v.getId();
            switch( id )
            {
               case R.id.graphtype_timespeed:
                  mViewFlipper.setDisplayedChild( 0 );
                  break;
               case R.id.graphtype_timealtitude:
                  mViewFlipper.setDisplayedChild( 1 );
                  break;
               case R.id.graphtype_distancespeed:
                  mViewFlipper.setDisplayedChild( 2 );
                  break;
               case R.id.graphtype_distancealtitude:
                  mViewFlipper.setDisplayedChild( 3 );
                  break;
               default:
                  break;
            }
            dismissDialog( DIALOG_GRAPHTYPE );
         }
      };
   private ViewFlipper mViewFlipper;
   private Animation mSlideLeftIn;
   private Animation mSlideLeftOut;
   private Animation mSlideRightIn;
   private Animation mSlideRightOut;
   private GestureDetector mGestureDetector;
   private GraphCanvas mGraphView2;
   private GraphCanvas mGraphView3;
   private GraphCanvas mGraphView4;

   /**
    * Called when the activity is first created.
    */
   @Override
   protected void onCreate( Bundle load )
   {
      super.onCreate( load );
      mUnits = new UnitsI18n( this, new UnitsI18n.UnitsChangeListener()
         {
            public void onUnitsChange()
            {
               drawTrackingStatistics();
            }
         } );
      setContentView( R.layout.statistics );

      mViewFlipper = (ViewFlipper) findViewById( R.id.flipper );
      mSlideLeftIn = AnimationUtils.loadAnimation( this, R.anim.slide_left_in );
      mSlideLeftOut = AnimationUtils.loadAnimation( this, R.anim.slide_left_out );
      mSlideRightIn = AnimationUtils.loadAnimation( this, R.anim.slide_right_in );
      mSlideRightOut = AnimationUtils.loadAnimation( this, R.anim.slide_right_out );

      mGraphView1 = (GraphCanvas) findViewById( R.id.graph_canvas1 );
      mGraphView2 = (GraphCanvas) findViewById( R.id.graph_canvas2 );
      mGraphView3 = (GraphCanvas) findViewById( R.id.graph_canvas3 );
      mGraphView4 = (GraphCanvas) findViewById( R.id.graph_canvas4 );
      mGraphView1.setType( GraphCanvas.TIMESPEEDGRAPH );
      mGraphView2.setType( GraphCanvas.DISTANCESPEEDGRAPH );
      mGraphView3.setType( GraphCanvas.TIMEALTITUDEGRAPH );
      mGraphView4.setType( GraphCanvas.DISTANCEALTITUDEGRAPH );

      mGestureDetector = new GestureDetector( new MyGestureDetector() );

      maxSpeedView = (TextView) findViewById( R.id.stat_maximumspeed );
      minAltitudeView = (TextView) findViewById( R.id.stat_minimalaltitide );
      maxAltitudeView = (TextView) findViewById( R.id.stat_maximumaltitude );
      overallavgSpeedView = (TextView) findViewById( R.id.stat_overallaveragespeed );
      avgSpeedView = (TextView) findViewById( R.id.stat_averagespeed );
      distanceView = (TextView) findViewById( R.id.stat_distance );
      starttimeView = (TextView) findViewById( R.id.stat_starttime );
      endtimeView = (TextView) findViewById( R.id.stat_endtime );
      waypointsView = (TextView) findViewById( R.id.stat_waypoints );

      if( load != null && load.containsKey( TRACKURI ) )
      {
         mTrackUri = Uri.withAppendedPath( Tracks.CONTENT_URI, load.getString( TRACKURI ) );
      }
      else
      {
         mTrackUri = this.getIntent().getData();
      }
      drawTrackingStatistics();
   }

   @Override
   protected void onRestoreInstanceState( Bundle load )
   {
      if( load != null )
      {
         super.onRestoreInstanceState( load );
      }
      if( load != null && load.containsKey( TRACKURI ) )
      {
         mTrackUri = Uri.withAppendedPath( Tracks.CONTENT_URI, load.getString( TRACKURI ) );
      }
   }

   @Override
   protected void onSaveInstanceState( Bundle save )
   {
      super.onSaveInstanceState( save );
      save.putString( TRACKURI, mTrackUri.getLastPathSegment() );
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onPause()
    */
   @Override
   protected void onPause()
   {
      super.onPause();
      ContentResolver resolver = this.getApplicationContext().getContentResolver();
      resolver.unregisterContentObserver( this.mTrackObserver );
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onResume()
    */
   @Override
   protected void onResume()
   {
      super.onResume();
      ContentResolver resolver = this.getApplicationContext().getContentResolver();
      resolver.unregisterContentObserver( this.mTrackObserver );
      resolver.registerContentObserver( mTrackUri, true, this.mTrackObserver );
   }

   @Override
   public boolean onCreateOptionsMenu( Menu menu )
   {
      boolean result = super.onCreateOptionsMenu( menu );
      menu.add( ContextMenu.NONE, MENU_GRAPHTYPE, ContextMenu.NONE, R.string.menu_graphtype ).setIcon( R.drawable.ic_menu_picture ).setAlphabeticShortcut( 't' );
      menu.add( ContextMenu.NONE, MENU_TRACKLIST, ContextMenu.NONE, R.string.menu_tracklist ).setIcon( R.drawable.ic_menu_show_list ).setAlphabeticShortcut( 'l' );
      menu.add( ContextMenu.NONE, MENU_SHARE, ContextMenu.NONE, R.string.menu_shareTrack ).setIcon( R.drawable.ic_menu_share ).setAlphabeticShortcut( 's' );
      return result;
   }

   @Override
   public boolean onOptionsItemSelected( MenuItem item )
   {
      boolean handled = false;
      switch( item.getItemId() )
      {
         case MENU_GRAPHTYPE:
            showDialog( DIALOG_GRAPHTYPE );
            handled = true;
            break;
         case MENU_TRACKLIST:
            Intent tracklistIntent = new Intent( this, TrackList.class );
            tracklistIntent.putExtra( Tracks._ID, mTrackUri.getLastPathSegment() );
            startActivityForResult( tracklistIntent, MENU_TRACKLIST );
            break;
         case MENU_SHARE:
            Intent actionIntent = new Intent( Intent.ACTION_RUN );
            actionIntent.setDataAndType( mTrackUri, Tracks.CONTENT_ITEM_TYPE );
            actionIntent.addFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION );
            startActivity( Intent.createChooser( actionIntent, getString( R.string.chooser_title ) ) );
            handled = true;
            break;
         default:
            handled = super.onOptionsItemSelected( item );
      }
      return handled;
   }

   @Override
   public boolean onTouchEvent( MotionEvent event )
   {
      if( mGestureDetector.onTouchEvent( event ) )
         return true;
      else
         return false;
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
         switch( requestCode )
         {
            case MENU_TRACKLIST:
               mTrackUri = intent.getData();
               drawTrackingStatistics();
               break;
         }
      }
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
      switch( id )
      {
         case DIALOG_GRAPHTYPE:
            builder = new AlertDialog.Builder( this );
            factory = LayoutInflater.from( this );
            view = factory.inflate( R.layout.graphtype, null );
            builder.setTitle( R.string.dialog_graphtype_title ).setIcon( android.R.drawable.ic_dialog_alert ).setNegativeButton( R.string.btn_cancel, null ).setView( view );
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
      switch( id )
      {
         case DIALOG_GRAPHTYPE:
            Button speedtime = (Button) dialog.findViewById( R.id.graphtype_timespeed );
            Button speeddistance = (Button) dialog.findViewById( R.id.graphtype_distancespeed );
            Button altitudetime = (Button) dialog.findViewById( R.id.graphtype_timealtitude );
            Button altitudedistance = (Button) dialog.findViewById( R.id.graphtype_distancealtitude );
            speedtime.setOnClickListener( mGraphControlListener );
            speeddistance.setOnClickListener( mGraphControlListener );
            altitudetime.setOnClickListener( mGraphControlListener );
            altitudedistance.setOnClickListener( mGraphControlListener );
         default:
            break;
      }
      super.onPrepareDialog( id, dialog );
   }

   private void drawTrackingStatistics()
   {
      calculating = true;
      String overallavgSpeedText = "Unknown";
      String avgSpeedText = "Unknown";
      String maxSpeedText = "Unknown";
      String maxAltitudeText = "Unknown";
      String minAltitudeText = "Unknown";
      String tracknameText = "Unknown";
      String waypointsText = "Unknown";
      String distanceText = "Unknown";
      long starttime = 0;
      long endtime = 0;
      double maxSpeeddb = 0;
      double maxAltitude = 0;
      double minAltitude = 0;
      double distanceTraveled = 0f;
      long duration = 1;
      long overallduration = 1;

      ContentResolver resolver = this.getApplicationContext().getContentResolver();

      Cursor waypointsCursor = null;
      try
      {
         waypointsCursor = resolver.query( Uri.withAppendedPath( mTrackUri, "waypoints" ), new String[] { "max  (" + Waypoints.TABLE + "." + Waypoints.SPEED + ")",
               "max  (" + Waypoints.TABLE + "." + Waypoints.ALTITUDE + ")", "min  (" + Waypoints.TABLE + "." + Waypoints.ALTITUDE + ")", "count(" + Waypoints.TABLE + "." + Waypoints._ID + ")" },
               null, null, null );
         if( waypointsCursor.moveToLast() )
         {
            maxSpeeddb = waypointsCursor.getDouble( 0 );
            maxAltitude = waypointsCursor.getDouble( 1 );
            minAltitude = waypointsCursor.getDouble( 2 );
            long nrWaypoints = waypointsCursor.getLong( 3 );
            waypointsText = nrWaypoints + "";
         }
      }
      finally
      {
         if( waypointsCursor != null )
         {
            waypointsCursor.close();
         }
      }
      Cursor trackCursor = null;
      try
      {
         trackCursor = resolver.query( mTrackUri, new String[] { Tracks.NAME }, null, null, null );
         if( trackCursor.moveToLast() )
         {
            tracknameText = trackCursor.getString( 0 );
         }
      }
      finally
      {
         if( trackCursor != null )
         {
            trackCursor.close();
         }
      }
      Cursor segments = null;
      Location lastLocation = null;
      Location currentLocation = null;
      try
      {
         Uri segmentsUri = Uri.withAppendedPath( this.mTrackUri, "segments" );
         segments = resolver.query( segmentsUri, new String[] { Segments._ID }, null, null, null );
         if( segments.moveToFirst() )
         {
            do
            {
               long segmentsId = segments.getLong( 0 );
               Cursor waypoints = null;
               try
               {
                  Uri waypointsUri = Uri.withAppendedPath( segmentsUri, segmentsId + "/waypoints" );
                  waypoints = resolver.query( waypointsUri, new String[] { Waypoints._ID, Waypoints.TIME, Waypoints.LONGITUDE, Waypoints.LATITUDE }, null, null, null );
                  if( waypoints.moveToFirst() )
                  {
                     do
                     {
                        if( starttime == 0 )
                        {
                           starttime = waypoints.getLong( 1 );
                        }
                        currentLocation = new Location( this.getClass().getName() );
                        currentLocation.setTime( waypoints.getLong( 1 ) );
                        currentLocation.setLongitude( waypoints.getDouble( 2 ) );
                        currentLocation.setLatitude( waypoints.getDouble( 3 ) );
                        if( lastLocation != null )
                        {
                           distanceTraveled += lastLocation.distanceTo( currentLocation );
                           duration += currentLocation.getTime() - lastLocation.getTime();
                        }
                        lastLocation = currentLocation;

                     }
                     while( waypoints.moveToNext() );
                     endtime = lastLocation.getTime();
                     overallduration = endtime - starttime;
                  }
               }
               finally
               {
                  if( waypoints != null )
                  {
                     waypoints.close();
                  }
               }
               lastLocation = null;
            }
            while( segments.moveToNext() );
         }
      }
      finally
      {
         if( segments != null )
         {
            segments.close();
         }
      }

      mGraphView1.setData( mTrackUri, starttime, endtime, distanceTraveled, minAltitude, maxAltitude, maxSpeeddb, mUnits );
      mGraphView2.setData( mTrackUri, starttime, endtime, distanceTraveled, minAltitude, maxAltitude, maxSpeeddb, mUnits );
      mGraphView3.setData( mTrackUri, starttime, endtime, distanceTraveled, minAltitude, maxAltitude, maxSpeeddb, mUnits );
      mGraphView4.setData( mTrackUri, starttime, endtime, distanceTraveled, minAltitude, maxAltitude, maxSpeeddb, mUnits );

      maxSpeeddb = mUnits.conversionFromMetersPerSecond( maxSpeeddb );
      maxAltitude = mUnits.conversionFromMeterToSmall( maxAltitude );
      minAltitude = mUnits.conversionFromMeterToSmall( minAltitude );
      double overallavgSpeedfl = mUnits.conversionFromMeterAndMiliseconds( distanceTraveled, overallduration );
      double avgSpeedfl = mUnits.conversionFromMeterAndMiliseconds( distanceTraveled, duration );
      distanceTraveled = mUnits.conversionFromMeter( distanceTraveled );
      avgSpeedText = String.format( "%.2f %s", avgSpeedfl, mUnits.getSpeedUnit() );
      overallavgSpeedText = String.format( "%.2f %s", overallavgSpeedfl, mUnits.getSpeedUnit() );
      distanceText = String.format( "%.2f %s", distanceTraveled, mUnits.getDistanceUnit() );
      maxSpeedText = String.format( "%.2f %s", maxSpeeddb, mUnits.getSpeedUnit() );
      minAltitudeText = String.format( "%.0f %s", minAltitude, mUnits.getDistanceSmallUnit() );
      maxAltitudeText = String.format( "%.0f %s", maxAltitude, mUnits.getDistanceSmallUnit() );

      maxSpeedView.setText( maxSpeedText );
      maxAltitudeView.setText( maxAltitudeText );
      minAltitudeView.setText( minAltitudeText );
      overallavgSpeedView.setText( overallavgSpeedText );
      avgSpeedView.setText( avgSpeedText );
      distanceView.setText( distanceText );
      starttimeView.setText( Long.toString( starttime ) );
      endtimeView.setText( Long.toString( endtime ) );
      String titleFormat = getString( R.string.stat_title );
      setTitle( String.format( titleFormat, tracknameText ) );
      waypointsView.setText( waypointsText );

      calculating = false;
   }
}
