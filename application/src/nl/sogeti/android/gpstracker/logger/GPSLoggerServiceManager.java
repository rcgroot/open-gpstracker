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
   private static final String LOG_TAGNAME = "GPSLoggerServiceManager";
   private static final String REMOTE_EXCEPTION = "REMOTE_EXCEPTION";
   private Context mCtx;
   private IGPSLoggerServiceRemote mGPSLoggerRemote;
   /**
    * Class for interacting with the main interface of the service.
    */
   private ServiceConnection mServiceConnection = new ServiceConnection()
   {
      public void onServiceConnected( ComponentName className, IBinder service )
      {
         Log.d(LOG_TAGNAME, "onServiceConnected()");
         GPSLoggerServiceManager.this.mGPSLoggerRemote = IGPSLoggerServiceRemote.Stub.asInterface( service );
         Log.d(LOG_TAGNAME, "GPSLoggerServiceRemote: " + GPSLoggerServiceManager.this.mGPSLoggerRemote);
      }
      public void onServiceDisconnected( ComponentName className )
      {
         Log.d(LOG_TAGNAME, "onServiceDisconnected()");
         GPSLoggerServiceManager.this.mGPSLoggerRemote = null;
      }
   };


   public GPSLoggerServiceManager(Context ctx)
   {
      Log.d(LOG_TAGNAME, "Created GPSLoggerServiceManager ");
      this.mCtx = ctx;
      SettingsManager settingsManager = SettingsManager.getInstance();
      connectToGPSLoggerService();

      if( settingsManager.isAutoStartService() )
      {
         startGPSLoggerService("Autostart");
      }

   }

   public boolean isLogging()
   {
      boolean logging = false;
      if ( this.mGPSLoggerRemote != null ) 
      { 
         try
         {
            Log.d( GPSLoggerServiceManager.LOG_TAGNAME, "mService.isLogging: " + this.mGPSLoggerRemote.isLogging() );
            logging = this.mGPSLoggerRemote.isLogging();
         }
         catch (RemoteException e)
         {
            Log.e( GPSLoggerServiceManager.REMOTE_EXCEPTION, "Could not determine GPSLoggerServiceStatus.", e );
            logging = false;
         }
      }
      else 
      {
         Log.e( LOG_TAGNAME, "No GPSLoggerRemote service connected to this manager" );
      }
      return logging;
   }

   public int startGPSLoggerService(String name)
   {
      connectToGPSLoggerService();
      if ( this.mGPSLoggerRemote != null ) 
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

   public void stopGPSLoggerService()
   {
      connectToGPSLoggerService();
      if ( this.mGPSLoggerRemote != null ) 
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
         Log.e( LOG_TAGNAME, "No GPSLoggerRemote service connected to this manager" );
      }
   }

   /**
    * Means by which an Activity lifecycle aware object hints about binding and unbinding
    */
   public void disconnectFromGPSLoggerService()
   {
      Log.d( LOG_TAGNAME, "ready to unbind : " + this.mServiceConnection );
      if( this.mServiceConnection != null )
      {
         this.mCtx.unbindService( this.mServiceConnection );
         this.mServiceConnection = null;
      }
   }

   /**
    * Means by which an Activity lifecycle aware object hints about binding and unbinding
    */
   public void connectToGPSLoggerService()
   {
      boolean succeed = this.mCtx.bindService( new Intent( GPSLoggerService.SERVICENAME ), this.mServiceConnection, Context.BIND_AUTO_CREATE );
      Log.d( LOG_TAGNAME, "bindService resulted in: "+succeed+" " + this.mServiceConnection );
   }
}