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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

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
public class GetBreadcrumbsBundlesTask extends BreadcrumbsTask
{

   final String TAG = "OGT.GetBreadcrumbsBundlesTask";
   private OAuthConsumer mConsumer;
   private DefaultHttpClient mHttpclient;
   
   private Set<Integer> mBundleIds;
   private LinkedList<Object[]> mBundles;

   /**
    * We pass the OAuth consumer and provider.
    * 
    * @param mContext Required to be able to start the intent to launch the
    *           browser.
    * @param httpclient
    * @param listener
    * @param provider The OAuthProvider object
    * @param mConsumer The OAuthConsumer object
    */
   public GetBreadcrumbsBundlesTask(BreadcrumbsAdapter adapter, ProgressListener listener, DefaultHttpClient httpclient, OAuthConsumer consumer)
   {
      super(adapter, listener);
      mHttpclient = httpclient;
      mConsumer = consumer;

   }

   /**
    * Retrieve the OAuth Request Token and present a browser to the user to
    * authorize the token.
    */
   @Override
   protected Void doInBackground(Void... params)
   {
      HttpEntity responseEntity = null;
      mBundleIds = new HashSet<Integer>();
      mBundles = new LinkedList<Object[]>();
      try
      {
         HttpUriRequest request = new HttpGet("http://api.gobreadcrumbs.com/v1/bundles.xml");

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

         String bundleName = null, bundleDescription = null;
         Integer activityId = null, bundleId = null;
         while (eventType != XmlPullParser.END_DOCUMENT)
         {
            if (eventType == XmlPullParser.START_TAG)
            {
               tagName = xpp.getName();
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
               if ("bundle".equals(xpp.getName()) && activityId != null && bundleId != null)
               {
                  mBundles.add( new Object[]{activityId, bundleId, bundleName, bundleDescription} );
               }
               tagName = null;
            }
            else if (eventType == XmlPullParser.TEXT)
            {
               if ("activity-id".equals(tagName))
               {
                  activityId = Integer.parseInt(xpp.getText());
               }
               else if ("description".equals(tagName))
               {
                  bundleDescription = xpp.getText();
               }
               else if ("id".equals(tagName))
               {
                  bundleId = Integer.parseInt(xpp.getText());
                  mBundleIds.add(bundleId);
               }
               else if ("name".equals(tagName))
               {
                  bundleName = xpp.getText();
               }
            }
            eventType = xpp.next();
         }
      }
      catch (OAuthMessageSignerException e)
      {
         mAdapter.removeAuthentication();
         handleError(e, "Failed to sign the request with authentication signature");
      }
      catch (OAuthExpectationFailedException e)
      {
         mAdapter.removeAuthentication();
         handleError(e, "The request did not authenticate");
      }
      catch (OAuthCommunicationException e)
      {
         mAdapter.removeAuthentication();
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
               Log.w(TAG, "Failed closing inputstream");
            }
         }
      }
      return null;
   }

   @Override
   protected void updateTracksData(BreadcrumbsTracks tracks)
   {
      tracks.setAllBundleIds( mBundleIds );
      
      for( Object[] bundle : mBundles )
      {
         Integer activityId = (Integer) bundle[0];
         Integer bundleId = (Integer) bundle[1];
         String bundleName = (String) bundle[2];
         String bundleDescription = (String) bundle[3];
         
         tracks.addBundle(activityId, bundleId, bundleName, bundleDescription);
      }
   }
}