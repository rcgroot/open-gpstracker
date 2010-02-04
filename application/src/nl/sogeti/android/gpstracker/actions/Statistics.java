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
   private TextView overallavgSpeed;
   private TextView avgSpeed;
   private TextView distance;
   private TextView endtime;
   private TextView starttime;
   private TextView trackname;
   private TextView maxSpeed;
   private TextView waypoints;
   private TextView minAltitude;
   private TextView maxAltitude;
   
   /** 
    * Called when the activity is first created. 
    *
    */
   @Override
   protected void onCreate( Bundle load )
   {
      super.onCreate( load );
      setContentView( R.layout.statistics );
      this.mTrackUri = this.getIntent().getData() ;
      
      GraphCanvas graph = (GraphCanvas) findViewById( R.id.graph_canvas );
      graph.setUri( mTrackUri );
      
      maxSpeed = (TextView)findViewById( R.id.stat_maximumspeed );
      minAltitude = (TextView)findViewById( R.id.stat_minimalaltitide );
      maxAltitude = (TextView)findViewById( R.id.stat_maximumaltitude );
      overallavgSpeed = (TextView)findViewById( R.id.stat_overallaveragespeed );
      avgSpeed = (TextView)findViewById( R.id.stat_averagespeed );
      distance = (TextView)findViewById( R.id.stat_distance );
      starttime = (TextView)findViewById( R.id.stat_starttime );
      endtime = (TextView)findViewById( R.id.stat_endtime );
      trackname = (TextView)findViewById( R.id.stat_trackname );
      waypoints  = (TextView)findViewById( R.id.stat_waypoints );     
      
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
      long starttimeText = 0;
      long endtimeText = 0;
      String waypointsText = "Unknown";
      String distanceText = "Unknown";
      
      TypedValue outValue = new TypedValue();
      this.getResources().getValue( R.raw.conversion_from_mps, outValue, false ) ;
      float conversion_from_mps =  outValue.getFloat();
      
      this.getResources().getValue( R.raw.conversion_from_meter, outValue, false ) ;
      float conversion_from_meter = outValue.getFloat();
      
      this.getResources().getValue( R.raw.conversion_from_meter_to_small, outValue, false ) ;
      float conversion_from_meter_to_small = outValue.getFloat();
      
      String speed_unit = this.getResources().getString( R.string.speed_unitname );
      String distance_unit = this.getResources().getString( R.string.distance_unitname );
      String distance_smallunit = this.getResources().getString( R.string.distance_smallunitname );
      
      ContentResolver resolver = this.getApplicationContext().getContentResolver();
      
      double maxSpeeddb = 0;
      double maxalti = 0;
      double minalti = 0;
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
            maxSpeeddb =  waypointsCursor.getDouble( 0 ) *  conversion_from_mps;
            maxalti = waypointsCursor.getDouble( 1 ) *  conversion_from_meter_to_small;
            minalti = waypointsCursor.getDouble( 2 ) *  conversion_from_meter_to_small;
            long avgSpeed = waypointsCursor.getLong(  3 );            
            waypointsText = avgSpeed+"";
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
      float distanceTraveled = 0f;
      long duration = 1;
      long overallduration = 1;
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
                        if( starttimeText == 0 )
                        {
                           starttimeText = waypoints.getLong( 1 );
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
                     endtimeText = lastLocation.getTime();
                     overallduration = endtimeText-starttimeText;
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
            
      float overallavgSpeedfl = (distanceTraveled * conversion_from_meter)/(overallduration/3600000f) ;
      float avgSpeedfl = (distanceTraveled * conversion_from_meter)/(duration/3600000f) ;
      avgSpeedText = String.format( "%.2f", avgSpeedfl )+" "+speed_unit;
      overallavgSpeedText = String.format( "%.2f", overallavgSpeedfl )+" "+speed_unit;
      distanceText =  String.format( "%.2f", distanceTraveled * conversion_from_meter )+" "+distance_unit;
      maxSpeedText = String.format( "%.2f", maxSpeeddb )+" "+speed_unit;
      minAltitudeText = String.format( "%.1f", minalti )+" "+distance_smallunit;
      maxAltitudeText = String.format( "%.1f", maxalti )+" "+distance_smallunit;
      
      
      maxSpeed.setText( maxSpeedText );
      maxAltitude.setText( maxAltitudeText );
      minAltitude.setText( minAltitudeText );
      overallavgSpeed.setText( overallavgSpeedText );
      avgSpeed.setText( avgSpeedText );
      distance.setText( distanceText );
      starttime.setText( Long.toString( starttimeText ) );
      endtime.setText( Long.toString( endtimeText ) );
      trackname.setText( tracknameText );
      waypoints.setText( waypointsText );
   }

}
