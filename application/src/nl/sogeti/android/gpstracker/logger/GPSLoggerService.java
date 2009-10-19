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

import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
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
   private static final String PRECISION = "precision";
   private static final String LOGATSTARTUP = "logatstartup";
   private static final String GPS_PROVIDER = LocationManager.GPS_PROVIDER;
   public static final String SERVICENAME = "nl.sogeti.android.gpstrack.logger.GPSLoggerService";
   private static final String TAG = GPSLoggerService.class.getName();
   
   private Context mContext;
   private LocationManager locationManager;
   private boolean logging;
   private PowerManager.WakeLock mWakeLock ;
   
   private long mTrackId = -1;
   private long segmentId = -1 ;
   private Location previousLocation;

   private OnSharedPreferenceChangeListener mSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener()
   {
      public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
      {
         if( key.equals( PRECISION ) )
         {
            requestLocationUpdates();
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
      public boolean isLogging() throws RemoteException
      {
         return GPSLoggerService.this.isLogging();
      }
      public long startLogging() throws RemoteException
      {
         GPSLoggerService.this.startLogging();
         return mTrackId;
      }
      public void stopLogging() throws RemoteException
      {
         GPSLoggerService.this.stopLogging();
      }
   };

   /**
    * Called by the system when the service is first created. Do not call this method directly. Be sure to call super.onCreate().
    */
   @Override
   public void onCreate()
   {
      super.onCreate();
      this.mContext = getApplicationContext();
      this.locationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
      
      boolean startImmidiatly = PreferenceManager.getDefaultSharedPreferences( this.mContext ).getBoolean( LOGATSTARTUP, false );
      Log.d( TAG, "Commence logging at startup:"+startImmidiatly );
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
    * @see nl.sogeti.android.gpstracker.IGPSLoggerService#startLogging()
    */
   public synchronized long startLogging()
   {
      startNewTrack() ;
      requestLocationUpdates();
      PreferenceManager.getDefaultSharedPreferences( this.mContext ).registerOnSharedPreferenceChangeListener( mSharedPreferenceChangeListener );
      PowerManager pm = (PowerManager) this.mContext.getSystemService( Context.POWER_SERVICE );
      this.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, SERVICENAME);
      this.mWakeLock.acquire();
      this.logging = true;
      return mTrackId;
   }

   private void requestLocationUpdates()
   {
      
      this.locationManager.removeUpdates( this.mLocationListener );
      int precision = new Integer( PreferenceManager.getDefaultSharedPreferences( this.mContext ).getString( PRECISION, "1" ) ).intValue();
      //Log.d( TAG, "requestLocationUpdates to precision "+precision );
      switch( precision )
      {
         case(0): // Coarse
            this.locationManager.requestLocationUpdates( GPS_PROVIDER, 15000l, 5F, this.mLocationListener );
            break;
         case(1): // Normal
            this.locationManager.requestLocationUpdates( GPS_PROVIDER, 5000l, 3F, this.mLocationListener );
            break;
         case(2): // Fine
            this.locationManager.requestLocationUpdates( GPS_PROVIDER, 1000l, 1F, this.mLocationListener );
            break;
         default:
            Log.e( TAG, "Unknown precision "+precision );
            break;
      }
      
   }

   /**
    * (non-Javadoc)
    * @see nl.sogeti.android.gpstracker.IGPSLoggerService#stopLogging()
    */
   public synchronized void stopLogging()
   {
      PreferenceManager.getDefaultSharedPreferences( this.mContext ).unregisterOnSharedPreferenceChangeListener( this.mSharedPreferenceChangeListener );
      this.locationManager.removeUpdates( this.mLocationListener );
      if( this.mWakeLock != null )
      {
         this.mWakeLock.release();
         this.mWakeLock = null ;
      }
      this.logging = false;
   }

   /**
    * (non-Javadoc)
    * @see nl.sogeti.android.gpstracker.IGPSLoggerService#isLogging()
    */
   public boolean isLogging()
   {
      return this.logging;
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
      if( previousLocation != null && proposedLocation.hasAccuracy() )
      {
         //Log.d(this.getClass().getCanonicalName(), "Distance traveled is: "+lastLocation.distanceTo( proposedLocation ));
         //Log.d(this.getClass().getCanonicalName(), "Accuratcy is: "+proposedLocation.getAccuracy() );
         acceptable = proposedLocation.getAccuracy() < previousLocation.distanceTo( proposedLocation ) ;
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
      segmentId = new Long(newSegment.getLastPathSegment()).longValue();
   }

   /**
    * Use the ContentResolver mechanism to store a received location
    * @param location
    */
   public void storeLocation(Context context, Location location )
   {   
      previousLocation = location;
      ContentValues args = new ContentValues();
      args.put( Waypoints.LATITUDE, new Double( location.getLatitude() ) );
      args.put( Waypoints.LONGITUDE, new Double( location.getLongitude() ) );
      args.put( Waypoints.SPEED, new Float( location.getSpeed() ) );
      context.getContentResolver().insert( Uri.withAppendedPath( Tracks.CONTENT_URI, mTrackId+"/segments/"+segmentId+"/waypoints" ), args );
   }
}