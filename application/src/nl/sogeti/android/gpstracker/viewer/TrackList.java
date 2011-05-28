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
import nl.sogeti.android.gpstracker.actions.ControlTracking;
import nl.sogeti.android.gpstracker.actions.DescribeTrack;
import nl.sogeti.android.gpstracker.actions.NameTrack;
import nl.sogeti.android.gpstracker.actions.Statistics;
import nl.sogeti.android.gpstracker.actions.utils.xml.GpxParser;
import nl.sogeti.android.gpstracker.adapter.BreadcrumbsAdapter;
import nl.sogeti.android.gpstracker.adapter.BreadcrumbsTracks;
import nl.sogeti.android.gpstracker.adapter.SectionedListAdapter;
import nl.sogeti.android.gpstracker.db.DatabaseHelper;
import nl.sogeti.android.gpstracker.db.GPStracking;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.oauth.PrepareRequestTokenActivity;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.Pair;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
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
   private static final int DIALOG_RENAME = Menu.FIRST + 23;
   private static final int DIALOG_DELETE = Menu.FIRST + 24;
   private static final int DIALOG_VACUUM = Menu.FIRST + 25;
   private static final int DIALOG_IMPORT = Menu.FIRST + 26;
   private static final int DIALOG_INSTALL = Menu.FIRST + 27;
   protected static final int DIALOG_ERROR = Menu.FIRST + 28;

   private static final int PICKER_OI = Menu.FIRST + 29;
   private static final int DESCRIBE  = Menu.FIRST + 30;
   public static final String OAUTH_TOKEN = "breadcrumbs_oauth_token";
   public static final String OAUTH_TOKEN_SECRET = "breadcrumbs_oauth_secret";

   private BreadcrumbsAdapter mBreadcrumbAdapter;
   private EditText mTrackNameView;
   private Uri mDialogUri;
   private String mDialogCurrentName = "";

   private Uri mImportFileUri;
   private ProgressBar mImportProgress;
   private String mErrorDialogMessage;
   private Exception mErrorDialogException;

   private OnClickListener mDeleteOnClickListener = new DialogInterface.OnClickListener()
   {
      public void onClick(DialogInterface dialog, int which)
      {
         getContentResolver().delete(mDialogUri, null, null);
      }
   };
   private OnClickListener mRenameOnClickListener = new DialogInterface.OnClickListener()
   {
      public void onClick(DialogInterface dialog, int which)
      {
         //         Log.d( TAG, "Context item selected: "+mDialogUri+" with name "+mDialogCurrentName );

         String trackName = mTrackNameView.getText().toString();
         ContentValues values = new ContentValues();
         values.put(Tracks.NAME, trackName);
         TrackList.this.getContentResolver().update(mDialogUri, values, null, null);
      }
   };
   private OnClickListener mVacuumOnClickListener = new DialogInterface.OnClickListener()
   {
      public void onClick(DialogInterface dialog, int which)
      {
         DatabaseHelper helper = new DatabaseHelper(TrackList.this);
         helper.vacuum();
      }
   };
   private OnClickListener mImportOnClickListener = new DialogInterface.OnClickListener()
   {
      public void onClick(DialogInterface dialog, int which)
      {
         new GpxParser(TrackList.this).execute(mImportFileUri);
      }
   };
   private final DialogInterface.OnClickListener mOiPickerDialogListener = new DialogInterface.OnClickListener()
   {
      public void onClick(DialogInterface dialog, int which)
      {
         Uri oiDownload = Uri.parse("market://details?id=org.openintents.filemanager");
         Intent oiAboutIntent = new Intent(Intent.ACTION_VIEW, oiDownload);
         try
         {
            startActivity(oiAboutIntent);
         }
         catch (ActivityNotFoundException e)
         {
            oiDownload = Uri.parse("http://openintents.googlecode.com/files/FileManager-1.1.3.apk");
            oiAboutIntent = new Intent(Intent.ACTION_VIEW, oiDownload);
            startActivity(oiAboutIntent);
         }
      }
   };

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      displayIntent(getIntent());

      this.setContentView(R.layout.tracklist);
      mImportProgress = (ProgressBar) findViewById(R.id.importProgress);

      ListView listView = getListView();
      listView.setItemsCanFocus(true);
      // Add the context menu (the long press thing)
      registerForContextMenu(listView);
   }

   @Override
   public Object onRetainNonConfigurationInstance()
   {
      return mBreadcrumbAdapter;
   }

   @Override
   protected void onDestroy()
   {
      if (mBreadcrumbAdapter != null && isFinishing())
      {
         mBreadcrumbAdapter.shutdown();
      }
      super.onDestroy();
   }

   @Override
   public void onNewIntent(Intent newIntent)
   {
      displayIntent(newIntent);
   }

   /*
    * (non-Javadoc)
    * @see android.app.ListActivity#onRestoreInstanceState(android.os.Bundle)
    */
   @Override
   protected void onRestoreInstanceState(Bundle state)
   {
      mDialogUri = state.getParcelable("URI");
      mDialogCurrentName = state.getString("NAME");
      mDialogCurrentName = mDialogCurrentName != null ? mDialogCurrentName : "";
      super.onRestoreInstanceState(state);
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
    */
   @Override
   protected void onSaveInstanceState(Bundle outState)
   {
      outState.putParcelable("URI", mDialogUri);
      outState.putString("NAME", mDialogCurrentName);
      super.onSaveInstanceState(outState);
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      boolean result = super.onCreateOptionsMenu(menu);

      menu.add(ContextMenu.NONE, MENU_SEARCH, ContextMenu.NONE, android.R.string.search_go).setIcon(android.R.drawable.ic_search_category_default)
            .setAlphabeticShortcut(SearchManager.MENU_KEY);
      menu.add(ContextMenu.NONE, MENU_VACUUM, ContextMenu.NONE, R.string.menu_vacuum).setIcon(android.R.drawable.ic_menu_crop);
      menu.add(ContextMenu.NONE, MENU_PICKER, ContextMenu.NONE, R.string.menu_picker).setIcon(android.R.drawable.ic_menu_add);

      return result;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      boolean handled = false;
      switch (item.getItemId())
      {
         case MENU_SEARCH:
            onSearchRequested();
            handled = true;
            break;
         case MENU_VACUUM:
            showDialog(DIALOG_VACUUM);
            break;
         case MENU_PICKER:
            try
            {
               Intent intent = new Intent("org.openintents.action.PICK_FILE");
               intent.putExtra("org.openintents.extra.TITLE", getString(R.string.dialog_import_picker));
               intent.putExtra("org.openintents.extra.BUTTON_TEXT", getString(R.string.menu_picker));
               startActivityForResult(intent, PICKER_OI);
            }
            catch (ActivityNotFoundException e)
            {
               showDialog(DIALOG_INSTALL);
            }
            break;
         default:
            handled = super.onOptionsItemSelected(item);
      }
      return handled;
   }

   @Override
   protected void onListItemClick(ListView listView, View view, int position, long id)
   {
      super.onListItemClick(listView, view, position, id);
      Log.d( TAG, "Clicked on view "+view);

      Object item = listView.getItemAtPosition(position);
      if (item instanceof String)
      {
         if (Constants.BREADCRUMBS_CONNECT.equals(item))
         {
            Intent i = new Intent(getApplicationContext(), PrepareRequestTokenActivity.class);
            i.putExtra(PrepareRequestTokenActivity.OAUTH_TOKEN_PREF, OAUTH_TOKEN);
            i.putExtra(PrepareRequestTokenActivity.OAUTH_TOKEN_SECRET_PREF, OAUTH_TOKEN_SECRET);

            i.putExtra(PrepareRequestTokenActivity.CONSUMER_KEY, getString(R.string.CONSUMER_KEY));
            i.putExtra(PrepareRequestTokenActivity.CONSUMER_SECRET, getString(R.string.CONSUMER_SECRET));
            i.putExtra(PrepareRequestTokenActivity.REQUEST_URL, Constants.REQUEST_URL);
            i.putExtra(PrepareRequestTokenActivity.ACCESS_URL, Constants.ACCESS_URL);
            i.putExtra(PrepareRequestTokenActivity.AUTHORIZE_URL, Constants.AUTHORIZE_URL);

            startActivity(i);
         }
      }
      else if (item instanceof Pair< ? , ? >)
      {
         @SuppressWarnings("unchecked")
         Pair<Integer, Integer> track = (Pair<Integer, Integer>) item;
         if (track.first == Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE)
         {
            mBreadcrumbAdapter.startDownloadTask(this, track);
         }
      }
      else
      {
         Intent intent = new Intent();
         Uri trackUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, id);
         intent.setData(trackUri);
         ComponentName caller = this.getCallingActivity();
         if (caller != null)
         {
            setResult(RESULT_OK, intent);
            finish();
         }
         else
         {
            intent.setClass(this, LoggerMap.class);
            startActivity(intent);
         }
      }
   }

   @Override
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
      menu.add(0, MENU_STATS, 0, R.string.menu_statistics);
      menu.add(0, MENU_SHARE, 0, R.string.menu_shareTrack);
      menu.add(0, MENU_RENAME, 0, R.string.menu_renameTrack);
      menu.add(0, MENU_DETELE, 0, R.string.menu_deleteTrack);
   }

   @Override
   public boolean onContextItemSelected(MenuItem item)
   {
      boolean handled = false;
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
      mDialogUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, cursor.getLong(0));
      mDialogCurrentName = cursor.getString(1);
      mDialogCurrentName = mDialogCurrentName != null ? mDialogCurrentName : "";
      switch (item.getItemId())
      {
         case MENU_DETELE:
         {
            showDialog(DIALOG_DELETE);
            handled = true;
            break;
         }
         case MENU_SHARE:
         {
            Intent actionIntent = new Intent(Intent.ACTION_RUN);
            actionIntent.setDataAndType(mDialogUri, Tracks.CONTENT_ITEM_TYPE);
            actionIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(actionIntent, getString(R.string.share_track)));
            handled = true;
            break;
         }
         case MENU_RENAME:
         {
            showDialog(DIALOG_RENAME);
            handled = true;
            break;
         }
         case MENU_STATS:
         {
            Intent actionIntent = new Intent(this, Statistics.class);
            actionIntent.setData(mDialogUri);
            startActivity(actionIntent);
            handled = true;
            break;
         }
         default:
            handled = super.onContextItemSelected(item);
            break;
      }
      return handled;
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onCreateDialog(int)
    */
   @Override
   protected Dialog onCreateDialog(int id)
   {
      Dialog dialog = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_RENAME:
            LayoutInflater factory = LayoutInflater.from(this);
            View view = factory.inflate(R.layout.namedialog, null);
            mTrackNameView = (EditText) view.findViewById(R.id.nameField);
            builder = new AlertDialog.Builder(this).setTitle(R.string.dialog_routename_title).setMessage(R.string.dialog_routename_message)
                  .setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(R.string.btn_okay, mRenameOnClickListener)
                  .setNegativeButton(R.string.btn_cancel, null).setView(view);
            dialog = builder.create();
            return dialog;
         case DIALOG_DELETE:
            builder = new AlertDialog.Builder(TrackList.this).setTitle(R.string.dialog_delete_title).setIcon(android.R.drawable.ic_dialog_alert)
                  .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, mDeleteOnClickListener);
            dialog = builder.create();
            String messageFormat = this.getResources().getString(R.string.dialog_delete_message);
            String message = String.format(messageFormat, "");
            ((AlertDialog) dialog).setMessage(message);
            return dialog;
         case DIALOG_VACUUM:
            builder = new AlertDialog.Builder(TrackList.this).setTitle(R.string.dialog_vacuum_title).setMessage(R.string.dialog_vacuum_message)
                  .setIcon(android.R.drawable.ic_dialog_alert).setNegativeButton(android.R.string.cancel, null)
                  .setPositiveButton(android.R.string.ok, mVacuumOnClickListener);
            dialog = builder.create();
            return dialog;
         case DIALOG_IMPORT:
            builder = new AlertDialog.Builder(TrackList.this).setTitle(R.string.dialog_import_title)
                  .setMessage(getString(R.string.dialog_import_message, mImportFileUri.getLastPathSegment())).setIcon(android.R.drawable.ic_dialog_alert)
                  .setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, mImportOnClickListener);
            dialog = builder.create();
            return dialog;
         case DIALOG_INSTALL:
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_nooipicker).setMessage(R.string.dialog_nooipicker_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(R.string.btn_install, mOiPickerDialogListener).setNegativeButton(R.string.btn_cancel, null);
            dialog = builder.create();
            return dialog;
         case DIALOG_ERROR:
            builder = new AlertDialog.Builder(this);
            builder.setIcon(android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                  .setMessage(mErrorDialogMessage + " (" + mErrorDialogException.getMessage() + ") ").setNeutralButton(android.R.string.cancel, null);
            dialog = builder.create();
            return dialog;
         default:
            return super.onCreateDialog(id);
      }
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
    */
   @Override
   protected void onPrepareDialog(int id, Dialog dialog)
   {
      super.onPrepareDialog(id, dialog);
      AlertDialog alert;
      switch (id)
      {
         case DIALOG_RENAME:
            mTrackNameView.setText(mDialogCurrentName);
            mTrackNameView.setSelection(0, mDialogCurrentName.length());
            break;
         case DIALOG_DELETE:
            alert = (AlertDialog) dialog;
            String messageFormat = this.getResources().getString(R.string.dialog_delete_message);
            String message = String.format(messageFormat, mDialogCurrentName);
            alert.setMessage(message);
            break;
         case DIALOG_ERROR:
            alert = (AlertDialog) dialog;
            alert.setMessage(mErrorDialogMessage + " (" + mErrorDialogException.getMessage() + ") ");
            break;
      }
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data)
   {
      if (resultCode != RESULT_CANCELED)
      {
         switch (requestCode)
         {
            case PICKER_OI:
               mImportFileUri = data.getData();
               new GpxParser(TrackList.this).execute(mImportFileUri);
               break;
            case DESCRIBE:
               Uri trackUri = data.getData();
               mBreadcrumbAdapter.startUploadTask(TrackList.this, trackUri );
               break;
            default:
               super.onActivityResult(requestCode, resultCode, data);
               break;
         }
      }
   }

   private void displayIntent(Intent intent)
   {
      final String queryAction = intent.getAction();
      final String orderby = Tracks.CREATION_TIME + " DESC";
      Cursor tracksCursor = null;
      if (Intent.ACTION_SEARCH.equals(queryAction))
      {
         // Got to SEARCH a query for tracks, make a list
         tracksCursor = doSearchWithIntent(intent);
      }
      else if (Intent.ACTION_VIEW.equals(queryAction))
      {
         Uri uri = intent.getData();
         if ("content".equals(uri.getScheme()) && GPStracking.AUTHORITY.equals(uri.getAuthority()))
         {
            // Got to VIEW a single track, instead hand it of to the LoggerMap
            Intent notificationIntent = new Intent(this, LoggerMap.class);
            notificationIntent.setData(uri);
            startActivity(notificationIntent);
            finish();
         }
         else if (uri.getScheme().equals("file") || uri.getScheme().equals("content"))
         {
            // Got to VIEW a GPX filename
            mImportFileUri = uri;
            showDialog(DIALOG_IMPORT);
            tracksCursor = managedQuery(Tracks.CONTENT_URI, new String[] { Tracks._ID, Tracks.NAME, Tracks.CREATION_TIME }, null, null, orderby);
         }
         else
         {
            Log.e(TAG, "Unable to VIEW " + uri);
         }
      }
      else
      {
         // Got to nothing, make a list of everything
         tracksCursor = managedQuery(Tracks.CONTENT_URI, new String[] { Tracks._ID, Tracks.NAME, Tracks.CREATION_TIME }, null, null, orderby);
      }
      displayCursor(tracksCursor);

   }

   private void displayCursor(Cursor tracksCursor)
   {
      SectionedListAdapter sectionedAdapter = new SectionedListAdapter(this);

      String[] fromColumns = new String[] { Tracks.NAME, Tracks.CREATION_TIME, Tracks._ID };
      int[] toItems = new int[] { R.id.listitem_name, R.id.listitem_from, R.id.bcSyncedCheckBox };
      SimpleCursorAdapter trackAdapter = new SimpleCursorAdapter(this, R.layout.trackitem, tracksCursor, fromColumns, toItems);

      sectionedAdapter.addSection("Local", trackAdapter);

      mBreadcrumbAdapter = (BreadcrumbsAdapter) getLastNonConfigurationInstance();
      if (mBreadcrumbAdapter == null)
      {
         mBreadcrumbAdapter = new BreadcrumbsAdapter(this);
      }
      else
      {
         mBreadcrumbAdapter.notifyDataSetChanged();
      }
      sectionedAdapter.addSection("GoBreadcrumbs", mBreadcrumbAdapter);

      // Enrich the track adapter with Breadcrumbs adapter data 
      trackAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder()
      {
         public boolean setViewValue(View view, final Cursor cursor, int columnIndex)
         {
            if (columnIndex == 0)
            {
               final long trackId = cursor.getLong(columnIndex);
               // Show the check if Breadcrumbs is online
               CheckBox checkbox = (CheckBox) view;
               if (mBreadcrumbAdapter.isOnline())
               {
                  checkbox.setVisibility(View.VISIBLE);
                  
                  // Disable the checkbox if marked online
                  BreadcrumbsTracks tracks = mBreadcrumbAdapter.getBreadcrumbsTracks();
                  boolean isOnline = tracks.isLocalTrackOnline(trackId);
                  checkbox.setEnabled(!isOnline);
                  
                  // Check the checkbox if determined synced
                  boolean isSynced = tracks.isLocalTrackSynced(trackId);
                  checkbox.setOnCheckedChangeListener(null);
                  checkbox.setChecked(isSynced);
                  checkbox.setOnCheckedChangeListener( new OnCheckedChangeListener()
                  {
                     public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                     {
                        if( isChecked )
                        {
                           Log.d( TAG, "View"+buttonView+" track id "+trackId );
                           // Start a naming of the track
                           Intent namingIntent = new Intent( TrackList.this, DescribeTrack.class );
                           namingIntent.setData( ContentUris.withAppendedId( Tracks.CONTENT_URI, trackId ) );
                           startActivityForResult(namingIntent, DESCRIBE );
                        }
                     }
                  });
               }
               else
               {
                  checkbox.setVisibility(View.INVISIBLE);
                  checkbox.setOnCheckedChangeListener(null);
               }
               return true;
            }
            return false;
         }
      });

      setListAdapter(sectionedAdapter);
   }

   private Cursor doSearchWithIntent(final Intent queryIntent)
   {
      final String queryString = queryIntent.getStringExtra(SearchManager.QUERY);
      Cursor cursor = managedQuery(Tracks.CONTENT_URI, new String[] { Tracks._ID, Tracks.NAME, Tracks.CREATION_TIME }, "name LIKE ?", new String[] { "%"
            + queryString + "%" }, null);
      return cursor;
   }

   public void startProgressBar(int max)
   {
      mImportProgress.setMax(max);
      mImportProgress.setVisibility(View.VISIBLE);
   }

   public void updateProgressBar(int increment, int max)
   {
      mImportProgress.setMax(max);
      mImportProgress.incrementProgressBy(increment);
   }

   public void stopProgressBar()
   {
      mImportProgress.setVisibility(View.GONE);
   }

   public void showErrorDialog(String errorDialogMessage, Exception errorDialogException)
   {
      mErrorDialogMessage = errorDialogMessage;
      mErrorDialogException = errorDialogException;
      showDialog(DIALOG_ERROR);
   }

}
