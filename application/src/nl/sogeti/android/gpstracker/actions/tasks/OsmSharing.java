/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Jul 9, 2011 Sogeti Nederland B.V. All Rights Reserved.
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
package nl.sogeti.android.gpstracker.actions.tasks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.ShareTrack;
import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.db.GPStracking;
import nl.sogeti.android.gpstracker.db.GPStracking.Media;
import nl.sogeti.android.gpstracker.db.GPStracking.MetaData;
import nl.sogeti.android.gpstracker.oauth.PrepareRequestTokenActivity;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.viewer.LoggerMap;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.ogt.http.HttpEntity;
import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.client.methods.HttpPost;
import org.apache.ogt.http.entity.mime.HttpMultipartMode;
import org.apache.ogt.http.entity.mime.MultipartEntity;
import org.apache.ogt.http.entity.mime.content.FileBody;
import org.apache.ogt.http.entity.mime.content.StringBody;
import org.apache.ogt.http.impl.client.DefaultHttpClient;
import org.apache.ogt.http.util.EntityUtils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * ????
 *
 * @version $Id:$
 * @author rene (c) Jul 9, 2011, Sogeti B.V.
 */
public class OsmSharing extends GpxCreator
{

   public static final String OAUTH_TOKEN = "openstreetmap_oauth_token";
   public static final String OAUTH_TOKEN_SECRET = "openstreetmap_oauth_secret";
   private static final String TAG = "OGT.OsmSharing";

   public OsmSharing(Context context, Uri trackUri, String chosenBaseFileName, boolean attachments, ProgressListener listener)
   {
      super(context, trackUri, chosenBaseFileName, attachments, listener);
   }

   @Override
   protected Uri doInBackground(Void... params)
   {
      Uri fileUri = super.doInBackground(params);
      sendToOsm(fileUri, mTrackUri);
      return fileUri;
   }
   
   @Override
   protected void onPostExecute(Uri resultFilename)
   {
      super.onPostExecute(resultFilename);
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
         requestOpenstreetmapOauthToken(mContext);
         return;
      }
      
      String visibility = PreferenceManager.getDefaultSharedPreferences(mContext).getString(Constants.OSM_VISIBILITY, "trackable");
      File gpxFile = new File(fileUri.getEncodedPath());

      String url = mContext.getString(R.string.osm_post_url);
      DefaultHttpClient httpclient = new DefaultHttpClient();
      HttpResponse response = null;
      String responseText = "";
      int statusCode = 0;
      Cursor metaData = null;
      String sources = null;
      HttpEntity responseEntity = null;
      try
      {
         metaData = mContext.getContentResolver().query(Uri.withAppendedPath(trackUri, "metadata"), new String[] { MetaData.VALUE }, MetaData.KEY + " = ? ",
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
         entity.addPart("description", new StringBody( ShareTrack.queryForTrackName(mContext.getContentResolver(), mTrackUri)));
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
         responseText = mContext.getString(R.string.osm_failed) + e.getLocalizedMessage();
         Toast toast = Toast.makeText(mContext, responseText, Toast.LENGTH_LONG);
         toast.show();
         Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
         editor.remove(OAUTH_TOKEN);
         editor.remove(OAUTH_TOKEN_SECRET);
         editor.commit();
      }
      catch (OAuthExpectationFailedException e)
      {
         Log.e(TAG, "Failed to upload to " + url + "Response: " + responseText, e);
         responseText = mContext.getString(R.string.osm_failed) + e.getLocalizedMessage();
         Toast toast = Toast.makeText(mContext, responseText, Toast.LENGTH_LONG);
         toast.show();
         Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
         editor.remove(OAUTH_TOKEN);
         editor.remove(OAUTH_TOKEN_SECRET);
         editor.commit();
      }
      catch (OAuthCommunicationException e)
      {
         Log.e(TAG, "Failed to upload to " + url + "Response: " + responseText, e);
         responseText = mContext.getString(R.string.osm_failed) + e.getLocalizedMessage();
         Toast toast = Toast.makeText(mContext, responseText, Toast.LENGTH_LONG);
         toast.show();
      }
      catch (IOException e)
      {
         Log.e(TAG, "Failed to upload to " + url + "Response: " + responseText, e);
         responseText = mContext.getString(R.string.osm_failed) + e.getLocalizedMessage();
         Toast toast = Toast.makeText(mContext, responseText, Toast.LENGTH_LONG);
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
         CharSequence text = mContext.getString(R.string.osm_success) + responseText;
         Toast toast = Toast.makeText(mContext, text, Toast.LENGTH_LONG);
         toast.show();
      }
      else
      {
         Log.e(TAG, "Failed to upload to error code " + statusCode + " " + responseText);
         CharSequence text = mContext.getString(R.string.osm_failed) + responseText;
         Toast toast = Toast.makeText(mContext, text, Toast.LENGTH_LONG);
         toast.show();
         if( statusCode == 401 )
         {
            Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
            editor.remove(OAUTH_TOKEN);
            editor.remove(OAUTH_TOKEN_SECRET);
            editor.commit();
         }
      }
   }
   
   private CommonsHttpOAuthConsumer osmConnectionSetup()
   {
      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
      String token = prefs.getString(OAUTH_TOKEN, "");
      String secret = prefs.getString(OAUTH_TOKEN_SECRET, "");
      boolean mAuthorized = !"".equals(token) && !"".equals(secret);
      CommonsHttpOAuthConsumer consumer = null;
      if (mAuthorized)
      {
         consumer = new CommonsHttpOAuthConsumer(mContext.getString(R.string.OSM_CONSUMER_KEY), mContext.getString(R.string.OSM_CONSUMER_SECRET));
         consumer.setTokenWithSecret(token, secret);
      }
      return consumer;
   }

   private String queryForNotes()
   {
      StringBuilder tags = new StringBuilder();
      ContentResolver resolver = mContext.getContentResolver();
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

   public static void requestOpenstreetmapOauthToken(Context context)
   {
      Intent i = new Intent(context.getApplicationContext(), PrepareRequestTokenActivity.class);
      i.putExtra(PrepareRequestTokenActivity.OAUTH_TOKEN_PREF, OAUTH_TOKEN);
      i.putExtra(PrepareRequestTokenActivity.OAUTH_TOKEN_SECRET_PREF, OAUTH_TOKEN_SECRET);

      i.putExtra(PrepareRequestTokenActivity.CONSUMER_KEY, context.getString(R.string.OSM_CONSUMER_KEY));
      i.putExtra(PrepareRequestTokenActivity.CONSUMER_SECRET, context.getString(R.string.OSM_CONSUMER_SECRET));
      i.putExtra(PrepareRequestTokenActivity.REQUEST_URL, Constants.OSM_REQUEST_URL);
      i.putExtra(PrepareRequestTokenActivity.ACCESS_URL, Constants.OSM_ACCESS_URL);
      i.putExtra(PrepareRequestTokenActivity.AUTHORIZE_URL, Constants.OSM_AUTHORIZE_URL);

      context.startActivity(i);
   }

   
}
