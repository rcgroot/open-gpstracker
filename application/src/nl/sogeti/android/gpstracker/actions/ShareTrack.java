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
import nl.sogeti.android.gpstracker.actions.tasks.GpxCreator;
import nl.sogeti.android.gpstracker.actions.tasks.GpxSharing;
import nl.sogeti.android.gpstracker.actions.tasks.JogmapSharing;
import nl.sogeti.android.gpstracker.actions.tasks.KmzCreator;
import nl.sogeti.android.gpstracker.actions.tasks.KmzSharing;
import nl.sogeti.android.gpstracker.actions.tasks.OsmSharing;
import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.actions.utils.StatisticsCalulator;
import nl.sogeti.android.gpstracker.adapter.BreadcrumbsAdapter;
import nl.sogeti.android.gpstracker.db.GPStracking;
import nl.sogeti.android.gpstracker.db.GPStracking.Media;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.UnitsI18n;
import nl.sogeti.android.gpstracker.viewer.LoggerMap;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.Toast;

public class ShareTrack extends Activity
{
   private static final String TAG = "OGT.ShareTrack";
   
   private static final int EXPORT_TYPE_KMZ = 0;
   private static final int EXPORT_TYPE_GPX = 1;
   private static final int EXPORT_TYPE_TEXTLINE = 2;
   private static final int EXPORT_TARGET_SAVE = 0;
   private static final int EXPORT_TARGET_SEND = 1;
   private static final int EXPORT_TARGET_JOGRUN = 2;
   private static final int EXPORT_TARGET_OSM = 3;
   private static final int EXPORT_TARGET_BREADCRUMBS = 4;
   private static final int EXPORT_TYPE_TWITDRIOD = 0;
   private static final int EXPORT_TYPE_SMS = 1;
   private static final int EXPORT_TYPE_TEXT = 2;

   private static final int PROGRESS_STEPS = 10;
   private static final int DIALOG_INSTALL_TWIDROID = Menu.FIRST + 27 ;
   private static final int DIALOG_ERROR = Menu.FIRST + 28;
   private static final int DIALOG_CONNECTBREADCRUMBS = Menu.FIRST + 29;
   private static final int DESCRIBE = 312;

   private RemoteViews mContentView;
   private int barProgress = 0;
   private Notification mNotification;
   private NotificationManager mNotificationManager;

   private EditText mFileNameView;
   private EditText mTweetView;
   private Spinner mShareTypeSpinner;
   private Spinner mShareTargetSpinner;
   private Uri mTrackUri;
   private StatisticsCalulator calculator;
   private OnClickListener mTwidroidDialogListener = new DialogInterface.OnClickListener()
   {
      public void onClick(DialogInterface dialog, int which)
      {
         Uri twidroidUri = Uri.parse("market://details?id=com.twidroid");
         Intent getTwidroid = new Intent(Intent.ACTION_VIEW, twidroidUri);
         try
         {
            startActivity(getTwidroid);
         }
         catch (ActivityNotFoundException e)
         {
            twidroidUri = Uri.parse("http://twidroid.com/download/");
            getTwidroid = new Intent(Intent.ACTION_VIEW, twidroidUri);
            startActivity(getTwidroid);
         }
      }
   };
   private OnClickListener mBreadcrumbsDialogListener = new OnClickListener()
   {
      public void onClick(DialogInterface dialog, int which)
      {
         BreadcrumbsAdapter breadcrumbAdapter = new BreadcrumbsAdapter(ShareTrack.this, null);
         breadcrumbAdapter.requestBreadcrumbsOauthToken(ShareTrack.this);
      }
   };
   private String mErrorDialogMessage;
   private Throwable mErrorDialogException;

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.sharedialog);

      mTrackUri = getIntent().getData();
      calculator = new StatisticsCalulator(this, new UnitsI18n(this, null));

      mFileNameView = (EditText) findViewById(R.id.fileNameField);
      mTweetView = (EditText) findViewById(R.id.tweetField);

      mShareTypeSpinner = (Spinner) findViewById(R.id.shareTypeSpinner);
      ArrayAdapter<CharSequence> shareTypeAdapter = ArrayAdapter.createFromResource(this, R.array.sharetype_choices, android.R.layout.simple_spinner_item);
      shareTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      mShareTypeSpinner.setAdapter(shareTypeAdapter);
      mShareTargetSpinner = (Spinner) findViewById(R.id.shareTargetSpinner);
      mShareTargetSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
      {
         public void onItemSelected(AdapterView< ? > arg0, View arg1, int position, long arg3)
         {
            if (position == EXPORT_TARGET_BREADCRUMBS)
            {
               BreadcrumbsAdapter breadcrumbAdapter = new BreadcrumbsAdapter(ShareTrack.this, null);
               boolean authorized = breadcrumbAdapter.connectionSetup();
               if (!authorized)
               {
                  showDialog(DIALOG_CONNECTBREADCRUMBS);
               }
            }
         }
         public void onNothingSelected(AdapterView< ? > arg0)
         { /* NOOP */
         }
      });

      mShareTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
      {
         public void onItemSelected(AdapterView< ? > arg0, View arg1, int position, long arg3)
         {
            adjustTargetToType(position);
         }
         public void onNothingSelected(AdapterView< ? > arg0)
         { /* NOOP */
         }
      });

      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      int lastType = prefs.getInt(Constants.EXPORT_TYPE, EXPORT_TYPE_KMZ);
      mShareTypeSpinner.setSelection(lastType);
      adjustTargetToType(lastType);

      mFileNameView.setText(queryForTrackName(getContentResolver(), mTrackUri));

      Button okay = (Button) findViewById(R.id.okayshare_button);
      okay.setOnClickListener(new View.OnClickListener()
      {
         public void onClick(View v)
         {
            share();
         }
      });

      Button cancel = (Button) findViewById(R.id.cancelshare_button);
      cancel.setOnClickListener(new View.OnClickListener()
      {
         public void onClick(View v)
         {
            ShareTrack.this.finish();
         }
      });
   }

   @Override
   protected void onResume()
   {
      super.onResume();
      
      // Upgrade from stored OSM username/password to OAuth authorization
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      if( prefs.contains(Constants.OSM_USERNAME) || prefs.contains(Constants.OSM_PASSWORD) )
      {
         Editor editor = prefs.edit();
         editor.remove(Constants.OSM_USERNAME);
         editor.remove(Constants.OSM_PASSWORD);
         editor.commit();
      }
   }
   
   /**
    * @see android.app.Activity#onCreateDialog(int)
    */
   @Override
   protected Dialog onCreateDialog(int id)
   {
      Dialog dialog = null;
      Builder builder = null;
      switch (id)
      {
         case DIALOG_INSTALL_TWIDROID:
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_notwidroid).setMessage(R.string.dialog_notwidroid_message).setIcon(android.R.drawable.ic_dialog_alert)
                  .setPositiveButton(R.string.btn_install, mTwidroidDialogListener).setNegativeButton(R.string.btn_cancel, null);
            dialog = builder.create();
            return dialog;
         case DIALOG_ERROR:
            builder = new AlertDialog.Builder(this);
            String exceptionMessage =  mErrorDialogException == null ? "" :  " (" + mErrorDialogException.getMessage() + ") "; 
            builder.setIcon(android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                  .setMessage(mErrorDialogMessage + exceptionMessage).setNeutralButton(android.R.string.cancel, null);
            dialog = builder.create();
            return dialog;
         case DIALOG_CONNECTBREADCRUMBS:
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_breadcrumbsconnect).setMessage(R.string.dialog_breadcrumbsconnect_message).setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(R.string.btn_okay, mBreadcrumbsDialogListener).setNegativeButton(R.string.btn_cancel, null);
      dialog = builder.create();
      return dialog;
         default:
            return super.onCreateDialog(id);
      }
   }

   /**
    * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
    */
   @Override
   protected void onPrepareDialog(int id, Dialog dialog)
   {
      super.onPrepareDialog(id, dialog);
      AlertDialog alert;
      switch (id)
      {
         case DIALOG_ERROR:
            alert = (AlertDialog) dialog;
            String exceptionMessage =  mErrorDialogException == null ? "" :  " (" + mErrorDialogException.getMessage() + ") ";
            alert.setMessage(mErrorDialogMessage + exceptionMessage);
            break;
      }
   }

   private void setGpxExportTargets()
   {
      ArrayAdapter<CharSequence> shareTargetAdapter = ArrayAdapter.createFromResource(this, R.array.sharegpxtarget_choices,
            android.R.layout.simple_spinner_item);
      shareTargetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      mShareTargetSpinner.setAdapter(shareTargetAdapter);
      int lastTarget = PreferenceManager.getDefaultSharedPreferences(this).getInt(Constants.EXPORT_GPXTARGET, EXPORT_TARGET_SEND);
      mShareTargetSpinner.setSelection(lastTarget);
   }

   private void setKmzExportTargets()
   {
      ArrayAdapter<CharSequence> shareTargetAdapter = ArrayAdapter.createFromResource(this, R.array.sharekmztarget_choices,
            android.R.layout.simple_spinner_item);
      shareTargetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      mShareTargetSpinner.setAdapter(shareTargetAdapter);
      int lastTarget = PreferenceManager.getDefaultSharedPreferences(this).getInt(Constants.EXPORT_KMZTARGET, EXPORT_TARGET_SEND);
      mShareTargetSpinner.setSelection(lastTarget);
   }

   private void setTextLineExportTargets()
   {
      ArrayAdapter<CharSequence> shareTargetAdapter = ArrayAdapter.createFromResource(this, R.array.sharetexttarget_choices,
            android.R.layout.simple_spinner_item);
      shareTargetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      mShareTargetSpinner.setAdapter(shareTargetAdapter);
      int lastTarget = PreferenceManager.getDefaultSharedPreferences(this).getInt(Constants.EXPORT_TXTTARGET, EXPORT_TYPE_TWITDRIOD);
      mShareTargetSpinner.setSelection(lastTarget);

   }

   private void share()
   {
      String chosenFileName = mFileNameView.getText().toString();
      String textLine = mTweetView.getText().toString();
      int type = (int) mShareTypeSpinner.getSelectedItemId();
      int target = (int) mShareTargetSpinner.getSelectedItemId();

      Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
      editor.putInt(Constants.EXPORT_TYPE, type);

      switch (type)
      {
         case EXPORT_TYPE_KMZ:
            editor.putInt(Constants.EXPORT_KMZTARGET, target);
            editor.commit();
            exportKmz(chosenFileName, target);
            break;
         case EXPORT_TYPE_GPX:
            editor.putInt(Constants.EXPORT_GPXTARGET, target);
            editor.commit();
            exportGpx(chosenFileName, target);
            break;
         case EXPORT_TYPE_TEXTLINE:
            editor.putInt(Constants.EXPORT_TXTTARGET, target);
            editor.commit();
            exportTextLine(textLine, target);
         default:
            Log.e(TAG, "Failed to determine sharing type" + type);
            break;
      }
   }

   protected void exportKmz(String chosenFileName, int target)
   {
      switch (target)
      {
         case EXPORT_TARGET_SEND:
            new KmzSharing(this, mTrackUri, chosenFileName, new ShareProgressListener(chosenFileName)).execute();
            break;
         case EXPORT_TARGET_SAVE:
            new KmzCreator(this, mTrackUri, chosenFileName, new ShareProgressListener(chosenFileName)).execute();
            break;
         default:
            Log.e(TAG, "Unable to determine target for sharing KMZ " + target);
            break;
      }
      ShareTrack.this.finish();
   }

   protected void exportGpx(String chosenFileName, int target)
   {
      switch (target)
      {
         case EXPORT_TARGET_SAVE:
            new GpxCreator(this, mTrackUri, chosenFileName, true, new ShareProgressListener(chosenFileName)).execute();
            ShareTrack.this.finish();
            break;
         case EXPORT_TARGET_SEND:
            new GpxSharing(this, mTrackUri, chosenFileName, true, new ShareProgressListener(chosenFileName)).execute();
            ShareTrack.this.finish();
            break;
         case EXPORT_TARGET_JOGRUN:
            new JogmapSharing(this, mTrackUri, chosenFileName, false, new ShareProgressListener(chosenFileName)).execute();
            ShareTrack.this.finish();
            break;
         case EXPORT_TARGET_OSM:
            new OsmSharing(this, mTrackUri, false, new ShareProgressListener(OsmSharing.OSM_FILENAME)).execute();
            ShareTrack.this.finish();
            break;
         case EXPORT_TARGET_BREADCRUMBS:
            sendToBreadcrumbs(mTrackUri);
            break;            
         default:
            Log.e(TAG, "Unable to determine target for sharing GPX " + target);
            break;
      }
   }

   protected void exportTextLine(String message, int target)
   {
      String subject = "Open GPS Tracker";
      switch (target)
      {
         case EXPORT_TYPE_TWITDRIOD:
            sendTwidroidTweet(message);
            break;
         case EXPORT_TYPE_SMS:
            sendSMS(message);
            ShareTrack.this.finish();
            break;
         case EXPORT_TYPE_TEXT:
            sentGenericText(subject, message);
            ShareTrack.this.finish();
            break;
      }

   }

   private void sendTwidroidTweet(String tweet)
   {
      final Intent intent = new Intent("com.twidroid.SendTweet");
      intent.putExtra("com.twidroid.extra.MESSAGE", tweet);
      intent.setType("application/twitter");
      try
      {
         startActivity(intent);
         ShareTrack.this.finish();
      }
      catch (ActivityNotFoundException e)
      {
         showDialog(DIALOG_INSTALL_TWIDROID);
      }
   }

   private void sendToBreadcrumbs(Uri mTrackUri)
   {
      // Start a description of the track
      Intent namingIntent = new Intent(this, DescribeTrack.class);
      namingIntent.setData(mTrackUri);
      startActivityForResult(namingIntent, DESCRIBE);
   }
   
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data)
   {
      if (resultCode != RESULT_CANCELED)
      {
         switch (requestCode)
         {
            case DESCRIBE:
               Uri trackUri = data.getData();
               BreadcrumbsAdapter adapter = new BreadcrumbsAdapter(this, null);
               adapter.startUploadTask(this, new ShareProgressListener("shareToGobreadcrumbs"), trackUri);
               break;
            default:
               super.onActivityResult(requestCode, resultCode, data);
               break;
         }
      }
   }

   private void sendSMS(String msg)
   {
      final Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setType("vnd.android-dir/mms-sms");
      intent.putExtra("sms_body", msg);
      startActivity(intent);
   }

   private void sentGenericText(String subject, String msg)
   {
      final Intent intent = new Intent(Intent.ACTION_SEND);
      intent.setType("text/plain");
      intent.putExtra(Intent.EXTRA_SUBJECT, subject);
      intent.putExtra(Intent.EXTRA_TEXT, msg);
      startActivity(intent);
   }

   private String createTweetText()
   {
      calculator.updateCalculations(mTrackUri);
      String name = queryForTrackName(getContentResolver(), mTrackUri);
      String distString = calculator.getDistanceText();
      String avgSpeed = calculator.getAvgSpeedText();
      String duration = calculator.getDurationText();
      return String.format(getString(R.string.tweettext, name, distString, avgSpeed, duration));
   }

   private void adjustTargetToType(int position)
   {
      switch (position)
      {
         case EXPORT_TYPE_KMZ:
            setKmzExportTargets();
            mFileNameView.setVisibility(View.VISIBLE);
            mTweetView.setVisibility(View.GONE);
            break;
         case EXPORT_TYPE_GPX:
            setGpxExportTargets();
            mFileNameView.setVisibility(View.VISIBLE);
            mTweetView.setVisibility(View.GONE);
            break;
         case EXPORT_TYPE_TEXTLINE:
            setTextLineExportTargets();
            mFileNameView.setVisibility(View.GONE);
            mTweetView.setVisibility(View.VISIBLE);
            if (mTweetView.getText().toString().equals(""))
            {
               mTweetView.setText(createTweetText());
            }
         default:
            break;
      }
   }

   public static void sendFile(Context context, Uri fileUri, String fileContentType, String body)
   {
      Intent sendActionIntent = new Intent(Intent.ACTION_SEND);
      sendActionIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject));
      sendActionIntent.putExtra(Intent.EXTRA_TEXT, body);
      sendActionIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
      sendActionIntent.setType(fileContentType);
      context.startActivity(Intent.createChooser(sendActionIntent, context.getString(R.string.sender_chooser)));
   }

   public static String queryForTrackName(ContentResolver resolver, Uri trackUri)
   {
      Cursor trackCursor = null;
      String name = null;
   
      try
      {
         trackCursor = resolver.query(trackUri, new String[] { Tracks.NAME }, null, null, null);
         if (trackCursor.moveToFirst())
         {
            name = trackCursor.getString(0);
         }
      }
      finally
      {
         if (trackCursor != null)
         {
            trackCursor.close();
         }
      }
      return name;
   }

   public class ShareProgressListener implements ProgressListener
   {
      private String mFileName;
      private int mGoal;
      private int mProgress;

      public ShareProgressListener(String sharename)
      {
         mFileName = sharename;
      }

      public void startNotification()
      {
         String ns = Context.NOTIFICATION_SERVICE;
         mNotificationManager = (NotificationManager) ShareTrack.this.getSystemService(ns);
         int icon = android.R.drawable.ic_menu_save;
         CharSequence tickerText = getString(R.string.ticker_saving) + "\"" + mFileName + "\"";

         mNotification = new Notification();
         PendingIntent contentIntent = PendingIntent.getActivity(ShareTrack.this, 0,
               new Intent(ShareTrack.this, LoggerMap.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT);

         mNotification.contentIntent = contentIntent;
         mNotification.tickerText = tickerText;
         mNotification.icon = icon;
         mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
         mContentView = new RemoteViews(getPackageName(), R.layout.savenotificationprogress);
         mContentView.setImageViewResource(R.id.icon, icon);
         mContentView.setTextViewText(R.id.progresstext, tickerText);

         mNotification.contentView = mContentView;
      }

      private void updateNotification()
      {
         //         Log.d( "TAG", "Progress " + progress + " of " + goal );
         if (mProgress > 0 && mProgress < mGoal)
         {
            if ((mProgress * PROGRESS_STEPS) / mGoal != barProgress)
            {
               barProgress = (mProgress * PROGRESS_STEPS) / mGoal;
               mContentView.setProgressBar(R.id.progress, mGoal, mProgress, false);
               mNotificationManager.notify(R.layout.savenotificationprogress, mNotification);
            }
         }
         else if (mProgress == 0)
         {
            mContentView.setProgressBar(R.id.progress, mGoal, mProgress, true);
            mNotificationManager.notify(R.layout.savenotificationprogress, mNotification);
         }
         else if (mProgress >= mGoal)
         {
            mContentView.setProgressBar(R.id.progress, mGoal, mProgress, false);
            mNotificationManager.notify(R.layout.savenotificationprogress, mNotification);
         }
      }

      public void endNotification(Uri file)
      {
         mNotificationManager.cancel(R.layout.savenotificationprogress);
      }

      public void setIndeterminate(boolean indeterminate)
      {
         Log.w(TAG, "Unsupported indeterminate progress display");
      }

      public void setMax(int max)
      {
         this.mGoal = max;
      }

      public void started()
      {
         startNotification();
      }

      public void increaseProgress(int value)
      {
         setProgress(mProgress + value);
      }

      public void setProgress(int value)
      {
         mProgress = value;
         updateNotification();
      }

      public void finished(Uri result)
      {
         endNotification(result);
      }

      public void showError(String task, String errorDialogMessage, Exception errorDialogException)
      {
         endNotification(null);

         mErrorDialogMessage = errorDialogMessage;
         mErrorDialogException = errorDialogException;
         if( !isFinishing() )
         {
            showDialog(DIALOG_ERROR);
         }
         else
         {
            Toast toast = Toast.makeText(ShareTrack.this, errorDialogMessage, Toast.LENGTH_LONG);
            toast.show();
         }
      }

   }
}
