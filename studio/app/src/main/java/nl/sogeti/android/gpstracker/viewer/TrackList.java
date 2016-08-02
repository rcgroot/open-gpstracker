/*------------------------------------------------------------------------------
 **     Ident: Sogeti Smart Mobile Solutions
 **    Author: rene
 ** Copyright: (c) Apr 24, 2011 Sogeti Nederland B.V. All Rights Reserved.
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

import android.app.Dialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.Statistics;
import nl.sogeti.android.gpstracker.actions.tasks.GpxParser;
import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.adapter.SectionedListAdapter;
import nl.sogeti.android.gpstracker.integration.ContentConstants;
import nl.sogeti.android.gpstracker.integration.ContentConstants.Tracks;
import nl.sogeti.android.gpstracker.service.db.DatabaseHelper;
import timber.log.Timber;

/**
 * Show a list view of all tracks, also doubles for showing search results
 *
 * @author rene (c) Jan 11, 2009, Sogeti B.V.
 * @version $Id$
 */
public class TrackList extends AppCompatActivity implements ProgressListener {
    protected static final int DIALOG_ERROR = Menu.FIRST + 28;
    private static final int MENU_DELETE = Menu.FIRST + 0;
    private static final int MENU_SHARE = Menu.FIRST + 1;
    private static final int MENU_RENAME = Menu.FIRST + 2;
    private static final int MENU_STATS = Menu.FIRST + 3;
    private static final int MENU_SEARCH = Menu.FIRST + 4;
    private static final int MENU_VACUUM = Menu.FIRST + 5;
    private static final int MENU_PICKER = Menu.FIRST + 6;
    private static final int DIALOG_RENAME = Menu.FIRST + 23;
    private static final int DIALOG_DELETE = Menu.FIRST + 24;
    private static final int DIALOG_VACUUM = Menu.FIRST + 25;
    private static final int DIALOG_IMPORT = Menu.FIRST + 26;
    private static final int FILE_PICKER = Menu.FIRST + 29;
    protected ListView mList;
    protected ListAdapter mAdapter;
    private EditText mTrackNameView;
    private Uri mDialogTrackUri;
    private String mDialogCurrentName = "";
    private String mErrorDialogMessage;
    private Exception mErrorDialogException;
    private Runnable mImportAction;
    private String mImportTrackName;
    private String mErrorTask;
    private int mPausePosition;
    private OnClickListener mDeleteOnClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            getContentResolver().delete(mDialogTrackUri, null, null);
        }
    };
    private OnClickListener mRenameOnClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            //         Log.d( TAG, "Context item selected: "+mDialogUri+" with name "+mDialogCurrentName );

            String trackName = mTrackNameView.getText().toString();
            ContentValues values = new ContentValues();
            values.put(Tracks.NAME, trackName);
            TrackList.this.getContentResolver().update(mDialogTrackUri, values, null, null);
        }
    };
    private OnClickListener mVacuumOnClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            DatabaseHelper helper = new DatabaseHelper(TrackList.this);
            helper.vacuum();
        }
    };
    private OnClickListener mImportOnClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            mImportAction.run();
        }
    };
    private boolean mFinishedStart = false;
    private Handler mHandler = new Handler();
    private Runnable mRequestFocus = new Runnable() {
        public void run() {
            mList.focusableViewAvailable(mList);
        }
    };
    private AdapterView.OnItemClickListener mOnClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            onListItemClick((ListView) parent, v, position, id);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.tracklist);
        Toolbar toolbar = (Toolbar) findViewById(R.id.support_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        displayIntent(getIntent());

        ListView listView = getListView();
        listView.setItemsCanFocus(true);
        // Add the context menu (the long press thing)
        registerForContextMenu(listView);

        if (savedInstanceState != null) {
            setSelection(savedInstanceState.getInt("POSITION"));
        }
    }

    @Override
    protected void onResume() {
        if (mPausePosition != 0) {
            getListView().setSelection(mPausePosition);
        }
        super.onResume();
    }

    @Override
    public void onNewIntent(Intent newIntent) {
        displayIntent(newIntent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("URI", mDialogTrackUri);
        outState.putString("NAME", mDialogCurrentName);
        outState.putInt("POSITION", getListView().getFirstVisiblePosition());
    }

    @Override
    protected void onPause() {
        mPausePosition = getListView().getFirstVisiblePosition();
        super.onPause();
    }

    /*******************************************************************/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        MenuItem item;
        item = menu.add(ContextMenu.NONE, MENU_SEARCH, ContextMenu.NONE, android.R.string.search_go)
                .setIcon(R.drawable.ic_search_24dp)
                .setAlphabeticShortcut(SearchManager.MENU_KEY);
        MenuItemCompat.setShowAsAction(item, MenuItem.SHOW_AS_ACTION_ALWAYS);
        item = menu.add(ContextMenu.NONE, MENU_PICKER, ContextMenu.NONE, R.string.menu_picker)
                .setIcon(R.drawable.ic_import_24dp);
        MenuItemCompat.setShowAsAction(item, MenuItem.SHOW_AS_ACTION_ALWAYS);
        item = menu.add(ContextMenu.NONE, MENU_VACUUM, ContextMenu.NONE, R.string.menu_vacuum)
                .setIcon(android.R.drawable.ic_menu_crop);
        MenuItemCompat.setShowAsAction(item, MenuItem.SHOW_AS_ACTION_NEVER);
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean handled = false;
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                handled = true;
                break;
            case MENU_SEARCH:
                onSearchRequested();
                handled = true;
                break;
            case MENU_VACUUM:
                showDialog(DIALOG_VACUUM);
                break;
            case MENU_PICKER:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, FILE_PICKER);
                break;
            default:
                handled = super.onOptionsItemSelected(item);
        }
        return handled;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (menuInfo instanceof AdapterView.AdapterContextMenuInfo) {
            AdapterView.AdapterContextMenuInfo itemInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
            TextView textView = (TextView) itemInfo.targetView.findViewById(R.id.listitem_name);
            if (textView != null) {
                menu.setHeaderTitle(textView.getText());
            }

            Object listItem = getListAdapter().getItem(itemInfo.position);
            if (listItem instanceof Cursor) {
                menu.add(0, MENU_STATS, 0, R.string.menu_statistics);
                menu.add(0, MENU_SHARE, 0, R.string.menu_shareTrack);
                menu.add(0, MENU_RENAME, 0, R.string.menu_renameTrack);
                menu.add(0, MENU_DELETE, 0, R.string.menu_deleteTrack);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        boolean handled = false;
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Timber.e("Bad menuInfo", e);
            return handled;
        }

        Object listItem = getListAdapter().getItem(info.position);
        if (listItem instanceof Cursor) {
            Cursor cursor = (Cursor) listItem;
            mDialogTrackUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, cursor.getLong(0));
            mDialogCurrentName = cursor.getString(1);
            mDialogCurrentName = mDialogCurrentName != null ? mDialogCurrentName : "";
            switch (item.getItemId()) {
                case MENU_DELETE: {
                    showDialog(DIALOG_DELETE);
                    handled = true;
                    break;
                }
                case MENU_SHARE: {
                    Intent actionIntent = new Intent(Intent.ACTION_RUN);
                    actionIntent.setDataAndType(mDialogTrackUri, Tracks.CONTENT_ITEM_TYPE);
                    actionIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(actionIntent, getString(R.string.share_track)));
                    handled = true;
                    break;
                }
                case MENU_RENAME: {
                    showDialog(DIALOG_RENAME);
                    handled = true;
                    break;
                }
                case MENU_STATS: {
                    Intent actionIntent = new Intent(this, Statistics.class);
                    actionIntent.setData(mDialogTrackUri);
                    startActivity(actionIntent);
                    handled = true;
                    break;
                }
                default:
                    handled = super.onContextItemSelected(item);
                    break;
            }
        }
        return handled;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        Builder builder = null;
        switch (id) {
            case DIALOG_RENAME:
                LayoutInflater factory = LayoutInflater.from(this);
                View view = factory.inflate(R.layout.namedialog, (ViewGroup) findViewById(android.R.id.content), false);
                mTrackNameView = (EditText) view.findViewById(R.id.nameField);
                builder = new AlertDialog.Builder(this).setTitle(R.string.dialog_routename_title).setMessage(R.string
                        .dialog_routename_message).setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(R.string.btn_okay, mRenameOnClickListener)
                        .setNegativeButton(R.string.btn_cancel, null).setView(view);
                dialog = builder.create();
                return dialog;
            case DIALOG_DELETE:
                builder = new AlertDialog.Builder(TrackList.this).setTitle(R.string.dialog_delete_title).setIcon(android
                        .R.drawable.ic_dialog_alert).setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok,
                                mDeleteOnClickListener);
                dialog = builder.create();
                String messageFormat = this.getResources().getString(R.string.dialog_delete_message);
                String message = String.format(messageFormat, "");
                ((AlertDialog) dialog).setMessage(message);
                return dialog;
            case DIALOG_VACUUM:
                builder = new AlertDialog.Builder(TrackList.this).setTitle(R.string.dialog_vacuum_title).setMessage(R
                        .string.dialog_vacuum_message).setIcon(android.R.drawable.ic_dialog_alert)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok,
                                mVacuumOnClickListener);
                dialog = builder.create();
                return dialog;
            case DIALOG_IMPORT:
                builder = new AlertDialog.Builder(TrackList.this).setTitle(R.string.dialog_import_title).setMessage
                        (getString(R.string.dialog_import_message, mImportTrackName))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok,
                                mImportOnClickListener);
                dialog = builder.create();
                return dialog;
            case DIALOG_ERROR:
                builder = new AlertDialog.Builder(this);
                builder.setIcon(android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                        .setMessage(mErrorDialogMessage).setNeutralButton(android.R.string.cancel, null);
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
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        AlertDialog alert;
        String message;
        switch (id) {
            case DIALOG_RENAME:
                mTrackNameView.setText(mDialogCurrentName);
                mTrackNameView.setSelection(0, mDialogCurrentName.length());
                break;
            case DIALOG_DELETE:
                alert = (AlertDialog) dialog;
                String messageFormat = this.getResources().getString(R.string.dialog_delete_message);
                message = String.format(messageFormat, mDialogCurrentName);
                alert.setMessage(message);
                break;
            case DIALOG_ERROR:
                alert = (AlertDialog) dialog;
                message = "Failed task:\n" + mErrorTask;
                message += "\n\n";
                message += "Reason:\n" + mErrorDialogMessage;
                if (mErrorDialogException != null) {
                    message += " (" + mErrorDialogException.getMessage() + ") ";
                }
                alert.setMessage(message);
                break;
            case DIALOG_IMPORT:
                alert = (AlertDialog) dialog;
                alert.setMessage(getString(R.string.dialog_import_message, mImportTrackName));
                break;
        }
    }

    /**
     * ProgressListener interface and UI actions (non-Javadoc)
     **/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case FILE_PICKER:
                    new GpxParser(TrackList.this, TrackList.this).execute(data.getData());
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
                    break;
            }
        }
    }

    /*******************************************************************/

    @Override
    public void setIndeterminate(boolean indeterminate) {
        setProgressBarIndeterminate(indeterminate);
    }

    private void displayIntent(Intent intent) {
        final String queryAction = intent.getAction();
        final String orderby = Tracks.CREATION_TIME + " DESC";
        Cursor tracksCursor = null;
        if (Intent.ACTION_SEARCH.equals(queryAction)) {
            // Got to SEARCH a query for tracks, make a list
            tracksCursor = doSearchWithIntent(intent);
        } else if (Intent.ACTION_VIEW.equals(queryAction)) {
            final Uri uri = intent.getData();
            if ("content".equals(uri.getScheme()) && ContentConstants.AUTHORITY.equals(uri.getAuthority())) {
                // Got to VIEW a single track, instead hand it of to the LoggerMap
                Intent notificationIntent = new Intent(this, LoggerMap.class);
                notificationIntent.setData(uri);
                startActivity(notificationIntent);
                finish();
            } else if (uri.getScheme().equals("file") || uri.getScheme().equals("content")) {

                mImportTrackName = uri.getLastPathSegment();
                // Got to VIEW a GPX filename
                mImportAction = new Runnable() {
                    @Override
                    public void run() {
                        new GpxParser(TrackList.this, TrackList.this).execute(uri);
                    }
                };
                showDialog(DIALOG_IMPORT);
                tracksCursor = managedQuery(Tracks.CONTENT_URI, new String[]{Tracks._ID, Tracks.NAME, Tracks
                        .CREATION_TIME}, null, null, orderby);
            } else {
                Timber.e("Unable to VIEW " + uri);
            }
        } else {
            // Got to nothing, make a list of everything
            tracksCursor = managedQuery(Tracks.CONTENT_URI, new String[]{Tracks._ID, Tracks.NAME, Tracks.CREATION_TIME
            }, null, null, orderby);
        }
        displayCursor(tracksCursor);

    }

    @Override
    public void started() {
        setProgressBarVisibility(true);
        setProgress(Window.PROGRESS_START);
    }

    // Copy of the ListActivity addition to Activity

    private Cursor doSearchWithIntent(final Intent queryIntent) {
        final String queryString = queryIntent.getStringExtra(SearchManager.QUERY);
        Cursor cursor = managedQuery(Tracks.CONTENT_URI, new String[]{Tracks._ID, Tracks.NAME, Tracks.CREATION_TIME
        }, "name LIKE ?", new String[]{"%" + queryString + "%"}, null);
        return cursor;
    }

    @Override
    public void finished(Uri result) {
        setProgressBarVisibility(false);
        setProgressBarIndeterminate(false);
    }

    private void displayCursor(Cursor tracksCursor) {
        SectionedListAdapter sectionedAdapter = new SectionedListAdapter(this);

        String[] fromColumns = new String[]{Tracks.NAME, Tracks.CREATION_TIME, Tracks._ID};
        int[] toItems = new int[]{R.id.listitem_name, R.id.listitem_from, R.id.bcSyncedCheckBox};
        SimpleCursorAdapter trackAdapter = new SimpleCursorAdapter(this, R.layout.trackitem, tracksCursor, fromColumns,
                toItems);
        sectionedAdapter.addSection("", trackAdapter);

        // Enrich the track adapter with Breadcrumbs adapter data
        trackAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, final Cursor cursor, int columnIndex) {
                if (columnIndex == 0) {
                    final long trackId = cursor.getLong(0);
                    final String trackName = cursor.getString(1);
                    // Show the check if Breadcrumbs is online
                    final CheckBox checkbox = (CheckBox) view;
                    final ProgressBar progressbar = (ProgressBar) ((View) view.getParent()).findViewById(R.id
                            .bcExportProgress);
                    checkbox.setVisibility(View.INVISIBLE);
                    checkbox.setOnCheckedChangeListener(null);

                    return true;
                }
                return false;
            }
        });

        setListAdapter(sectionedAdapter);
    }

    @Override
    public void showError(String task, String errorDialogMessage, Exception errorDialogException) {
        mErrorTask = task;
        mErrorDialogMessage = errorDialogMessage;
        mErrorDialogException = errorDialogException;
        Timber.e(errorDialogMessage, errorDialogException);
        if (!isFinishing()) {
            showDialog(DIALOG_ERROR);
        }
        setProgressBarVisibility(false);
        setProgressBarIndeterminate(false);
    }

    protected void onListItemClick(ListView listView, View view, int position, long id) {
        Intent intent = new Intent();
        Uri trackUri = ContentUris.withAppendedId(Tracks.CONTENT_URI, id);
        intent.setData(trackUri);
        ComponentName caller = this.getCallingActivity();
        if (caller != null) {
            setResult(RESULT_OK, intent);
            finish();
        } else {
            intent.setClass(this, LoggerMap.class);
            startActivity(intent);
        }

    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        ensureList();
        mDialogTrackUri = state.getParcelable("URI");
        mDialogCurrentName = state.getString("NAME");
        mDialogCurrentName = mDialogCurrentName != null ? mDialogCurrentName : "";
        getListView().setSelection(state.getInt("POSITION"));
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(mRequestFocus);
        super.onDestroy();
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        View emptyView = findViewById(android.R.id.empty);
        mList = (ListView) findViewById(android.R.id.list);
        if (mList == null) {
            throw new RuntimeException(
                    "Your content must have a ListView whose id attribute is " +
                            "'android.R.id.list'");
        }
        if (emptyView != null) {
            mList.setEmptyView(emptyView);
        }
        mList.setOnItemClickListener(mOnClickListener);
        if (mFinishedStart) {
            setListAdapter(mAdapter);
        }
        mHandler.post(mRequestFocus);
        mFinishedStart = true;
    }

    public ListAdapter getListAdapter() {
        return mAdapter;
    }

    public void setListAdapter(ListAdapter adapter) {
        synchronized (this) {
            ensureList();
            mAdapter = adapter;
            mList.setAdapter(adapter);
        }
    }

    private void ensureList() {
        if (mList != null) {
            return;
        }
        setContentView(new ListView(this));

    }

    public ListView getListView() {
        ensureList();
        return mList;
    }

    public void setSelection(int position) {
        mList.setSelection(position);
    }
}
