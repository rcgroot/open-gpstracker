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

import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.test.mock.MockContentResolver;
import junit.framework.Assert;
import junit.framework.TestCase;

public class ExportGPXTest extends TestCase
{

   
   
   public void testIntentCreation()
   {
      ContentResolver resolver = new MockContentResolver();
      
      Intent actionIntent = new Intent(Intent.ACTION_SEND, Uri.withAppendedPath( Tracks.CONTENT_URI, ""+0 ) );
      
      // Action match
      Assert.assertEquals( "Action", actionIntent.getAction(), Intent.ACTION_SEND  );
      
      // Category match
      Assert.assertEquals( "Category", actionIntent.getCategories(), null );
      
      // Data match
      Assert.assertEquals( "Mock Infered Data Type", null, actionIntent.resolveType( resolver ) );
      Assert.assertEquals( "Mock Data Type", actionIntent.getType(), null ) ;
      
      Assert.assertEquals( "Data Schema", actionIntent.getScheme(), "content" );
      Assert.assertEquals( "Data Authority", actionIntent.getData().getAuthority(), "nl.sogeti.android.gpstracker" );
      Assert.assertEquals( "Data Path", actionIntent.getData().getPath(), "/tracks/0" );
   }
}
