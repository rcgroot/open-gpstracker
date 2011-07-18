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

import java.text.ParseException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.tasks.GpxParser;
import nl.sogeti.android.gpstracker.actions.utils.ProgressListener;
import nl.sogeti.android.gpstracker.adapter.tasks.DownloadBreadcrumbsTrackTask;
import nl.sogeti.android.gpstracker.adapter.tasks.GetBreadcrumbsActivitiesTask;
import nl.sogeti.android.gpstracker.adapter.tasks.GetBreadcrumbsBundlesTask;
import nl.sogeti.android.gpstracker.adapter.tasks.GetBreadcrumbsTracksTask;
import nl.sogeti.android.gpstracker.adapter.tasks.UploadBreadcrumbsTrackTask;
import nl.sogeti.android.gpstracker.oauth.PrepareRequestTokenActivity;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.Pair;
import nl.sogeti.android.gpstracker.viewer.TrackList;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.ogt.http.conn.ClientConnectionManager;
import org.apache.ogt.http.conn.scheme.PlainSocketFactory;
import org.apache.ogt.http.conn.scheme.Scheme;
import org.apache.ogt.http.conn.scheme.SchemeRegistry;
import org.apache.ogt.http.impl.client.DefaultHttpClient;
import org.apache.ogt.http.impl.conn.tsccm.ThreadSafeClientConnManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * ????
 * 
 * @version $Id:$
 * @author rene (c) Apr 24, 2011, Sogeti B.V.
 */
public class BreadcrumbsAdapter extends BaseAdapter implements Observer
{
   private static final String TAG = "OGT.BreadcrumbsAdapter";

   public static final String OAUTH_TOKEN = "breadcrumbs_oauth_token";
   public static final String OAUTH_TOKEN_SECRET = "breadcrumbs_oauth_secret";
   
   boolean mAuthorized;
   private Context mContext;
   private LayoutInflater mInflater;
   private BreadcrumbsTracks mTracks;
   private DefaultHttpClient mHttpClient;

   private GetBreadcrumbsBundlesTask mPlannedBundleTask;
   private Queue<GetBreadcrumbsTracksTask> mPlannedTrackTasks;

   private boolean mFinishing;
   private OnSharedPreferenceChangeListener tokenChangedListener;
   private ProgressListener mListener;

   public BreadcrumbsAdapter(Context ctx, ProgressListener listener)
   {
      super();
      mContext = ctx;
      mListener = listener;
      mInflater = LayoutInflater.from(mContext);

      SchemeRegistry schemeRegistry = new SchemeRegistry();
      schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
      ClientConnectionManager cm = new ThreadSafeClientConnManager(schemeRegistry);
      mHttpClient = new DefaultHttpClient(cm);

      mTracks = new BreadcrumbsTracks(mContext.getContentResolver());
      mTracks.addObserver(this);
      mPlannedTrackTasks = new LinkedList<GetBreadcrumbsTracksTask>();
   }

   public boolean connectionSetup()
   {
      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
      String token = prefs.getString(OAUTH_TOKEN, "");
      String secret = prefs.getString(OAUTH_TOKEN_SECRET, "");
      mAuthorized = !"".equals(token) && !"".equals(secret);
      if (mAuthorized)
      {
         CommonsHttpOAuthConsumer consumer = getOAuthConsumer();
         if (mTracks.readCache(mContext))
         {
            new GetBreadcrumbsActivitiesTask(this, mListener, mHttpClient, consumer).execute();
            mPlannedBundleTask = new GetBreadcrumbsBundlesTask(this, mListener, mHttpClient, consumer);
         }
      }
      return mAuthorized;
   }

   public CommonsHttpOAuthConsumer getOAuthConsumer()
   {
      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
      String token = prefs.getString(OAUTH_TOKEN, "");
      String secret = prefs.getString(OAUTH_TOKEN_SECRET, "");
      CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(mContext.getString(R.string.CONSUMER_KEY), mContext.getString(R.string.CONSUMER_SECRET));
      consumer.setTokenWithSecret(token, secret);
      return consumer;
   }

   /*
    * (non-Javadoc)
    * @see android.widget.Adapter#getCount()
    */
   public int getCount()
   {
      if (mAuthorized)
      {
         return mTracks.positions();
      }
      else
      {
         return 1;
      }
   }

   /*
    * (non-Javadoc)
    * @see android.widget.Adapter#getItem(int)
    */
   public Object getItem(int position)
   {
      if (mAuthorized)
      {
         return mTracks.getItemForPosition(position);
      }
      else
      {
         return Constants.BREADCRUMBS_CONNECT;
      }

   }

   /*
    * (non-Javadoc)
    * @see android.widget.Adapter#getItemId(int)
    */
   public long getItemId(int position)
   {
      return position;
   }

   /*
    * (non-Javadoc)
    * @see android.widget.Adapter#getView(int, android.view.View,
    * android.view.ViewGroup)
    */
   public View getView(int position, View convertView, ViewGroup parent)
   {
      View view = null;
      if (mAuthorized)
      {
         int type = getItemViewType(position);
         if (convertView == null)
         {
            switch (type)
            {
               case Constants.BREADCRUMBS_ACTIVITY_ITEM_VIEW_TYPE:
                  view = mInflater.inflate(R.layout.breadcrumbs_activity, null);
                  break;
               case Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE:
                  view = mInflater.inflate(R.layout.breadcrumbs_bundle, null);
                  break;
               case Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE:
                  view = mInflater.inflate(R.layout.breadcrumbs_track, null);
                  break;
               default:
                  view = new TextView(null);
                  break;
            }
         }
         else
         {
            view = convertView;
         }
         Pair<Integer, Integer> item = mTracks.getItemForPosition(position);
         String name;
         switch (type)
         {
            case Constants.BREADCRUMBS_ACTIVITY_ITEM_VIEW_TYPE:
               name = mTracks.getValueForItem((Pair<Integer, Integer>) item, BreadcrumbsTracks.NAME);
               ((TextView) view).setText(name);
               break;
            case Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE:
               name = mTracks.getValueForItem((Pair<Integer, Integer>) item, BreadcrumbsTracks.NAME);
               ((TextView) view).setText(name);
               break;
            case Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE:
               TextView nameView = (TextView) view.findViewById(R.id.listitem_name);
               TextView dateView = (TextView) view.findViewById(R.id.listitem_from);

               nameView.setText(mTracks.getValueForItem(item, BreadcrumbsTracks.NAME));
               String dateString = mTracks.getValueForItem(item, BreadcrumbsTracks.ENDTIME);
               if(dateString != null)
               {
                  try
                  {
                     Long date = GpxParser.parseXmlDateTime(dateString);
                     dateView.setText(date.toString());
                  }
                  catch (ParseException e)
                  {
                     Log.w(TAG, "Unable to parse Breadcrumbs end-time " + dateString);
                  }
               }
               break;
            default:
               view = new TextView(null);
               break;
         }
      }
      else
      {
         if (convertView == null)
         {
            view = mInflater.inflate(R.layout.breadcrumbs_connect, null);
         }
         else
         {
            view = convertView;
         }
         ((TextView) view).setText(R.string.breadcrumbs_connect);
      }
      return view;
   }

   @Override
   public int getViewTypeCount()
   {
      int types = 4;
      return types;
   }

   @Override
   public int getItemViewType(int position)
   {
      if (mAuthorized)
      {
         Pair<Integer, Integer> item = mTracks.getItemForPosition(position);
         if (item.first == Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE)
         {
            if (!mFinishing && !mTracks.areTracksLoaded(item) && !mTracks.areTracksLoadingScheduled(item))
            {
               mPlannedTrackTasks.add(new GetBreadcrumbsTracksTask(this, mListener, mHttpClient, getOAuthConsumer(), item.second));
               mTracks.addTracksLoadingScheduled(item);
               executeNextTask();
            }
         }
         return item.first;
      }
      else
      {
         return Constants.BREADCRUMBS_CONNECT_ITEM_VIEW_TYPE;
      }
   }

   @Override
   public boolean areAllItemsEnabled()
   {
      return false;
   };

   @Override
   public boolean isEnabled(int position)
   {
      int itemViewType = getItemViewType(position);
      return itemViewType == Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE || itemViewType == Constants.BREADCRUMBS_CONNECT_ITEM_VIEW_TYPE;
   }

   public BreadcrumbsTracks getBreadcrumbsTracks()
   {
      return mTracks;
   }

   public synchronized void finishedTask()
   {
      executeNextTask();
   }

   private synchronized void executeNextTask()
   {
      if (mPlannedBundleTask != null)
      {
         mPlannedBundleTask.execute();
         mPlannedBundleTask = null;
      }
      else
      {
         GetBreadcrumbsTracksTask next = mPlannedTrackTasks.poll();
         if (next != null)
         {
            GetBreadcrumbsTracksTask task = (GetBreadcrumbsTracksTask) next;
            task.execute();
         }
      }
   }

   public synchronized void shutdown()
   {
      if (tokenChangedListener != null)
      {
         PreferenceManager.getDefaultSharedPreferences(mContext).unregisterOnSharedPreferenceChangeListener(tokenChangedListener);
      }
      mAuthorized = false;
      mFinishing = true;
      mPlannedBundleTask = null;
      mPlannedTrackTasks.clear();
      mHttpClient.getConnectionManager().shutdown();
      mHttpClient = null;

      mTracks.persistCache(mContext);
      mTracks = null;
   }

   public void startDownloadTask(Context context, ProgressListener listener, Pair<Integer, Integer> track)
   {
      new DownloadBreadcrumbsTrackTask(context, listener, this, mHttpClient, getOAuthConsumer(), track).execute();
   }

   public void startUploadTask(Context context, ProgressListener listener, Uri trackUri)
   {
      new UploadBreadcrumbsTrackTask(context, this, listener, mHttpClient, getOAuthConsumer(), trackUri).execute();
   }

   public boolean isOnline()
   {
      return mAuthorized;
   }

   public void requestBreadcrumbsOauthToken(final Activity activity)
   {
      tokenChangedListener = new OnSharedPreferenceChangeListener()
      {
         public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
         {
            if (OAUTH_TOKEN.equals(key))
            {
               PreferenceManager.getDefaultSharedPreferences(mContext).unregisterOnSharedPreferenceChangeListener(tokenChangedListener);
               activity.runOnUiThread(new Runnable()
               {
                  public void run()
                  {
                     connectionSetup();
                  }
               });
            }
         }
      };
      PreferenceManager.getDefaultSharedPreferences(mContext).registerOnSharedPreferenceChangeListener(tokenChangedListener);

      Intent i = new Intent(mContext.getApplicationContext(), PrepareRequestTokenActivity.class);
      i.putExtra(PrepareRequestTokenActivity.OAUTH_TOKEN_PREF, OAUTH_TOKEN);
      i.putExtra(PrepareRequestTokenActivity.OAUTH_TOKEN_SECRET_PREF, OAUTH_TOKEN_SECRET);

      i.putExtra(PrepareRequestTokenActivity.CONSUMER_KEY, mContext.getString(R.string.CONSUMER_KEY));
      i.putExtra(PrepareRequestTokenActivity.CONSUMER_SECRET, mContext.getString(R.string.CONSUMER_SECRET));
      i.putExtra(PrepareRequestTokenActivity.REQUEST_URL, Constants.REQUEST_URL);
      i.putExtra(PrepareRequestTokenActivity.ACCESS_URL, Constants.ACCESS_URL);
      i.putExtra(PrepareRequestTokenActivity.AUTHORIZE_URL, Constants.AUTHORIZE_URL);

      mContext.startActivity(i);
   }

   public void update(Observable observable, Object data)
   {
      notifyDataSetChanged();
   }
}
