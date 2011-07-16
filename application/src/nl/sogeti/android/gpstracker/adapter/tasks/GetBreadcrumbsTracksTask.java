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
import java.util.LinkedList;

import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.adapter.BreadcrumbsAdapter;
import nl.sogeti.android.gpstracker.adapter.BreadcrumbsTracks;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.ogt.http.HttpEntity;
import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.client.methods.HttpGet;
import org.apache.ogt.http.client.methods.HttpUriRequest;
import org.apache.ogt.http.impl.client.DefaultHttpClient;
import org.apache.ogt.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

/**
 * An asynchronous task that communicates with Twitter to retrieve a request
 * token. (OAuthGetRequestToken) After receiving the request token from Twitter,
 * pop a browser to the user to authorize the Request Token.
 * (OAuthAuthorizeToken)
 */
public class GetBreadcrumbsTracksTask extends BreadcrumbsTask
{

   final String TAG = "OGT.GetBreadcrumbsTracksTask";
   private OAuthConsumer mConsumer;
   private DefaultHttpClient mHttpclient;
   private Integer mBundleId;
   private LinkedList<Object[]> mTracks;

   /**
    * We pass the OAuth consumer and provider.
    * 
    * @param mContext Required to be able to start the intent to launch the
    *           browser.
    * @param httpclient
    * @param provider The OAuthProvider object
    * @param mConsumer The OAuthConsumer object
    */
   public GetBreadcrumbsTracksTask(BreadcrumbsAdapter adapter, ProgressListener listener, DefaultHttpClient httpclient, OAuthConsumer consumer, Integer bundleId)
   {
      super(adapter, listener);
      mHttpclient = httpclient;
      mConsumer = consumer;
      mBundleId = bundleId;
   }

   /**
    * Retrieve the OAuth Request Token and present a browser to the user to
    * authorize the token.
    */
   @Override
   protected Void doInBackground(Void... params)
   {
      mTracks = new LinkedList<Object[]>();
      HttpEntity responseEntity = null;
      try
      {

         HttpUriRequest request = new HttpGet("http://api.gobreadcrumbs.com/v1/bundles/" + mBundleId + "/tracks.xml");
         mConsumer.sign(request);
         if (isCancelled())
         {
            throw new IOException("Fail to execute request due to canceling");
         }
         HttpResponse response = mHttpclient.execute(request);
         responseEntity = response.getEntity();
         InputStream instream = responseEntity.getContent();

         XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
         factory.setNamespaceAware(true);
         XmlPullParser xpp = factory.newPullParser();
         xpp.setInput(instream, "UTF-8");

         String tagName = null;
         int eventType = xpp.getEventType();

         String trackName = null, description = null, difficulty = null, startTime = null, endTime = null, trackRating = null, isPublic = null;
         Integer trackId = null, bundleId = null, totalTime = null;
         Float lat = null, lng = null, totalDistance = null;
         while (eventType != XmlPullParser.END_DOCUMENT)
         {
            if (eventType == XmlPullParser.START_TAG)
            {
               tagName = xpp.getName();
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
               if ("track".equals(xpp.getName()) && trackId != null && bundleId != null)
               {
                  mTracks.add(new Object[] { trackId, trackName, bundleId, description, difficulty, startTime, endTime, isPublic, lat, lng, totalDistance,
                        totalTime, trackRating });
               }
               tagName = null;
            }
            else if (eventType == XmlPullParser.TEXT)
            {
               if ("bundle-id".equals(tagName))
               {
                  bundleId = Integer.parseInt(xpp.getText());
               }
               else if ("description".equals(tagName))
               {
                  description = xpp.getText();
               }
               else if ("difficulty".equals(tagName))
               {
                  difficulty = xpp.getText();
               }
               else if ("start-time".equals(tagName))
               {
                  startTime = xpp.getText();
               }
               else if ("end-time".equals(tagName))
               {
                  endTime = xpp.getText();
               }
               else if ("id".equals(tagName))
               {
                  trackId = Integer.parseInt(xpp.getText());
               }
               else if ("is-public".equals(tagName))
               {
                  isPublic = xpp.getText();
               }
               else if ("lat".equals(tagName))
               {
                  lat = Float.parseFloat(xpp.getText());
               }
               else if ("lng".equals(tagName))
               {
                  lng = Float.parseFloat(xpp.getText());
               }
               else if ("name".equals(tagName))
               {
                  trackName = xpp.getText();
               }
               else if ("track-rating".equals(tagName))
               {
                  trackRating = xpp.getText();
               }
            }
            eventType = xpp.next();
         }
         Log.d(TAG, "Read inputstream from http response anything available: " + instream.read());
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
      catch (XmlPullParserException e)
      {
         handleError(e, "A problem while reading the XML data");
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
      return null;
   }

   @Override
   protected void updateTracksData(BreadcrumbsTracks tracks)
   {
      for (Object[] track : mTracks)
      {
         Integer trackId = (Integer) track[0];
         String trackName = (String) track[1];
         Integer bundleId = (Integer) track[2];
         String description = (String) track[3];
         String difficulty = (String) track[4];
         String startTime = (String) track[5];
         String endTime = (String) track[6];
         String isPublic = (String) track[7];
         Float lat = (Float) track[8];
         Float lng = (Float) track[9];
         Float totalDistance = (Float) track[10];
         Integer totalTime = (Integer) track[11];
         String trackRating = (String) track[12];
         
         tracks.addTrack(trackId, trackName, bundleId, description, difficulty, startTime, endTime, isPublic, lat, lng, totalDistance, totalTime, trackRating);
      }

   }
}