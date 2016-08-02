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

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.Assert;

import nl.sogeti.android.gpstracker.tests.R;
import nl.sogeti.android.gpstracker.tests.utils.MockGPSLoggerDriver;

/**
 * ????
 *
 * @author rene (c) Jan 22, 2009, Sogeti B.V.
 * @version $Id$
 */
public class MockGPSLoggerServiceTest extends AndroidTestCase {
    MockGPSLoggerDriver service;

    public MockGPSLoggerServiceTest() {
        this.service = new MockGPSLoggerDriver(getContext(), R.xml.denhaagdenbosch, 1000);
    }

    @SmallTest
    public void testGPGGACreateLocationCommand() {
        String command = MockGPSLoggerDriver.createGPGGALocationCommand(5.117719d, 52.096524d, 0d);
        Assert.assertTrue("Start of a NMEA sentence: ", command.startsWith("GPGGA"));
        Assert.assertTrue("Body of a NMEA sentence", command.contains("05205.791440"));
    }

    @SmallTest
    public void testGPRMCreateLocationCommand() {
        String command = MockGPSLoggerDriver.createGPRMCLocationCommand(5.117719d, 52.096524d, 0d, 0d);
        Assert.assertTrue("Start of a NMEA sentence: ", command.startsWith("GPRMC"));
        Assert.assertTrue("Body of a NMEA sentence", command.contains("05205.791440"));
    }

    @SmallTest
    public void testCalulateChecksum() {
        Assert.assertEquals("4F", MockGPSLoggerDriver.calulateChecksum("GPGGA,064746.000,4925.4895,N,00103.9255,E,1,05," +
                "2.1,-68.0,M,47.1,M,,0000"));
        Assert.assertEquals("47", MockGPSLoggerDriver.calulateChecksum("GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9," +
                "545.4,M,46.9,M,,"));
        Assert.assertEquals("39", MockGPSLoggerDriver.calulateChecksum("GPRMC,120557.916,A,5058.7456,N,00647.0515,E,0" +
                ".00,82.33,220503,,"));
    }
}
