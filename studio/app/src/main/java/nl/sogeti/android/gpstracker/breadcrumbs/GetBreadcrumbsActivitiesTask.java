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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.tasks.XmlCreator;
import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.adapter.BreadcrumbsAdapter;
import nl.sogeti.android.gpstracker.util.Pair;
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
 * An asynchronous task that communicates with Twitter to retrieve a request token. (OAuthGetRequestToken) After receiving the request token from Twitter, pop a browser to the user to authorize the
 * Request Token. (OAuthAuthorizeToken)
 */
public class GetBreadcrumbsActivitiesTask extends BreadcrumbsTask
{

   private LinkedList<Pair<Integer, String>> mActivities;
   final String TAG = "OGT.GetBreadcrumbsActivitiesTask";
   private OAuthConsumer mConsumer;

   /**
    * We pass the OAuth consumer and provider.
    * 
    * @param mContext Required to be able to start the intent to launch the browser.
    * @param httpclient
    * @param provider The OAuthProvider object
    * @param mConsumer The OAuthConsumer object
    */
   public GetBreadcrumbsActivitiesTask(Context context, BreadcrumbsService adapter, ProgressListener listener, OAuthConsumer consumer)
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
      mActivities = new LinkedList<Pair<Integer, String>>();
      HttpURLConnection connection = null;
      try
      {
         URL request = new URL("http://api.gobreadcrumbs.com/v1/activities.xml");
         if (isCancelled())
         {
            throw new IOException("Fail to execute request due to canceling");
         }
         connection = (HttpURLConnection) request.openConnection();
         connection.setDoOutput(false);
         connection.setDoInput(true);
         mConsumer.sign(connection);
         Log.d(TAG, connection.getRequestProperties().toString());
         if (BreadcrumbsAdapter.DEBUG)
         {
            Log.d(TAG, "Execute request: " + request);
         }
         InputStream stream = connection.getInputStream();
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

         String activityName = null;
         Integer activityId = null;
         while (eventType != XmlPullParser.END_DOCUMENT)
         {
            if (eventType == XmlPullParser.START_TAG)
            {
               tagName = xpp.getName();
            }
            else if (eventType == XmlPullParser.END_TAG)
            {
               if ("activity".equals(xpp.getName()) && activityId != null && activityName != null)
               {
                  mActivities.add(new Pair<Integer, String>(activityId, activityName));
               }
               tagName = null;
            }
            else if (eventType == XmlPullParser.TEXT)
            {
               if ("id".equals(tagName))
               {
                  activityId = Integer.parseInt(xpp.getText());
               }
               else if ("name".equals(tagName))
               {
                  activityName = xpp.getText();
               }
            }
            eventType = xpp.next();
         }
      }
      catch (OAuthMessageSignerException e)
      {
         mService.removeAuthentication();
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_activity), e, "Failed to sign the request with authentication signature");
      }
      catch (OAuthExpectationFailedException e)
      {
         mService.removeAuthentication();
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_activity), e, "The request did not authenticate");
      }
      catch (OAuthCommunicationException e)
      {
         mService.removeAuthentication();
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_activity), e, "The authentication communication failed");
      }
      catch (IOException e)
      {
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_activity), e, "A problem during communication");
      }
      catch (XmlPullParserException e)
      {
         handleError(mContext.getString(R.string.taskerror_breadcrumbs_activity), e, "A problem while reading the XML data");
      }
      finally
      {
         if (connection != null)
            connection.disconnect();
      }
      return null;
   }

   @Override
   protected void updateTracksData(BreadcrumbsTracks tracks)
   {
      for (Pair<Integer, String> activity : mActivities)
      {
         tracks.addActivity(activity.first, activity.second);
      }
   }
}