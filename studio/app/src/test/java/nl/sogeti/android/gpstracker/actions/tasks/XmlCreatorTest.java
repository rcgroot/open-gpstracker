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
package nl.sogeti.android.gpstracker.actions.tasks;

import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.Assert;
import junit.framework.TestCase;

public class XmlCreatorTest extends TestCase {

    @SmallTest
    public void testCleanFilename() {
        String dirty = "abc=+:;/123";
        String clean = XmlCreator.cleanFilename(dirty, "ERROR");
        Assert.assertEquals("Cleaned", "abc123", clean);
    }

    @SmallTest
    public void testCleanFilenameEmpty() {
        String dirty = "";
        String clean = XmlCreator.cleanFilename(dirty, "Untitled");
        Assert.assertEquals("Cleaned", "Untitled", clean);
    }

    @SmallTest
    public void testCleanFilenameNull() {
        String dirty = null;
        String clean = XmlCreator.cleanFilename(dirty, "Untitled2");
        Assert.assertEquals("Cleaned", "Untitled2", clean);
    }

    @SmallTest
    public void testCleanFilenameAllSpecial() {
        String dirty = "!!??";
        String clean = XmlCreator.cleanFilename(dirty, "Untitled3");
        Assert.assertEquals("Cleaned", "Untitled3", clean);
    }
}
