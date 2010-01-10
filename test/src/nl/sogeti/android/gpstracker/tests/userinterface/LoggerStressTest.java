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
import android.os.Debug;
import android.test.ActivityInstrumentationTestCase2;
import android.test.PerformanceTestCase;
import android.test.suitebuilder.annotation.LargeTest;

/**
 * Goal is to feed as the LoggerMap as many points as possible to give it a good workout.
 *
 * @version $Id$
 * @author rene (c) Mar 15, 2009, Sogeti B.V.
 */
public class LoggerStressTest extends ActivityInstrumentationTestCase2<LoggerMap> implements PerformanceTestCase
{
   private static final Class<LoggerMap> CLASS = LoggerMap.class;
   private static final String PACKAGE = "nl.sogeti.android.gpstracker";
   private LoggerMap mLoggermap;
   private GPSLoggerServiceManager mLoggerServiceManager;
   private Intermediates mIntermediates;

   public LoggerStressTest()
   {
      super( PACKAGE, CLASS );
   }

   @Override
   protected void setUp() throws Exception 
   {
      super.setUp();
      this.mLoggermap = getActivity();
      this.mLoggerServiceManager = new GPSLoggerServiceManager(this.mLoggermap);
   }  

   protected void tearDown() throws Exception
   {
      this.mLoggerServiceManager.shutdown();
      super.tearDown();
   }
   
   /**
    * Just pours a lot of tracking actions at the application
    * 
    * @throws InterruptedException
    */
   @LargeTest
   public void testLapsAroundUtrecht() throws InterruptedException
   {    
      // Our data feeder to the emulator
      MockGPSLoggerDriver service = new MockGPSLoggerDriver( getInstrumentation().getContext(), R.xml.rondjesingelutrecht, 10 );

      this.sendKeys( "T T T T" );
      this.sendKeys( "MENU DPAD_RIGHT T T E S T R O U T E ENTER");
      this.sendKeys("ENTER"); 

      // Start method tracing for Issue 18
      //Debug.startMethodTracing("testLapsAroundUtrecht");
      if( this.mIntermediates != null )
      {
         this.mIntermediates.startTiming( true ) ;
      }

      service.run();

      // Start method tracing for Issue 18
      if( this.mIntermediates != null )
      {
         this.mIntermediates.finishTiming( true ) ;
      }
      //Debug.stopMethodTracing();
   }

   public boolean isPerformanceOnly()
   {
      return true;
   }

   public int startPerformance( Intermediates intermediates )
   {
      this.mIntermediates = intermediates;
      return 1;
   }
}
