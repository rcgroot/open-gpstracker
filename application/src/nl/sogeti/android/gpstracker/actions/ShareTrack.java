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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.tasks.GpxCreator;
import nl.sogeti.android.gpstracker.actions.tasks.GpxParser;
import nl.sogeti.android.gpstracker.actions.tasks.KmzCreator;
import nl.sogeti.android.gpstracker.actions.tasks.XmlCreator;
import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.actions.utils.StatisticsCalulator;
import nl.sogeti.android.gpstracker.adapter.BreadcrumbsAdapter;
import nl.sogeti.android.gpstracker.adapter.tasks.UploadBreadcrumbsTrackTask;
import nl.sogeti.android.gpstracker.db.GPStracking;
import nl.sogeti.android.gpstracker.db.GPStracking.Media;
import nl.sogeti.android.gpstracker.db.GPStracking.MetaData;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.oauth.PrepareRequestTokenActivity;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.UnitsI18n;
import nl.sogeti.android.gpstracker.viewer.LoggerMap;
import nl.sogeti.android.gpstracker.viewer.TrackList;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.ogt.http.HttpEntity;
import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.client.HttpClient;
import org.apache.ogt.http.client.methods.HttpPost;
import org.apache.ogt.http.conn.ClientConnectionManager;
import org.apache.ogt.http.conn.scheme.PlainSocketFactory;
import org.apache.ogt.http.conn.scheme.Scheme;
import org.apache.ogt.http.conn.scheme.SchemeRegistry;
import org.apache.ogt.http.entity.mime.HttpMultipartMode;
import org.apache.ogt.http.entity.mime.MultipartEntity;
import org.apache.ogt.http.entity.mime.content.FileBody;
import org.apache.ogt.http.entity.mime.content.StringBody;
import org.apache.ogt.http.impl.client.DefaultHttpClient;
import org.apache.ogt.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.ogt.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
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
   public static final String OAUTH_TOKEN = "openstreetmap_oauth_token";
   public static final String OAUTH_TOKEN_SECRET = "openstreetmap_oauth_secret";

   protected static final int DIALOG_FILENAME = 11;
   protected static final int PROGRESS_STEPS = 10;
   private static final int DIALOG_INSTALL_TWIDROID = 34;
   private static final String TAG = "OGT.ShareTrack";
   protected static final int DIALOG_ERROR = Menu.FIRST + 28;
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

      mFileNameView.setText(queryForTrackName());

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
      
      // Upgrade from stored username / password to OAuth authorization
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      if( prefs.contains(Constants.OSM_USERNAME) || prefs.contains(Constants.OSM_PASSWORD) )
      {
         Editor editor = prefs.edit();
         editor.remove(Constants.OSM_USERNAME);
         editor.remove(Constants.OSM_PASSWORD);
         editor.commit();
         this.requestOpenstreetmapOauthToken();
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
            builder.setIcon(android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                  .setMessage(mErrorDialogMessage + " (" + mErrorDialogException.getMessage() + ") ").setNeutralButton(android.R.string.cancel, null);
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
            alert.setMessage(mErrorDialogMessage + " (" + mErrorDialogException.getMessage() + ") ");
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
      EndJob endJob = null;
      switch (target)
      {
         case EXPORT_TARGET_SEND:
            endJob = new EndJob()
            {
               @Override
               public void shareFile(Uri fileUri)
               {
                  sendFile(fileUri, getString(R.string.email_kmzbody), getContentType());
               }
            };
            break;
         case EXPORT_TARGET_SAVE:
            endJob = null;
            break;
         default:
            Log.e(TAG, "Unable to determine target for sharing KMZ " + target);
            break;
      }
      if (endJob != null)
      {
         KmzCreator kmzCreator = new KmzCreator(this, mTrackUri, chosenFileName, new ShareProgressListener(chosenFileName, endJob));
         kmzCreator.execute();
         ShareTrack.this.finish();
      }
   }

   protected void exportGpx(String chosenFileName, int target)
   {
      boolean attachments = true;
      EndJob endJob = null;
      GpxCreator gpxCreator;
      switch (target)
      {
         case EXPORT_TARGET_SEND:
            attachments = true;
            endJob = new EndJob()
            {
               @Override
               public void shareFile(Uri fileUri)
               {
                  sendFile(fileUri, getString(R.string.email_gpxbody), getContentType());
               }
            };
            gpxCreator = new GpxCreator(this, mTrackUri, chosenFileName, attachments, new ShareProgressListener(chosenFileName, endJob));
            gpxCreator.execute();
            break;
         case EXPORT_TARGET_SAVE:
            attachments = true;
            gpxCreator = new GpxCreator(this, mTrackUri, chosenFileName, attachments, new ShareProgressListener(chosenFileName, null));
            gpxCreator.execute();
            break;
         case EXPORT_TARGET_JOGRUN:
            attachments = false;
            endJob = new EndJob()
            {
               @Override
               public void shareFile(Uri fileUri)
               {
                  sendToJogmap(fileUri);
               }
            };
            gpxCreator = new GpxCreator(this, mTrackUri, chosenFileName, attachments, new ShareProgressListener(chosenFileName, endJob));
            gpxCreator.execute();
            break;
         case EXPORT_TARGET_OSM:
            attachments = false;
            endJob = new EndJob()
            {
               @Override
               public void shareFile(Uri fileUri)
               {
                  sendToOsm(fileUri, mTrackUri);
               }
            };
            gpxCreator = new GpxCreator(this, mTrackUri, chosenFileName, attachments, new ShareProgressListener(chosenFileName, endJob));
            gpxCreator.execute();
            break;
         case EXPORT_TARGET_BREADCRUMBS:
            sendToBreadcrumbs(mTrackUri);
            break;            
         default:
            Log.e(TAG, "Unable to determine target for sharing GPX " + target);
            break;
      }
      ShareTrack.this.finish();
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

   private void sendFile(Uri fileUri, String fileContentType, String body)
   {
      Intent sendActionIntent = new Intent(Intent.ACTION_SEND);
      sendActionIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));
      sendActionIntent.putExtra(Intent.EXTRA_TEXT, body);
      sendActionIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
      sendActionIntent.setType(fileContentType);
      startActivity(Intent.createChooser(sendActionIntent, getString(R.string.sender_chooser)));
   }

   private void sendToJogmap(Uri fileUri)
   {
      String authCode = PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.JOGRUNNER_AUTH, "");
      File gpxFile = new File(fileUri.getEncodedPath());
      HttpClient httpclient = new DefaultHttpClient();
      URI jogmap = null;
      String jogmapResponseText = "";
      int statusCode = 0;
      HttpEntity responseEntity = null;
      try
      {
         jogmap = new URI(getString(R.string.jogmap_post_url));
         HttpPost method = new HttpPost(jogmap);

         MultipartEntity entity = new MultipartEntity();
         entity.addPart("id", new StringBody(authCode));
         entity.addPart("mFile", new FileBody(gpxFile));
         method.setEntity(entity);
         HttpResponse response = httpclient.execute(method);

         statusCode = response.getStatusLine().getStatusCode();
         responseEntity = response.getEntity();
         InputStream stream = responseEntity.getContent();
         jogmapResponseText = XmlCreator.convertStreamToString(stream);
      }
      catch (IOException e)
      {
         Log.e(TAG, "Failed to upload to " + jogmap.toString(), e);
         CharSequence text = getString(R.string.jogmap_failed) + e.getLocalizedMessage();
         Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
         toast.show();
      }
      catch (URISyntaxException e)
      {
         Log.e(TAG, "Failed to use configured URI " + jogmap.toString(), e);
         CharSequence text = getString(R.string.jogmap_failed) + e.getLocalizedMessage();
         Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
         toast.show();
      }
      finally
      {
         if (responseEntity != null)
         {
            try
            {
               EntityUtils.consume(responseEntity);
            }
            catch (IOException e)
            {
               Log.e(TAG, "Failed to close the content stream", e);
            }
         }
      }
      if (statusCode == 200)
      {
         CharSequence text = getString(R.string.jogmap_success) + jogmapResponseText;
         Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
         toast.show();
      }
      else
      {
         Log.e(TAG, "Wrong status code " + statusCode);
         CharSequence text = getString(R.string.jogmap_failed) + jogmapResponseText;
         Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
         toast.show();
      }
   }

   /**
    * POST a (GPX) file to the 0.6 API of the OpenStreetMap.org website
    * publishing this track to the public.
    * 
    * @param fileUri
    * @param contentType
    */
   private void sendToOsm(final Uri fileUri, final Uri trackUri)
   {
      CommonsHttpOAuthConsumer consumer = osmConnectionSetup();
      if( consumer == null )
      {
         requestOpenstreetmapOauthToken();
         return;
      }
      
      String visibility = PreferenceManager.getDefaultSharedPreferences(this).getString(Constants.OSM_VISIBILITY, "trackable");
      File gpxFile = new File(fileUri.getEncodedPath());

      String url = getString(R.string.osm_post_url);
      DefaultHttpClient httpclient = new DefaultHttpClient();
      HttpResponse response = null;
      String responseText = "";
      int statusCode = 0;
      Cursor metaData = null;
      String sources = null;
      HttpEntity responseEntity = null;
      try
      {
         metaData = this.getContentResolver().query(Uri.withAppendedPath(trackUri, "metadata"), new String[] { MetaData.VALUE }, MetaData.KEY + " = ? ",
               new String[] { Constants.DATASOURCES_KEY }, null);
         if (metaData.moveToFirst())
         {
            sources = metaData.getString(0);
         }
         if (sources != null && sources.contains(LoggerMap.GOOGLE_PROVIDER))
         {
            throw new IOException("Unable to upload track with materials derived from Google Maps.");
         }

         // The POST to the create node
         HttpPost method = new HttpPost(url);
         consumer.sign(method);
         
         // Build the multipart body with the upload data
         MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
         entity.addPart("file", new FileBody(gpxFile));
         entity.addPart("description", new StringBody(queryForTrackName()));
         entity.addPart("tags", new StringBody(queryForNotes()));
         entity.addPart("visibility", new StringBody(visibility));
         method.setEntity(entity);

         // Execute the POST to OpenStreetMap
         response = httpclient.execute(method);

         // Read the response
         statusCode = response.getStatusLine().getStatusCode();
         responseEntity = response.getEntity();
         InputStream stream = responseEntity.getContent();
         responseText = XmlCreator.convertStreamToString(stream);
      }
      catch (OAuthMessageSignerException e)
      {
         Log.e(TAG, "Failed to upload to " + url + "Response: " + responseText, e);
         responseText = getString(R.string.osm_failed) + e.getLocalizedMessage();
         Toast toast = Toast.makeText(this, responseText, Toast.LENGTH_LONG);
         toast.show();
         Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
         editor.remove(OAUTH_TOKEN);
         editor.remove(OAUTH_TOKEN_SECRET);
         editor.commit();
      }
      catch (OAuthExpectationFailedException e)
      {
         Log.e(TAG, "Failed to upload to " + url + "Response: " + responseText, e);
         responseText = getString(R.string.osm_failed) + e.getLocalizedMessage();
         Toast toast = Toast.makeText(this, responseText, Toast.LENGTH_LONG);
         toast.show();
         Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
         editor.remove(OAUTH_TOKEN);
         editor.remove(OAUTH_TOKEN_SECRET);
         editor.commit();
      }
      catch (OAuthCommunicationException e)
      {
         Log.e(TAG, "Failed to upload to " + url + "Response: " + responseText, e);
         responseText = getString(R.string.osm_failed) + e.getLocalizedMessage();
         Toast toast = Toast.makeText(this, responseText, Toast.LENGTH_LONG);
         toast.show();
      }
      catch (IOException e)
      {
         Log.e(TAG, "Failed to upload to " + url + "Response: " + responseText, e);
         responseText = getString(R.string.osm_failed) + e.getLocalizedMessage();
         Toast toast = Toast.makeText(this, responseText, Toast.LENGTH_LONG);
         toast.show();
      }
      finally
      {
         if (responseEntity != null)
         {
            try
            {
               EntityUtils.consume(responseEntity);
            }
            catch (IOException e)
            {
               Log.e(TAG, "Failed to close the content stream", e);
            }
         }
         if (metaData != null)
         {
            metaData.close();
         }
      }

      if (statusCode == 200)
      {
         Log.i(TAG, responseText);
         CharSequence text = getString(R.string.osm_success) + responseText;
         Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
         toast.show();
      }
      else
      {
         Log.e(TAG, "Failed to upload to error code " + statusCode + " " + responseText);
         CharSequence text = getString(R.string.osm_failed) + responseText;
         Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
         toast.show();
         if( statusCode == 401 )
         {
            Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.remove(OAUTH_TOKEN);
            editor.remove(OAUTH_TOKEN_SECRET);
            editor.commit();
         }
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
               adapter.startUploadTask(this, new ShareProgressListener("shareToGobreadcrumbs", null), trackUri);
               break;
            default:
               super.onActivityResult(requestCode, resultCode, data);
               break;
         }
      }
   }

   public CommonsHttpOAuthConsumer osmConnectionSetup()
   {
      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      String token = prefs.getString(OAUTH_TOKEN, "");
      String secret = prefs.getString(OAUTH_TOKEN_SECRET, "");
      boolean mAuthorized = !"".equals(token) && !"".equals(secret);
      CommonsHttpOAuthConsumer consumer = null;
      if (mAuthorized)
      {
         consumer = new CommonsHttpOAuthConsumer(this.getString(R.string.OSM_CONSUMER_KEY), this.getString(R.string.OSM_CONSUMER_SECRET));
         consumer.setTokenWithSecret(token, secret);
      }
      return consumer;
   }

   public void requestOpenstreetmapOauthToken()
   {
      Intent i = new Intent(this.getApplicationContext(), PrepareRequestTokenActivity.class);
      i.putExtra(PrepareRequestTokenActivity.OAUTH_TOKEN_PREF, OAUTH_TOKEN);
      i.putExtra(PrepareRequestTokenActivity.OAUTH_TOKEN_SECRET_PREF, OAUTH_TOKEN_SECRET);

      i.putExtra(PrepareRequestTokenActivity.CONSUMER_KEY, this.getString(R.string.OSM_CONSUMER_KEY));
      i.putExtra(PrepareRequestTokenActivity.CONSUMER_SECRET, this.getString(R.string.OSM_CONSUMER_SECRET));
      i.putExtra(PrepareRequestTokenActivity.REQUEST_URL, Constants.OSM_REQUEST_URL);
      i.putExtra(PrepareRequestTokenActivity.ACCESS_URL, Constants.OSM_ACCESS_URL);
      i.putExtra(PrepareRequestTokenActivity.AUTHORIZE_URL, Constants.OSM_AUTHORIZE_URL);

      this.startActivity(i);
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
      String name = queryForTrackName();
      String distString = calculator.getDistanceText();
      String avgSpeed = calculator.getAvgSpeedText();
      String duration = calculator.getDurationText();
      return String.format(getString(R.string.tweettext, name, distString, avgSpeed, duration));
   }

   private String queryForTrackName()
   {
      ContentResolver resolver = getContentResolver();
      Cursor trackCursor = null;
      String name = null;

      try
      {
         trackCursor = resolver.query(mTrackUri, new String[] { Tracks.NAME }, null, null, null);
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

   private String queryForNotes()
   {
      StringBuilder tags = new StringBuilder();
      ContentResolver resolver = getContentResolver();
      Cursor mediaCursor = null;
      Uri mediaUri = Uri.withAppendedPath(mTrackUri, "media");
      try
      {
         mediaCursor = resolver.query(mediaUri, new String[] { Media.URI }, null, null, null);
         if (mediaCursor.moveToFirst())
         {
            do
            {
               Uri noteUri = Uri.parse(mediaCursor.getString(0));
               if (noteUri.getScheme().equals("content") && noteUri.getAuthority().equals(GPStracking.AUTHORITY + ".string"))
               {
                  String tag = noteUri.getLastPathSegment().trim();
                  if (!tag.contains(" "))
                  {
                     if (tags.length() > 0)
                     {
                        tags.append(" ");
                     }
                     tags.append(tag);
                  }
               }
            }
            while (mediaCursor.moveToNext());
         }
      }
      finally
      {
         if (mediaCursor != null)
         {
            mediaCursor.close();
         }
      }
      return tags.toString();
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

   public class ShareProgressListener implements ProgressListener
   {
      private String mFileName;
      private EndJob mEndJob;
      private int mGoal;
      private int mProgress;

      public ShareProgressListener(String sharename, EndJob endJob)
      {
         mFileName = sharename;
         mEndJob = endJob;
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
         if (mEndJob != null && file != null)
         {
            mEndJob.shareFile(file);
         }
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
         showDialog(DIALOG_ERROR);
      }

   }

   public static abstract class EndJob
   {
      private String mType;

      void setContentType(String type)
      {
         mType = type;
      }

      String getContentType()
      {
         return mType;
      }

      abstract void shareFile(Uri fileUri);
   }
}
