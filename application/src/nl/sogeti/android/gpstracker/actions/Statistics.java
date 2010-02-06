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
import android.app.Activity;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * Display some calulations based on a track
 *
 * @version $Id$
 * @author rene (c) Oct 19, 2009, Sogeti B.V.
 */
public class Statistics extends Activity
{
   
   private final ContentObserver mTrackObserver = new ContentObserver(new Handler()) 
   {
      @Override
      public void onChange(boolean selfUpdate) 
      {
         Statistics.this.drawTrackingStatistics();
      }
   };
   
   Uri mTrackUri = null;
   private TextView overallavgSpeedView;
   private TextView avgSpeedView;
   private TextView distanceView;
   private TextView endtimeView;
   private TextView starttimeView;
   private TextView tracknameView;
   private TextView maxSpeedView;
   private TextView waypointsView;
   private TextView minAltitudeView;
   private TextView maxAltitudeView;

   private UnitsI18n mUnits;

   private GraphCanvas graphView;
   
   /** 
    * Called when the activity is first created. 
    *
    */
   @Override
   protected void onCreate( Bundle load )
   {
      super.onCreate( load );
      mUnits = new UnitsI18n( this );
      setContentView( R.layout.statistics );
      this.mTrackUri = this.getIntent().getData() ;
      
      graphView = (GraphCanvas) findViewById( R.id.graph_canvas );
      
      maxSpeedView = (TextView)findViewById( R.id.stat_maximumspeed );
      minAltitudeView = (TextView)findViewById( R.id.stat_minimalaltitide );
      maxAltitudeView = (TextView)findViewById( R.id.stat_maximumaltitude );
      overallavgSpeedView = (TextView)findViewById( R.id.stat_overallaveragespeed );
      avgSpeedView = (TextView)findViewById( R.id.stat_averagespeed );
      distanceView = (TextView)findViewById( R.id.stat_distance );
      starttimeView = (TextView)findViewById( R.id.stat_starttime );
      endtimeView = (TextView)findViewById( R.id.stat_endtime );
      tracknameView = (TextView)findViewById( R.id.stat_trackname );
      waypointsView  = (TextView)findViewById( R.id.stat_waypoints );     
      
      drawTrackingStatistics();
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



   private void drawTrackingStatistics()
   {
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

      Cursor waypointsCursor = null ;
      try
      {
         waypointsCursor = resolver.query
               ( Uri.withAppendedPath( mTrackUri, "waypoints" )
               , new String[] { "max  ("+Waypoints.TABLE+"."+Waypoints.SPEED   +")", 
                                "max  ("+Waypoints.TABLE+"."+Waypoints.ALTITUDE+")", 
                                "min  ("+Waypoints.TABLE+"."+Waypoints.ALTITUDE+")", 
                                "count("+Waypoints.TABLE+"."+Waypoints._ID     +")" }
               , null
               , null
               , null );
         if( waypointsCursor.moveToLast() )
         {
            maxSpeeddb = mUnits.conversionFromMetersPerSecond( waypointsCursor.getDouble( 0 ) );
            maxAltitude = mUnits.conversionFromMeterToSmall( waypointsCursor.getDouble( 1 ) );
            minAltitude = mUnits.conversionFromMeterToSmall( waypointsCursor.getDouble( 2 ) );
            long nrWaypoints = waypointsCursor.getLong(  3 );            
            waypointsText = nrWaypoints+"";
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
               ( mTrackUri
               , new String[] { Tracks.NAME }
               , null
               , null
               , null );
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
         segments = resolver.query( 
               segmentsUri, 
               new String[] { Segments._ID }, 
               null, null, null );
         if(segments.moveToFirst())
         {
            do 
            {
               long segmentsId = segments.getLong( 0 );
               Cursor waypoints = null;
               try 
               {
                  Uri waypointsUri = Uri.withAppendedPath( segmentsUri, segmentsId+"/waypoints" );
                  waypoints = resolver.query( 
                        waypointsUri, 
                        new String[] { Waypoints._ID, Waypoints.TIME, Waypoints.LONGITUDE, Waypoints.LATITUDE }, 
                        null, null, null );
                  if(waypoints.moveToFirst())
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
                     while( waypoints.moveToNext());
                     endtime = lastLocation.getTime();
                     overallduration = endtime-starttime;
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
      
      graphView.setData( mTrackUri, starttime, endtime, distanceTraveled, mUnits );
            
      double overallavgSpeedfl = mUnits.conversionFromMeterAndMiliseconds( distanceTraveled, overallduration );
      double avgSpeedfl        = mUnits.conversionFromMeterAndMiliseconds( distanceTraveled, duration );
      distanceTraveled         = mUnits.conversionFromMeter( distanceTraveled );
      avgSpeedText        = String.format( "%.2f %s", avgSpeedfl, mUnits.getSpeedUnit() );
      overallavgSpeedText = String.format( "%.2f %s", overallavgSpeedfl,  mUnits.getSpeedUnit() );
      distanceText        = String.format( "%.2f %s", distanceTraveled, mUnits.getDistanceUnit() );
      maxSpeedText        = String.format( "%.2f %s", maxSpeeddb, mUnits.getSpeedUnit() );
      minAltitudeText     = String.format( "%.0f %s", minAltitude, mUnits.getDistanceSmallUnit() );
      maxAltitudeText     = String.format( "%.0f %s", maxAltitude, mUnits.getDistanceSmallUnit() );
      
      maxSpeedView.setText( maxSpeedText );
      maxAltitudeView.setText( maxAltitudeText );
      minAltitudeView.setText( minAltitudeText );
      overallavgSpeedView.setText( overallavgSpeedText );
      avgSpeedView.setText( avgSpeedText );
      distanceView.setText( distanceText );
      starttimeView.setText( Long.toString( starttime ) );
      endtimeView.setText( Long.toString( endtime ) );
      tracknameView.setText( tracknameText );
      waypointsView.setText( waypointsText );
   }

}
