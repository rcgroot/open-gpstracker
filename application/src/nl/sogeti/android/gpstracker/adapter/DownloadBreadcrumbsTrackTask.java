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

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.utils.xml.GpxParser;
import nl.sogeti.android.gpstracker.db.GPStracking.MetaData;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.db.GPStracking.TracksColumns;
import nl.sogeti.android.gpstracker.util.Pair;
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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
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
public class DownloadBreadcrumbsTrackTask extends GpxParser
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
    * @param adapter          
    * @param httpclient
    * @param provider The OAuthProvider object
    * @param mConsumer The OAuthConsumer object
    */
   public DownloadBreadcrumbsTrackTask(TrackList trackList, BreadcrumbsAdapter adapter, DefaultHttpClient httpclient, OAuthConsumer consumer,
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
   protected Uri doInBackground(Uri... params)
   {
      Uri trackUri = null;
      InputStream fis = null;
      String trackName = mAdapter.getBreadcrumbsTracks().getValueForItem(mTrack, BreadcrumbsTracks.NAME);
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

         trackUri = importTrack(fis, trackName);
      }
      catch (OAuthMessageSignerException e)
      {
         mErrorDialogMessage = mTrackList.getString(R.string.error_importgpx_xml);
         mErrorDialogException = e;
         trackUri = null;
      }
      catch (OAuthExpectationFailedException e)
      {
         mErrorDialogMessage = mTrackList.getString(R.string.error_importgpx_xml);
         mErrorDialogException = e;
         trackUri = null;
      }
      catch (OAuthCommunicationException e)
      {
         mErrorDialogMessage = mTrackList.getString(R.string.error_importgpx_xml);
         mErrorDialogException = e;
         trackUri = null;
      }
      catch (IOException e)
      {
         mErrorDialogMessage = mTrackList.getString(R.string.error_importgpx_xml);
         mErrorDialogException = e;
         trackUri = null;
      }
      return trackUri;
   }

   @Override
   protected void onPostExecute(Uri result)
   {
      super.onPostExecute(result);

      long ogtTrackId = Long.parseLong(result.getLastPathSegment());
      Uri metadataUri = Uri.withAppendedPath(ContentUris.withAppendedId(Tracks.CONTENT_URI, ogtTrackId), "metadata");
      
      BreadcrumbsTracks tracks = mAdapter.getBreadcrumbsTracks();
      Integer bcTrackId     = mTrack.second;
      Integer bcBundleId    = tracks.getBundleIdForTrackId(bcTrackId);
      Integer bcActivityId  = tracks.getActivityIdForBundleId(bcBundleId);
      String  bcDifficulty  = tracks.getValueForItem(mTrack, BreadcrumbsTracks.DIFFICULTY);
      String  bcRating      = tracks.getValueForItem(mTrack, BreadcrumbsTracks.RATING);
      String  bcPublic      = tracks.getValueForItem(mTrack, BreadcrumbsTracks.ISPUBLIC);
      String  bcDescription = tracks.getValueForItem(mTrack, BreadcrumbsTracks.DESCRIPTION);
      ContentValues[] metaValues = { 
            buildContentValues( BreadcrumbsTracks.TRACK_ID,    Long.toString(bcTrackId)),
            buildContentValues( BreadcrumbsTracks.TRACK_ID,    bcDifficulty),
            buildContentValues( BreadcrumbsTracks.TRACK_ID,    bcDescription),
            buildContentValues( BreadcrumbsTracks.TRACK_ID,    bcRating),
            buildContentValues( BreadcrumbsTracks.TRACK_ID,    bcPublic),
            buildContentValues( BreadcrumbsTracks.BUNDLE_ID,   Integer.toString(bcBundleId)),
            buildContentValues( BreadcrumbsTracks.ACTIVITY_ID, Integer.toString(bcActivityId))
            };
      
      ContentResolver resolver = mTrackList.getContentResolver();
      resolver.bulkInsert(metadataUri, metaValues);
      
      mAdapter.finishedTask();
   }
   
   private ContentValues buildContentValues( String key, String value)
   {
      ContentValues contentValues = new ContentValues();
      contentValues.put(MetaData.KEY, key);
      contentValues.put(MetaData.VALUE, value);
      return contentValues;
   }

}