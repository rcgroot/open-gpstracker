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
package nl.sogeti.android.gpstracker.tests.userinterface;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.logger.GPSLoggerServiceManager;
import nl.sogeti.android.gpstracker.tests.utils.MockGPSLoggerDriver;
import nl.sogeti.android.gpstracker.viewer.LoggerMap;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

/** 
 * 
 * @version $Id$
 * @author rene (c) Jan 22, 2009, Sogeti B.V.
 */
public class LoggerMapDemoTest extends ActivityInstrumentationTestCase2<LoggerMap>
{

   private static final Class<LoggerMap> CLASS = LoggerMap.class;
   private static final String PACKAGE = "nl.sogeti.android.gpstracker";
   private LoggerMap mLoggermap;
   private GPSLoggerServiceManager mLoggerServiceManager;
   private MapView mMapView;


   public LoggerMapDemoTest()
   {
      super( PACKAGE, CLASS );
   }


   @Override
   protected void setUp() throws Exception 
   {
      super.setUp();
      this.mLoggermap = getActivity();
      this.mLoggerServiceManager = new GPSLoggerServiceManager(this.mLoggermap);
      this.mMapView = (MapView) this.mLoggermap.findViewById( R.id.myMapView );
   }  

   protected void tearDown() throws Exception
   {
      this.mLoggerServiceManager.disconnectFromGPSLoggerService();
      super.tearDown();
   }

   /**
    * Start tracking and allow it to go on for 30 seconds
    * @throws InterruptedException 
    * 
    */
   @LargeTest
   public void testTracking() throws InterruptedException 
   {
      // Our data feeder to the emulator
      MockGPSLoggerDriver service = new MockGPSLoggerDriver( this.mLoggermap );
      try
      {
         /*
         Thread.sleep( 1 * 1000 );
         // Browse the Utrecht map
         service.sendSMS("Selecting a previous recorded track");
         Thread.sleep( 1 * 1000 );
         this.sendKeys( "MENU" );
         this.sendKeys( "DPAD_RIGHT DPAD_RIGHT" );
         Thread.sleep( 3 * 1000 );
         this.sendKeys( "L" );
         Thread.sleep( 2 * 1000 );

         service.sendSMS("The walk around the \"singel\" in Utrecht");
         this.mMapView.getController().animateTo( new GeoPoint(52095580, 5118041) );
         Thread.sleep( 2 * 1000 );
         this.sendKeys( "DPAD_CENTER" );

         Thread.sleep( 1 * 1000 );
         service.sendSMS("Zooming");
         this.sendKeys( "S T T T T T" );
         service.sendSMS("The swimming bits are GPS inaccuracy");
         Thread.sleep( 5 * 1000 );
         //service.sendSMS("Some buildings are 500 years old"); 
         //this.sendKeys( "S T " );
         this.sendKeys( "S G" );
*/
         // Start feeding the GPS API with location data
         new Thread( service ).start();
         service.sendSMS("Let start a new route");
         Thread.sleep( 1 * 1000 );
    
         // Start tracking 
         this.sendKeys( "MENU DPAD_RIGHT" );
         Thread.sleep( 3 * 1000 );

         this.sendKeys( "T" );
         Thread.sleep( 1 * 1000 );

         this.sendKeys("D E M O R O U T E ENTER");
         Thread.sleep( 2 * 1000 );
         service.sendSMS("It is already tracking me!");
         Thread.sleep( 2 * 1000 );
         this.sendKeys("ENTER");
         Thread.sleep( 2 * 1000 );
         
         service.sendSMS("Where are we?");
         Thread.sleep( 2 * 1000 );
         
         this.mMapView.getController().setZoom( 11 );
         
         int seconds = 0 ;
         while( service.getPositions() > 3 )
         {
            // Track
            Thread.sleep( 1 * 1000 );
            seconds++;
         }
         
         this.sendKeys( "T T T" );
         Thread.sleep( 1 * 1000 );
         service.sendSMS("Parked and arrived");
         Thread.sleep( 1 * 1000 );

         
         // Stop tracking
         service.sendSMS("Stopping tracking");
         this.sendKeys( "MENU DPAD_RIGHT" );
         Thread.sleep( 2 * 1000 );
         this.sendKeys( "T" );
         Thread.sleep( 1 * 1000 );
         

         service.sendSMS("Is the track stored allright?");
         Thread.sleep( 1 * 1000 );
         this.sendKeys( "MENU DPAD_RIGHT DPAD_RIGHT" );
         Thread.sleep( 2 * 1000 );
         this.sendKeys( "L" );
         this.sendKeys( "DPAD_DOWN DPAD_DOWN" );
         Thread.sleep( 2 * 1000 );
         service.sendSMS("Yes, it is");
         Thread.sleep( 1 * 1000 );
         this.sendKeys( "DPAD_CENTER" );
         Thread.sleep( 1 * 1000 );
         this.sendKeys( "G G" );
         Thread.sleep( 1 * 1000 );
         this.sendKeys( "G G" );
         Thread.sleep( 1 * 1000 );
         this.sendKeys( "G G" );
         Thread.sleep( 1 * 1000 );
         this.sendKeys( "G G" );
         service.sendSMS("Thank you for watching this demo.");
         Thread.sleep( 10 * 1000 );
      }
      finally
      {
         // Stop feeding the GPS API with location data
         service.stop();
      }

      Thread.sleep( 5 * 1000 );
   }
}
