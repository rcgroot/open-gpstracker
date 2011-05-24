/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.ShareTrack;
import nl.sogeti.android.gpstracker.actions.ShareTrack.EndJob;
import nl.sogeti.android.gpstracker.actions.ShareTrack.ProgressMonitor;
import nl.sogeti.android.gpstracker.actions.utils.xml.GpxCreator;
import nl.sogeti.android.gpstracker.actions.utils.xml.XmlCreator;
import nl.sogeti.android.gpstracker.db.GPStracking.MetaData;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.viewer.TrackList;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentUris;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

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
      super(trackList, trackUri, "uploadToGobreadcrumbs", false, null);
      mAdapter = adapter;
      mHttpClient = httpclient;
      mConsumer = consumer;
   }
   /**
    * Retrieve the OAuth Request Token and present a browser to the user to
    * authorize the token.
    */
   @Override
   protected String doInBackground(Void... params)
   {      
      String resultFilename = exportGpx();
      
      File gpxFile = new File(resultFilename);
      
      int statusCode = 0 ;
      String responseText = null;
      try
      {
         String gpxString = XmlCreator.convertStreamToString(new FileInputStream(gpxFile));
         
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
         entity.addPart("bundle_id",   new StringBody("1"));
         entity.addPart("description", new StringBody("2"));
         entity.addPart("difficulty",  new StringBody("3"));
         entity.addPart("rating",      new StringBody("4"));
         entity.addPart("public",      new StringBody("false"));
         method.setEntity(entity);
         
         // Execute the POST to OpenStreetMap
         HttpResponse response = mHttpClient.execute(method);

         statusCode = response.getStatusLine().getStatusCode();
         InputStream stream = response.getEntity().getContent();
         responseText = XmlCreator.convertStreamToString(stream);
         Log.d( TAG, "Uploaded track "+entity.toString()+" and received response: "+responseText);
         
      }
      catch (IOException e)
      {
         CharSequence text = mContext.getString( R.string.ticker_failed ) + " \"http://api.gobreadcrumbs.com/v1/tracks\" " + mContext.getString( R.string.error_buildxml );
         setError( e, text );
         cancel(false);
      }
      catch (OAuthMessageSignerException e)
      {
         CharSequence text = mContext.getString( R.string.ticker_failed ) + " \"http://api.gobreadcrumbs.com/v1/tracks\" " + mContext.getString( R.string.error_buildxml );
         setError( e, text );
         cancel(false);
      }
      catch (OAuthExpectationFailedException e)
      {
         CharSequence text = mContext.getString( R.string.ticker_failed ) + " \"http://api.gobreadcrumbs.com/v1/tracks\" " + mContext.getString( R.string.error_buildxml );
         setError( e, text );
         cancel(false);
      }
      catch (OAuthCommunicationException e)
      {
         CharSequence text = mContext.getString( R.string.ticker_failed ) + " \"http://api.gobreadcrumbs.com/v1/tracks\" " + mContext.getString( R.string.error_buildxml );
         setError( e, text );
         cancel(false);
      }
      if (statusCode == 200)
      {
         Log.d( TAG, "Excellent code 200" );
      }
      else
      {
         Log.e( TAG, ""+statusCode );
      }
      return responseText;
   }
   
   @Override
   protected void onPostExecute(String result)
   {      
      mAdapter.finishedTask();
   }

}