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

import nl.sogeti.android.gpstracker.logger.GPSLoggerServiceManager;
import nl.sogeti.android.gpstracker.tests.R;
import nl.sogeti.android.gpstracker.tests.utils.MockGPSLoggerDriver;
import nl.sogeti.android.gpstracker.viewer.LoggerMap;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

/**
 * @version $Id$
 * @author rene (c) Jan 22, 2009, Sogeti B.V.
 */
public class OpenGPSTrackerDemo extends ActivityInstrumentationTestCase2<LoggerMap>
{

   private static final int ZOOM_LEVEL = 16;
   private static final Class<LoggerMap> CLASS = LoggerMap.class;
   private static final String PACKAGE = "nl.sogeti.android.gpstracker";
   private LoggerMap mLoggermap;
   private GPSLoggerServiceManager mLoggerServiceManager;
   private MapView mMapView;
   private MockGPSLoggerDriver mSender;

   public OpenGPSTrackerDemo()
   {
      super( PACKAGE, CLASS );
   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      this.mLoggermap = getActivity();
      this.mMapView = (MapView) this.mLoggermap.findViewById( nl.sogeti.android.gpstracker.R.id.myMapView );
      this.mSender = new MockGPSLoggerDriver();
   }

   protected void tearDown() throws Exception
   {
      this.mLoggerServiceManager.shutdown();
      super.tearDown();
   }

   
   
   /**
    * Start tracking and allow it to go on for 30 seconds
    * 
    * @throws InterruptedException
    */
   @LargeTest
   public void testTracking() throws InterruptedException
   {
            
      a_introSingelUtrecht30Seconds();

      c_startRoute10Seconds();

      d_showDrawMethods30seconds();
   
      e_statistics10Seconds();
      
      f_showPrecision30seconds();
      
      g_stopTracking10Seconds();
      
      h_shareTrack30Seconds();
      
      i_finish10Seconds();
      
   }

   @SmallTest
   public void a_introSingelUtrecht30Seconds() throws InterruptedException
   {
      this.mMapView.getController().setZoom( ZOOM_LEVEL);
      Thread.sleep( 1 * 1000 );
      // Browse the Utrecht map
      sendMessage( "Selecting a previous recorded track" );
      Thread.sleep( 1 * 1000 );
      this.sendKeys( "MENU DPAD_RIGHT" );
      Thread.sleep( 2 * 1000 );
      this.sendKeys( "L" );
      Thread.sleep( 2 * 1000 );
      sendMessage( "The walk around the \"singel\" in Utrecht" );
      this.sendKeys( "DPAD_CENTER" );
      Thread.sleep( 2 * 1000 );
   
      Thread.sleep( 2 * 1000 );
      sendMessage( "Scrolling about" );
      this.mMapView.getController().animateTo( new GeoPoint( 52095829, 5118599 ) );
      Thread.sleep( 2 * 1000 );
      this.mMapView.getController().animateTo( new GeoPoint( 52096778, 5125090 ) );
      Thread.sleep( 2 * 1000 );
      this.mMapView.getController().animateTo( new GeoPoint( 52085117, 5128255 ) );
      Thread.sleep( 2 * 1000 );
      this.mMapView.getController().animateTo( new GeoPoint( 52081517, 5121646 ) );
      Thread.sleep( 2 * 1000 );
      this.mMapView.getController().animateTo( new GeoPoint( 52093535, 5116711 ) );
      Thread.sleep( 2 * 1000 );
      this.sendKeys( "G G" );
      Thread.sleep( 5 * 1000 );
   }

   @SmallTest
   public void c_startRoute10Seconds() throws InterruptedException
   {      
      sendMessage( "Lets start a new route" );
      Thread.sleep( 1 * 1000 );
      this.sendKeys( "MENU DPAD_RIGHT DPAD_LEFT" );
      Thread.sleep( 2 * 1000 );
      this.sendKeys( "T" );//Toggle start/stop tracker
      Thread.sleep( 1 * 1000 );

      this.mMapView.getController().setZoom( ZOOM_LEVEL);
      
      this.sendKeys( "D E M O SPACE R O U T E ENTER" );
      Thread.sleep( 5 * 1000 );
      sendMessage( "The GPS logger is already running as a background service" );
      Thread.sleep( 5 * 1000 );
      this.sendKeys( "ENTER" );

      this.sendKeys( "T T T T" );
      
      Thread.sleep( 30 * 1000 );
      
      this.sendKeys( "G G" );
   }

   @SmallTest
   public void d_showDrawMethods30seconds() throws InterruptedException
   {
      sendMessage( "Track drawing color has different options" );
      
      this.mMapView.getController().setZoom( ZOOM_LEVEL );
      this.sendKeys( "MENU DPAD_RIGHT DPAD_RIGHT DPAD_RIGHT" );
      Thread.sleep( 2 * 1000 );
      this.sendKeys( "S" );
      Thread.sleep( 3 * 1000 );
      this.sendKeys( "DPAD_CENTER" );
      Thread.sleep( 1 * 1000 );
      this.sendKeys( "DPAD_UP DPAD_UP DPAD_UP DPAD_UP" );
      Thread.sleep( 2 * 1000 );
      this.sendKeys( "DPAD_CENTER" );
      Thread.sleep( 1 * 1000 );
      this.sendKeys( "BACK" );

      sendMessage( "Plain green" );

      Thread.sleep( 15 * 1000 );
      
      this.sendKeys( "MENU DPAD_RIGHT DPAD_RIGHT DPAD_RIGHT" );
      Thread.sleep( 2 * 1000 );
      this.sendKeys( "S" );
      Thread.sleep( 3 * 1000 );
      this.sendKeys( "MENU" );
      Thread.sleep( 1 * 1000 );
      this.sendKeys( "DPAD_CENTER" );
      Thread.sleep( 1 * 1000 );
      this.sendKeys( "DPAD_UP DPAD_UP DPAD_UP DPAD_UP" );
      Thread.sleep( 2 * 1000 );
      this.sendKeys( "DPAD_DOWN" );      
      Thread.sleep( 2 * 1000 );
      this.sendKeys( "DPAD_DOWN" );
      Thread.sleep( 2 * 1000 );
      this.sendKeys( "DPAD_DOWN" );      
      Thread.sleep( 2 * 1000 );
      this.sendKeys( "DPAD_DOWN DPAD_DOWN" );
      Thread.sleep( 2 * 1000 );
      this.sendKeys( "DPAD_UP");
      Thread.sleep( 2 * 1000 );
      this.sendKeys( "DPAD_CENTER" );
      Thread.sleep( 2 * 1000 );
      this.sendKeys( "BACK" );
      
      sendMessage( "Average speeds drawn" );
      
      Thread.sleep( 15 * 1000 );
   }

   @SmallTest
   public void e_statistics10Seconds() throws InterruptedException
   {      
      // Show of the statistics screen
      sendMessage( "Lets look at some statistics" );
      this.sendKeys( "MENU DPAD_RIGHT DPAD_RIGHT" );
      Thread.sleep( 2 * 1000 );
      this.sendKeys( "E" );
      Thread.sleep( 2 * 1000 );
      sendMessage( "Shows the basics on time, speed and distance" );
      Thread.sleep( 10 * 1000 );
      this.sendKeys( "BACK" );
   }

   @SmallTest
   public void f_showPrecision30seconds() throws InterruptedException
   {
      this.mMapView.getController().setZoom( ZOOM_LEVEL );
      
      sendMessage( "There are options on the precision of tracking" );
      
      this.sendKeys( "MENU DPAD_RIGHT DPAD_RIGHT DPAD_RIGHT" );
      Thread.sleep( 2 * 1000 );
      this.sendKeys( "S" );
      Thread.sleep( 3 * 1000 );
      this.sendKeys( "DPAD_DOWN DPAD_DOWN" );
      Thread.sleep( 1 * 1000 );
      this.sendKeys( "DPAD_CENTER" );
      Thread.sleep( 1 * 1000 );
      this.sendKeys( "DPAD_UP DPAD_UP" );
      Thread.sleep( 2 * 1000 );      
      this.sendKeys( "DPAD_DOWN DPAD_DOWN" );
      Thread.sleep( 2 * 1000 );
      this.sendKeys( "DPAD_UP" );
      Thread.sleep( 1 * 1000 );
      this.sendKeys( "DPAD_CENTER" );
      Thread.sleep( 1 * 1000 );
      this.sendKeys( "BACK" );
      
      sendMessage( "Course will drain the battery the least" );
      Thread.sleep( 5 * 1000 );
      sendMessage( "Fine will store the best track" );
      
      Thread.sleep( 10 * 1000 );
   }

   @SmallTest
   public void g_stopTracking10Seconds() throws InterruptedException
   {
      this.mMapView.getController().setZoom( ZOOM_LEVEL );
      
      Thread.sleep( 5 * 1000 );
      // Stop tracking
      sendMessage( "Stopping tracking" );
      this.sendKeys( "MENU DPAD_RIGHT DPAD_LEFT" );
      Thread.sleep( 2 * 1000 );
      this.sendKeys( "T" );
      Thread.sleep( 2 * 1000 );
   
      sendMessage( "Is the track stored?" );
      Thread.sleep( 1 * 1000 );
      this.sendKeys( "MENU DPAD_RIGHT" );
      Thread.sleep( 2 * 1000 );
      this.sendKeys( "L" );
      this.sendKeys( "DPAD_DOWN DPAD_DOWN" );
      Thread.sleep( 2 * 1000 );
      this.sendKeys( "DPAD_CENTER" );
      Thread.sleep( 2 * 1000 );
   }

   private void h_shareTrack30Seconds()
   {
      // TODO Auto-generated method stub
      
   }

   @SmallTest
   public void i_finish10Seconds() throws InterruptedException
   {
      this.mMapView.getController().setZoom( ZOOM_LEVEL );
      
      this.sendKeys( "G G" );
      Thread.sleep( 1 * 1000 );
      this.sendKeys( "G G" );
      Thread.sleep( 1 * 1000 );
      this.sendKeys( "G G" );
      sendMessage( "Thank you for watching this demo." );
      Thread.sleep( 10 * 1000 );

      Thread.sleep( 5 * 1000 );
   }

   private void sendMessage( String string )
   {
      this.mSender.sendSMS( string );
   }
}
