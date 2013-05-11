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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.tasks.XmlCreator;
import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.adapter.BreadcrumbsAdapter;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.util.Log;

/**
 * An asynchronous task that communicates with Twitter to retrieve a request token. (OAuthGetRequestToken) After
 * receiving the request token from Twitter, pop a browser to the user to authorize the Request Token.
 * (OAuthAuthorizeToken)
 */
public class GetBreadcrumbsBundlesTask extends BreadcrumbsTask
{

   final String TAG = "OGT.GetBreadcrumbsBundlesTask";
   private OAuthConsumer mConsumer;

   private Set<Integer> mBundleIds;
   private LinkedList<Object[]> mBundles;

   /**
    * We pass the OAuth consumer and provider.
    * 
    * @param mContext Required to be able to start the intent to launch the browser.
    * @param httpclient
    * @param listener
    * @param provider The OAuthProvider object
    * @param mConsumer The OAuthConsumer object
    */
   public GetBreadcrumbsBundlesTask(Context context, BreadcrumbsService adapter, ProgressListener listener,
         OAuthConsumer consumer)
   {
      super(context, adapter, listener);
      mConsumer = consumer;

   }

   /**
    * Retrieve the OAuth Request Token and present a browser to the user to authorize the token.
    */
   @Override
   protected Void doInBackground(Void... params)
   {
      mBundleIds = new HashSet<Integer>();
      mBundles = new LinkedList<Object[]>();
      try
      {
         URL request = new URL("http://api.gobreadcrumbs.com/v1/bundles.xml");
         if (isCancelled())
         {
            throw new IOException("Fail to execute request due to canceling");
         }
         mConsumer.sign(request);
         if (BreadcrumbsAdapter.DEBUG)
         {
            Log.d(TAG, "Execute request: " + request);
         }
         InputStream stream = request.openStream();
         if (BreadcrumbsAdapter.DEBUG)
         {
            stream = XmlCreator.convertStreamToLoggedStream(TAG, stream);
         }

         XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
         factory.setNamespaceAware(true);
         XmlPullParser xpp = factory.newPullParser();
         xpp.setInput(stream, "UTF-8");

         String tagName = null;
         int eventType = xpp.getEventType();

         String bundleName = null, bundleDescription = null;
         Integer bundleId = null;
         while (eventType != XmlPullParser.END_DOCUMENT)
         {
            if (eventType == XmlPullParser.START_TAG)
            {
               tagName = xpp.getName();
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
               if ("bundle".equals(xpp.getName()) && bundleId != null)
               {
                  mBundles.add(new Object[] { bundleId, bundleName, bundleDescription });
               }
               tagName = null;
            }
            else if (eventType == XmlPullParser.TEXT)
            {
               if ("description".equals(tagName))
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
         mService.removeAuthentication();
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_bundle), e,
               "Failed to sign the request with authentication signature");
      }
      catch (OAuthExpectationFailedException e)
      {
         mService.removeAuthentication();
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_bundle), e, "The request did not authenticate");
      }
      catch (OAuthCommunicationException e)
      {
         mService.removeAuthentication();
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_bundle), e,
               "The authentication communication failed");
      }
      catch (IOException e)
      {
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_bundle), e, "A problem during communication");
      }
      catch (XmlPullParserException e)
      {
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_bundle), e,
               "A problem while reading the XML data");
      }
      catch (IllegalStateException e)
      {
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_bundle), e, "A problem during communication");
      }
      return null;
   }

   @Override
   protected void updateTracksData(BreadcrumbsTracks tracks)
   {
      tracks.setAllBundleIds(mBundleIds);

      for (Object[] bundle : mBundles)
      {
         Integer bundleId = (Integer) bundle[0];
         String bundleName = (String) bundle[1];
         String bundleDescription = (String) bundle[2];

         tracks.addBundle(bundleId, bundleName, bundleDescription);
      }
   }
}