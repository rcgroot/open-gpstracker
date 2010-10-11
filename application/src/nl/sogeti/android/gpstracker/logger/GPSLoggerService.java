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

import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.db.GPStracking.Media;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.util.Constants;
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
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GpsStatus.Listener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * A system service as controlling the background logging of gps locations.
 * 
 * @version $Id$
 * @author rene (c) Jan 22, 2009, Sogeti B.V.
 */
public class GPSLoggerService extends Service
{

   /**
    * <code>MAX_REASONABLE_SPEED</code> is about 216 kilometer per hour or 134 mile per hour.
    */
   private static final int MAX_REASONABLE_SPEED = 60;
   
   /**
    * <code>MAX_REASONABLE_ALTITUDECHANGE</code> between the last few waypoints and a new one the difference should be less then 
    * 200 meter.
    */
   private static final int MAX_REASONABLE_ALTITUDECHANGE = 200;
   
   private static final String TAG = "OGT.GPSLoggerService";
   private static final int LOGGING_FINE = 0;
   private static final int LOGGING_NORMAL = 1;
   private static final int LOGGING_COARSE = 2;
   private static final int LOGGING_GLOBAL = 3;
   private static final String SERVICESTATE_STATE = "SERVICESTATE_STATE";
   private static final String SERVICESTATE_PRECISION = "SERVICESTATE_PRECISION";
   private static final String SERVICESTATE_SEGMENTID = "SERVICESTATE_SEGMENTID";
   private static final String SERVICESTATE_TRACKID = "SERVICESTATE_TRACKID";

   private static final int ADDGPSSTATUSLISTENER = 0;
   private static final int REQUEST_FINEGPS_LOCATIONUPDATES = 1;
   private static final int REQUEST_NORMALGPS_LOCATIONUPDATES = 2;
   private static final int REQUEST_COARSEGPS_LOCATIONUPDATES = 3;
   private static final int REQUEST_GLOBALGPS_LOCATIONUPDATES = 4;
   private static final int REGISTERONSHAREDPREFERENCECHANGELISTENER = 5;
   private static final int STOPLOOPER = 6;
   
   private Context mContext;
   private LocationManager mLocationManager;
   private NotificationManager mNoticationManager;
   private PowerManager.WakeLock mWakeLock;
   private Handler mHandler;


   private boolean mSpeedSanityCheck;
   private long mTrackId = -1;
   private long mSegmentId = -1;
   private long mWaypointId = -1;
   private int mPrecision;
   private int mLoggingState = Constants.STOPPED;
   private boolean mStartNextSegment;

   private Location mPreviousLocation;
   private Notification mNotification;

   private Vector<Location> mWeakLocations;
   private Queue<Double> mAltitudes;

   /**
    * <code>mAcceptableAccuracy</code> indicates the maximum acceptable accuracy of a waypoint in meters.
    */
   private float mMaxAcceptableAccuracy = 20;
   private int mSatellites = 0;

   /**
    * Listens to changes in preference to precision and sanity checks
    */
   private OnSharedPreferenceChangeListener mSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener()
      {
         public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
         {
            if( key.equals( Constants.PRECISION ) )
            {
               requestLocationUpdates();
               setupNotification();
            }
            else if( key.equals( Constants.SPEEDSANITYCHECK ) )
            {
               mSpeedSanityCheck = sharedPreferences.getBoolean( Constants.SPEEDSANITYCHECK, true );
            }
         }
      };
   /**
    * Listens to location changes and provider availability
    */
   private LocationListener mLocationListener = new LocationListener()
      {
         public void onLocationChanged( Location location )
         {
            Location filteredLocation = locationFilter( location );
            if( filteredLocation != null )
            {
               if( mStartNextSegment )
               {
                  mStartNextSegment = false;
                  startNewSegment();
               }
               storeLocation( filteredLocation );
            }
         }

         public void onProviderDisabled( String provider )
         {
//            Log.d( TAG, "onProviderDisabled( String " + provider + " )" );
            if( mPrecision != LOGGING_GLOBAL && provider.equals( LocationManager.GPS_PROVIDER ) )
            {
               disabledProviderNotification( R.string.service_gpsdisabled );
            }
            else if( mPrecision == LOGGING_GLOBAL && provider.equals( LocationManager.NETWORK_PROVIDER ) )
            {
               disabledProviderNotification( R.string.service_datadisabled );
            }

         }

         public void onProviderEnabled( String provider )
         {
            if( mPrecision != LOGGING_GLOBAL && provider.equals( LocationManager.GPS_PROVIDER ) )
            {
               enabledProviderNotification( R.string.service_gpsenabled );
               mStartNextSegment = true;
            }
            else if( mPrecision == LOGGING_GLOBAL && provider.equals( LocationManager.NETWORK_PROVIDER ) )
            {
               enabledProviderNotification( R.string.service_dataenabled );
            }
         }

         public void onStatusChanged( String provider, int status, Bundle extras )
         {
            Log.w( TAG, String.format( "Provider %s changed to status %d", provider, status ) );
         }
      };
   /**
    * Listens to GPS status changes
    */
   private Listener mStatusListener = new GpsStatus.Listener()
      {
         public synchronized void onGpsStatusChanged( int event )
         {
            switch( event )
            {
               case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                  GpsStatus status = mLocationManager.getGpsStatus( null );
                  mSatellites = 0;
                  Iterable<GpsSatellite> list = status.getSatellites();
                  for( GpsSatellite satellite : list )
                  {
                     if( satellite.usedInFix() )
                     {
                        mSatellites++;
                     }
                  }
                  updateNotification();
                  break;
               default:
                  break;
            }
         }
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

         public Uri storeMediaUri( Uri mediaUri ) throws RemoteException
         {
            GPSLoggerService.this.storeMediaUri( mediaUri );
            return null;
         }

         public boolean isMediaPrepared() throws RemoteException
         {
            return GPSLoggerService.this.isMediaPrepared();
         }
      };

   private Semaphore handlerGatekeeper;

   
   private class GPSLoggerServiceThread extends Thread
   {
      public void run()
      {
         Looper.prepare();
         mHandler = new Handler()
         {
             public void handleMessage(Message msg) 
             {
                _handleMessage(msg);
             }
         };
         handlerGatekeeper.release();
         Looper.loop();
     }
   }
   
   /**
    * Message handler method to do the work off-loaded by mHandler to GPSLoggerServiceThread
    * 
    * @param msg
    */
   private void _handleMessage(Message msg) 
   {
      switch (msg.what)
      {
         case ADDGPSSTATUSLISTENER:
            this.mLocationManager.addGpsStatusListener( mStatusListener );
            break;
         case REGISTERONSHAREDPREFERENCECHANGELISTENER:
            PreferenceManager.getDefaultSharedPreferences( this.mContext ).registerOnSharedPreferenceChangeListener( mSharedPreferenceChangeListener );
            break;
         case REQUEST_FINEGPS_LOCATIONUPDATES:
            mMaxAcceptableAccuracy = 10f;
            mLocationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 1000l, 5F, this.mLocationListener );
            break;
         case REQUEST_NORMALGPS_LOCATIONUPDATES:
            mMaxAcceptableAccuracy = 20f;
            mLocationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 15000l, 10F, this.mLocationListener );
            break;
         case REQUEST_COARSEGPS_LOCATIONUPDATES:
            mMaxAcceptableAccuracy = 50f;
            mLocationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 30000l, 25F, this.mLocationListener );
            break;
         case REQUEST_GLOBALGPS_LOCATIONUPDATES:
            mMaxAcceptableAccuracy = 1000f;
            mLocationManager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER, 300000l, 500F, this.mLocationListener );
            if( !isNetworkConnected() )
            {
               disabledProviderNotification( R.string.service_connectiondisabled );
            }
            break;
         case STOPLOOPER:
            mLocationManager.removeGpsStatusListener( mStatusListener );
            mLocationManager.removeUpdates( mLocationListener );
            Looper.myLooper().quit();
            break;
      }
   }
   /**
    * Called by the system when the service is first created. Do not call this method directly. Be sure to call super.onCreate().
    */
   @Override
   public void onCreate()
   {
      super.onCreate();
      
      handlerGatekeeper = new Semaphore( 0 );
      new GPSLoggerServiceThread().start();
      
      mWeakLocations = new Vector<Location>( 3 );
      mAltitudes = new LinkedList<Double>();
      mLoggingState = Constants.STOPPED;
      mStartNextSegment = false;
      mContext = getApplicationContext();
      mLocationManager = (LocationManager) this.mContext.getSystemService( Context.LOCATION_SERVICE );
      mNoticationManager = (NotificationManager) this.mContext.getSystemService( Context.NOTIFICATION_SERVICE );
      mNoticationManager.cancel( R.layout.map );

      SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences( this.mContext );
      mSpeedSanityCheck = sharedPreferences.getBoolean( Constants.SPEEDSANITYCHECK, true );
      boolean startImmidiatly = PreferenceManager.getDefaultSharedPreferences( this.mContext ).getBoolean( Constants.LOGATSTARTUP, false );

      crashRestoreState();
      
      if( startImmidiatly && mLoggingState == Constants.STOPPED )
      {
         startLogging();
         ContentValues values = new ContentValues();
         values.put( Tracks.NAME, "Recorded at startup" );
         getContentResolver().update( ContentUris.withAppendedId( Tracks.CONTENT_URI, mTrackId ), values, null, null );
      }
   }

   /**
    * (non-Javadoc)
    * 
    * @see android.app.Service#onDestroy()
    */
   @Override
   public void onDestroy()
   {
      super.onDestroy();
      stopLogging();
      Message msg = Message.obtain();
      msg.what = STOPLOOPER;
      sendMessage( msg );
   }

   private void crashProtectState()
   {
      SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences( mContext );
      Editor editor = preferences.edit();
      editor.putLong( SERVICESTATE_TRACKID, mTrackId );
      editor.putLong( SERVICESTATE_SEGMENTID, mSegmentId );
      editor.putInt( SERVICESTATE_PRECISION, mPrecision );
      editor.putInt( SERVICESTATE_STATE, mLoggingState );
      editor.commit();
   }

   private synchronized void crashRestoreState()
   {
      SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences( mContext );
      long previousState = preferences.getInt( SERVICESTATE_STATE, Constants.STOPPED );
      if( previousState == Constants.LOGGING || previousState == Constants.PAUSED )
      {
         Log.w( TAG, "Recovering from a crash or kill and restoring state." );
         setupNotification();

         mTrackId = preferences.getLong( SERVICESTATE_TRACKID, -1 );
         mSegmentId = preferences.getLong( SERVICESTATE_SEGMENTID, -1 );
         mPrecision = preferences.getInt( SERVICESTATE_PRECISION, -1 );
         if( previousState == Constants.LOGGING )
         {
            mLoggingState = Constants.PAUSED;
            resumeLogging();
         }
         else if( previousState == Constants.PAUSED )
         {
            mLoggingState = Constants.LOGGING;
            pauseLogging();
         }
      }
   }

   /**
    * (non-Javadoc)
    * 
    * @see android.app.Service#onBind(android.content.Intent)
    */
   @Override
   public IBinder onBind( Intent intent )
   {
      return this.mBinder;
   }

   /**
    * (non-Javadoc)
    * 
    * @see nl.sogeti.android.gpstracker.IGPSLoggerService#getLoggingState()
    */
   protected boolean isLogging()
   {
      return this.mLoggingState == Constants.LOGGING;
   }

   protected boolean isMediaPrepared()
   {
      return !( mTrackId < 0 || mSegmentId < 0 || mWaypointId < 0 );
   }

   /**
    * (non-Javadoc)
    * 
    * @see nl.sogeti.android.gpstracker.IGPSLoggerService#startLogging()
    */
   public synchronized void startLogging()
   {
      if( this.mLoggingState == Constants.STOPPED )
      {
         startNewTrack();
         requestLocationUpdates();

         Message msg = Message.obtain();
         msg.what = ADDGPSSTATUSLISTENER;
         sendMessage( msg );
         this.mLoggingState = Constants.LOGGING;
         updateWakeLock();
         setupNotification();
         crashProtectState();
      }
   }

   public synchronized void pauseLogging()
   {
      if( this.mLoggingState == Constants.LOGGING )
      {
         mLocationManager.removeGpsStatusListener( mStatusListener );
         mLocationManager.removeUpdates( mLocationListener );
         mLoggingState = Constants.PAUSED;
         mPreviousLocation = null;
         updateWakeLock();
         updateNotification();
         crashProtectState();
      }
   }

   public synchronized void resumeLogging()
   {
      if( this.mLoggingState == Constants.PAUSED )
      {
         if( mPrecision != LOGGING_GLOBAL )
         {
            mStartNextSegment = true;
         }
         requestLocationUpdates();

         Message msg = Message.obtain();
         msg.what = ADDGPSSTATUSLISTENER;
         sendMessage( msg );
         
         this.mLoggingState = Constants.LOGGING;
         updateWakeLock();
         updateNotification();
         crashProtectState();
      }
   }

   /**
    * (non-Javadoc)
    * 
    * @see nl.sogeti.android.gpstracker.IGPSLoggerService#stopLogging()
    */
   public synchronized void stopLogging()
   {
      if( this.mLoggingState == Constants.PAUSED || this.mLoggingState == Constants.LOGGING )
      {
         PreferenceManager.getDefaultSharedPreferences( this.mContext ).unregisterOnSharedPreferenceChangeListener( this.mSharedPreferenceChangeListener );
         
         mLocationManager.removeGpsStatusListener( mStatusListener );
         mLocationManager.removeUpdates( mLocationListener );

         mLoggingState = Constants.STOPPED;
         updateWakeLock();
         mNoticationManager.cancel( R.layout.map );
         crashProtectState();
      }
   }

   private void setupNotification()
   {
      mNoticationManager.cancel( R.layout.map );

      int icon = R.drawable.ic_maps_indicator_current_position;
      CharSequence tickerText = getResources().getString( R.string.service_start );
      long when = System.currentTimeMillis();

      mNotification = new Notification( icon, tickerText, when );
      mNotification.flags |= Notification.FLAG_ONGOING_EVENT;

      updateNotification();
   }

   private void updateNotification()
   {
      CharSequence contentTitle = getResources().getString( R.string.app_name );

      String precision = getResources().getStringArray( R.array.precision_choices )[mPrecision];
      String state = getResources().getStringArray( R.array.state_choices )[mLoggingState - 1];
      CharSequence contentText;
      switch( mPrecision )
      {
         case ( LOGGING_GLOBAL ):
            contentText = getResources().getString( R.string.service_networkstatus, state, precision );
            break;
         default:
            contentText = getResources().getString( R.string.service_gpsstatus, state, precision, mSatellites );
            break;
      }
      Intent notificationIntent = new Intent( this, LoggerMap.class );
      notificationIntent.setData( ContentUris.withAppendedId( Tracks.CONTENT_URI, mTrackId ) );

      PendingIntent contentIntent = PendingIntent.getActivity( this, 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK );
      mNotification.setLatestEventInfo( this, contentTitle, contentText, contentIntent );

      mNoticationManager.notify( R.layout.map, mNotification );
   }

   private void enabledProviderNotification( int resId )
   {
      mNoticationManager.cancel( R.id.icon );
      CharSequence text = mContext.getString( resId );
      Toast toast = Toast.makeText( mContext.getApplicationContext(), text, Toast.LENGTH_LONG );
      toast.show();
   }

   private void disabledProviderNotification( int resId )
   {
      int icon = R.drawable.ic_maps_indicator_current_position;
      CharSequence tickerText = getResources().getString( resId );
      long when = System.currentTimeMillis();
      Notification gpsNotification = new Notification( icon, tickerText, when );
      gpsNotification.flags |= Notification.FLAG_AUTO_CANCEL;

      CharSequence contentTitle = getResources().getString( R.string.app_name );
      CharSequence contentText = getResources().getString( resId );
      Intent notificationIntent = new Intent( this, LoggerMap.class );
      notificationIntent.setData( ContentUris.withAppendedId( Tracks.CONTENT_URI, mTrackId ) );
      PendingIntent contentIntent = PendingIntent.getActivity( this, 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK );
      gpsNotification.setLatestEventInfo( this, contentTitle, contentText, contentIntent );

      mNoticationManager.notify( R.id.icon, gpsNotification );
   }

   private void requestLocationUpdates()
   {
      this.mLocationManager.removeUpdates( mLocationListener );
      mPrecision = new Integer( PreferenceManager.getDefaultSharedPreferences( this.mContext ).getString( Constants.PRECISION, "1" ) ).intValue();
      //Log.d( TAG, "requestLocationUpdates to precision "+precision );
      Message msg = Message.obtain();
      switch( mPrecision )
      {
         case ( LOGGING_FINE ): // Fine
            msg.what = REQUEST_FINEGPS_LOCATIONUPDATES;
            sendMessage( msg );
            break;
         case ( LOGGING_NORMAL ): // Normal
            msg.what = REQUEST_NORMALGPS_LOCATIONUPDATES;
            sendMessage( msg );
            break;
         case ( LOGGING_COARSE ): // Coarse
            msg.what = REQUEST_COARSEGPS_LOCATIONUPDATES;
            sendMessage( msg );
            break;
         case ( REQUEST_GLOBALGPS_LOCATIONUPDATES ): // Global
            msg.what = REQUEST_COARSEGPS_LOCATIONUPDATES;
            sendMessage( msg ); 
            break;
         default:
            Log.e( TAG, "Unknown precision " + mPrecision );
            break;
      }
      handlerGatekeeper.release();
   }
   
   private void sendMessage( Message msg ) 
   {
      try
      {
         handlerGatekeeper.acquire();
         mHandler.sendMessage(msg);
         handlerGatekeeper.release();
      }
      catch( InterruptedException e )
      {
         Log.e( TAG, "Interrupted while waiting for handler to be created for message: "+msg );
      }
   }

   private void updateWakeLock()
   {
      if( this.mLoggingState == Constants.LOGGING )
      {
         Message msg = Message.obtain();
         msg.what = REGISTERONSHAREDPREFERENCECHANGELISTENER;
         sendMessage( msg );
         
         PowerManager pm = (PowerManager) this.mContext.getSystemService( Context.POWER_SERVICE );
         this.mWakeLock = pm.newWakeLock( PowerManager.PARTIAL_WAKE_LOCK, TAG );
         this.mWakeLock.acquire();
      }
      else
      {
         if( this.mWakeLock != null )
         {
            this.mWakeLock.release();
            this.mWakeLock = null;
         }
      }
   }

   /**
    * Some GPS waypoints received are of to low a quality for tracking use. Here we filter those out.
    * 
    * @param proposedLocation
    * @return either the (cleaned) original or null when unacceptable
    */
   public Location locationFilter( Location proposedLocation )
   {
      // Do not log a waypoint which is more inaccurate then is configured to be acceptable
      if( proposedLocation != null && proposedLocation.getAccuracy() > mMaxAcceptableAccuracy )
      {
         Log.w( TAG, String.format( "A weak location was recieved, lots of inaccuracy... (%f is more then max %f)", proposedLocation.getAccuracy(), mMaxAcceptableAccuracy ) );
         proposedLocation = addBadLocation( proposedLocation );
      }

      // Do not log a waypoint which might be on any side of the previous waypoint
      if( proposedLocation != null && mPreviousLocation != null && proposedLocation.getAccuracy() > mPreviousLocation.distanceTo( proposedLocation ) )
      {
         Log.w( TAG, String.format( "A weak location was recieved, not quite clear from the previous waypoint... (%f more then max %f)", proposedLocation.getAccuracy(), mPreviousLocation.distanceTo( proposedLocation ) ) );
         proposedLocation = addBadLocation( proposedLocation );
      }
      
      // Speed checks for NETWORK logging, check if the proposed location could be reached from the previous one in sane speed
      if( mSpeedSanityCheck && proposedLocation != null && mPreviousLocation != null &&  mPrecision == LOGGING_GLOBAL )
      {
         // To avoid near instant teleportation on network location or glitches cause continent hopping
         float meters = proposedLocation.distanceTo( mPreviousLocation );
         long seconds = ( proposedLocation.getTime() - mPreviousLocation.getTime() ) / 1000L;
         if( meters / seconds > MAX_REASONABLE_SPEED )
         {
            Log.w( TAG, "A strange location was recieved, a really high speed, prob wrong..." );
            proposedLocation = addBadLocation( proposedLocation );
         }
      }

      // Remove speed if not sane
      if( mSpeedSanityCheck && proposedLocation != null && proposedLocation.getSpeed() > MAX_REASONABLE_SPEED )
      {
         Log.w( TAG, "A strange speed, a really high speed, prob wrong..." );
         proposedLocation.removeSpeed();
      }
      
      // Remove altitude if not sane
      if( mSpeedSanityCheck && proposedLocation != null && proposedLocation.hasAltitude() )
      {
         if( ! addSaneAltitude( proposedLocation.getAltitude() ) )
         {
            Log.w( TAG, "A strange altitude, a really big difference, prob wrong..." );
            proposedLocation.removeAltitude();
         }
      }
      // Older bad locations will not be needed
      if( proposedLocation != null )
      {
         mWeakLocations.clear();
      }
      return proposedLocation;
   }
   
   /**
    * Store a bad location, when to many bad locations are stored the the storage is cleared and the least bad one is returned
    * 
    * @param location bad location
    * @return null when the bad location is stored or the least bad one if the storage was full
    */
   private Location addBadLocation( Location location )
   {
      mWeakLocations.add( location );
      if( mWeakLocations.size() < 3 )
      {
         location = null;
      }
      else
      {
         Location best = mWeakLocations.lastElement();
         for( Location whimp : mWeakLocations )
         {
            if( whimp.hasAccuracy() && best.hasAccuracy() && whimp.getAccuracy() < best.getAccuracy() )
            {
               best = whimp;
            }
            else
            {
               if( whimp.hasAccuracy() && !best.hasAccuracy() )
               {
                  best = whimp;
               }
            }
         }
         mWeakLocations.clear();
         location = best;
      }
      return location;
   }
   
   /**
    * Builds a bit of knowledge about altitudes to expect and 
    * return if the added value is deemed sane.
    * 
    * @param altitude 
    * @return whether the altitude is considered sane
    */
   private boolean addSaneAltitude( double altitude )
   {
      boolean sane = true;
      double avg = 0;
      int elements = 0;
      // Even insane altitude shifts increases alter perception
      mAltitudes.add( altitude );
      if( mAltitudes.size() > 3 )
      {
         mAltitudes.poll();
      }
      for( Double alt : mAltitudes )
      {
         avg += alt;
         elements++;
      }
      avg = avg / elements;
      sane = Math.abs( altitude - avg ) < MAX_REASONABLE_ALTITUDECHANGE;

//      Log.d( TAG, String.format( "Added %f to a total of %d and it was deemed %s ", altitude, mAltitudes.size(), sane ) );
      
      return sane;
   }
   
   /**
    * Trigged by events that start a new track
    */
   private void startNewTrack()
   {
      Uri newTrack = this.mContext.getContentResolver().insert( Tracks.CONTENT_URI, null );
      mTrackId = new Long( newTrack.getLastPathSegment() ).longValue();
      mStartNextSegment = true;
   }

   /**
    * Trigged by events that start a new segment
    */
   private void startNewSegment()
   {
      this.mPreviousLocation = null;
      Uri newSegment = this.mContext.getContentResolver().insert( Uri.withAppendedPath( Tracks.CONTENT_URI, mTrackId + "/segments" ), null );
      mSegmentId = new Long( newSegment.getLastPathSegment() ).longValue();
   }

   protected void storeMediaUri( Uri mediaUri )
   {
      //      Log.d( TAG, "Retrieved MediaUri to store on track: "+mediaUri );
      if( isMediaPrepared() )
      {
         Uri mediaInsertUri = Uri.withAppendedPath( Tracks.CONTENT_URI, mTrackId + "/segments/" + mSegmentId + "/waypoints/" + mWaypointId + "/media" );
         ContentValues args = new ContentValues();
         args.put( Media.URI, mediaUri.toString() );
         mContext.getContentResolver().insert( mediaInsertUri, args );
      }
      else
      {
         Log.e( TAG, "No logging done under which to store the track" );
      }
   }

   /**
    * Use the ContentResolver mechanism to store a received location
    * 
    * @param location
    */
   public void storeLocation( Location location )
   {
      if( !isLogging() )
      {
         Log.e( TAG, String.format( "Not logging but storing location %s, prepare to fail", location.toString() ) );
      }

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

      Uri waypointInsertUri = Uri.withAppendedPath( Tracks.CONTENT_URI, mTrackId + "/segments/" + mSegmentId + "/waypoints" );
      Uri inserted = mContext.getContentResolver().insert( waypointInsertUri, args );
      mWaypointId = Long.parseLong( inserted.getLastPathSegment() );
   }

   private boolean isNetworkConnected()
   {
      ConnectivityManager connMgr = (ConnectivityManager) getSystemService( Context.CONNECTIVITY_SERVICE );
      NetworkInfo info = connMgr.getActiveNetworkInfo();

      return ( info != null && info.isConnected() );
   }
}