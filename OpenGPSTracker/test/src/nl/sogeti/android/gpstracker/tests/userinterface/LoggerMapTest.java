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
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.logger.GPSLoggerServiceManager;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.viewer.map.CommonLoggerMap;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.test.ActivityInstrumentationTestCase2;
import android.test.FlakyTest;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.Suppress;

import com.google.android.maps.MapView;

/** 
 * 
 * @version $Id$
 * @author rene (c) Jan 22, 2009, Sogeti B.V.
 */
public class LoggerMapTest extends ActivityInstrumentationTestCase2<CommonLoggerMap>
{
   private static final Class<CommonLoggerMap> CLASS = CommonLoggerMap.class;
   private static final String PACKAGE = "nl.sogeti.android.gpstracker";
   private CommonLoggerMap mLoggermap;
   private MapView mMapView;

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
      this.mMapView.setSatellite( false );
   }

   protected void tearDown() throws Exception
   {
      super.tearDown();
   }

   /**
    * Usecase A: Start logging
    * 
    * Start the MapView and start / stop the logging
    * @throws InterruptedException 
    * 
    */
   @MediumTest
   @Suppress
   public void testStartTracking() throws InterruptedException
   {
      GPSLoggerServiceManager serviceManager = new GPSLoggerServiceManager(  this.getInstrumentation().getContext() );
      serviceManager.startup( getActivity(), null );
      Assert.assertEquals( "The service should not be logging", Constants.STOPPED ,serviceManager.getLoggingState() );

      this.sendKeys( "MENU T DPAD_DOWN ENTER" );
      this.sendKeys("T E S T R O U T E ENTER ENTER");
      Assert.assertTrue("Title contains the current route name", this.mLoggermap.getTitle().toString().contains( "testroute" ));
      Assert.assertEquals( "The service should be logging", Constants.LOGGING ,serviceManager.getLoggingState() );

      this.sendKeys( "MENU T DPAD_DOWN DPAD_DOWN DPAD_DOWN DPAD_DOWN DPAD_CENTER" );
      Assert.assertEquals( "The service should not be logging", Constants.STOPPED ,serviceManager.getLoggingState() );
      serviceManager.shutdown( getActivity() );
   }
   
   /**
    * B: Background loging
    * @throws Exception 
    * 
    * 
    */
   @FlakyTest
   @MediumTest
   @Suppress
   public void testBackgroundTracking() throws Exception
   {
      GPSLoggerServiceManager serviceManager = new GPSLoggerServiceManager(  this.getInstrumentation().getContext() );
      serviceManager.startup( getActivity(), null );
      Assert.assertEquals( "The service should not be logging", Constants.STOPPED ,serviceManager.getLoggingState() );
      
      serviceManager.startGPSLogging("testBackgroundTracking");
      Assert.assertEquals( "The service should be logging", Constants.LOGGING ,serviceManager.getLoggingState() );

      //this.setUp();
      Assert.assertEquals( "The service should be logging", Constants.LOGGING ,serviceManager.getLoggingState() );

      this.sendKeys( "MENU T DPAD_DOWN ENTER" );     
      Assert.assertEquals( "The service should not be logging", Constants.STOPPED ,serviceManager.getLoggingState() );

      //this.sendKeys( "HOME" );
      Assert.assertEquals( "The service should not be logging", Constants.STOPPED ,serviceManager.getLoggingState() );
      //this.setUp();
      Assert.assertEquals( "The service should not be logging", Constants.STOPPED ,serviceManager.getLoggingState() );
      serviceManager.stopGPSLogging();
      
      serviceManager.shutdown( getActivity() );
   }    

   /**
    * 
    *  C: Review route
    * 
    */
   @MediumTest
   @Suppress
   public void testMapKeyControls()
   {     
      //1. Applicatie starten
      int startZoomlevel = this.mMapView.getZoomLevel();

      //2. Review map
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
   }

   /**
    * 
    *  Switch orientation during route review
    * 
    */
   @MediumTest
   @Suppress
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
   
   /**
    * @throws InterruptedException 
    * 
    * 
    */
   @LargeTest
   @Suppress
   public void testSpeedIncreaseColoring() throws InterruptedException
   {
      final int total = 250;
      final double lat1 = 52.195d;
      final double lon1 = 5.040d;
      final double lat2 = 51.882d;
      final double lon2 = 5.040d;
      final ContentResolver resolver = this.getActivity().getContentResolver();

      // E.g. returns: content://nl.sogeti.android.gpstracker/tracks/2
      final Uri trackUri = resolver.insert( Tracks.CONTENT_URI, null );
      final Uri segmentUri = resolver.insert( Uri.withAppendedPath( trackUri, "segments" ), null );
      final Uri waypointUri = Uri.withAppendedPath( segmentUri, "waypoints" );
      
      mLoggermap.runOnUiThread(
            new Runnable()
               {
                  public void run()
                  {
                     Intent newTrack = new Intent( Intent.ACTION_VIEW, trackUri );
                     mLoggermap.startActivity(newTrack);
                  }
               }
            );
      ContentValues wp = new ContentValues();
      wp.put( Waypoints.ACCURACY, new Double( 1d ) );
      wp.put( Waypoints.ALTITUDE, new Double( 5d ) );
      
      for( int step = 0; step < total / 2; step++ )
      {
         Thread.sleep( 1750 );
         double latitude = lat1 + ( ( lat1 - lat2 ) / total ) * step;
         double longtitude = lon1 + ( ( lon2 - lon1 ) / total ) * step;
         wp.put( Waypoints.LATITUDE, new Double( latitude ) );
         wp.put( Waypoints.LONGITUDE, new Double( longtitude ) );
         wp.put( Waypoints.SPEED, new Double( step / 5d ) );
         resolver.insert( waypointUri, wp );
      }
   }
}
