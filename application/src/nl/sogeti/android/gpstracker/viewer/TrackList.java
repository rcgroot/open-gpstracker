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
import nl.sogeti.android.gpstracker.actions.ExportGPX;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 *
 * @version $Id$
 * @author rene (c) Jan 11, 2009, Sogeti B.V.
 */
public class TrackList extends ListActivity
{
   private static final String TAG = "nl.sogeti.android.gpstracker.viewer.TrackList";
   private static final int MENU_DETELE = 0;
   private static final int MENU_EXPORT = 1;
   private static final int MENU_RENAME = 2;
   private Cursor mTracksCursor;
   SimpleCursorAdapter mNotes;
   private EditText mTrackNameView;

   private class DeleteClickListener implements DialogInterface.OnClickListener 
   {
      private Uri mUri;
      public DeleteClickListener(Uri uri) 
      {
         this.mUri = uri;
      }

      public void onClick(DialogInterface dialog, int which) 
      {
         getContentResolver().delete(this.mUri, null, null);

         TrackList.this.mTracksCursor.requery();
         TrackList.this.mNotes.notifyDataSetChanged();
      }
   }
   
   class RenameClickListener implements DialogInterface.OnClickListener 
   {
      private Uri mUri;
      public RenameClickListener(Uri uri) 
      {
         this.mUri = uri;
      }
      
      public void onClick( DialogInterface dialog, int which )
      {
         String trackName = mTrackNameView.getText().toString();
         
         ContentValues values = new ContentValues();
         values.put( Tracks.NAME, trackName);
         getContentResolver().update(
               mUri, 
               values, null, null );
         mTrackNameView = null;
      }
   } ;


   @Override
   protected void onCreate(Bundle savedInstanceState) 
   {
      super.onCreate(savedInstanceState);
      this.setContentView(R.layout.tracklist);

      this.mTracksCursor = managedQuery( Tracks.CONTENT_URI, new String[] { Tracks._ID, Tracks.NAME, Tracks.CREATION_TIME }, null, null, null);
      
      // Create an array to specify the fields we want to display in the list (only TITLE)
      // and an array of the fields we want to bind those fields to (in this case just text1)
      String[] fromColumns = new String[]{Tracks.NAME, Tracks.CREATION_TIME}; 
      int[] toItems = new int[]{R.id.listitem_name, R.id.listitem_from};

      // Now create a simple cursor adapter and set it to display
      this.mNotes = new SimpleCursorAdapter(this, R.layout.trackitem, this.mTracksCursor, fromColumns, toItems);
      setListAdapter(this.mNotes);

      // Add the context menu (the long press thing)
      registerForContextMenu(getListView());
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

   public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) 
   {
      if (menuInfo instanceof AdapterView.AdapterContextMenuInfo) 
      {
         AdapterView.AdapterContextMenuInfo itemInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
         TextView textView = (TextView) itemInfo.targetView.findViewById(android.R.id.text1);
         if (textView != null) 
         {
            menu.setHeaderTitle(textView.getText());
         }
      }
      menu.add(0, MENU_DETELE, 0, R.string.menu_deleteTrack);
      menu.add(0, MENU_EXPORT, 0, R.string.menu_shareTrack);
      menu.add(0, MENU_RENAME, 0, R.string.menu_renameTrack );
   }

   @Override
   public boolean onContextItemSelected(MenuItem item) {
      boolean handled = false ;
      AdapterView.AdapterContextMenuInfo info;
      try 
      {
         info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
      } 
      catch (ClassCastException e) 
      {
         Log.e(TAG, "Bad menuInfo", e);
         return handled;
      }

      Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
      switch (item.getItemId()) 
      {
         case MENU_DETELE: 
         {
            // Get confirmation
            Uri uri = ContentUris.withAppendedId( Tracks.CONTENT_URI, cursor.getLong( 0 ) );
            new AlertDialog.Builder(TrackList.this)
            .setTitle(R.string.dialog_deletetitle)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setMessage(R.string.dialog_deleteconfirmation)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, new DeleteClickListener(uri))
            .show();
            handled = true;
            break;
         }
         case MENU_EXPORT: 
         {
            Uri uri = ContentUris.withAppendedId( Tracks.CONTENT_URI, cursor.getLong( 0 ) );
            Intent actionIntent = new Intent(Intent.ACTION_SEND, uri );
            this.sendBroadcast( actionIntent, android.Manifest.permission.ACCESS_FINE_LOCATION );
            handled = true;
            break;
         }
         case MENU_RENAME: 
         {
            Uri uri = ContentUris.withAppendedId( Tracks.CONTENT_URI, cursor.getLong( 0 ) );
            CharSequence currentName = cursor.getString( 1 );
            LayoutInflater factory = LayoutInflater.from( this );
            View view = factory.inflate( R.layout.namedialog, null );
            mTrackNameView = (EditText) view.findViewById( R.id.nameField );
            mTrackNameView.setText( currentName );
            LoggerMap.createTrackTitleDialog( this, view, new RenameClickListener( uri ) ).show();
            handled = true;
            break;
         }
         default:
            handled = super.onContextItemSelected(item);
         break;
      }
      return handled;
   }



}
