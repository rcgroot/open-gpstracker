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
package nl.sogeti.android.gpstracker.breadcrumbs;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.tasks.GpxCreator;
import nl.sogeti.android.gpstracker.actions.tasks.XmlCreator;
import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.adapter.BreadcrumbsAdapter;
import nl.sogeti.android.gpstracker.db.GPStracking.MetaData;
import nl.sogeti.android.gpstracker.util.MultipartStreamer;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * An asynchronous task that communicates with Twitter to retrieve a request token. (OAuthGetRequestToken) After receiving the request token from Twitter, pop a browser to the user to authorize the
 * Request Token. (OAuthAuthorizeToken)
 */
public class UploadBreadcrumbsTrackTask extends GpxCreator
{

   final String TAG = "OGT.UploadBreadcrumbsTrackTask";
   private BreadcrumbsService mService;
   private OAuthConsumer mConsumer;
   private String mActivityId;
   private String mBundleId;
   private String mDescription;
   private String mIsPublic;
   private String mBundleName;
   private String mBundleDescription;
   private boolean mIsBundleCreated;
   private List<File> mPhotoUploadQueue;

   /**
    * Constructor: create a new UploadBreadcrumbsTrackTask.
    * 
    * @param context
    * @param adapter
    * @param listener
    * @param httpclient
    * @param consumer
    * @param trackUri
    * @param name
    */
   public UploadBreadcrumbsTrackTask(Context context, BreadcrumbsService adapter, ProgressListener listener, OAuthConsumer consumer, Uri trackUri, String name)
   {
      super(context, trackUri, name, true, listener);
      mService = adapter;
      mConsumer = consumer;
      mPhotoUploadQueue = new LinkedList<File>();
   }

   /**
    * Retrieve the OAuth Request Token and present a browser to the user to authorize the token.
    */
   @Override
   protected Uri doInBackground(Void... params)
   {
      // Leave room in the progressbar for uploading
      determineProgressGoal();
      mProgressAdmin.setUpload(true);

      // Build GPX file
      Uri gpxFile = exportGpx();

      if (isCancelled())
      {
         String text = mContext.getString(R.string.ticker_failed) + " \"http://api.gobreadcrumbs.com/v1/tracks\" " + mContext.getString(R.string.error_buildxml);
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_upload), new IOException("Fail to execute request due to canceling"), text);
      }

      // Collect GPX Import option params
      mActivityId = null;
      mBundleId = null;
      mDescription = null;
      mIsPublic = null;

      Uri metadataUri = Uri.withAppendedPath(mTrackUri, "metadata");
      Cursor cursor = null;
      try
      {
         cursor = mContext.getContentResolver().query(metadataUri, new String[] { MetaData.KEY, MetaData.VALUE }, null, null, null);
         if (cursor.moveToFirst())
         {
            do
            {
               String key = cursor.getString(0);
               if (BreadcrumbsTracks.ACTIVITY_ID.equals(key))
               {
                  mActivityId = cursor.getString(1);
               }
               else if (BreadcrumbsTracks.BUNDLE_ID.equals(key))
               {
                  mBundleId = cursor.getString(1);
               }
               else if (BreadcrumbsTracks.DESCRIPTION.equals(key))
               {
                  mDescription = cursor.getString(1);
               }
               else if (BreadcrumbsTracks.ISPUBLIC.equals(key))
               {
                  mIsPublic = cursor.getString(1);
               }
            }
            while (cursor.moveToNext());
         }
      }
      finally
      {
         if (cursor != null)
         {
            cursor.close();
         }
      }
      if ("-1".equals(mActivityId))
      {
         String text = "Unable to upload without a activity id stored in meta-data table";
         IllegalStateException e = new IllegalStateException(text);
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_upload), e, text);
      }

      int statusCode = 0;
      String responseText = null;
      Uri trackUri = null;
      HttpURLConnection connection = null;
      MultipartStreamer multipart = null;
      try
      {
         if ("-1".equals(mBundleId))
         {
            mBundleDescription = "";//mContext.getString(R.string.breadcrumbs_bundledescription);
            mBundleName = mContext.getString(R.string.app_name);
            mBundleId = createOpenGpsTrackerBundle();
         }

         String gpxString = XmlCreator.convertStreamToString(mContext.getContentResolver().openInputStream(gpxFile));

         URL method = new URL("http://api.gobreadcrumbs.com:80/v1/tracks");
         if (isCancelled())
         {
            throw new IOException("Fail to execute request due to canceling");
         }
         // Build the multipart body with the upload data
         connection = (HttpURLConnection) method.openConnection();
         multipart = new MultipartStreamer(connection);
         mConsumer.sign(connection);

         multipart.addFormField("import_type", "GPX");
         //entity.addPart("gpx",         new FileBody(gpxFile));
         multipart.addFormField("gpx", gpxString);
         multipart.addFormField("bundle_id", mBundleId);
         multipart.addFormField("activity_id", mActivityId);
         multipart.addFormField("description", mDescription);
         //         entity.addPart("difficulty",  new StringBody("3"));
         //         entity.addPart("rating",      new StringBody("4"));
         multipart.addFormField("public", mIsPublic);

         // Execute the POST to OpenStreetMap
         multipart.flush();
         mProgressAdmin.addUploadProgress();

         statusCode = connection.getResponseCode();
         InputStream stream = connection.getInputStream();
         responseText = XmlCreator.convertStreamToString(stream);

         if (BreadcrumbsAdapter.DEBUG)
         {
            Log.d(TAG, "Upload Response: " + responseText);
         }

         Pattern p = Pattern.compile(">([0-9]+)</id>");
         Matcher m = p.matcher(responseText);
         if (m.find())
         {
            Integer trackId = Integer.valueOf(m.group(1));
            trackUri = Uri.parse("http://api.gobreadcrumbs.com/v1/tracks/" + trackId + "/placemarks.gpx");
            for (File photo : mPhotoUploadQueue)
            {
               uploadPhoto(photo, trackId);
            }
         }

      }
      catch (OAuthMessageSignerException e)
      {
         mService.removeAuthentication();
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_upload), e, "Failed to sign the request with authentication signature");
      }
      catch (OAuthExpectationFailedException e)
      {
         mService.removeAuthentication();
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_upload), e, "The request did not authenticate");
      }
      catch (OAuthCommunicationException e)
      {
         mService.removeAuthentication();
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_upload), e, "The authentication communication failed");
      }
      catch (IOException e)
      {
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_upload), e, "A problem during communication");
      }
      finally
      {
         close(multipart);
         if (connection != null)
            connection.disconnect();
      }

      if (statusCode == 200 || statusCode == 201)
      {
         if (trackUri == null)
         {
            handleError(mContext.getString(R.string.taskerror_breadcrumbs_upload), new IOException("Unable to retrieve URI from response"), responseText);
         }
      }
      else
      {
         //mAdapter.removeAuthentication();

         handleError(mContext.getString(R.string.taskerror_breadcrumbs_upload), new IOException("Status code: " + statusCode), responseText);
      }
      return trackUri;
   }

   private String createOpenGpsTrackerBundle() throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, IOException
   {
      URL method = new URL("http://api.gobreadcrumbs.com/v1/bundles.xml");
      if (isCancelled())
      {
         throw new IOException("Fail to execute request due to canceling");
      }
      HttpURLConnection connection = null;
      MultipartStreamer multipart = null;
      String bundleId = null;
      try
      {
         connection = (HttpURLConnection) method.openConnection();
         multipart = new MultipartStreamer(connection);
         mConsumer.sign(connection);
         multipart.addFormField("name", mBundleName);
         multipart.addFormField("activity_id", mActivityId);
         multipart.addFormField("description", mBundleDescription);
         multipart.flush();

         InputStream stream = connection.getInputStream();
         String responseText = XmlCreator.convertStreamToString(stream);
         Pattern p = Pattern.compile(">([0-9]+)</id>");
         Matcher m = p.matcher(responseText);
         if (m.find())
         {
            bundleId = m.group(1);

            ContentValues values = new ContentValues();
            values.put(MetaData.KEY, BreadcrumbsTracks.BUNDLE_ID);
            values.put(MetaData.VALUE, bundleId);
            Uri metadataUri = Uri.withAppendedPath(mTrackUri, "metadata");

            mContext.getContentResolver().insert(metadataUri, values);
            mIsBundleCreated = true;
         }
         else
         {
            String text = "Unable to upload (yet) without a bunld id stored in meta-data table";
            IllegalStateException e = new IllegalStateException(text);
            handleError(mContext.getString(R.string.taskerror_breadcrumbs_upload), e, text);
         }
      }
      finally
      {
         close(multipart);
         if (connection != null)
            connection.disconnect();
      }
      return bundleId;
   }

   /**
    * Queue's media
    * 
    * @param inputFilePath
    * @return file path relative to the export dir
    * @throws IOException
    */
   @Override
   protected String includeMediaFile(String inputFilePath) throws IOException
   {
      File source = new File(inputFilePath);
      if (source.exists())
      {
         mProgressAdmin.setPhotoUpload(source.length());
         mPhotoUploadQueue.add(source);
      }
      return source.getName();
   }

   private void uploadPhoto(File photo, Integer trackId) throws IOException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException
   {
      URL request = new URL("http://api.gobreadcrumbs.com/v1/photos.xml");
      if (isCancelled())
      {
         throw new IOException("Fail to execute request due to canceling");
      }
      HttpURLConnection connection = null;
      MultipartStreamer multipart = null;
      String responseText = "";
      try
      {
         connection = (HttpURLConnection) request.openConnection();
         multipart = new MultipartStreamer(connection);
         mConsumer.sign(connection);
         multipart.addFormField("name", photo.getName());
         multipart.addFormField("track_id", Integer.toString(trackId));
         //entity.addPart("description", new StringBody(""));
         multipart.addFilePart("file", photo);

         InputStream stream = connection.getInputStream();
         responseText = XmlCreator.convertStreamToString(stream);
      }
      finally
      {
         close(multipart);
         if (connection != null)
            connection.disconnect();
      }
      mProgressAdmin.addPhotoUploadProgress(photo.length());

      Log.i(TAG, "Uploaded photo " + responseText);
   }

   @Override
   protected void onPostExecute(Uri result)
   {
      BreadcrumbsTracks tracks = mService.getBreadcrumbsTracks();
      Uri metadataUri = Uri.withAppendedPath(mTrackUri, "metadata");
      List<String> segments = result.getPathSegments();
      Integer bcTrackId = Integer.valueOf(segments.get(segments.size() - 2));

      ArrayList<ContentValues> metaValues = new ArrayList<ContentValues>();

      metaValues.add(buildContentValues(BreadcrumbsTracks.TRACK_ID, Long.toString(bcTrackId)));
      if (mDescription != null)
      {
         metaValues.add(buildContentValues(BreadcrumbsTracks.DESCRIPTION, mDescription));
      }
      if (mIsPublic != null)
      {
         metaValues.add(buildContentValues(BreadcrumbsTracks.ISPUBLIC, mIsPublic));
      }
      metaValues.add(buildContentValues(BreadcrumbsTracks.BUNDLE_ID, mBundleId));
      metaValues.add(buildContentValues(BreadcrumbsTracks.ACTIVITY_ID, mActivityId));

      // Store in OGT provider
      ContentResolver resolver = mContext.getContentResolver();
      resolver.bulkInsert(metadataUri, metaValues.toArray(new ContentValues[1]));

      // Store in Breadcrumbs adapter
      tracks.addSyncedTrack(Long.valueOf(mTrackUri.getLastPathSegment()), bcTrackId);
      if (mIsBundleCreated)
      {
         mService.getBreadcrumbsTracks().addBundle(Integer.parseInt(mBundleId), mBundleName, mBundleDescription);
      }
      //"http://api.gobreadcrumbs.com/v1/tracks/" + trackId + "/placemarks.gpx"
      mService.getBreadcrumbsTracks().addTrack(bcTrackId, mName, Integer.valueOf(mBundleId), mDescription, null, null, null, mIsPublic, null, null, null, null, null);

      super.onPostExecute(result);
   }

   private ContentValues buildContentValues(String key, String value)
   {
      ContentValues contentValues = new ContentValues();
      contentValues.put(MetaData.KEY, key);
      contentValues.put(MetaData.VALUE, value);
      return contentValues;
   }

   private void close(Closeable connection)
   {
      try
      {
         if (connection != null)
         {
            connection.close();
         }
      }
      catch (IOException e)
      {
         Log.w(TAG, "Failed to close ", e);
      }
   }
}