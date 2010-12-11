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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Vector;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.Statistics;
import nl.sogeti.android.gpstracker.db.DatabaseHelper;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.Waypoints;
import nl.sogeti.android.gpstracker.util.ProgressFilterInputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
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
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * Show a list view of all tracks, also doubles for showing search results
 * 
 * @version $Id$
 * @author rene (c) Jan 11, 2009, Sogeti B.V.
 */
public class TrackList extends ListActivity
{
   private static final String TAG = "OGT.TrackList";
   private static final int MENU_DETELE = Menu.FIRST + 0;
   private static final int MENU_SHARE = Menu.FIRST + 1;
   private static final int MENU_RENAME = Menu.FIRST + 2;
   private static final int MENU_STATS = Menu.FIRST + 3;
   private static final int MENU_SEARCH = Menu.FIRST + 4;
   private static final int MENU_VACUUM = Menu.FIRST + 5;
   private static final int MENU_PICKER = Menu.FIRST + 6;
   

   public static final int DIALOG_FILENAME = Menu.FIRST + 22;
   private static final int DIALOG_RENAME  = Menu.FIRST + 23;
   private static final int DIALOG_DELETE  = Menu.FIRST + 24;
   private static final int DIALOG_VACUUM  = Menu.FIRST + 25;
   private static final int DIALOG_IMPORT  = Menu.FIRST + 26;
   private static final int DIALOG_INSTALL = Menu.FIRST + 27;
   
   
   private static final int PICKER_OI      = Menu.FIRST + 27;
   
   private EditText mTrackNameView;
   private Uri mDialogUri;
   private String mDialogCurrentName = "";

   private Uri mImportFileUri;
   private ProgressBar mImportProgress;
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
   private OnClickListener mVacuumOnClickListener = new DialogInterface.OnClickListener()
      {
         public void onClick( DialogInterface dialog, int which )
         {
            DatabaseHelper helper = new DatabaseHelper( TrackList.this );
            helper.vacuum();
         }
      };
   private OnClickListener mImportOnClickListener = new DialogInterface.OnClickListener()
      {
         public void onClick( DialogInterface dialog, int which )
         {
            mImportProgress.setVisibility( View.VISIBLE );
            new Thread( xmlParser ).start();
         }
      };
   private final DialogInterface.OnClickListener mOiPickerDialogListener = new DialogInterface.OnClickListener()
      {
         public void onClick( DialogInterface dialog, int which )
         {
            Uri oiDownload = Uri.parse( "market://details?id=org.openintents.filemanager" );
            Intent oiAboutIntent = new Intent( Intent.ACTION_VIEW, oiDownload );
            try
            {
               startActivity( oiAboutIntent );
            }
            catch (ActivityNotFoundException e)
            {
               oiDownload = Uri.parse( "http://openintents.googlecode.com/files/FileManager-1.1.3.apk" );
               oiAboutIntent = new Intent( Intent.ACTION_VIEW, oiDownload );
               startActivity( oiAboutIntent );
            }
         }
      };
      
   @Override
   protected void onCreate( Bundle savedInstanceState )
   {
      super.onCreate( savedInstanceState );
      this.setContentView( R.layout.tracklist );
      mImportProgress = (ProgressBar) findViewById( R.id.importProgress );

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
      mDialogCurrentName = mDialogCurrentName != null ? mDialogCurrentName : "";
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

      menu.add( ContextMenu.NONE, MENU_SEARCH, ContextMenu.NONE, android.R.string.search_go ).setIcon( android.R.drawable.ic_search_category_default ).setAlphabeticShortcut( SearchManager.MENU_KEY );
      menu.add( ContextMenu.NONE, MENU_VACUUM, ContextMenu.NONE, R.string.menu_vacuum ).setIcon( android.R.drawable.ic_menu_crop );
      menu.add( ContextMenu.NONE, MENU_PICKER, ContextMenu.NONE, R.string.menu_picker ).setIcon( android.R.drawable.ic_menu_add );
      
      return result;
   }

   @Override
   public boolean onOptionsItemSelected( MenuItem item )
   {
      boolean handled = false;
      switch( item.getItemId() )
      {
         case MENU_SEARCH:
            onSearchRequested();
            handled = true;
            break;
         case MENU_VACUUM:
            showDialog( DIALOG_VACUUM );
            break;
         case MENU_PICKER:
            try
            {
               Intent intent = new Intent("org.openintents.action.PICK_FILE");
               intent.putExtra("org.openintents.extra.TITLE", getString( R.string.dialog_import_picker ));
               intent.putExtra("org.openintents.extra.BUTTON_TEXT", getString( R.string.menu_picker ));
               startActivityForResult(intent, PICKER_OI );
            }
            catch (ActivityNotFoundException e) 
            {
               showDialog( DIALOG_INSTALL );
            }
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

      Intent intent = new Intent();
      intent.setData( ContentUris.withAppendedId( Tracks.CONTENT_URI, id ) );

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
      mDialogCurrentName = mDialogCurrentName != null ? mDialogCurrentName : "";
      switch( item.getItemId() )
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
            startActivity( Intent.createChooser( actionIntent, getString( R.string.share_track ) ) );
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
            Intent actionIntent = new Intent( this, Statistics.class );
            actionIntent.setData( mDialogUri );
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
      switch( id )
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
            builder = new AlertDialog.Builder( TrackList.this ).setTitle( R.string.dialog_delete_title ).setIcon( android.R.drawable.ic_dialog_alert )
                  .setNegativeButton( android.R.string.cancel, null ).setPositiveButton( android.R.string.ok, mDeleteOnClickListener );
            dialog = builder.create();
            String messageFormat = this.getResources().getString( R.string.dialog_delete_message );
            String message = String.format( messageFormat, "" );
            ( (AlertDialog) dialog ).setMessage( message );
            return dialog;
         case DIALOG_VACUUM:
            builder = new AlertDialog.Builder( TrackList.this ).setTitle( R.string.dialog_vacuum_title ).setMessage( R.string.dialog_vacuum_message ).setIcon( android.R.drawable.ic_dialog_alert )
                  .setNegativeButton( android.R.string.cancel, null ).setPositiveButton( android.R.string.ok, mVacuumOnClickListener );
            dialog = builder.create();
            return dialog;
         case DIALOG_IMPORT:
            builder = new AlertDialog.Builder( TrackList.this ).setTitle( R.string.dialog_import_title ).setMessage( getString( R.string.dialog_import_message, mImportFileUri.getLastPathSegment() ) )
                  .setIcon( android.R.drawable.ic_dialog_alert ).setNegativeButton( android.R.string.cancel, null ).setPositiveButton( android.R.string.ok, mImportOnClickListener );
            dialog = builder.create();
            return dialog;
         case DIALOG_INSTALL:
            builder = new AlertDialog.Builder( this );
            builder
               .setTitle( R.string.dialog_nooipicker )
               .setMessage( R.string.dialog_nooipicker_message )
               .setIcon( android.R.drawable.ic_dialog_alert )
               .setPositiveButton( R.string.btn_install, mOiPickerDialogListener )
               .setNegativeButton( R.string.btn_cancel, null );
            dialog = builder.create();
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
      switch( id )
      {
         case DIALOG_RENAME:
            mTrackNameView.setText( mDialogCurrentName );
            mTrackNameView.setSelection( 0, mDialogCurrentName.length() );
            break;
         case DIALOG_DELETE:
            AlertDialog alert = (AlertDialog) dialog;
            String messageFormat = this.getResources().getString( R.string.dialog_delete_message );
            String message = String.format( messageFormat, mDialogCurrentName );
            alert.setMessage( message );
            break;
      }
   }

   @Override
   protected void onActivityResult( int requestCode, int resultCode, Intent data )
   {
      if( resultCode != RESULT_CANCELED )
      {
         switch( requestCode )
         {
            case PICKER_OI:
               mImportFileUri = data.getData();
               mImportProgress.setVisibility( View.VISIBLE );
               new Thread( xmlParser ).start();
               break;
            default:
               super.onActivityResult( requestCode, resultCode, data );
               break;
         }
      }
   }
   
   private void displayIntent( Intent intent )
   {
      final String queryAction = intent.getAction();
      Cursor tracksCursor = null;
      if( Intent.ACTION_SEARCH.equals( queryAction ) )
      {
         // Got to SEARCH a query for tracks, make a list
         tracksCursor = doSearchWithIntent( intent );
      }
      else if( Intent.ACTION_VIEW.equals( queryAction ) )
      {
         Uri uri = intent.getData();

         // Got to VIEW a GPX filename
         if( uri.getScheme().equals( "file" ) )
         {
            mImportFileUri = uri;
            showDialog( DIALOG_IMPORT );
            tracksCursor = managedQuery( Tracks.CONTENT_URI, new String[] { Tracks._ID, Tracks.NAME, Tracks.CREATION_TIME }, null, null, null );
         }
         else
         {
            // Got to VIEW a single track, instead had it of to the LoggerMap
            Intent notificationIntent = new Intent( this, LoggerMap.class );
            notificationIntent.setData( uri );
            startActivity( notificationIntent );
         }
      }
      else
      {
         // Got to nothing, make a list of everything
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

   public static final SimpleDateFormat ZULU_DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" );
   static
   {
      TimeZone utc = TimeZone.getTimeZone( "UTC" );
      ZULU_DATE_FORMAT.setTimeZone( utc ); // ZULU_DATE_FORMAT format ends with Z for UTC so make that true
   }

   private Runnable xmlParser = new Runnable()
      {
         public void run()
         {
            int eventType;
            ContentValues lastPosition = null;
            Vector<ContentValues> bulk = new Vector<ContentValues>();
            boolean speed = false;
            boolean elevation = false;
            boolean name = false;
            boolean time = false;

            Uri trackUri = null;
            Uri segment = null;
            try
            {
               XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
               XmlPullParser xmlParser = factory.newPullParser();

               File file = new File( mImportFileUri.getPath() );
               int length = file.length() < Integer.MAX_VALUE ? (int) file.length() : Integer.MAX_VALUE;
               mImportProgress.setMax( length );
               mImportProgress.setProgress( 0 );
               mImportProgress.setVisibility( View.VISIBLE );
               FileInputStream fis = new FileInputStream( file );
               ProgressFilterInputStream pfis = new ProgressFilterInputStream( fis, mImportProgress );
               BufferedInputStream bis = new BufferedInputStream( pfis );
               xmlParser.setInput( new InputStreamReader( bis ) );

               String filename = mImportFileUri.getLastPathSegment();

               eventType = xmlParser.getEventType();

               while (eventType != XmlPullParser.END_DOCUMENT)
               {
                  ContentResolver contentResolver = TrackList.this.getContentResolver();
                  if( eventType == XmlPullParser.START_TAG )
                  {
                     if( xmlParser.getName().equals( "name" ) )
                     {
                        name = true;
                     }
                     else
                     {
                        ContentValues trackContent = new ContentValues();
                        trackContent.put( Tracks.NAME, filename );
                        if( xmlParser.getName().equals( "trk" ) )
                        {
                           trackUri = contentResolver.insert( Tracks.CONTENT_URI, trackContent );
                        }
                        else if( xmlParser.getName().equals( "trkseg" ) )
                        {
                           segment = contentResolver.insert( Uri.withAppendedPath( trackUri, "segments" ), trackContent );
                        }
                        else if( xmlParser.getName().equals( "trkpt" ) )
                        {
                           lastPosition = new ContentValues();
                           lastPosition.put( Waypoints.LATITUDE, new Double( xmlParser.getAttributeValue( 0 ) ) );
                           lastPosition.put( Waypoints.LONGITUDE, new Double( xmlParser.getAttributeValue( 1 ) ) );
                        }
                        else if( xmlParser.getName().equals( "speed" ) )
                        {
                           speed = true;
                        }
                        else if( xmlParser.getName().equals( "ele" ) )
                        {
                           elevation = true;
                        }
                        else if( xmlParser.getName().equals( "time" ) )
                        {
                           time = true;
                        }
                     }
                  }
                  else if( eventType == XmlPullParser.END_TAG )
                  {
                     if( xmlParser.getName().equals( "name" ) )
                     {
                        name = false;
                     }
                     else if( xmlParser.getName().equals( "speed" ) )
                     {
                        speed = false;
                     }
                     else if( xmlParser.getName().equals( "ele" ) )
                     {
                        elevation = false;
                     }
                     else if( xmlParser.getName().equals( "time" ) )
                     {
                        time = false;
                     }
                     else if( xmlParser.getName().equals( "trkseg" ) )
                     {
                        contentResolver.bulkInsert( Uri.withAppendedPath( segment, "waypoints" ), bulk.toArray( new ContentValues[bulk.size()] ) );
                     }
                     else if( xmlParser.getName().equals( "trkpt" ) )
                     {
                        bulk.add( lastPosition );
                        lastPosition = null;
                     }
                  }
                  else if( eventType == XmlPullParser.TEXT )
                  {
                     if( name )
                     {
                        ContentValues nameValues = new ContentValues();
                        nameValues.put( Tracks.NAME, xmlParser.getText() );
                        contentResolver.update( trackUri, nameValues, null, null );
                     }
                     else if( lastPosition != null && speed )
                     {
                        lastPosition.put( Waypoints.SPEED, new Float( xmlParser.getText() ) );
                     }
                     else if( lastPosition != null && elevation )
                     {
                        lastPosition.put( Waypoints.ALTITUDE, Double.parseDouble( xmlParser.getText() ) );
                     }
                     else if( lastPosition != null && time )
                     {
                        lastPosition.put( Waypoints.TIME, new Long( ZULU_DATE_FORMAT.parse( xmlParser.getText() ).getTime() ) );
                     }
                  }
                  eventType = xmlParser.next();
               }
            }
            catch (XmlPullParserException e)
            {
               Log.e( TAG, "Error", e );
            }
            catch (IOException e)
            {
               Log.e( TAG, "Error", e );
            }
            catch (ParseException e)
            {
               Log.e( TAG, "Error", e );
            }
            finally
            {
               TrackList.this.runOnUiThread( new Runnable()
                  {

                     public void run()
                     {
                        mImportProgress.setVisibility( View.GONE );
                     }
                  } );
            }
         }
      };
}
