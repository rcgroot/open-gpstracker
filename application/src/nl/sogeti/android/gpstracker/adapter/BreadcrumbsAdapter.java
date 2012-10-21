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

import java.util.LinkedList;
import java.util.List;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.tasks.GpxParser;
import nl.sogeti.android.gpstracker.breadcrumbs.BreadcrumbsService;
import nl.sogeti.android.gpstracker.breadcrumbs.BreadcrumbsTracks;
import nl.sogeti.android.gpstracker.util.Constants;
import nl.sogeti.android.gpstracker.util.Pair;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * Organizes Breadcrumbs tasks based on demands on the BaseAdapter functions
 * 
 * @version $Id:$
 * @author rene (c) Apr 24, 2011, Sogeti B.V.
 */
public class BreadcrumbsAdapter extends BaseAdapter
{
   private static final String TAG = "OGT.BreadcrumbsAdapter";

   public static final boolean DEBUG = false;

   private Activity mContext;
   private LayoutInflater mInflater;
   private BreadcrumbsService mService;
   private List<Pair<Integer, Integer>> breadcrumbItems = new LinkedList<Pair<Integer, Integer>>();

   public BreadcrumbsAdapter(Activity ctx, BreadcrumbsService service)
   {
      super();
      mContext = ctx;
      mService = service;
      mInflater = LayoutInflater.from(mContext);
   }

   public void setService(BreadcrumbsService service)
   {
      mService = service;
      updateItemList();
   }

   /**
    * Reloads the current list of known breadcrumb listview items
    * 
    */
   public void updateItemList()
   {
      mContext.runOnUiThread(new Runnable()
         {
            @Override
            public void run()
            {
               if (mService != null)
               {
                  breadcrumbItems = mService.getAllItems();
                  notifyDataSetChanged();
               }
            }
         });
   }

   /**
    * @see android.widget.Adapter#getCount()
    */
   @Override
   public int getCount()
   {
      if (mService != null)
      {
         if (mService.isAuthorized())
         {
            return breadcrumbItems.size();
         }
         else
         {
            return 1;
         }
      }
      else
      {
         return 0;
      }

   }

   /**
    * @see android.widget.Adapter#getItem(int)
    */
   @Override
   public Object getItem(int position)
   {
      if (mService.isAuthorized())
      {
         return breadcrumbItems.get(position);
      }
      else
      {
         return Constants.BREADCRUMBS_CONNECT;
      }

   }

   /**
    * @see android.widget.Adapter#getItemId(int)
    */
   @Override
   public long getItemId(int position)
   {
      return position;
   }

   /**
    * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
    */
   @Override
   public View getView(int position, View convertView, ViewGroup parent)
   {
      View view = null;
      if (mService.isAuthorized())
      {
         int type = getItemViewType(position);
         if (convertView == null)
         {
            switch (type)
            {
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
         Pair<Integer, Integer> item = breadcrumbItems.get(position);
         mService.willDisplayItem(item);
         String name;
         switch (type)
         {
            case Constants.BREADCRUMBS_BUNDLE_ITEM_VIEW_TYPE:
               name = mService.getValueForItem((Pair<Integer, Integer>) item, BreadcrumbsTracks.NAME);
               ((TextView) view.findViewById(R.id.listitem_name)).setText(name);
               break;
            case Constants.BREADCRUMBS_TRACK_ITEM_VIEW_TYPE:
               TextView nameView = (TextView) view.findViewById(R.id.listitem_name);
               TextView dateView = (TextView) view.findViewById(R.id.listitem_from);

               nameView.setText(mService.getValueForItem(item, BreadcrumbsTracks.NAME));
               String dateString = mService.getValueForItem(item, BreadcrumbsTracks.ENDTIME);
               if (dateString != null)
               {
                  Long date = GpxParser.parseXmlDateTime(dateString);
                  dateView.setText(date.toString());
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
      if (mService.isAuthorized())
      {
         Pair<Integer, Integer> item = breadcrumbItems.get(position);
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
}
