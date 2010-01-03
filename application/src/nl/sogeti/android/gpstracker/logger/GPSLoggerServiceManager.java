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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * 
 * Class to interact with the service that tracks and logs the locations
 *
 * @version $Id$
 * @author rene (c) Jan 18, 2009, Sogeti B.V.
 */
public class GPSLoggerServiceManager
{
   private static final String TAG = "GPSLoggerServiceManager";
   private static final String REMOTE_EXCEPTION = "REMOTE_EXCEPTION";
   private static final int DISCONNECTED = 0;
   private static final int CONNECTING = 1;
   protected static final int CONNECTED = 2;
   private static final int DISCONNECTING = 3;
   private Context mCtx;
   private IGPSLoggerServiceRemote mGPSLoggerRemote;
   private int connection = DISCONNECTED;
   /**
    * Class for interacting with the main interface of the service.
    */
   private ServiceConnection mServiceConnection = new ServiceConnection()
   {
      public void onServiceConnected( ComponentName className, IBinder service )
      {
         Log.d( TAG, "onServiceConnected() on "+statusToString( connection ) );
         connection = CONNECTED;
         GPSLoggerServiceManager.this.mGPSLoggerRemote = IGPSLoggerServiceRemote.Stub.asInterface( service );
      }
      public void onServiceDisconnected( ComponentName className )
      {
         Log.d( TAG, "onServiceDisconnected() on "+statusToString( connection ) );
         connection = DISCONNECTED;
         GPSLoggerServiceManager.this.mGPSLoggerRemote = null;
      }
   };


   public GPSLoggerServiceManager(Context ctx)
   {
      this.mCtx = ctx;
      connectToGPSLoggerService();
   }

   public boolean isLogging()
   {
      boolean logging = false;
      if ( verifyConnection() ) 
      { 
         try
         {
            logging = this.mGPSLoggerRemote.isLogging();
         }
         catch (RemoteException e)
         {
            logging = false;
         }
      }
      else 
      {
         Log.e( TAG, "No GPSLoggerRemote service connected to this manager on status "+statusToString( connection ) );
      }
      return logging;
   }

   public long startGPSLogging(String name)
   {
      if ( verifyConnection() ) 
      { 
         try
         {
            return this.mGPSLoggerRemote.startLogging();
         }
         catch (RemoteException e)
         {
            Log.e( GPSLoggerServiceManager.REMOTE_EXCEPTION, "Could not start GPSLoggerService.", e );
         }
      }
      return -1;
   }

   public void stopGPSLogging()
   {
      connectToGPSLoggerService();
      if ( verifyConnection() ) 
      { 
         try
         {
            this.mGPSLoggerRemote.stopLogging();
         }
         catch (RemoteException e)
         {
            Log.e( GPSLoggerServiceManager.REMOTE_EXCEPTION, "Could not stop GPSLoggerService.", e );
         }
      }
      else 
      {
         Log.e( TAG, "No GPSLoggerRemote service connected to this manager" );
      }
   }
   
   

   private boolean verifyConnection()
   {
      
      switch( connection )
      {
         case( CONNECTED ):
            return true;
         case( DISCONNECTED ):
            connectToGPSLoggerService();
            try
            {
               Thread.sleep( 500 );
            }
            catch (InterruptedException e)
            {
               Log.w( TAG, "Disrupted whilst waiting for connection to complete" );
               return false;
            }
            return verifyConnection();
         case( CONNECTING ):
            return false;
         case( DISCONNECTING ):
            return false;
         default:
            return false;
      }

   }

   /**
    * Means by which an Activity lifecycle aware object hints about binding and unbinding
    */
   public void disconnectFromGPSLoggerService()
   {
      Log.d( TAG, "disconnectFromGPSLoggerService() on "+statusToString( connection ) );
      if( connection == CONNECTED )
      {
         connection = DISCONNECTING;
         try
         {
            this.mCtx.unbindService( this.mServiceConnection );
         }
         catch (IllegalArgumentException e) 
         {
            Log.e( TAG, "Failed to unbind a service, prehaps the service disapearded?", e );
         }
      }
      else 
      {
         Log.w( TAG, "Attempting to disconnect whilst "+statusToString( connection ) );
      }
   }

   /**
    * Means by which an Activity lifecycle aware object hints about binding and unbinding
    */
   private void connectToGPSLoggerService()
   {
      Log.d( TAG, "connectToGPSLoggerService() on "+statusToString( connection ) );
      if( connection == DISCONNECTED )
      {
         connection = CONNECTING;
         this.mCtx.bindService( new Intent( GPSLoggerService.SERVICENAME ), this.mServiceConnection, Context.BIND_AUTO_CREATE );
      }
      else 
      {
         Log.w( TAG, "Attempting to connect whilst "+statusToString( connection ) );
      }
   }
   
   private String statusToString( int status )
   {
      switch( status )
      {
         case DISCONNECTED:
            return "DISCONNECTED";
         case CONNECTING:
            return "DISCONNECTED";
         case CONNECTED:
            return "CONNECTED";
         case DISCONNECTING:
            return "DISCONNECTING";
         default:
            return "UNKNOWN";
      }
   }
}