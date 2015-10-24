/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Oct 20, 2012 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 */
package nl.sogeti.android.gpstracker.breadcrumbs;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.oauth.PrepareRequestTokenActivity;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.Pair;
import oauth.signpost.basic.DefaultOAuthConsumer;

/**
 * ????
 *
 * @author rene (c) Oct 20, 2012, Sogeti B.V.
 * @version $Id:$
 */
public class BreadcrumbsService extends Service implements Observer, ProgressListener
{
   public static final String OAUTH_TOKEN = "breadcrumbs_oauth_token";
   public static final String OAUTH_TOKEN_SECRET = "breadcrumbs_oauth_secret";
   public static final String NOTIFY_DATA_SET_CHANGED = "nl.sogeti.android.gpstracker.intent.action" +
         ".NOTIFY_DATA_SET_CHANGED";
   public static final String NOTIFY_PROGRESS_CHANGED = "nl.sogeti.android.gpstracker.intent.action" +
         ".NOTIFY_PROGRESS_CHANGED";
   public static final String PROGRESS_INDETERMINATE = null;
   public static final String PROGRESS = null;
   public static final String PROGRESS_STATE = null;
   public static final String PROGRESS_RESULT = null;
   public static final String PROGRESS_TASK = null;
   public static final String PROGRESS_MESSAGE = null;
   public static final int PROGRESS_STARTED = 1;
   public static final int PROGRESS_FINISHED = 2;
   public static final int PROGRESS_ERROR = 3;
   private static final String TAG = "OGT.BreadcrumbsService";
   private final IBinder mBinder = new LocalBinder();
   boolean mAuthorized;
   ExecutorService mExecutor;
   private BreadcrumbsTracks mTracks;
   private OnSharedPreferenceChangeListener tokenChangedListener;
   private boolean mFinishing;

   @Override
   public void onCreate()
   {
      super.onCreate();
      mExecutor = Executors.newFixedThreadPool(1);
      mTracks = new BreadcrumbsTracks(this.getContentResolver());
      mTracks.addObserver(this);

      connectionSetup();
   }

   @Override
   public void onDestroy()
   {
      if (tokenChangedListener != null)
      {
         PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(
               tokenChangedListener);
      }
      mAuthorized = false;
      mFinishing = true;
      new AsyncTask<Void, Void, Void>()
      {
         public void executeOn(Executor executor)
         {
            if (Build.VERSION.SDK_INT >= 11)
            {
               executeOnExecutor(executor);
            }
            else
            {
               execute();
            }
         }

         @Override
         protected Void doInBackground(Void... params)
         {
            mExecutor.shutdown();
            return null;
         }
      }.executeOn(mExecutor);
      mTracks.persistCache(this);

      super.onDestroy();
   }

   /**
    * @see android.app.Service#onBind(android.content.Intent)
    */
   @Override
   public IBinder onBind(Intent intent)
   {
      return mBinder;
   }

   private boolean connectionSetup()
   {
      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      String token = prefs.getString(OAUTH_TOKEN, "");
      String secret = prefs.getString(OAUTH_TOKEN_SECRET, "");
      mAuthorized = !"".equals(token) && !"".equals(secret);
      if (mAuthorized)
      {
         DefaultOAuthConsumer consumer = getOAuthConsumer();
         if (mTracks.readCache(this))
         {
            new GetBreadcrumbsActivitiesTask(this, this, this, consumer).executeOn(mExecutor);
            new GetBreadcrumbsBundlesTask(this, this, this, consumer).executeOn(mExecutor);
         }
      }
      return mAuthorized;
   }

   public DefaultOAuthConsumer getOAuthConsumer()
   {
      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      String token = prefs.getString(OAUTH_TOKEN, "");
      String secret = prefs.getString(OAUTH_TOKEN_SECRET, "");
      DefaultOAuthConsumer consumer = new DefaultOAuthConsumer(this.getString(R.string.CONSUMER_KEY),
            this.getString(R.string.CONSUMER_SECRET));
      consumer.setTokenWithSecret(token, secret);
      return consumer;
   }

   public void removeAuthentication()
   {
      Log.w(TAG, "Removing Breadcrumbs OAuth tokens");
      Editor e = PreferenceManager.getDefaultSharedPreferences(this).edit();
      e.remove(OAUTH_TOKEN);
      e.remove(OAUTH_TOKEN_SECRET);
      e.commit();
   }

   /**
    * Use a locally stored token or start the request activity to collect one
    */
   public void collectBreadcrumbsOauthToken()
   {
      if (!connectionSetup())
      {
         tokenChangedListener = new OnSharedPreferenceChangeListener()
         {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
            {
               if (OAUTH_TOKEN.equals(key))
               {
                  PreferenceManager.getDefaultSharedPreferences(BreadcrumbsService.this)
                                   .unregisterOnSharedPreferenceChangeListener(tokenChangedListener);
                  connectionSetup();
               }
            }
         };
         PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(
               tokenChangedListener);

         Intent i = new Intent(this.getApplicationContext(), PrepareRequestTokenActivity.class);
         i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         i.putExtra(PrepareRequestTokenActivity.OAUTH_TOKEN_PREF, OAUTH_TOKEN);
         i.putExtra(PrepareRequestTokenActivity.OAUTH_TOKEN_SECRET_PREF, OAUTH_TOKEN_SECRET);

         i.putExtra(PrepareRequestTokenActivity.CONSUMER_KEY, this.getString(R.string.CONSUMER_KEY));
         i.putExtra(PrepareRequestTokenActivity.CONSUMER_SECRET, this.getString(R.string.CONSUMER_SECRET));
         i.putExtra(PrepareRequestTokenActivity.REQUEST_URL, Constants.REQUEST_URL);
         i.putExtra(PrepareRequestTokenActivity.ACCESS_URL, Constants.ACCESS_URL);
         i.putExtra(PrepareRequestTokenActivity.AUTHORIZE_URL, Constants.AUTHORIZE_URL);

         this.startActivity(i);
      }
   }

   public void startDownloadTask(Context context, ProgressListener listener, Pair<Integer, Integer> track)
   {
      new DownloadBreadcrumbsTrackTask(context, listener, this, getOAuthConsumer(), track).executeOn(mExecutor);
   }

   public void startUploadTask(Context context, ProgressListener listener, Uri trackUri, String name)
   {
      new UploadBreadcrumbsTrackTask(context, this, listener, getOAuthConsumer(), trackUri, name).executeOn(mExecutor);
   }

   public boolean isAuthorized()
   {
      return mAuthorized;
   }

   public void willDisplayItem(Pair<Integer, Integer> item)
   {
      if (item.first == Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE)
      {
         if (!mFinishing && !mTracks.areTracksLoaded(item) && !mTracks.areTracksLoadingScheduled(item))
         {
            new GetBreadcrumbsTracksTask(this, this, this, getOAuthConsumer(), item.second).executeOn(mExecutor);
            mTracks.addTracksLoadingScheduled(item);
         }
      }
   }

   public List<Pair<Integer, Integer>> getAllItems()
   {
      List<Pair<Integer, Integer>> items = mTracks.getAllItems();

      return items;
   }

   public List<Pair<Integer, Integer>> getActivityList()
   {
      List<Pair<Integer, Integer>> activities = mTracks.getActivityList();
      return activities;
   }

   public List<Pair<Integer, Integer>> getBundleList()
   {
      List<Pair<Integer, Integer>> bundles = mTracks.getBundleList();

      return bundles;
   }

   public String getValueForItem(Pair<Integer, Integer> item, String name)
   {
      return mTracks.getValueForItem(item, name);
   }

   public void clearAllCache()
   {
      mTracks.clearAllCache(this);
   }

   protected BreadcrumbsTracks getBreadcrumbsTracks()
   {
      return mTracks;
   }

   public boolean isLocalTrackSynced(long trackId)
   {
      return mTracks.isLocalTrackSynced(trackId);
   }

   /****
    * Observer interface
    */

   @Override
   public void update(Observable observable, Object data)
   {
      Intent broadcast = new Intent();
      broadcast.setAction(BreadcrumbsService.NOTIFY_DATA_SET_CHANGED);
      getApplicationContext().sendBroadcast(broadcast);
   }

   /****
    * ProgressListener interface
    */

   @Override
   public void setIndeterminate(boolean indeterminate)
   {
      Intent broadcast = new Intent();
      broadcast.putExtra(BreadcrumbsService.PROGRESS_INDETERMINATE, indeterminate);
      broadcast.setAction(BreadcrumbsService.NOTIFY_PROGRESS_CHANGED);
      getApplicationContext().sendBroadcast(broadcast);
   }

   @Override
   public void started()
   {
      Intent broadcast = new Intent();
      broadcast.putExtra(BreadcrumbsService.PROGRESS_STATE, BreadcrumbsService.PROGRESS_STARTED);
      broadcast.setAction(BreadcrumbsService.NOTIFY_PROGRESS_CHANGED);
      getApplicationContext().sendBroadcast(broadcast);
   }

   @Override
   public void setProgress(int value)
   {
      Intent broadcast = new Intent();
      broadcast.putExtra(BreadcrumbsService.PROGRESS, value);
      broadcast.setAction(BreadcrumbsService.NOTIFY_PROGRESS_CHANGED);
      getApplicationContext().sendBroadcast(broadcast);
   }

   @Override
   public void finished(Uri result)
   {
      Intent broadcast = new Intent();
      broadcast.putExtra(BreadcrumbsService.PROGRESS_STATE, BreadcrumbsService.PROGRESS_FINISHED);
      broadcast.putExtra(BreadcrumbsService.PROGRESS_RESULT, result);
      broadcast.setAction(BreadcrumbsService.NOTIFY_PROGRESS_CHANGED);
      getApplicationContext().sendBroadcast(broadcast);
   }

   @Override
   public void showError(String task, String errorMessage, Exception exception)
   {
      Intent broadcast = new Intent();
      broadcast.putExtra(BreadcrumbsService.PROGRESS_STATE, BreadcrumbsService.PROGRESS_ERROR);
      broadcast.putExtra(BreadcrumbsService.PROGRESS_TASK, task);
      broadcast.putExtra(BreadcrumbsService.PROGRESS_MESSAGE, errorMessage);
      broadcast.putExtra(BreadcrumbsService.PROGRESS_RESULT, exception);
      broadcast.setAction(BreadcrumbsService.NOTIFY_PROGRESS_CHANGED);
      getApplicationContext().sendBroadcast(broadcast);
   }

   /**
    * Class used for the client Binder. Because we know this service always runs in the same process as its clients, we
    * don't need to deal with IPC.
    */
   public class LocalBinder extends Binder
   {
      public BreadcrumbsService getService()
      {
         return BreadcrumbsService.this;
      }
   }
}
