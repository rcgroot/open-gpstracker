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
package nl.sogeti.android.gpstracker.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.utils.xml.GpxCreator;
import nl.sogeti.android.gpstracker.actions.utils.xml.XmlCreator;
import nl.sogeti.android.gpstracker.db.GPStracking.MetaData;
import nl.sogeti.android.gpstracker.viewer.TrackList;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

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

   final String TAG = "OGT.GetBreadcrumbsActivitiesTask";
   private BreadcrumbsAdapter mAdapter;
   private OAuthConsumer mConsumer;
   private DefaultHttpClient mHttpClient;
   
   /**
    * We pass the OAuth consumer and provider.
    * 
    * @param trackList Required to be able to start the intent to launch the
    *           browser.
    * @param httpclient 
    * @param mConsumer The OAuthConsumer object
    * @param trackId
    */
   public UploadBreadcrumbsTrackTask(TrackList trackList, BreadcrumbsAdapter adapter, DefaultHttpClient httpclient, OAuthConsumer consumer, Uri trackUri)
   {
      super(trackList, trackUri, "uploadToGobreadcrumbs", false, trackList);
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
      String activityId = null;
      String bundleId = null;
      String description = null;
      String isPublic = null;

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
                  activityId =  cursor.getString(1);
               }
               else if( BreadcrumbsTracks.BUNDLE_ID.equals(key) )
               {
                  bundleId =  cursor.getString(1);
               }
               else if( BreadcrumbsTracks.DESCRIPTION.equals(key) )
               {
                  description =  cursor.getString(1);
               }
               else if( BreadcrumbsTracks.ISPUBLIC.equals(key) )
               {
                  isPublic =  cursor.getString(1);
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
      
      //TODO create bundle if no existing ID
      
      int statusCode = 0 ;
      String responseText = null;
      Uri trackUri = null;
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
         entity.addPart("bundle_id",   new StringBody(bundleId));
         entity.addPart("description", new StringBody(description));
//         entity.addPart("difficulty",  new StringBody("3"));
//         entity.addPart("rating",      new StringBody("4"));
         entity.addPart("public",      new StringBody(isPublic));
         method.setEntity(entity);
         
         // Execute the POST to OpenStreetMap
         HttpResponse response = mHttpClient.execute(method);
         this.publishProgress(getMaximumProgress()/2);

         statusCode = response.getStatusLine().getStatusCode();
         InputStream stream = response.getEntity().getContent();
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
      catch (IOException e)
      {
         String text = mContext.getString( R.string.ticker_failed ) + " \"http://api.gobreadcrumbs.com/v1/tracks\" " + mContext.getString( R.string.error_buildxml );
         handleError( e, text );
      }
      catch (OAuthMessageSignerException e)
      {
         String text = mContext.getString( R.string.ticker_failed ) + " \"http://api.gobreadcrumbs.com/v1/tracks\" " + mContext.getString( R.string.error_buildxml );
         handleError( e, text );
      }
      catch (OAuthExpectationFailedException e)
      {
         String text = mContext.getString( R.string.ticker_failed ) + " \"http://api.gobreadcrumbs.com/v1/tracks\" " + mContext.getString( R.string.error_buildxml );
         handleError( e, text );
      }
      catch (OAuthCommunicationException e)
      {
         String text = mContext.getString( R.string.ticker_failed ) + " \"http://api.gobreadcrumbs.com/v1/tracks\" " + mContext.getString( R.string.error_buildxml );
         handleError( e, text );
      }

      if (statusCode == 200 || statusCode == 201 )
      {
         Log.d( TAG, "Excellent response status code "+statusCode );
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
      mAdapter.finishedTask();
   }

}