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
package nl.sogeti.android.gpstracker.tests;

import junit.framework.TestSuite;
import nl.sogeti.android.gpstracker.tests.actions.ExportGPXTest;
import nl.sogeti.android.gpstracker.tests.db.GPStrackingProviderTest;
import nl.sogeti.android.gpstracker.tests.gpsmock.MockGPSLoggerServiceTest;
import nl.sogeti.android.gpstracker.tests.logger.GPSLoggerServiceTest;
import android.test.InstrumentationTestRunner;
import android.test.InstrumentationTestSuite;

/**
 * Perform unit tests Run on the adb shell:
 * 
 * <pre>
 *   am instrument -w nl.sogeti.android.gpstracker.tests/.GPStrackingInstrumentation
 * </pre>
 * 
 * @version $Id$
 * @author rene (c) Jan 22, 2009, Sogeti B.V.
 */
public class GPStrackingInstrumentation extends InstrumentationTestRunner
{

   /**
    * (non-Javadoc)
    * 
    * @see android.test.InstrumentationTestRunner#getAllTests()
    */
   @Override
   public TestSuite getAllTests()
   {
      TestSuite suite = new InstrumentationTestSuite(this);
      suite.setName("GPS Tracking Testsuite");
      suite.addTestSuite(GPStrackingProviderTest.class);
      suite.addTestSuite(MockGPSLoggerServiceTest.class);
      suite.addTestSuite(GPSLoggerServiceTest.class);
      suite.addTestSuite(ExportGPXTest.class);

      //      suite.addTestSuite( OpenGPSTrackerDemo.class );   // The demo recorded for youtube
      //      suite.addTestSuite( MapStressTest.class );       // The stress test of the map viewer
      return suite;
   }

   /**
    * (non-Javadoc)
    * 
    * @see android.test.InstrumentationTestRunner#getLoader()
    */
   @Override
   public ClassLoader getLoader()
   {
      return GPStrackingInstrumentation.class.getClassLoader();
   }
}
