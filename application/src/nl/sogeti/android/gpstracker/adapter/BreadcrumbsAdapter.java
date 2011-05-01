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
package nl.sogeti.android.gpstracker.adapter;

import java.util.SortedSet;
import java.util.TreeSet;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.Pair;
import oauth.signpost.OAuth;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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
public class BreadcrumbsAdapter extends BaseAdapter
{
   private static final String TAG = "OGT.BreadcrumbsAdapter";
   boolean mOnline;
   private Context mContext;
   private LayoutInflater mInflater;
   private CommonsHttpOAuthConsumer mConsumer;
   private BreadcrumbsTracks mTracks;
   private DefaultHttpClient mHttpClient;
   private GetBreadcrumbsTracksTask mTracksTask;
   private SortedSet<Integer> mBundlesTasks;

   public BreadcrumbsAdapter(Context ctx, DefaultHttpClient httpclient)
   {
      super();
      mContext = ctx;
      mInflater = LayoutInflater.from(mContext);
      mHttpClient = httpclient;

      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
      String token  = prefs.getString(OAuth.OAUTH_TOKEN, "");
      String secret = prefs.getString(OAuth.OAUTH_TOKEN_SECRET, "");
      mConsumer = new CommonsHttpOAuthConsumer(mContext.getString(R.string.CONSUMER_KEY), mContext.getString(R.string.CONSUMER_SECRET));
      mConsumer.setTokenWithSecret(token, secret);
      mOnline = !"".equals(token) && !"".equals(secret);
      
      mTracks = new BreadcrumbsTracks();
      mBundlesTasks = new TreeSet<Integer>();
      new GetBreadcrumbsBundlesTask(this, mHttpClient, mConsumer).execute();
   }

   /*
    * (non-Javadoc)
    * @see android.widget.Adapter#getCount()
    */
   public int getCount()
   {
      if (mOnline)
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
      if( mOnline )
      {
         Pair<Integer, Integer> item = mTracks.getItemForPosition(position);
         return mTracks.getKeyForItem(item, "NAME");
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
      TextView textView;
      if (mOnline)
      {
         if (convertView == null)
         {
            int type = getItemViewType(position);
            switch (type)
            {
               case Constants.BREADCRUMBS_ACTIVITY_ITEM_VIEW_TYPE:
                  textView = (TextView) mInflater.inflate(R.layout.breadcrumbs_activity, null);
                  break;
               case Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE:
                  textView = (TextView) mInflater.inflate(R.layout.breadcrumbs_bundle, null);
                  break;
               case Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE:
                  textView = (TextView) mInflater.inflate(R.layout.breadcrumbs_track, null);
                  break;
               default:
                  textView = new TextView(null);
                  break;
            }
         }
         else
         {
            textView = (TextView) convertView;
         }
         textView.setText((String) getItem(position));
      }
      else
      {
         if (convertView == null)
         {
            textView = (TextView) mInflater.inflate(R.layout.breadcrumbs_connect, null);
         }
         else
         {
            textView = (TextView) convertView;
         }
         textView.setText(R.string.breadcrumbs_connect);
         
      }
      return textView;
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
      if (mOnline)
      {
         Pair<Integer, Integer> item = mTracks.getItemForPosition(position);
         if( item.first == Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE )
         {
            if( !mTracks.areTracksLoaded( item ) )
            {
               mBundlesTasks.add( item.second );
            }
         }
         return item.first;
      }
      else
      {
         return Constants.BREADCRUMBS_CONNECT_ITEM_VIEW_TYPE;
      }
   }

   public BreadcrumbsTracks getBreadcrumbsTracks()
   {
      return mTracks ;
   }

   public void finishedBundles(GetBreadcrumbsBundlesTask getBreadcrumbsBundlesTask)
   {
      Log.d( TAG, "Reset bundles to download because of bundle finish" );
      notifyDataSetChanged();
      new GetBreadcrumbsActivitiesTask(this, mHttpClient, mConsumer).execute();
   }

   public void finishedActivities(GetBreadcrumbsActivitiesTask getBreadcrumbsActivitiesTask)
   {
      notifyDataSetChanged();
      nextTrack();
   }

   public void finishedTrack(GetBreadcrumbsTracksTask getBreadcrumbsTracksTask)
   {
      notifyDataSetChanged();
      nextTrack();
   }

   private void nextTrack()
   {
      if( mBundlesTasks.size() > 0 )
      {
         mTracksTask = new GetBreadcrumbsTracksTask(this, mHttpClient, mConsumer);
         Integer bundleId = mBundlesTasks.first();
         mBundlesTasks.clear();
         mTracksTask.setBundleId( bundleId );
         mTracksTask.execute();
      }
   }
}
