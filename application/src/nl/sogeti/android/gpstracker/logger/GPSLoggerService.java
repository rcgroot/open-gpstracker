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

import nl.sogeti.android.gpstracker.db.GPStracking.Segments;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;

/**
 * A system service as controlling the background logging of gps locations.
 * 
 * @version $Id$
 * @author rene (c) Jan 22, 2009, Sogeti B.V.
 */
public class GPSLoggerService extends Service
{  
   private static final String GPS_PROVIDER = LocationManager.GPS_PROVIDER;
   //private static final String GPS_PROVIDER = "gps";
   public static final String SERVICENAME = "nl.sogeti.android.gpstrack.logger.GPSLoggerService";
   private Context mCtx;
   private LocationManager locationManager;
   private boolean logging;
   private PowerManager.WakeLock mWakeLock ;
   
   private LocationListener mLocationListener =  new LocationListener() {
      public void onLocationChanged( Location location )
      {
         storeLocation(location);
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
      public int startLogging() throws RemoteException
      {
         return GPSLoggerService.this.startLogging();
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
      this.mCtx = getApplicationContext();
      this.locationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
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
   public synchronized int startLogging()
   {
      int trackid = startNewTrack() ;
      this.locationManager.requestLocationUpdates( GPS_PROVIDER, 1000, 0F, this.mLocationListener );
      PowerManager pm = (PowerManager) this.mCtx.getSystemService( Context.POWER_SERVICE );
      this.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, SERVICENAME);
      this.mWakeLock.acquire();
      this.logging = true;
      return trackid;
   }

   /**
    * (non-Javadoc)
    * @see nl.sogeti.android.gpstracker.IGPSLoggerService#stopLogging()
    */
   public synchronized void stopLogging()
   {
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
    * Trigged by events that start a new track
    */
   private int startNewTrack() 
   {
      Uri newTrack = this.mCtx.getContentResolver().insert( Tracks.CONTENT_URI, null );
      return new Integer(newTrack.getLastPathSegment()).intValue();
   }
   
   /**
    * Trigged by events that start a new segment
    */
   private void startNewSegment() 
   {
      this.mCtx.getContentResolver().insert( Segments.CONTENT_URI, null );
   }
   
   /**
    * Use the ContentResolver mechanism to store a received location
    * @param location
    */
   private void storeLocation( Location location )
   {   
      ContentValues args = new ContentValues();
      args.put( Waypoints.LATITUDE, new Double( location.getLatitude() ) );
      args.put( Waypoints.LONGITUDE, new Double( location.getLongitude() ) );
      this.mCtx.getContentResolver().insert( Waypoints.CONTENT_URI, args );
   }
}