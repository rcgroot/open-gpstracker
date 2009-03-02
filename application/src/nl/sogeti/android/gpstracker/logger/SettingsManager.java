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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import android.util.Log;

/**
 *
 * @version $Id$
 * @author rene (c) Jan 18, 2009, Sogeti B.V.
 */
public class SettingsManager
{
   public static final String LOG_TAG = "GPS_TRACKER_SETTINGS_MANAGER";

   private static SettingsManager settingsManager;

   private static final boolean DEFAULT_AUTO_START_SERVICE = false;

   public static synchronized SettingsManager getInstance()
   {
      if (SettingsManager.settingsManager == null)
      {
         SettingsManager.settingsManager = new SettingsManager();
      }
      return SettingsManager.settingsManager;
   }

   private boolean autoStartService = SettingsManager.DEFAULT_AUTO_START_SERVICE;

   private Properties properties;

   private File propertiesFile;

   private SettingsManager()
   {
      readPropertiesFromFile();
   }

   public boolean isAutoStartService()
   {
      return this.autoStartService;
   }

   private void readPropertiesFromFile()
   {
      this.properties = new Properties();
      this.propertiesFile = new File( "GPSTracker.properties" );
      if (this.propertiesFile.exists())
      {
         FileInputStream propertiesFileInputStream = null;
         try
         {
            propertiesFileInputStream = new FileInputStream( this.propertiesFile );
            this.properties.load( propertiesFileInputStream );

            this.autoStartService = Boolean.valueOf( this.properties.getProperty( "auto_start_service", String.valueOf( SettingsManager.DEFAULT_AUTO_START_SERVICE ) ) );
        }
         catch (FileNotFoundException e)
         {
            Log.e( SettingsManager.LOG_TAG, "Coult not read properties", e );
         }
         catch (IOException e)
         {
            Log.e( SettingsManager.LOG_TAG, "Coult not read properties", e );
         }
         finally 
         {
            if( propertiesFileInputStream != null )
            {
               try
               {
                  propertiesFileInputStream.close();
               }
               catch (IOException e)
               {
                  Log.e( SettingsManager.LOG_TAG, "Could not close properties", e );

               }
            }
         }
      }
   }

   public void setAutoStartService( boolean autoStartService )
   {
      this.autoStartService = autoStartService;
      writePropertiesToFile();
   }

   private void writePropertiesToFile()
   {
      FileOutputStream propertiesFileOutputStream = null;
      try
      {
         boolean newfile = this.propertiesFile.createNewFile();
         if(!newfile) 
         {
            Log.e( SettingsManager.LOG_TAG, "Overwriting old file" );
         }
         propertiesFileOutputStream = new FileOutputStream( this.propertiesFile );
         this.properties.save( propertiesFileOutputStream, "The GPSTracker properties" );
      }
      catch (FileNotFoundException e)
      {
         Log.e( SettingsManager.LOG_TAG, "Coult not write properties", e );
      }
      catch (IOException e)
      {
         Log.e( SettingsManager.LOG_TAG, "Coult not write properties", e );
      }
      finally 
      {
         if( propertiesFileOutputStream != null )
         {
            try
            {
               propertiesFileOutputStream.close();
            }
            catch (IOException e)
            {
               Log.e( SettingsManager.LOG_TAG, "Could not close properties", e );

            }
         }
      }

   }

}
