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

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.util.Constants;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;

/**
 * Prepares a OAuthConsumer and OAuthProvider OAuthConsumer is configured with
 * the consumer key & consumer secret. Both key and secret are retrieved from 
 * the extras in the Intent 
 * 
 * OAuthProvider is configured with the 3
 * OAuth endpoints. These are retrieved from the extras in the Intent.
 * 
 * Execute the OAuthRequestTokenTask to retrieve the request,
 * and authorize the request. After the request is authorized, a callback is
 * made here and this activity finishes to return to the last Activity on the 
 * stack.
 */
public class PrepareRequestTokenActivity extends Activity
{
   /**
    * Name of the Extra in the intent holding the consumer secret
    */
   public static final String CONSUMER_SECRET = "CONSUMER_SECRET";
   /**
    * Name of the Extra in the intent holding the consumer key
    */
   public static final String CONSUMER_KEY = "CONSUMER_KEY";
   /**
    * Name of the Extra in the intent holding the authorizationWebsiteUrl
    */
   public static final String AUTHORIZE_URL = "AUTHORIZE_URL";
   /**
    * Name of the Extra in the intent holding the accessTokenEndpointUrl
    */
   public static final String ACCESS_URL = "ACCESS_URL";
   /**
    * Name of the Extra in the intent holding the requestTokenEndpointUrl
    */
   public static final String REQUEST_URL = "REQUEST_URL";
   /**
    * String value of the key in the DefaultSharedPreferences 
    * in which to store the permission token 
    */
   public static final String OAUTH_TOKEN_PREF = "OAUTH_TOKEN";
   /**
    * String value of the key in the DefaultSharedPreferences 
    * in which to store the permission secret 
    */
   public static final String OAUTH_TOKEN_SECRET_PREF = "OAUTH_TOKEN_SECRET";


   final String TAG = "OGT.PrepareRequestTokenActivity";

   private OAuthConsumer consumer;
   private OAuthProvider provider;

   private String mTokenKey;

   private String mSecretKey;
   private OAuthRequestTokenTask mTask;

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.oauthentication);
      
      String key        = getIntent().getStringExtra(CONSUMER_KEY);
      String secret     = getIntent().getStringExtra(CONSUMER_SECRET);
      
      String requestUrl = getIntent().getStringExtra(REQUEST_URL);
      String accessUrl  = getIntent().getStringExtra(ACCESS_URL);
      String authUrl    = getIntent().getStringExtra(AUTHORIZE_URL);
      
      TextView tv = (TextView) findViewById(R.id.detail);
      tv.setText(requestUrl);
      
      
      mTokenKey  = getIntent().getStringExtra(OAUTH_TOKEN_PREF); 
      mSecretKey = getIntent().getStringExtra(OAUTH_TOKEN_SECRET_PREF); 
      
      this.consumer = new CommonsHttpOAuthConsumer(key, secret);
      this.provider = new CommonsHttpOAuthProvider(requestUrl, accessUrl, authUrl);

      mTask = new OAuthRequestTokenTask(this, consumer, provider);
      mTask.execute();
   }
   
   @Override
   protected void onResume()
   {
      super.onResume();
      
      // Will not be called if onNewIntent() was called with callback scheme
      Status status = mTask.getStatus();
      if( status != Status.RUNNING )
      {
         finish();
      }
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
