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
package nl.sogeti.android.gpstracker.viewer;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/**
 *
 * @version $Id$
 * @author rene (c) Jan 11, 2009, Sogeti B.V.
 */
public class TrackList extends ListActivity
{
   @Override
   protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       this.setContentView(R.layout.tracklist);
       
       // Get all of the rows from the database and create the item list
       ContentResolver resolver = this.getContentResolver();
       Cursor tracksCursor = resolver.query( 
             Tracks.CONTENT_URI, 
             new String[] { Tracks._ID, Tracks.NAME, Tracks.CREATION_TIME }, 
             null, null, null );
       startManagingCursor(tracksCursor);

       // Create an array to specify the fields we want to display in the list (only TITLE)
       String[] from = new String[]{Tracks.NAME, Tracks.CREATION_TIME};

       // and an array of the fields we want to bind those fields to (in this case just text1)
       int[] to = new int[]{R.id.listitem_name, R.id.listitem_from};

       // Now create a simple cursor adapter and set it to display
       SimpleCursorAdapter notes = 
           new SimpleCursorAdapter(this, R.layout.trackitem, tracksCursor, from, to);
       setListAdapter(notes);
   }
   
   @Override
   protected void onListItemClick(ListView l, View v, int position, long id) {
       super.onListItemClick(l, v, position, id);
      
       Intent mIntent = new Intent();
       Bundle bundle = new Bundle();
       bundle.putLong( Tracks._ID, id );
       mIntent.putExtras(bundle);
       setResult(RESULT_OK, mIntent);
       finish();
   }
   
   
}
