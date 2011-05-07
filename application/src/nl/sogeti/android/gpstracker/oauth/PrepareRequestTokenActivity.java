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
package nl.sogeti.android.gpstracker.oauth;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.util.Constants;
import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Prepares a OAuthConsumer and OAuthProvider OAuthConsumer is configured with
 * the consumer key & consumer secret. OAuthProvider is configured with the 3
 * OAuth endpoints. Execute the OAuthRequestTokenTask to retrieve the request,
 * and authorize the request. After the request is authorized, a callback is
 * made here.
 */
public class PrepareRequestTokenActivity extends Activity
{

   final String TAG = "OGT.PrepareRequestTokenActivity";

   private OAuthConsumer consumer;
   private OAuthProvider provider;

   private String mTokenKey;

   private String mSecretKey;

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      String key = getIntent().getStringExtra("CONSUMER_KEY");
      String secret = getIntent().getStringExtra("CONSUMER_SECRET");
      String requestUrl = getIntent().getStringExtra("REQUEST_URL");
      String accessUrl = getIntent().getStringExtra("ACCESS_URL");
      String authUrl = getIntent().getStringExtra("AUTHORIZE_URL");
      mTokenKey = OAuth.OAUTH_TOKEN;
      mSecretKey = OAuth.OAUTH_TOKEN_SECRET;
      super.onCreate(savedInstanceState);
      
      this.consumer = new CommonsHttpOAuthConsumer(getString(R.string.CONSUMER_KEY), getString(R.string.CONSUMER_SECRET));
      this.provider = new CommonsHttpOAuthProvider(Constants.REQUEST_URL, Constants.ACCESS_URL, Constants.AUTHORIZE_URL);

      Log.i(TAG, "Starting task to retrieve request token.");
      new OAuthRequestTokenTask(this, consumer, provider).execute();
   }

   /**
    * Called when the OAuthRequestTokenTask finishes (user has authorized the
    * request token). The callback URL will be intercepted here.
    */
   @Override
   public void onNewIntent(Intent intent)
   {
      super.onNewIntent(intent);
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      final Uri uri = intent.getData();
      if (uri != null && uri.getScheme().equals(Constants.OAUTH_CALLBACK_SCHEME))
      {
         Log.i(TAG, "Callback received : " + uri);
         Log.i(TAG, "Retrieving Access Token");
         new RetrieveAccessTokenTask(this, consumer, provider, prefs, mTokenKey, mSecretKey).execute(uri);
         finish();
      }
   }
}
