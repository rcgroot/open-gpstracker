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
package nl.sogeti.android.gpstracker.actions;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.adapter.BreadcrumbsAdapter;
import nl.sogeti.android.gpstracker.adapter.BreadcrumbsTracks;
import nl.sogeti.android.gpstracker.db.GPStracking.MetaData;
import nl.sogeti.android.gpstracker.util.Constants;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

/**
 * Empty Activity that pops up the dialog to describe the track
 * 
 * @version $Id: NameTrack.java 888 2011-03-14 19:44:44Z rcgroot@gmail.com $
 * @author rene (c) Jul 27, 2010, Sogeti B.V.
 */
public class DescribeTrack extends Activity implements ProgressListener
{
   private static final int DIALOG_TRACKDESCRIPTION = 42;

   protected static final String TAG = "OGT.DescribeTrack";

   private static final String ACTIVITY_ID = "ACTIVITY_ID";

   private static final String BUNDLE_ID = "BUNDLE_ID";

   private boolean finishedLoading = false;
   
   private Spinner mActivitySpinner;
   private Spinner mBundleSpinner;
   private EditText mDescriptionText;
   private CheckBox mPublicCheck;
   private Button mOkayButton;
   private boolean paused;
   private Uri mTrackUri;

   private final DialogInterface.OnClickListener mTrackDescriptionDialogListener = new DialogInterface.OnClickListener()
   {
      public void onClick(DialogInterface dialog, int which)
      {
         switch (which)
         {
            case DialogInterface.BUTTON_POSITIVE:
               Uri metadataUri = Uri.withAppendedPath(mTrackUri, "metadata");
               Integer activityId = BreadcrumbsTracks.getIdForActivity((String) mActivitySpinner.getSelectedItem());
               Integer bundleId = BreadcrumbsTracks.getIdForBundle(activityId, (String) mBundleSpinner.getSelectedItem());
               saveBreadcrumbsPreference(mActivitySpinner.getSelectedItemPosition(), mBundleSpinner.getSelectedItemPosition());
               String description = mDescriptionText.getText().toString();
               String isPublic = Boolean.toString(mPublicCheck.isChecked());
               ContentValues[] metaValues = { buildContentValues(BreadcrumbsTracks.ACTIVITY_ID, activityId.toString()),
                     buildContentValues(BreadcrumbsTracks.BUNDLE_ID, bundleId.toString()), buildContentValues(BreadcrumbsTracks.DESCRIPTION, description),
                     buildContentValues(BreadcrumbsTracks.ISPUBLIC, isPublic), };
               getContentResolver().bulkInsert(metadataUri, metaValues);
               Intent data = new Intent();
               data.setData(mTrackUri);
               if( getIntent().getExtras() != null && getIntent().getExtras().containsKey(Constants.NAME))
               {
                  data.putExtra(Constants.NAME, getIntent().getExtras().getString(Constants.NAME));
               }
               setResult(RESULT_OK, data);
               break;
            case DialogInterface.BUTTON_NEGATIVE:
               break;
            default:
               Log.e(TAG, "Unknown option ending dialog:" + which);
               break;
         }
         finish();
      }
   };

   private BreadcrumbsAdapter mBreadcrumbAdapter;

   private ProgressBar mProgressSpinner;

   private AlertDialog mDialog;


   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);

      this.setVisible(false);
      paused = false;

      mTrackUri = this.getIntent().getData();
   }

   @Override
   protected void onPause()
   {
      super.onPause();
      paused = true;
   }

   /*
    * (non-Javadoc)
    * @see com.google.android.maps.MapActivity#onPause()
    */
   @Override
   protected void onResume()
   {
      super.onResume();
      if (mTrackUri != null)
      {
         showDialog(DIALOG_TRACKDESCRIPTION);
      }
      else
      {
         Log.e(TAG, "Describing track without a track URI supplied.");
         finish();
      }
   }
   
   @Override
   protected Dialog onCreateDialog(int id)
   {
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_TRACKDESCRIPTION:
            builder = new AlertDialog.Builder(this);
            factory = LayoutInflater.from(this);
            view = factory.inflate(R.layout.describedialog, null);
            mActivitySpinner = (Spinner) view.findViewById(R.id.activity);
            mBundleSpinner = (Spinner) view.findViewById(R.id.bundle);
            mDescriptionText = (EditText) view.findViewById(R.id.description);
            mPublicCheck = (CheckBox) view.findViewById(R.id.public_checkbox);
            mProgressSpinner = (ProgressBar) view.findViewById(R.id.progressSpinner);
            builder.setTitle(R.string.dialog_description_title).setMessage(R.string.dialog_description_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(R.string.btn_okay, mTrackDescriptionDialogListener)
                  .setNegativeButton(R.string.btn_cancel, mTrackDescriptionDialogListener).setView(view);
            mDialog = builder.create();
            setUiEnabled(finishedLoading);
            
            mDialog.setOnDismissListener(new OnDismissListener()
            {
               public void onDismiss(DialogInterface dialog)
               {
                  if (!paused)
                  {
                     finish();
                  }
               }
            });
            return mDialog;
         default:
            return super.onCreateDialog(id);
      }
   }
   
   @Override
   protected void onPrepareDialog(int id, Dialog dialog)
   {
      switch (id)
      {
         case DIALOG_TRACKDESCRIPTION:
            setUiEnabled(finishedLoading);
            connectBreadcrumbs();
            break;
         default:
            super.onPrepareDialog(id, dialog);
            break;
      }
   }
   
   private void connectBreadcrumbs()
   {
      mBreadcrumbAdapter = new BreadcrumbsAdapter(this, this);
      boolean authorized = mBreadcrumbAdapter.connectionSetup();
      if (!authorized)
      {
         mBreadcrumbAdapter.requestBreadcrumbsOauthToken(this);
      }
   }
   private void saveBreadcrumbsPreference(int activityPosition, int bundlePosition)
   {
      Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
      editor.putInt(ACTIVITY_ID, activityPosition);
      editor.putInt(BUNDLE_ID, bundlePosition);
      editor.commit();
   }
   
   private void loadBreadcrumbsPreference()
   {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      
      int activityPos = prefs.getInt(ACTIVITY_ID, 0);
      activityPos = activityPos < mActivitySpinner.getCount() ? activityPos : 0; 
      mActivitySpinner.setSelection(activityPos);
      
      int bundlePos = prefs.getInt(BUNDLE_ID, 0);
      bundlePos = bundlePos < mBundleSpinner.getCount() ? bundlePos : 0; 
      mBundleSpinner.setSelection(bundlePos);
   }

   private ContentValues buildContentValues(String key, String value)
   {
      ContentValues contentValues = new ContentValues();
      contentValues.put(MetaData.KEY, key);
      contentValues.put(MetaData.VALUE, value);
      return contentValues;
   }

   public void started()
   {
      finishedLoading = false;
   }

   public void finished(Uri result)
   {
      finishedLoading = true;
      setUiEnabled(finishedLoading);
   }

   public void showError(String task, String errorMessage, Exception exception)
   {
      setUiEnabled(true);
      finish();
   }
   // Ignored only spinner
   public void setIndeterminate(boolean indeterminate){}
   public void setMax(int max){}
   public void increaseProgress(int value){}

   
   private void setUiEnabled(boolean enabled)
   {
      if (mProgressSpinner != null)
      {
         if (enabled)
         {
            mProgressSpinner.setVisibility(View.GONE);
         }
         else
         {
            mProgressSpinner.setVisibility(View.VISIBLE);
         }
      }

      if( mDialog != null )
      {
         mOkayButton = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
      }
      for (View view : new View[] { mActivitySpinner, mBundleSpinner, mDescriptionText, mPublicCheck, mOkayButton })
      {
         if (view != null)
         {
            view.setEnabled(enabled);
         }
      }
      if (enabled)
      {
         mActivitySpinner.setAdapter(mBreadcrumbAdapter.getBreadcrumbsTracks().getActivityAdapter(this));
         mBundleSpinner.setAdapter(mBreadcrumbAdapter.getBreadcrumbsTracks().getBundleAdapter(this));
         loadBreadcrumbsPreference();
      }
   }
}
