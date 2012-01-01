/*------------------------------------------------------------------------------
 **     Ident: Sogeti Smart Mobile Solutions
 **    Author: rene
 ** Copyright: (c) Apr 24, 2011 Sogeti Nederland B.V. All Rights Reserved.
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
package nl.sogeti.android.gpstracker.actions.utils;

import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.UnitsI18n;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;

public class StatisticsCalulator extends AsyncTask<Uri, Void, Void>
{

   private static final String TAG = "OGT.StatisticsCalulator";
   private Context mContext;   
   private String overallavgSpeedText = "Unknown";
   private String avgSpeedText = "Unknown";
   private String maxSpeedText = "Unknown";
   private String ascensionText = "Unknown";
   private String minSpeedText = "Unknown";
   private String tracknameText = "Unknown";
   private String waypointsText = "Unknown";
   private String distanceText = "Unknown";
   private long mStarttime = -1;
   private long mEndtime = -1;
   private UnitsI18n mUnits;
   private double mMaxSpeed;
   private double mMaxAltitude;	
   private double mMinAltitude;
   private double mAscension;
   private double mDistanceTraveled;
   private long mDuration;
   private double mAverageActiveSpeed;
   private StatisticsDelegate mDelegate;
   
   
   public StatisticsCalulator( Context ctx, UnitsI18n units, StatisticsDelegate delegate )
   {
      mContext = ctx;
      mUnits = units;
      mDelegate = delegate;
   }

   private void updateCalculations( Uri trackUri )
   {
      mStarttime = -1;
      mEndtime = -1;
      mMaxSpeed = 0;
      mAverageActiveSpeed = 0;
      mMaxAltitude = 0;
      mMinAltitude = 0;
      mAscension = 0;
      mDistanceTraveled = 0f;
      mDuration = 0;
      long duration = 1;
      double ascension = 0;

      ContentResolver resolver = mContext.getContentResolver();

      Cursor waypointsCursor = null;
      try
      {
         waypointsCursor = resolver.query( 
               Uri.withAppendedPath( trackUri, "waypoints" ), 
               new String[] { "max  (" + Waypoints.TABLE + "." + Waypoints.SPEED + ")"
                            , "max  (" + Waypoints.TABLE + "." + Waypoints.ALTITUDE + ")"
                            , "min  (" + Waypoints.TABLE + "." + Waypoints.ALTITUDE + ")"
                            , "count(" + Waypoints.TABLE + "." + Waypoints._ID + ")" },
               null, null, null );
         if( waypointsCursor.moveToLast() )
         {
            mMaxSpeed = waypointsCursor.getDouble( 0 );
            mMaxAltitude = waypointsCursor.getDouble( 1 );
            mMinAltitude = waypointsCursor.getDouble( 2 );
            long nrWaypoints = waypointsCursor.getLong( 3 );
            waypointsText = nrWaypoints + "";
         }
         waypointsCursor.close();
         waypointsCursor = resolver.query( 
               Uri.withAppendedPath( trackUri, "waypoints" ), 
               new String[] { "avg  (" + Waypoints.TABLE + "." + Waypoints.SPEED + ")" },
               Waypoints.TABLE + "." + Waypoints.SPEED +"  > ?", 
               new String[] { ""+Constants.MIN_STATISTICS_SPEED }, 
               null );
         if( waypointsCursor.moveToLast() )
         {
            mAverageActiveSpeed = waypointsCursor.getDouble( 0 );
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
         trackCursor = resolver.query( trackUri, new String[] { Tracks.NAME }, null, null, null );
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
      Location lastAltitudeLocation = null;
      Location currentLocation = null;
      try
      {
         Uri segmentsUri = Uri.withAppendedPath( trackUri, "segments" );
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
                  waypoints = resolver.query( waypointsUri, new String[] { Waypoints._ID, Waypoints.TIME, Waypoints.LONGITUDE, Waypoints.LATITUDE, Waypoints.ALTITUDE }, null, null, null );
                  if( waypoints.moveToFirst() )
                  {
                     do
                     {
                        if( mStarttime < 0 )
                        {
                           mStarttime = waypoints.getLong( 1 );
                        }
                        currentLocation = new Location( this.getClass().getName() );
                        currentLocation.setTime( waypoints.getLong( 1 ) );
                        currentLocation.setLongitude( waypoints.getDouble( 2 ) );
                        currentLocation.setLatitude( waypoints.getDouble( 3 ) );
                        currentLocation.setAltitude( waypoints.getDouble( 4 ) );
                        
                        // Do no include obvious wrong 0.0 lat 0.0 long, skip to next value in while-loop
                        if( currentLocation.getLatitude() == 0.0d || currentLocation.getLongitude() == 0.0d )
                        {
                           continue;
                        }
                        
                        if( lastLocation != null )
                        {
                           float travelPart = lastLocation.distanceTo( currentLocation );
                           long timePart = currentLocation.getTime() - lastLocation.getTime();
                           mDistanceTraveled += travelPart;
                           duration += timePart;
                        }
                        if( currentLocation.hasAltitude() )
                        {
                           if( lastAltitudeLocation != null  )
                           {
                              if( currentLocation.getTime() - lastAltitudeLocation.getTime() > 5*60*1000 ) // more then a 5m of climbing
                              {
                                 if( currentLocation.getAltitude() > lastAltitudeLocation.getAltitude()+1 ) // more then 1m climb
                                 {
                                    ascension += currentLocation.getAltitude() - lastAltitudeLocation.getAltitude();
                                    lastAltitudeLocation = currentLocation;
                                 }
                                 else
                                 {
                                    lastAltitudeLocation = currentLocation;
                                 }
                              }
                           }
                           else
                           {
                              lastAltitudeLocation = currentLocation;
                           }
                        }
                        lastLocation = currentLocation;
                        mEndtime = lastLocation.getTime();
                     }
                     while( waypoints.moveToNext() );
                     mDuration = mEndtime - mStarttime;
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
      double maxSpeed          = mUnits.conversionFromMetersPerSecond( mMaxSpeed );
      double overallavgSpeedfl = mUnits.conversionFromMeterAndMiliseconds( mDistanceTraveled, mDuration );
      double avgSpeedfl        = mUnits.conversionFromMeterAndMiliseconds( mDistanceTraveled, duration );
      double traveled          = mUnits.conversionFromMeter( mDistanceTraveled );
      avgSpeedText        = mUnits.formatSpeed( avgSpeedfl, true ); 
      overallavgSpeedText = mUnits.formatSpeed( overallavgSpeedfl, true );
      maxSpeedText        = mUnits.formatSpeed( maxSpeed, true );
      distanceText        = String.format( "%.2f %s", traveled,          mUnits.getDistanceUnit() );
      ascensionText       = String.format( "%.0f %s", ascension,         mUnits.getHeightUnit() );
   }

   /**
    * Get the overallavgSpeedText.
    *
    * @return Returns the overallavgSpeedText as a String.
    */
   public String getOverallavgSpeedText()
   {
      return overallavgSpeedText;
   }

   /**
    * Get the avgSpeedText.
    *
    * @return Returns the avgSpeedText as a String.
    */
   public String getAvgSpeedText()
   {
      return avgSpeedText;
   }

   /**
    * Get the maxSpeedText.
    *
    * @return Returns the maxSpeedText as a String.
    */
   public String getMaxSpeedText()
   {
      return maxSpeedText;
   }

   /**
    * Get the minSpeedText.
    *
    * @return Returns the minSpeedText as a String.
    */
   public String getMinSpeedText()
   {
      return minSpeedText;
   }
   
   /**
    * Get the tracknameText.
    *
    * @return Returns the tracknameText as a String.
    */
   public String getTracknameText()
   {
      return tracknameText;
   }

   /**
    * Get the waypointsText.
    *
    * @return Returns the waypointsText as a String.
    */
   public String getWaypointsText()
   {
      return waypointsText;
   }

   /**
    * Get the distanceText.
    *
    * @return Returns the distanceText as a String.
    */
   public String getDistanceText()
   {
      return distanceText;
   }

   /**
    * Get the starttime.
    *
    * @return Returns the starttime as a long.
    */
   public long getStarttime()
   {
      return mStarttime;
   }

   /**
    * Get the endtime.
    *
    * @return Returns the endtime as a long.
    */
   public long getEndtime()
   {
      return mEndtime;
   }

   /**
    * Get the maximum speed.
    *
    * @return Returns the maxSpeeddb as m/s in a double.
    */
   public double getMaxSpeed()
   {
      return mMaxSpeed;
   }
   
   /**
    * Get the min speed.
    *
    * @return Returns the average speed as m/s in a double.
    */
   public double getAverageStatisicsSpeed()
   {
      return mAverageActiveSpeed;
   }

   /**
    * Get the maxAltitude.
    *
    * @return Returns the maxAltitude as a double.
    */
   public double getMaxAltitude()
   {
      return mMaxAltitude;
   }

   /**
    * Get the minAltitude.
    *
    * @return Returns the minAltitude as a double.
    */
   public double getMinAltitude()
   {
      return mMinAltitude;
   }

   /**
    * Get the total ascension in m.
    *
    * @return Returns the ascension as a double.
    */
   public double getAscension()
   {
      return mAscension;
   }
   
   public CharSequence getAscensionText()
   {
      return ascensionText;
   }
   
   /**
    * Get the distanceTraveled.
    *
    * @return Returns the distanceTraveled as a float.
    */
   public double getDistanceTraveled()
   {
      return mDistanceTraveled;
   }

   /**
    * Get the mUnits.
    *
    * @return Returns the mUnits as a UnitsI18n.
    */
   public UnitsI18n getUnits()
   {
      return mUnits;
   }

   public String getDurationText()
   {
      long s = mDuration / 1000;
      String duration = String.format("%dh:%02dm:%02ds", s/3600, (s%3600)/60, (s%60));

      return duration;
   }

   @Override
   protected Void doInBackground(Uri... params)
   {
      this.updateCalculations(params[0]);
      return null;
   }
   
   @Override
   protected void onPostExecute(Void result)
   {
      super.onPostExecute(result);
      if( mDelegate != null )
      {
         mDelegate.finishedCalculations(this);
      }
      
   }
}
