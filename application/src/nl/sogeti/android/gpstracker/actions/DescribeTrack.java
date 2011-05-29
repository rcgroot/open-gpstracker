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
import nl.sogeti.android.gpstracker.adapter.BreadcrumbsTracks;
import nl.sogeti.android.gpstracker.db.GPStracking.MetaData;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

/**
 * Empty Activity that pops up the dialog to describe the track
 *
 * @version $Id: NameTrack.java 888 2011-03-14 19:44:44Z rcgroot@gmail.com $
 * @author rene (c) Jul 27, 2010, Sogeti B.V.
 */
public class DescribeTrack extends Activity
{
   private static final int DIALOG_TRACKDESCRIPTION = 42;

   protected static final String TAG = "OGT.DescribeTrack";

   private Spinner  mActivitySpinner;
   private Spinner  mBundleSpinner;
   private EditText mDescriptionText;
   private CheckBox mPublicCheck;
   private boolean paused;
   private Uri mTrackUri;
   
   private final DialogInterface.OnClickListener mTrackDescriptionDialogListener = new DialogInterface.OnClickListener()
   {
      public void onClick( DialogInterface dialog, int which )
      {
         switch( which )
         {
            case DialogInterface.BUTTON_POSITIVE:
               Uri metadataUri = Uri.withAppendedPath(mTrackUri, "metadata");
               String  activityId = BreadcrumbsTracks.getIdForActivity( (String) mBundleSpinner.getSelectedItem() ).toString();
               String  bundleId   = BreadcrumbsTracks.getIdForBundle( (String) mBundleSpinner.getSelectedItem() ).toString();
               String description = mDescriptionText.getText().toString() ;
               String isPublic    = Boolean.toString(mPublicCheck.isChecked());
               ContentValues[] metaValues = { 
                     buildContentValues( BreadcrumbsTracks.ACTIVITY_ID, activityId),
                     buildContentValues( BreadcrumbsTracks.BUNDLE_ID,   bundleId),
                     buildContentValues( BreadcrumbsTracks.DESCRIPTION, description),
                     buildContentValues( BreadcrumbsTracks.ISPUBLIC,    isPublic),
                     };
               getContentResolver().bulkInsert(metadataUri, metaValues);
               Intent data = new Intent();
               data.setData(mTrackUri);
               setResult(RESULT_OK, data);
               break;
            case DialogInterface.BUTTON_NEGATIVE:
               break;
            default:
               Log.e( TAG, "Unknown option ending dialog:"+which );
               break;
         }
         finish();
      }


   };

   private OnItemSelectedListener mActivitiyListener = new OnItemSelectedListener()
   {

      public void onItemSelected(AdapterView< ? > adapter, View arg1, int position, long id)
      {
         mBundleSpinner.setEnabled(true);
         mBundleSpinner.setAdapter( BreadcrumbsTracks.getBundleAdapter(DescribeTrack.this, (CharSequence) adapter.getItemAtPosition(position)) );
      }

      public void onNothingSelected(AdapterView< ? > arg0)
      {
         mBundleSpinner.setEnabled(false);
      }
   };

   @Override
   protected void onCreate( Bundle savedInstanceState )
   {
      super.onCreate( savedInstanceState );
      this.setVisible( false );
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
      if(  mTrackUri != null )
      {
         showDialog( DIALOG_TRACKDESCRIPTION );
      }
      else
      {
         Log.e(TAG, "Describing track without a track URI supplied." );
         finish();
      }
   }
   
   @Override
   protected Dialog onCreateDialog( int id )
   {
      Dialog dialog = null;
      LayoutInflater factory = null;
      View view = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_TRACKDESCRIPTION:
            builder = new AlertDialog.Builder( this );
            factory = LayoutInflater.from( this );
            view = factory.inflate( R.layout.describedialog, null );
            mActivitySpinner = (Spinner) view.findViewById( R.id.activity );
            mActivitySpinner.setOnItemSelectedListener(mActivitiyListener);
            mBundleSpinner = (Spinner) view.findViewById( R.id.bundle );
            mDescriptionText = (EditText) view.findViewById( R.id.description );
            mPublicCheck = (CheckBox) view.findViewById( R.id.public_checkbox );
            builder
               .setTitle( R.string.dialog_description_title )
               .setMessage( R.string.dialog_description_message )
               .setIcon( android.R.drawable.ic_dialog_alert )
               .setPositiveButton( R.string.btn_okay, mTrackDescriptionDialogListener )
               .setNegativeButton( R.string.btn_cancel, mTrackDescriptionDialogListener )
               .setView( view );
            dialog = builder.create();
            dialog.setOnDismissListener( new OnDismissListener()
            {
               public void onDismiss( DialogInterface dialog )
               {
                  if( !paused )
                  {
                     finish();
                  }
               }
            });
            return dialog;
         default:
            return super.onCreateDialog( id );
      }
   }
   
   @Override
   protected void onPrepareDialog( int id, Dialog dialog )
   {
      switch (id)
      {
         case DIALOG_TRACKDESCRIPTION:
            mActivitySpinner.setAdapter( BreadcrumbsTracks.getActivityAdapter(this) );
            mBundleSpinner.setAdapter( BreadcrumbsTracks.getBundleAdapter(this, "") );
            break;
         default:
            super.onPrepareDialog( id, dialog );
            break;
      }
   }
   
   private ContentValues buildContentValues( String key, String value)
   {
      ContentValues contentValues = new ContentValues();
      contentValues.put(MetaData.KEY, key);
      contentValues.put(MetaData.VALUE, value);
      return contentValues;
   }
}
   