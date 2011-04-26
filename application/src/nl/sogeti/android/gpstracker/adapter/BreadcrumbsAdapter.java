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

import java.util.List;
import java.util.Map;
import java.util.Vector;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.util.Constants;
import oauth.signpost.OAuth;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
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
   private static final int ACTIVITY_ITEM_VIEW_TYPE = 1;
   private static final String TAG = "OGT.BreadcrumbsAdapter";
   boolean mOnline;
   private Context mContext;
   private LayoutInflater mInflater;
   private CommonsHttpOAuthConsumer mConsumer;
   private List<String> mActivities = new Vector<String>();
   private Map<String, Integer> mActivityMappings;
   private DefaultHttpClient mHttpclient;

   public BreadcrumbsAdapter(Context ctx)
   {
      super();
      mContext = ctx;
      mInflater = LayoutInflater.from(mContext);
      mHttpclient = new DefaultHttpClient();

      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
      String token  = prefs.getString(OAuth.OAUTH_TOKEN, "");
      String secret = prefs.getString(OAuth.OAUTH_TOKEN_SECRET, "");
      mConsumer = new CommonsHttpOAuthConsumer(mContext.getString(R.string.CONSUMER_KEY), mContext.getString(R.string.CONSUMER_SECRET));
      mConsumer.setTokenWithSecret(token, secret);
      mOnline = !"".equals(token) && !"".equals(secret);
      new GetBreadcrumbsBundlesTask(this, mHttpclient, mConsumer).execute();
   }

   /*
    * (non-Javadoc)
    * @see android.widget.Adapter#getCount()
    */
   public int getCount()
   {
      if (mOnline)
      {
         return mActivities.size();
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
         return mActivities.get(position);
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

      if (mOnline)
      {
         TextView textView;
         if (convertView == null)
         {
            textView = (TextView) mInflater.inflate(R.layout.breadcrumbs_activity, null);
         }
         else
         {
            textView = (TextView) convertView;
         }
         textView.setText((String) getItem(position));
         return textView;
      }
      else
      {
         TextView textView;
         if (convertView == null)
         {
            textView = (TextView) mInflater.inflate(R.layout.breadcrumbs_connect, null);
         }
         else
         {
            textView = (TextView) convertView;
         }
         textView.setText(R.string.breadcrumbs_connect);
         return textView;
      }
   }

   @Override
   public int getViewTypeCount()
   {
      int types = 2;
      return types;
   }

   @Override
   public int getItemViewType(int position)
   {
      if (mOnline)
      {
         return 0;
      }
      else
      {
         return ACTIVITY_ITEM_VIEW_TYPE;
      }
   }

   public void setActivities(List<String> activities, Map<String, Integer> activityMappings)
   {
      Log.d( TAG, "Received list of activities "+activities) ;
      mActivities = activities;
      mActivityMappings = activityMappings;
      this.notifyDataSetChanged();
   }
}
