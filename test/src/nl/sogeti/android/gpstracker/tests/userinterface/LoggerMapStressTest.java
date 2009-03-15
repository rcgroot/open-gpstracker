/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Mar 15, 2009 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 */
package nl.sogeti.android.gpstracker.tests.userinterface;

import com.google.android.maps.MapView;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.logger.GPSLoggerServiceManager;
import nl.sogeti.android.gpstracker.tests.gpsmock.MockGPSLoggerDriver;
import nl.sogeti.android.gpstracker.viewer.LoggerMap;
import android.os.Debug;
import android.test.ActivityInstrumentationTestCase;

/**
 * Goal is to feed as the LoggerMap as many points as possible to give it a good workout.
 *
 * @version $Id:$
 * @author rene (c) Mar 15, 2009, Sogeti B.V.
 */
public class LoggerMapStressTest extends ActivityInstrumentationTestCase<LoggerMap>
{
   private static final Class<LoggerMap> CLASS = LoggerMap.class;
   private static final String PACKAGE = "nl.sogeti.android.gpstracker";
   private LoggerMap mLoggermap;
   private GPSLoggerServiceManager mLoggerServiceManager;
   private MapView mMapView;

   public LoggerMapStressTest()
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
    * Just pours a lot of tracking actions at the application
    * 
    * @throws InterruptedException
    */
   public void testLapsAroundUtrecht() throws InterruptedException
   {    
      // Our data feeder to the emulator
      MockGPSLoggerDriver service = new MockGPSLoggerDriver( this.mLoggermap );
      service.setTimeout( 10 );
      service.setRoute( R.xml.rondjesingelutrecht );

      this.sendKeys( "T T T T T T T" );
      this.sendKeys( "MENU DPAD_RIGHT T T E S T R O U T E ENTER");
      this.sendKeys("ENTER");      
      Thread feeder = new Thread( service );
      feeder.start();

      // Start method tracing for Issue 18
      Debug.startMethodTracing("testLapsAroundUtrecht");
      while( feeder.isAlive() )
      {
         Thread.sleep( 5 * 1000 );
      }
      // Start method tracing for Issue 18
      Debug.stopMethodTracing();
   }

}
