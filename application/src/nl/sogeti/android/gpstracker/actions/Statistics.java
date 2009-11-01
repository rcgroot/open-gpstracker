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
   private TextView avgSpeed;
   private TextView distance;
   private TextView endtime;
   private TextView starttime;
   private TextView trackname;
   private TextView maxSpeed;
   private TextView waypoints;
   
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
      
      maxSpeed = (TextView)findViewById( R.id.stat_maximumspeed );
      avgSpeed = (TextView)findViewById( R.id.stat_averagespeed );
      distance = (TextView)findViewById( R.id.stat_distance );
      starttime = (TextView)findViewById( R.id.stat_starttime );
      endtime = (TextView)findViewById( R.id.stat_endtime );
      trackname = (TextView)findViewById( R.id.stat_trackname );
      waypoints  = (TextView)findViewById( R.id.stat_waypoints );
      
      ContentResolver resolver = this.getApplicationContext().getContentResolver();
      resolver.registerContentObserver( mTrackUri, false, this.mTrackObserver );
      
      drawTrackingStatistics();
   }
   
   private void drawTrackingStatistics()
   {
      String avgSpeedText = "Unknown";
      String maxSpeedText = "Unknown";
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
      
      String speed_unit = this.getResources().getString( R.string.speed_unitname );
      String distance_unit = this.getResources().getString( R.string.distance_unitname );
      
      ContentResolver resolver = this.getApplicationContext().getContentResolver();
      
      double maxSpeeddb = 0;
      Cursor waypointsCursor = null ;
      try
      {
         waypointsCursor = resolver.query
               ( Uri.withAppendedPath( mTrackUri, "waypoints" )
               , new String[] { "max("+Waypoints.TABLE+"."+Waypoints.SPEED+")" }
               , null
               , null
               , null );
         if( waypointsCursor.moveToLast() )
         {
            maxSpeeddb = waypointsCursor.getDouble( 0 );            
            maxSpeeddb = maxSpeeddb *  conversion_from_mps;
         }
      }
      finally
      {
         if( waypointsCursor != null )
         {
            waypointsCursor.close();
         }
      }
      try
      {
         waypointsCursor = resolver.query
               ( Uri.withAppendedPath( mTrackUri, "waypoints" )
               , new String[] { "count("+Waypoints.TABLE+"."+Waypoints._ID+")" }
               , null
               , null
               , null );
         if( waypointsCursor.moveToLast() )
         {
            long avgSpeed = waypointsCursor.getLong(  0 );            
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
               , new String[] { Tracks.NAME, Tracks.CREATION_TIME }
               , null
               , null
               , null );
         if( trackCursor.moveToLast() )
         {
            tracknameText = trackCursor.getString( 0 );
            starttimeText = trackCursor.getLong( 1 );
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
                        currentLocation = new Location( this.getClass().getName() );
                        endtimeText = waypoints.getLong( 1 );
                        currentLocation.setLongitude( waypoints.getDouble( 2 ) );
                        currentLocation.setLatitude( waypoints.getDouble( 3 ) );
                        if( lastLocation != null )
                        {
                           distanceTraveled += lastLocation.distanceTo( currentLocation );
                        }
                        lastLocation = currentLocation;
                     }
                     while( waypoints.moveToNext());
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
      
      long roundingTmp;
      float avgSpeedfl = (distanceTraveled * conversion_from_meter)/((endtimeText-starttimeText)/3600000f) ;
      roundingTmp = (long) (avgSpeedfl * 100);
      avgSpeedText =  roundingTmp / 100f +" "+speed_unit;
      
      roundingTmp = (long) (distanceTraveled * conversion_from_meter * 100);
      distanceTraveled = roundingTmp / 100f;
      distanceText =  distanceTraveled + " "+distance_unit;
      
      roundingTmp = (long) (maxSpeeddb * 100);
      maxSpeedText = roundingTmp / 100f+" "+speed_unit;
      
      maxSpeed.setText( maxSpeedText );
      avgSpeed.setText( avgSpeedText );
      distance.setText( distanceText );
      starttime.setText( Long.toString( starttimeText ) );
      endtime.setText( Long.toString( endtimeText ) );
      trackname.setText( tracknameText );
      waypoints.setText( waypointsText );
   }

}
