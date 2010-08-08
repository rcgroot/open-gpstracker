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
package nl.sogeti.android.gpstracker.tests.actions;

import nl.sogeti.android.gpstracker.actions.utils.XmlCreator;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.test.mock.MockContentResolver;
import android.test.suitebuilder.annotation.SmallTest;
import junit.framework.Assert;
import junit.framework.TestCase;

public class ExportGPXTest extends TestCase
{   
   @SmallTest
   public void testIntentCreation()
   {
      ContentResolver resolver = new MockContentResolver();

      Uri uri = ContentUris.withAppendedId( Tracks.CONTENT_URI, 0 );
      Intent actionIntent = new Intent(Intent.ACTION_RUN, uri );
      actionIntent.setDataAndType( uri, Tracks.CONTENT_ITEM_TYPE );
      actionIntent.addFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION );
      
      // Action match
      Assert.assertEquals( "Action", actionIntent.getAction(), Intent.ACTION_RUN  );
      
      // Category match
      Assert.assertEquals( "Category", actionIntent.getCategories(), null );
      
      // Data match
      Assert.assertEquals( "Mock Infered Data Type", Tracks.CONTENT_ITEM_TYPE, actionIntent.resolveType( resolver ) );
      Assert.assertEquals( "Mock Data Type", Tracks.CONTENT_ITEM_TYPE, actionIntent.getType() ) ;
      
      Assert.assertEquals( "Data Schema", "content", actionIntent.getScheme() );
      Assert.assertEquals( "Data Authority", "nl.sogeti.android.gpstracker", actionIntent.getData().getAuthority() );
      Assert.assertEquals( "Data Path", "/tracks/0", actionIntent.getData().getPath() );
   }
   
   @SmallTest
   public void testCleanFilename()
   {
      String dirty = "abc=+:;/123";
      String clean = XmlCreator.cleanFilename( dirty, "ERROR" );
      Assert.assertEquals( "Cleaned", "abc123" , clean );
   }
   
   @SmallTest
   public void testCleanFilenameEmpty()
   {
      String dirty = "";
      String clean = XmlCreator.cleanFilename( dirty, "Untitled" );
      Assert.assertEquals( "Cleaned", "Untitled" , clean );
   }
   
   @SmallTest
   public void testCleanFilenameNull()
   {
      String dirty = null;
      String clean = XmlCreator.cleanFilename( dirty, "Untitled2" );
      Assert.assertEquals( "Cleaned", "Untitled2" , clean );
   }
   
   @SmallTest
   public void testCleanFilenameAllSpecial()
   {
      String dirty = "!!??";
      String clean = XmlCreator.cleanFilename( dirty, "Untitled3" );
      Assert.assertEquals( "Cleaned", "Untitled3" , clean );
   }
}
