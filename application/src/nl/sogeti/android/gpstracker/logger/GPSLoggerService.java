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
package nl.sogeti.android.gpstracker.logger;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.viewer.LoggerMap;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * A system service as controlling the background logging of gps locations.
 * 
 * @version $Id$
 * @author rene (c) Jan 22, 2009, Sogeti B.V.
 */
public class GPSLoggerService extends Service 
{  
   public static final String SERVICENAME = "nl.sogeti.android.gpstracker.intent.action.GPSLoggerService";
   public static final int UNKNOWN = -1;
   public static final int RUNNING = 1;
   public static final int PAUSED = 2;
   public static final int STOPPED = 3;
   
   private static final String PRECISION = "precision";
   private static final String LOGATSTARTUP = "logatstartup";
   private static final String GPS_PROVIDER = LocationManager.GPS_PROVIDER;
   private static final String TAG = GPSLoggerService.class.getName();
   
   private Context mContext;
   private LocationManager mLocationManager;
   private NotificationManager mNoticationService;
   private int mLoggingState = UNKNOWN;
   private PowerManager.WakeLock mWakeLock ;
   
   private long mTrackId = -1;
   private long mSegmentId = -1 ;
   private Location mPreviousLocation;  
   private int mAcceptableAccuracy = 20;
   

   private OnSharedPreferenceChangeListener mSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener()
   {
      public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
      {
         if( key.equals( PRECISION ) )
         {
            requestLocationUpdates();
            setupNotification();
         }
      }
   };
   private LocationListener mLocationListener =  new LocationListener() 
   {
      public void onLocationChanged( Location location )
      {
         if( isLocationAcceptable(location) )
         {
            storeLocation(GPSLoggerService.this.mContext, location);
         }
      }
      public void onProviderDisabled( String provider ){   }
      public void onProviderEnabled( String provider ){ startNewSegment() ;  }
      public void onStatusChanged( String provider, int status, Bundle extras ){ }
   };  
   private IBinder mBinder = new IGPSLoggerServiceRemote.Stub() 
   {
      public int loggingState() throws RemoteException
      {
         return mLoggingState;
      }
      public long startLogging() throws RemoteException
      {
         GPSLoggerService.this.startLogging();
         return mTrackId;
      }      
      public void pauseLogging() throws RemoteException
      {
         GPSLoggerService.this.pauseLogging();
      }
      public long resumeLogging() throws RemoteException
      {
         GPSLoggerService.this.resumeLogging();
         return mSegmentId;
      }
      public void stopLogging() throws RemoteException
      {
         GPSLoggerService.this.stopLogging();
      }

   };
   private int mPrecision;
   private Notification mNotification;

   /**
    * Called by the system when the service is first created. Do not call this method directly. Be sure to call super.onCreate().
    */
   @Override
   public void onCreate()
   {
      super.onCreate();
      this.mLoggingState = STOPPED;
      this.mContext = getApplicationContext();
      this.mLocationManager = (LocationManager) this.mContext.getSystemService( Context.LOCATION_SERVICE );
      this.mNoticationService = (NotificationManager) this.mContext.getSystemService( Context.NOTIFICATION_SERVICE );
      mNoticationService.cancel( R.layout.map );
      
      boolean startImmidiatly = PreferenceManager.getDefaultSharedPreferences( this.mContext ).getBoolean( LOGATSTARTUP, false );
//      Log.d( TAG, "Commence logging at startup:"+startImmidiatly );
      if( startImmidiatly )
      {
         startLogging();
         ContentValues values = new ContentValues();
         values.put( Tracks.NAME, "Recorded at startup");
         getContentResolver().update(
               ContentUris.withAppendedId( Tracks.CONTENT_URI, mTrackId ), 
               values, null, null );
      }
   }

   /**
    * 
    * (non-Javadoc)
    * @see android.app.Service#onDestroy()
    */
   @Override
   public void onDestroy()
   {
      stopLogging();
      super.onDestroy();
   }

   /**
    * (non-Javadoc)
    * @see android.app.Service#onBind(android.content.Intent)
    */
   @Override
   public IBinder onBind(Intent intent) 
   {
      return this.mBinder;
   }

   /**
    * (non-Javadoc)
    * @see nl.sogeti.android.gpstracker.IGPSLoggerService#getLoggingState()
    */
   protected boolean isLogging()
   {
      return this.mLoggingState == RUNNING;
   }

   /**
    * (non-Javadoc)
    * @see nl.sogeti.android.gpstracker.IGPSLoggerService#startLogging()
    */
   protected synchronized long startLogging()
   {
      startNewTrack() ;
      requestLocationUpdates();
      this.mLoggingState = RUNNING;
      updateWakeLock();

      setupNotification();
     
      return mTrackId;
   }

   protected synchronized void pauseLogging()
   {
      if( this.mLoggingState == RUNNING )
      {
         this.mLocationManager.removeUpdates( this.mLocationListener );
         this.mLoggingState = PAUSED;
         updateWakeLock();
         updateNotification();
      }
   }
   
   protected synchronized void resumeLogging()
   {
      if( this.mLoggingState == PAUSED )
      {
         startNewSegment();
         requestLocationUpdates();
         this.mLoggingState = RUNNING;
         updateWakeLock();
         updateNotification();
      }
   }

   /**
    * (non-Javadoc)
    * @see nl.sogeti.android.gpstracker.IGPSLoggerService#stopLogging()
    */
   protected synchronized void stopLogging()
   {
      PreferenceManager.getDefaultSharedPreferences( this.mContext ).unregisterOnSharedPreferenceChangeListener( this.mSharedPreferenceChangeListener );
      this.mLocationManager.removeUpdates( this.mLocationListener );
      this.mLoggingState = STOPPED;
      updateWakeLock();
      mNoticationService.cancel( R.layout.map );
   }

   private void setupNotification()
   {
      mNoticationService.cancel( R.layout.map );
      
      int icon = R.drawable.ic_maps_indicator_current_position;
      CharSequence tickerText = this.getResources().getString( R.string.logcontrol_start );
      long when = System.currentTimeMillis();         
      
      mNotification = new Notification(icon, tickerText, when);
      mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
      
      updateNotification();
   }
   
   private void updateNotification()
   {
      CharSequence contentTitle = this.getResources().getString( R.string.app_name );
      
      String precision = this.getResources().getStringArray( R.array.precision_choices )[mPrecision];
      CharSequence contentText = this.getResources().getString( R.string.service_status, mLoggingState );
      
      Intent notificationIntent = new Intent(this, LoggerMap.class);
      notificationIntent.putExtra( LoggerMap.EXTRA_TRACK_ID, mTrackId );
      
      PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
      mNotification.setLatestEventInfo(this, contentTitle, contentText, contentIntent); 
      mNoticationService.notify( R.layout.map, mNotification );
   }

   private void requestLocationUpdates()
   {
      this.mLocationManager.removeUpdates( this.mLocationListener );
      mPrecision = new Integer( PreferenceManager.getDefaultSharedPreferences( this.mContext ).getString( PRECISION, "1" ) ).intValue();
      //Log.d( TAG, "requestLocationUpdates to precision "+precision );
      switch( mPrecision )
      {
         case(0): // Fine
            this.mAcceptableAccuracy = 10;
            this.mLocationManager.requestLocationUpdates( GPS_PROVIDER, 1000l, 5F, this.mLocationListener );
            break;
         case(1): // Normal
            this.mAcceptableAccuracy = 20;
            this.mLocationManager.requestLocationUpdates( GPS_PROVIDER, 15000l, 10F, this.mLocationListener );
            break;
         case(2): // Coarse
            this.mAcceptableAccuracy = 50;
            this.mLocationManager.requestLocationUpdates( GPS_PROVIDER, 30000l, 25F, this.mLocationListener );
            break;
         default:
            Log.e( TAG, "Unknown precision "+mPrecision );
            break;
      }
   }

   private void updateWakeLock()
   {
      if( this.mLoggingState == RUNNING )
      {
         PreferenceManager.getDefaultSharedPreferences( this.mContext ).registerOnSharedPreferenceChangeListener( mSharedPreferenceChangeListener );
         PowerManager pm = (PowerManager) this.mContext.getSystemService( Context.POWER_SERVICE );
         this.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, SERVICENAME);
         this.mWakeLock.acquire();
      }
      else
      {
         if( this.mWakeLock != null )
         {
            this.mWakeLock.release();
            this.mWakeLock = null ;
         }
      }
   }

   /**
    * Some GPS waypoints received are of to low a quality for tracking use. Here we 
    * filter those out.
    * 
    * @param proposedLocation
    * @return if the location is accurate enough
    */
   public boolean isLocationAcceptable( Location proposedLocation )
   {
      boolean acceptable = true; 
      if( mPreviousLocation != null && proposedLocation.hasAccuracy() )
      {
         acceptable = proposedLocation.getAccuracy() < this.mAcceptableAccuracy 
                        && proposedLocation.getAccuracy() < mPreviousLocation.distanceTo( proposedLocation ) ;
      }
      return acceptable;
   }

   /**
    * Trigged by events that start a new track
    */
   private void startNewTrack() 
   {
      Uri newTrack = this.mContext.getContentResolver().insert( Tracks.CONTENT_URI, null );
      mTrackId =  new Long(newTrack.getLastPathSegment()).longValue();
      startNewSegment() ;
   }

   /**
    * Trigged by events that start a new segment
    */
   private void startNewSegment() 
   {
      Uri newSegment = this.mContext.getContentResolver().insert( Uri.withAppendedPath( Tracks.CONTENT_URI, mTrackId+"/segments" ), null ); 
      mSegmentId = new Long(newSegment.getLastPathSegment()).longValue();
   }

   /**
    * Use the ContentResolver mechanism to store a received location
    * @param location
    */
   public void storeLocation(Context context, Location location )
   {   
      mPreviousLocation = location;
      ContentValues args = new ContentValues();
      
      args.put( Waypoints.LATITUDE, new Double( location.getLatitude() ) );
      args.put( Waypoints.LONGITUDE, new Double( location.getLongitude() ) );
      args.put( Waypoints.SPEED, new Float( location.getSpeed() ) );
      args.put( Waypoints.TIME, new Long( System.currentTimeMillis() ) );
      
//      Log.d( TAG, "Location based time sent to ContentProvider"+  DateFormat.getInstance().format(new Date( args.getAsLong( Waypoints.TIME ) ) ) );
      
      if( location.hasAccuracy() )
      {
         args.put( Waypoints.ACCURACY, new Float( location.getAccuracy() ) );
      }
      if( location.hasAltitude() )
      {
         args.put( Waypoints.ALTITUDE, new Double( location.getAltitude() ) );
         
      }
      if( location.hasBearing() )
      {
         args.put( Waypoints.BEARING, new Float( location.getBearing() ) );
      }

      Uri waypointInsertUri = Uri.withAppendedPath( Tracks.CONTENT_URI, mTrackId+"/segments/"+mSegmentId+"/waypoints" );
      context.getContentResolver().insert( waypointInsertUri, args );
   }
}