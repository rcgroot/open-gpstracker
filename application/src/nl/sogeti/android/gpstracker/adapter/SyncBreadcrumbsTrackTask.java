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

import java.io.IOException;
import java.io.InputStream;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.util.Pair;
import nl.sogeti.android.gpstracker.viewer.GpxParser;
import nl.sogeti.android.gpstracker.viewer.TrackList;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Adapter;

/**
 * An asynchronous task that communicates with Twitter to retrieve a request
 * token. (OAuthGetRequestToken) After receiving the request token from Twitter,
 * pop a browser to the user to authorize the Request Token.
 * (OAuthAuthorizeToken)
 */
public class SyncBreadcrumbsTrackTask extends GpxParser
{

   final String TAG = "OGT.GetBreadcrumbsTracksTask";
   private BreadcrumbsAdapter mAdapter;
   private OAuthConsumer mConsumer;
   private DefaultHttpClient mHttpclient;
   private Pair<Integer, Integer> mTrack;

   /**
    * We pass the OAuth consumer and provider.
    * 
    * @param mContext Required to be able to start the intent to launch the
    *           browser.
    * @param httpclient
    * @param provider The OAuthProvider object
    * @param mConsumer The OAuthConsumer object
    */
   public SyncBreadcrumbsTrackTask(TrackList trackList, BreadcrumbsAdapter adapter, DefaultHttpClient httpclient, OAuthConsumer consumer,
         Pair<Integer, Integer> track)
   {
      super(trackList);
      mAdapter = adapter;
      mHttpclient = httpclient;
      mConsumer = consumer;
      mTrack = track;
   }

   /**
    * Retrieve the OAuth Request Token and present a browser to the user to
    * authorize the token.
    */
   @Override
   protected Boolean doInBackground(Uri... params)
   {
      Boolean result = new Boolean(false);
      InputStream fis = null;
      String trackName = mAdapter.getBreadcrumbsTracks().getKeyForItem(mTrack, BreadcrumbsTracks.NAME);
      try
      {
         HttpUriRequest request = new HttpGet("http://api.gobreadcrumbs.com/v1/tracks/" + mTrack.second + "/placemarks.gpx");
         mConsumer.sign(request);
         if (isCancelled())
         {
            throw new IOException("Fail to execute request due to canceling");
         }
         HttpResponse response = mHttpclient.execute(request);
         HttpEntity entity = response.getEntity();
         fis = entity.getContent();
         result = new Boolean(true);
      }
      catch (OAuthMessageSignerException e)
      {
         mErrorDialogMessage = mTrackList.getString(R.string.error_importgpx_xml);
         mErrorDialogException = e;
         result = new Boolean(false);
      }
      catch (OAuthExpectationFailedException e)
      {
         mErrorDialogMessage = mTrackList.getString(R.string.error_importgpx_xml);
         mErrorDialogException = e;
         result = new Boolean(false);
      }
      catch (OAuthCommunicationException e)
      {
         mErrorDialogMessage = mTrackList.getString(R.string.error_importgpx_xml);
         mErrorDialogException = e;
         result = new Boolean(false);
      }
      catch (IOException e)
      {
         mErrorDialogMessage = mTrackList.getString(R.string.error_importgpx_xml);
         mErrorDialogException = e;
         result = new Boolean(false);
      }
      
      if (result.booleanValue())
      {
         result = importTrack(fis, trackName);
      }

      return result;
   }

   @Override
   protected void onPostExecute(Boolean result)
   {
      super.onPostExecute(result);
      mAdapter.finishedTrackSyncTask(this);
   }

}