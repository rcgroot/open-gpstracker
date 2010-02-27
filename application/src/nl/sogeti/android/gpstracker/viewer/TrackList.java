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
import nl.sogeti.android.gpstracker.util.Constants;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * @version $Id$
 * @author rene (c) Jan 11, 2009, Sogeti B.V.
 */
public class TrackList extends ListActivity
{
   private static final String TAG = "OGT.TrackList";
   private static final int MENU_DETELE = 0;
   private static final int MENU_SHARE = 1;
   private static final int MENU_RENAME = 2;
   private static final int MENU_STATS = 3;

   public static final int DIALOG_FILENAME = 0;
   private static final int DIALOG_RENAME = 23;
   private static final int DIALOG_DELETE = 24;
   private static final int MENU_SEARCH = 0;

   private EditText mTrackNameView;
   private Uri mDialogUri;
   private String mDialogCurrentName = "";

   private OnClickListener mDeleteOnClickListener = new DialogInterface.OnClickListener()
      {
         public void onClick( DialogInterface dialog, int which )
         {
            getContentResolver().delete( mDialogUri, null, null );
         }
      };
   private OnClickListener mRenameOnClickListener = new DialogInterface.OnClickListener()
      {
         public void onClick( DialogInterface dialog, int which )
         {
            //         Log.d( TAG, "Context item selected: "+mDialogUri+" with name "+mDialogCurrentName );

            String trackName = mTrackNameView.getText().toString();
            ContentValues values = new ContentValues();
            values.put( Tracks.NAME, trackName );
            TrackList.this.getContentResolver().update( mDialogUri, values, null, null );
         }
      };

   @Override
   protected void onCreate( Bundle savedInstanceState )
   {
      super.onCreate( savedInstanceState );
      this.setContentView( R.layout.tracklist );
      displayIntent( getIntent() );

      // Add the context menu (the long press thing)
      registerForContextMenu( getListView() );
   }

   @Override
   public void onNewIntent( Intent newIntent )
   {
      displayIntent( newIntent );
   }

   /*
    * (non-Javadoc)
    * @see android.app.ListActivity#onRestoreInstanceState(android.os.Bundle)
    */
   @Override
   protected void onRestoreInstanceState( Bundle state )
   {
      mDialogUri = state.getParcelable( "URI" );
      mDialogCurrentName = state.getString( "NAME" );
      super.onRestoreInstanceState( state );
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
    */
   @Override
   protected void onSaveInstanceState( Bundle outState )
   {
      outState.putParcelable( "URI", mDialogUri );
      outState.putString( "NAME", mDialogCurrentName );
      super.onSaveInstanceState( outState );
   }
   
   @Override
   public boolean onCreateOptionsMenu( Menu menu )
   {
      boolean result = super.onCreateOptionsMenu( menu );
   
      menu.add( ContextMenu.NONE, MENU_SEARCH, ContextMenu.NONE, android.R.string.search_go ).setIcon( android.R.drawable.ic_search_category_default).setAlphabeticShortcut( SearchManager.MENU_KEY );
      return result;
   }
   
   @Override
   public boolean onOptionsItemSelected( MenuItem item )
   {
      boolean handled = false;
      switch (item.getItemId())
      {
         case MENU_SEARCH:
            onSearchRequested();
            handled = true;
            break;
         default:
            handled = super.onOptionsItemSelected( item );
      }
      return handled;
   }

   @Override
   protected void onListItemClick( ListView l, View v, int position, long id )
   {
      super.onListItemClick( l, v, position, id );
      

      Bundle bundle = new Bundle();
      bundle.putLong( Constants.EXTRA_TRACK_ID, id );
      
      Intent intent = new Intent();
      intent.putExtras( bundle );
      
      ComponentName caller = this.getCallingActivity();
      if( caller != null )
      {
         setResult( RESULT_OK, intent );
         finish();
      }
      else
      {
         intent.setClass( this, LoggerMap.class );
         startActivity( intent );
      }
   }

   @Override
   public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo )
   {
      if( menuInfo instanceof AdapterView.AdapterContextMenuInfo )
      {
         AdapterView.AdapterContextMenuInfo itemInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
         TextView textView = (TextView) itemInfo.targetView.findViewById( android.R.id.text1 );
         if( textView != null )
         {
            menu.setHeaderTitle( textView.getText() );
         }
      }
      menu.add( 0, MENU_STATS, 0, R.string.menu_statistics );
      menu.add( 0, MENU_SHARE, 0, R.string.menu_shareTrack );
      menu.add( 0, MENU_RENAME, 0, R.string.menu_renameTrack );
      menu.add( 0, MENU_DETELE, 0, R.string.menu_deleteTrack );
   }

   @Override
   public boolean onContextItemSelected( MenuItem item )
   {
      boolean handled = false;
      AdapterView.AdapterContextMenuInfo info;
      try
      {
         info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
      }
      catch (ClassCastException e)
      {
         Log.e( TAG, "Bad menuInfo", e );
         return handled;
      }

      Cursor cursor = (Cursor) getListAdapter().getItem( info.position );
      mDialogUri = ContentUris.withAppendedId( Tracks.CONTENT_URI, cursor.getLong( 0 ) );
      mDialogCurrentName = cursor.getString( 1 );
      switch (item.getItemId())
      {
         case MENU_DETELE:
         {
            showDialog( DIALOG_DELETE );
            handled = true;
            break;
         }
         case MENU_SHARE:
         {
            Intent actionIntent = new Intent( Intent.ACTION_RUN );
            actionIntent.setDataAndType( mDialogUri, Tracks.CONTENT_ITEM_TYPE );
            actionIntent.addFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION );
            startActivity( Intent.createChooser( actionIntent, getString( R.string.chooser_title ) ) );
            handled = true;
            break;
         }
         case MENU_RENAME:
         {
            showDialog( DIALOG_RENAME );
            handled = true;
            break;
         }
         case MENU_STATS:
         {
            Intent actionIntent = new Intent( Intent.ACTION_VIEW, mDialogUri );
            startActivity( actionIntent );
            handled = true;
            break;
         }
         default:
            handled = super.onContextItemSelected( item );
            break;
      }
      return handled;
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onCreateDialog(int)
    */
   @Override
   protected Dialog onCreateDialog( int id )
   {
      Dialog dialog = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_RENAME:
            LayoutInflater factory = LayoutInflater.from( this );
            View view = factory.inflate( R.layout.namedialog, null );
            mTrackNameView = (EditText) view.findViewById( R.id.nameField );

            builder = new AlertDialog.Builder( this ).setTitle( R.string.dialog_routename_title ).setMessage( R.string.dialog_routename_message ).setIcon( android.R.drawable.ic_dialog_alert )
                  .setPositiveButton( R.string.btn_okay, mRenameOnClickListener ).setNegativeButton( R.string.btn_cancel, null ).setView( view );
            dialog = builder.create();
            return dialog;
         case DIALOG_DELETE:
            builder = new AlertDialog.Builder( TrackList.this ).setTitle( R.string.dialog_deletetitle ).setIcon( android.R.drawable.ic_dialog_alert ).setNegativeButton( android.R.string.cancel, null )
                  .setPositiveButton( android.R.string.ok, mDeleteOnClickListener );
            dialog = builder.create();
            String messageFormat = this.getResources().getString( R.string.dialog_deleteconfirmation );
            String message = String.format( messageFormat, "" );
            ( (AlertDialog) dialog ).setMessage( message );
            return dialog;
         default:
            return super.onCreateDialog( id );
      }
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
    */
   @Override
   protected void onPrepareDialog( int id, Dialog dialog )
   {
      super.onPrepareDialog( id, dialog );
      switch (id)
      {
         case DIALOG_RENAME:
            mTrackNameView.setText( mDialogCurrentName );
            mTrackNameView.setSelection( 0, mDialogCurrentName.length() );
            break;
         case DIALOG_DELETE:
            AlertDialog alert = (AlertDialog) dialog;
            String messageFormat = this.getResources().getString( R.string.dialog_deleteconfirmation );
            String message = String.format( messageFormat, mDialogCurrentName );
            alert.setMessage( message );
            break;
      }
   }

   private void displayIntent( Intent intent )
   {
      final String queryAction = intent.getAction();
      Cursor tracksCursor = null;
      if( Intent.ACTION_SEARCH.equals( queryAction ) )
      {
         tracksCursor = doSearchWithIntent( intent );
      }
      else if( Intent.ACTION_VIEW.equals( queryAction ) )
      {
         long trackId = Long.parseLong( Uri.parse(  intent.getDataString() ).getLastPathSegment() );
         Intent notificationIntent = new Intent(this, LoggerMap.class);
         Intent trackIntent = notificationIntent;
         trackIntent.putExtra( Constants.EXTRA_TRACK_ID, trackId );
         startActivity( trackIntent );
      }
      else
      {
         tracksCursor = managedQuery( Tracks.CONTENT_URI, new String[] { Tracks._ID, Tracks.NAME, Tracks.CREATION_TIME }, null, null, null );
      }
   
      displayCursor( tracksCursor );
   }

   private void displayCursor( Cursor tracksCursor )
   {
      // Create an array to specify the fields we want to display in the list (only TITLE)
      // and an array of the fields we want to bind those fields to (in this case just text1)
      String[] fromColumns = new String[] { Tracks.NAME, Tracks.CREATION_TIME };
      int[] toItems = new int[] { R.id.listitem_name, R.id.listitem_from };
      // Now create a simple cursor adapter and set it to display
      SimpleCursorAdapter trackAdapter = new SimpleCursorAdapter( this, R.layout.trackitem, tracksCursor, fromColumns, toItems );
      setListAdapter( trackAdapter );
   }

   private Cursor doSearchWithIntent( final Intent queryIntent )
   {
      final String queryString = queryIntent.getStringExtra( SearchManager.QUERY );
      Cursor cursor = managedQuery( Tracks.CONTENT_URI, new String[] { Tracks._ID, Tracks.NAME, Tracks.CREATION_TIME }, "name LIKE ?", new String[] { "%" + queryString + "%" }, null );
      return cursor;
   }

}
