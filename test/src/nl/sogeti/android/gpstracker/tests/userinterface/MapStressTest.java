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
import nl.sogeti.android.gpstracker.viewer.LoggerMap;
import android.os.Debug;
import android.test.ActivityInstrumentationTestCase2;
import android.test.PerformanceTestCase;
import android.test.suitebuilder.annotation.LargeTest;

/**
 * Goal is to feed as the LoggerMap as many points as possible to give it a good workout.
 *
 * @version $Id: LoggerMapStressTest.java 47 2009-05-17 19:15:00Z rcgroot $
 * @author rene (c) Mar 15, 2009, Sogeti B.V.
 */
public class MapStressTest extends ActivityInstrumentationTestCase2<LoggerMap> implements PerformanceTestCase
{
   private static final Class<LoggerMap> CLASS = LoggerMap.class;
   private static final String PACKAGE = "nl.sogeti.android.gpstracker";
   private LoggerMap mLoggermap;
   private GPSLoggerServiceManager mLoggerServiceManager;
   private Intermediates mIntermediates;

   public MapStressTest()
   {
      super( PACKAGE, CLASS );
   }

   @Override
   protected void setUp() throws Exception 
   {
      super.setUp();
      this.mLoggermap = getActivity();
   }  

   protected void tearDown() throws Exception
   {
      super.tearDown();
   }
   
   /**
    * Open the first track in the list and scroll around 
    * forcing redraws during a perf test
    * 
    * @throws InterruptedException
    */
   @LargeTest
   public void testBrowseFirstTrack() throws InterruptedException
   {    
      final int duration = 10;
      int seconds = 0;
      String[] timeActions = {"T", "T", "T", "T","G", "T", "T", "T", "T","G", "G", "G", "T", "T", "T", "T","G", "G", "G"};
      
      this.sendKeys( "MENU L" );
      this.sendKeys( "DPAD_RIGHT ENTER");
      this.sendKeys("ENTER");      
      // Start method tracing for Issue 18
      Debug.startMethodTracing("testBrowseFirstTrack");
      if( this.mIntermediates != null )
      {
         this.mIntermediates.startTiming( true ) ;
      }
      while( seconds < duration )
      {
         Thread.sleep( 1 * 1000 );
         this.sendKeys( timeActions[seconds] );
         seconds++;
      }
      if( this.mIntermediates != null )
      {
         this.mIntermediates.finishTiming( true ) ;
      }
      Debug.stopMethodTracing();
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
