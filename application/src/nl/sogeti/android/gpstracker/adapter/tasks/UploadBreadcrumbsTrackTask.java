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
package nl.sogeti.android.gpstracker.adapter.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.tasks.GpxCreator;
import nl.sogeti.android.gpstracker.actions.tasks.XmlCreator;
import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.adapter.BreadcrumbsAdapter;
import nl.sogeti.android.gpstracker.adapter.BreadcrumbsTracks;
import nl.sogeti.android.gpstracker.db.GPStracking.MetaData;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.ogt.http.HttpEntity;
import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.client.HttpClient;
import org.apache.ogt.http.client.methods.HttpPost;
import org.apache.ogt.http.entity.mime.HttpMultipartMode;
import org.apache.ogt.http.entity.mime.MultipartEntity;
import org.apache.ogt.http.entity.mime.content.StringBody;
import org.apache.ogt.http.impl.client.DefaultHttpClient;
import org.apache.ogt.http.util.EntityUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * An asynchronous task that communicates with Twitter to retrieve a request
 * token. (OAuthGetRequestToken) After receiving the request token from Twitter,
 * pop a browser to the user to authorize the Request Token.
 * (OAuthAuthorizeToken)
 */
public class UploadBreadcrumbsTrackTask extends GpxCreator
{

   final String TAG = "OGT.UploadBreadcrumbsTrackTask";
   private BreadcrumbsAdapter mAdapter;
   private OAuthConsumer mConsumer;
   private HttpClient mHttpClient;
   private String mActivityId;
   private String mBundleId;
   private String mDescription;
   private String mIsPublic;
   

   /**
    * 
    * Constructor: create a new UploadBreadcrumbsTrackTask.
    * @param context
    * @param adapter
    * @param listener
    * @param httpclient
    * @param consumer
    * @param trackUri
    */
   public UploadBreadcrumbsTrackTask(Context context, BreadcrumbsAdapter adapter, ProgressListener listener, HttpClient httpclient, OAuthConsumer consumer, Uri trackUri)
   {
      super(context, trackUri, "uploadToGobreadcrumbs", false, listener);
      mAdapter = adapter;
      mHttpClient = httpclient;
      mConsumer = consumer;
   }
   /**
    * Retrieve the OAuth Request Token and present a browser to the user to
    * authorize the token.
    */
   @Override
   protected Uri doInBackground(Void... params)
   {
      // Leave room in the progressbar for uploading
      determineProgressGoal();
      setMaximumProgress(getMaximumProgress()*2);
      
      // Build GPX file
      Uri gpxFile = exportGpx();
      
      if (isCancelled())
      {
         String text = mContext.getString( R.string.ticker_failed ) + " \"http://api.gobreadcrumbs.com/v1/tracks\" " + mContext.getString( R.string.error_buildxml );
         handleError(new IOException("Fail to execute request due to canceling"), text);
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
         cursor  = mContext.getContentResolver().query(
            metadataUri, new String[]{MetaData.KEY, MetaData.VALUE}, 
            null, null, null);
         if( cursor.moveToFirst())
         {
            do
            {
               String key = cursor.getString(0);
               if( BreadcrumbsTracks.ACTIVITY_ID.equals(key) )
               {
                  mActivityId =  cursor.getString(1);
               }
               else if( BreadcrumbsTracks.BUNDLE_ID.equals(key) )
               {
                  mBundleId =  cursor.getString(1);
               }
               else if( BreadcrumbsTracks.DESCRIPTION.equals(key) )
               {
                  mDescription =  cursor.getString(1);
               }
               else if( BreadcrumbsTracks.ISPUBLIC.equals(key) )
               {
                  mIsPublic =  cursor.getString(1);
               }
            }
            while(cursor.moveToNext());
         }
      }
      finally
      {
         if( cursor != null )
         {
            cursor.close();
         }
      }
      if( "-1".equals( mActivityId ) )
      {
         String text = "Unable to upload without a activity id stored in meta-data table";
         IllegalStateException e = new IllegalStateException(text);
         handleError(e, text);
      }
      if( "-1".equals(mBundleId) )
      {
         String text = "Unable to upload (yet) without a bunld id stored in meta-data table";
         IllegalStateException e =  new IllegalStateException(text);
         handleError(e, text);
      }
      
      int statusCode = 0 ;
      String responseText = null;
      Uri trackUri = null;
      HttpEntity responseEntity = null;
      try
      {
         String gpxString = XmlCreator.convertStreamToString( mContext.getContentResolver().openInputStream(gpxFile));
         
         HttpPost method = new HttpPost("http://api.gobreadcrumbs.com/v1/tracks");         
         mConsumer.sign(method);
         if( isCancelled() )
         {
            throw new IOException("Fail to execute request due to canceling");
         }
         // Build the multipart body with the upload data
         MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
         entity.addPart("import_type", new StringBody("GPX"));
         //entity.addPart("gpx",         new FileBody(gpxFile));
         entity.addPart("gpx",         new StringBody(gpxString));
         entity.addPart("bundle_id",   new StringBody(mBundleId));
         entity.addPart("description", new StringBody(mDescription));
//         entity.addPart("difficulty",  new StringBody("3"));
//         entity.addPart("rating",      new StringBody("4"));
         entity.addPart("public",      new StringBody(mIsPublic));
         method.setEntity(entity);
         
         // Execute the POST to OpenStreetMap
         HttpResponse response = mHttpClient.execute(method);
         this.publishProgress(getMaximumProgress()/2);

         statusCode = response.getStatusLine().getStatusCode();
         responseEntity = response.getEntity();
         InputStream stream = responseEntity.getContent();
         responseText = XmlCreator.convertStreamToString(stream);
         Log.d( TAG, "Uploaded track "+entity.toString()+" and received response: "+responseText);
         
         //TODO: Check for error in the response
         Pattern p = Pattern.compile(">([0-9]+)</id>");
         Matcher m = p.matcher(responseText);
         if( m.find() )
         {
            trackUri = Uri.parse("http://api.gobreadcrumbs.com/v1/tracks/"+m.group(1)+"/placemarks.gpx");
         }
      }
      catch (OAuthMessageSignerException e)
      {
         handleError(e, "Failed to sign the request with authentication signature");
      }
      catch (OAuthExpectationFailedException e)
      {
         handleError(e, "The request did not authenticate");
      }
      catch (OAuthCommunicationException e)
      {
         handleError(e, "The authentication communication failed");
      }
      catch (IOException e)
      {
         handleError(e, "A problem during communication");
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
               Log.e( TAG, "Failed to close the content stream", e);
            }
         }
      }

      if (statusCode == 200 || statusCode == 201 )
      {
         Log.d( TAG, "Excellent response status code "+statusCode );
         if( trackUri == null )
         {
            handleError( new IOException("Unable to retrieve URI from response"), responseText );
         }
      }
      else
      {
         String text = mContext.getString( R.string.ticker_failed ) + " \"http://api.gobreadcrumbs.com/v1/tracks\" " + mContext.getString( R.string.error_buildxml );
         handleError( new IOException("Status code: "+statusCode), text );
      }
      return trackUri;
   }
   
   @Override
   protected void onPostExecute(Uri result)
   {      
      BreadcrumbsTracks tracks = mAdapter.getBreadcrumbsTracks();
      Uri metadataUri = Uri.withAppendedPath(mTrackUri, "metadata");
      List<String> segments = result.getPathSegments();
      Integer bcTrackId = new Integer( segments.get(segments.size()-2) );

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
      tracks.addSyncedTrack(new Long( mTrackUri.getLastPathSegment()), bcTrackId);
      
      mAdapter.finishedTask();
      
      super.onPostExecute(result);
   }
   
   private ContentValues buildContentValues(String key, String value)
   {
      ContentValues contentValues = new ContentValues();
      contentValues.put(MetaData.KEY, key);
      contentValues.put(MetaData.VALUE, value);
      return contentValues;
   }

}