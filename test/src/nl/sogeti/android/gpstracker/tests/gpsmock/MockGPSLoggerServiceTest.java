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

import junit.framework.Assert;
import nl.sogeti.android.gpstracker.tests.utils.MockGPSLoggerDriver;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

/**
 * ????
 *
 *
 * @version $Id$
 * @author rene (c) Jan 22, 2009, Sogeti B.V.
 */
public class MockGPSLoggerServiceTest extends AndroidTestCase
{
   private static final String LOG_TAG = "MockGPSLoggerServiceTest";
   MockGPSLoggerDriver service;
   
   public MockGPSLoggerServiceTest()
   {
      this.service = new MockGPSLoggerDriver( getContext() );
   }
   
   @SmallTest
   public void testCreateLocationCommand()
   {
      Log.d(LOG_TAG, "Service: "+this.service ); 
      String command = MockGPSLoggerDriver.createLocationCommand( 5.117719d, 52.096524d, 0d );
      Assert.assertTrue("Start of a NMEA sentence: ", command.startsWith( "geo nmea $GPGGA" ));
      Assert.assertTrue("End of a NMEA sentence", command.endsWith( "05205.791440,N,00507.063140,E,1,10,0.0,0.0,0,0.0,0,0.0,0000\r\n" ));
   }
}
