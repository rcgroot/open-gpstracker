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
package nl.sogeti.android.gpstracker.tests.gpsmock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import nl.sogeti.android.gpstracker.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;

/**
 * Service that logs GPS-location
 * 
 * @version $Id$
 * @author Maarten van Berkel (maarten.van.berkel@sogeti.nl / +0586)
 *
 */
public class MockGPSLoggerService implements Runnable
{
   private static final String LOG_TAG = "GPSLoggerService";
   private final static int DEFAULT_TIMEOUT = 2000;
   private boolean running = true;
   private int timeout;
   private Context mContext;
   private TelnetPositionSender sender;
   private ArrayList<SimplePosition> positions;

   public MockGPSLoggerService(Context context)
   {
      Log.d( MockGPSLoggerService.LOG_TAG, "Constructor LoggerThread()" );
      this.timeout = DEFAULT_TIMEOUT;
      this.mContext = context;
      this.sender = new TelnetPositionSender();
   }

   public int getPositions()
   {
      return this.positions.size();
   }

   public int getTimeout()
   {
      return this.timeout;
   }

   private void prepareRun(int xmlResource) 
   {
      this.positions = new ArrayList<SimplePosition>();
      XmlResourceParser xmlParser = this.mContext.getResources().getXml( xmlResource );
      doUglyXMLParsing( this.positions, xmlParser );
   }

   public void run()
   {
      Log.d( MockGPSLoggerService.LOG_TAG, "GPSLoggerService.run() start" );
      prepareRun( R.xml.denhaagdenbosch );      
      //prepareRun( R.xml.rondjesingelutrecht );


      while (this.running && ( this.positions.size() > 0 ) )
      {
         SimplePosition position = this.positions.remove( 0 );
         Log.d( LOG_TAG, "Position: " + position );
         String nmeaCommand = createLocationCommand(position.getLongitude(), position.getLatitude(), 0);
         this.sender.sendCommand( nmeaCommand );

         try
         {
            Thread.sleep( this.timeout );
         }
         catch (InterruptedException e)
         {
            Log.d( MockGPSLoggerService.LOG_TAG, "Interrupted" );
         }
      }
      Log.d( MockGPSLoggerService.LOG_TAG, "GPSLoggerService.run() finished" );
   }

   public void stop()
   {
      this.running = false;
   }

   private void doUglyXMLParsing( ArrayList<SimplePosition> positions, XmlResourceParser xmlParser )
   {
      int eventType;
      try
      {
         eventType = xmlParser.getEventType();

         while (eventType != XmlPullParser.END_DOCUMENT)
         {
            if 
            (
                  ( eventType == XmlPullParser.START_TAG ) && 
                  ( xmlParser.getName().equals( "trkpt" ) || xmlParser.getName().equals( "rtept" ) )
            )
            {
               positions.add( new SimplePosition( xmlParser.getAttributeFloatValue( 0, 12.3456F ), xmlParser.getAttributeFloatValue( 1, 12.3456F ) ) );
            }
            eventType = xmlParser.next();
         }
      }
      catch (XmlPullParserException e)
      { /* ignore */
      }
      catch (IOException e)
      {/* ignore */
      }
   }

   protected static String createLocationCommand(double longitude, double latitude, double elevation) 
   {
      
      final String COMMAND_GPS = 
         "geo nmea $GPGGA,%1$02d%2$02d%3$02d.%4$03d," + //$NON-NLS-1$
         "%5$03d%6$09.6f,%7$c,%8$03d%9$09.6f,%10$c," + //$NON-NLS-1$
         "1,10,0.0,0.0,0,0.0,0,0.0,0000\r\n"; //$NON-NLS-1$
      Calendar c = Calendar.getInstance();

      double absLong = Math.abs(longitude);
      int longDegree = (int)Math.floor(absLong);
      char longDirection = 'E';
      if (longitude < 0) {
         longDirection = 'W';
      }

      double longMinute = (absLong - Math.floor(absLong)) * 60;

      double absLat = Math.abs(latitude);
      int latDegree = (int)Math.floor(absLat);
      char latDirection = 'N';
      if (latitude < 0) {
         latDirection = 'S';
      }

      double latMinute = (absLat - Math.floor(absLat)) * 60;

      String command = String.format(COMMAND_GPS,
            c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE),
            c.get(Calendar.SECOND), c.get(Calendar.MILLISECOND),
            latDegree, latMinute, latDirection,
            longDegree, longMinute, longDirection);

      return command;
   }

   class SimplePosition {

      double lat, lng;

      public SimplePosition(float latitude, float longtitude)
      {
         this.lat = latitude;
         this.lng = longtitude;
      }

      public double getLatitude()
      {
         return this.lat;
      }

      public double getLongitude()
      {
         return this.lng;
      }      
   }

   public void sendSMS( String string )
   {
      this.sender.sendCommand( "sms send 31886606607 "+string+"\r\n");
   }

}