package nl.sogeti.android.gpstracker.actions.utils;

import java.util.Calendar;

import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.util.UnitsI18n;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;

public class StatisticsCalulator
{

   private Context mContext;   
   private String overallavgSpeedText = "Unknown";
   private String avgSpeedText = "Unknown";
   private String maxSpeedText = "Unknown";
   private String maxAltitudeText = "Unknown";
   private String minAltitudeText = "Unknown";
   private String tracknameText = "Unknown";
   private String waypointsText = "Unknown";
   private String distanceText = "Unknown";
   private long mStarttime = -1;
   private long mEndtime = -1;
   private UnitsI18n mUnits;
   private double mMaxSpeed;
   private double mMaxAltitude;
   private double mMinAltitude;
   private double mDistanceTraveled;
   private long mDuration;
   
   
   public StatisticsCalulator( Context ctx, UnitsI18n units )
   {
      mContext = ctx;
      mUnits = units;
   }

   public void updateCalculations( Uri mTrackUri )
   {
      mMaxSpeed = 0;
      mMaxAltitude = 0;
      mMinAltitude = 0;
      mDistanceTraveled = 0f;
      long duration = 1;

      ContentResolver resolver = mContext.getContentResolver();

      Cursor waypointsCursor = null;
      try
      {
         waypointsCursor = resolver.query( Uri.withAppendedPath( mTrackUri, "waypoints" ), new String[] { "max  (" + Waypoints.TABLE + "." + Waypoints.SPEED + ")",
               "max  (" + Waypoints.TABLE + "." + Waypoints.ALTITUDE + ")", "min  (" + Waypoints.TABLE + "." + Waypoints.ALTITUDE + ")", "count(" + Waypoints.TABLE + "." + Waypoints._ID + ")" },
               null, null, null );
         if( waypointsCursor.moveToLast() )
         {
            mMaxSpeed = waypointsCursor.getDouble( 0 );
            mMaxAltitude = waypointsCursor.getDouble( 1 );
            mMinAltitude = waypointsCursor.getDouble( 2 );
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
         Uri segmentsUri = Uri.withAppendedPath( mTrackUri, "segments" );
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
                        if( mStarttime < 0 )
                        {
                           mStarttime = waypoints.getLong( 1 );
                        }
                        currentLocation = new Location( this.getClass().getName() );
                        currentLocation.setTime( waypoints.getLong( 1 ) );
                        currentLocation.setLongitude( waypoints.getDouble( 2 ) );
                        currentLocation.setLatitude( waypoints.getDouble( 3 ) );
                        if( lastLocation != null )
                        {
                           mDistanceTraveled += lastLocation.distanceTo( currentLocation );
                           duration += currentLocation.getTime() - lastLocation.getTime();
                        }
                        lastLocation = currentLocation;

                     }
                     while( waypoints.moveToNext() );
                     mEndtime = lastLocation.getTime();
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

      double speed             = mUnits.conversionFromMetersPerSecond( mMaxSpeed );
      double maxAltitude       = mUnits.conversionFromMeterToHeight( mMaxAltitude );
      double minAltitude       = mUnits.conversionFromMeterToHeight( mMinAltitude );
      double overallavgSpeedfl = mUnits.conversionFromMeterAndMiliseconds( mDistanceTraveled, mDuration );
      double avgSpeedfl        = mUnits.conversionFromMeterAndMiliseconds( mDistanceTraveled, duration );
      double traveled          = mUnits.conversionFromMeter( mDistanceTraveled );
      avgSpeedText        = String.format( "%.2f %s", avgSpeedfl, mUnits.getSpeedUnit() );
      overallavgSpeedText = String.format( "%.2f %s", overallavgSpeedfl, mUnits.getSpeedUnit() );
      distanceText        = String.format( "%.2f %s", traveled, mUnits.getDistanceUnit() );
      maxSpeedText        = String.format( "%.2f %s", speed, mUnits.getSpeedUnit() );
      minAltitudeText     = String.format( "%.0f %s", minAltitude, mUnits.getHeightUnit() );
      maxAltitudeText     = String.format( "%.0f %s", maxAltitude, mUnits.getHeightUnit() );
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
    * Get the maxAltitudeText.
    *
    * @return Returns the maxAltitudeText as a String.
    */
   public String getMaxAltitudeText()
   {
      return maxAltitudeText;
   }

   /**
    * Get the minAltitudeText.
    *
    * @return Returns the minAltitudeText as a String.
    */
   public String getMinAltitudeText()
   {
      return minAltitudeText;
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
    * Get the maxSpeeddb.
    *
    * @return Returns the maxSpeeddb as a double.
    */
   public double getMaxSpeed()
   {
      return mMaxSpeed;
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
}
