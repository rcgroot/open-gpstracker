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

import junit.framework.Assert;
import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.logger.GPSLoggerServiceManager;
import nl.sogeti.android.gpstracker.viewer.LoggerMap;
import android.content.pm.ActivityInfo;
import android.test.ActivityInstrumentationTestCase;
import com.google.android.maps.MapView;

/** 
 * 
 * @version $Id$
 * @author rene (c) Jan 22, 2009, Sogeti B.V.
 */
public class LoggerMapTest extends ActivityInstrumentationTestCase<LoggerMap>
{
   
   private static final Class<LoggerMap> CLASS = LoggerMap.class;
   private static final String PACKAGE = "nl.sogeti.android.gpstracker";
   private LoggerMap mLoggermap;
   private MapView mMapView;
   private GPSLoggerServiceManager mLoggerServiceManager;

   public LoggerMapTest()
   {
      super( PACKAGE, CLASS );
   }
   

   @Override
   protected void setUp() throws Exception 
   {
       super.setUp();
       this.mLoggermap = getActivity();
       this.mMapView = (MapView) this.mLoggermap.findViewById( R.id.myMapView );
       this.mLoggerServiceManager = new GPSLoggerServiceManager(this.mLoggermap);
       this.mLoggerServiceManager.stopGPSLoggerService();
   }  
   
   protected void tearDown() throws Exception
   {
      this.mLoggerServiceManager.stopGPSLoggerService();
      this.mLoggerServiceManager.disconnectFromGPSLoggerService();
      super.tearDown();
   }
   
   /**
    * Usecase A: Start logging
    * 
    * Start the MapView and start / stop the logging
    * @throws InterruptedException 
    * 
    */
   public void testStartTracking() throws InterruptedException
   {

      Assert.assertTrue( "No tracking at startup", !this.mLoggerServiceManager.isLogging() );

      this.sendKeys( "MENU T" );
      this.sendKeys("T E S T R O U T E ENTER ENTER");
      Assert.assertTrue("Title contains the current route name", this.mLoggermap.getTitle().toString().contains( "testroute" ));
      Assert.assertTrue( "Tracking started", this.mLoggerServiceManager.isLogging() );
      
      this.sendKeys( "MENU T" );
      Assert.assertTrue( "Tracking stoped", !this.mLoggerServiceManager.isLogging() );
   }

   /**
    * B: Background loging
    * @throws Exception 
    * 
    * 
    */
   public void testBackgroundTracking() throws Exception
   {
      Assert.assertTrue( "No tracking at startup", !this.mLoggerServiceManager.isLogging() );
      
      this.mLoggerServiceManager.startGPSLoggerService("testBackgroundTracking");
      Assert.assertTrue( "Tracking started", this.mLoggerServiceManager.isLogging() );
      
      //this.setUp();
      Assert.assertTrue( "Still continue tracking", this.mLoggerServiceManager.isLogging() );
      
      this.sendKeys( "MENU T" );     
      Assert.assertTrue( "Tracking stopped", !this.mLoggerServiceManager.isLogging() );
      
      //this.sendKeys( "HOME" );
      Assert.assertTrue( "Tracking still stopped", !this.mLoggerServiceManager.isLogging() );
      //this.setUp();
      Assert.assertTrue( "And still tracking is stopped", !this.mLoggerServiceManager.isLogging() );
   }    
   
   /**
    * 
    *  C: Review route
    * 
    */
   public void testMapKeyControls()
   {
      //1. Applicatie starten
      int startZoomlevel = this.mMapView.getZoomLevel();
      this.mMapView.setStreetView( true );
      
      //2. Route historie openen
      this.sendKeys( "MENU L" );     
      
      //3. Route uit de historie openen
      this.sendKeys( "DPAD_DOWN DPAD_DOWN DPAD_CENTER");
      
      //4. Review route
      this.sendKeys( "T T" );
      Assert.assertEquals("Twice zoomed in", startZoomlevel+2, this.mMapView.getZoomLevel());
      this.sendKeys( "G G" );
      Assert.assertEquals("Not zoomed in", startZoomlevel, this.mMapView.getZoomLevel());
      this.sendKeys( "S" );
      Assert.assertTrue("In satellite mode", this.mMapView.isSatellite() );
      this.sendKeys( "G G" );
      Assert.assertEquals("Twice zoomed out", startZoomlevel-2, this.mMapView.getZoomLevel());
      this.sendKeys( "T T" );
      Assert.assertEquals("Not zoomed in", startZoomlevel, this.mMapView.getZoomLevel());
      this.sendKeys( "S" );
      
      //5. Verwijderen route
      this.sendKeys( "DPAD_DOWN DPAD_DOWN MENU D");
      //TODO
      
      //6. Applicatie stoppen 
   }
   
   /**
    * 
    *  Switch orientation during route review
    * 
    */
   public void testOrientationSwitch()
   {
      this.mLoggermap.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
      
      // Route historie openen
      this.sendKeys( "MENU L" );     
      
      // Route uit de historie openen
      this.sendKeys( "DPAD_DOWN DPAD_DOWN DPAD_CENTER");
      
      // Review route
      this.sendKeys( "T T" );
      
      // Switch orientation
      this.mLoggermap.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

      this.sendKeys( "G G" );
      
      // Switch orientation
      this.mLoggermap.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


   }
}
