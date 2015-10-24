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
package nl.sogeti.android.gpstracker.oauth;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;

public class RetrieveAccessTokenTask extends AsyncTask<Uri, Void, Void>
{
   private static final String TAG = "OGT.RetrieveAccessTokenTask";
   private OAuthProvider provider;
   private OAuthConsumer consumer;
   private SharedPreferences prefs;
   private String mTokenKey;
   private String mSecretKey;

   public RetrieveAccessTokenTask(Context context, OAuthConsumer consumer, OAuthProvider provider, SharedPreferences
         prefs, String tokenKey, String secretKey)
   {
      this.consumer = consumer;
      this.provider = provider;
      this.prefs = prefs;
      mTokenKey = tokenKey;
      mSecretKey = secretKey;
   }

   /**
    * Retrieve the oauth_verifier, and store the oauth and oauth_token_secret for future API calls.
    */
   @Override
   protected Void doInBackground(Uri... params)
   {
      final Uri uri = params[0];
      final String oauth_verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);

      try
      {
         provider.retrieveAccessToken(consumer, oauth_verifier);

         final Editor edit = prefs.edit();
         edit.putString(mTokenKey, consumer.getToken());
         edit.putString(mSecretKey, consumer.getTokenSecret());
         edit.commit();

         Log.i(TAG, "OAuth - Access Token Retrieved and stored to " + mTokenKey + " and " + mSecretKey);
         Log.i(TAG, "OAuth - Consumer token '" + consumer.getToken() + "'");
         Log.i(TAG, "OAuth - Consumer secret '" + consumer.getTokenSecret() + "'");
      }
      catch (Exception e)
      {
         Log.e(TAG, "OAuth - Access Token Retrieval Error", e);
      }

      return null;
   }
}