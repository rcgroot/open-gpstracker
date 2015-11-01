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

import android.content.Context;
import android.database.DataSetObserver;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

import java.util.LinkedHashMap;
import java.util.Map;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.util.Constants;

/**
 * Combines multiple Adapters into a sectioned ListAdapter
 *
 * @author rene (c) Apr 24, 2011, Sogeti B.V.
 * @version $Id:$
 */
public class SectionedListAdapter extends BaseAdapter
{
   @SuppressWarnings("unused")
   private static final String TAG = "OGT.SectionedListAdapter";
   private Map<String, BaseAdapter> mSections;
   private ArrayAdapter<String> mHeaders;

   public SectionedListAdapter(Context ctx)
   {
      mHeaders = new ArrayAdapter<>(ctx, R.layout.section_header);
      mSections = new LinkedHashMap<>();
   }

   public void addSection(String name, BaseAdapter adapter)
   {
      mHeaders.add(name);
      mSections.put(name, adapter);
   }

   @Override
   public void registerDataSetObserver(DataSetObserver observer)
   {
      super.registerDataSetObserver(observer);
      for (Adapter adapter : mSections.values())
      {
         adapter.registerDataSetObserver(observer);
      }
   }

   @Override
   public void unregisterDataSetObserver(DataSetObserver observer)
   {
      super.unregisterDataSetObserver(observer);
      for (Adapter adapter : mSections.values())
      {
         adapter.unregisterDataSetObserver(observer);
      }
   }

   @Override
   public boolean areAllItemsEnabled()
   {
      return false;
   }

   @Override
   public boolean isEnabled(int position)
   {
      if (getItemViewType(position) == Constants.SECTIONED_HEADER_ITEM_VIEW_TYPE)
      {
         return false;
      }
      else
      {
         int countDown = position;
         for (String section : mSections.keySet())
         {
            BaseAdapter adapter = mSections.get(section);
            countDown--;
            int size = adapter.getCount();

            if (countDown < size)
            {
               return adapter.isEnabled(countDown);
            }
            // otherwise jump into next section
            countDown -= size;
         }
      }
      return false;
   }

   @Override
   public int getItemViewType(int position)
   {
      int type = 1;
      Adapter adapter;
      int countDown = position;
      for (String section : mSections.keySet())
      {
         adapter = mSections.get(section);
         int size = adapter.getCount() + 1;

         if (countDown == 0)
         {
            return Constants.SECTIONED_HEADER_ITEM_VIEW_TYPE;
         }
         else if (countDown < size)
         {
            return type + adapter.getItemViewType(countDown - 1);
         }
         countDown -= size;
         type += adapter.getViewTypeCount();
      }
      return ListAdapter.IGNORE_ITEM_VIEW_TYPE;
   }

   @Override
   public int getViewTypeCount()
   {
      int types = 1;
      for (Adapter section : mSections.values())
      {
         types += section.getViewTypeCount();
      }
      return types;
   }

   @Override
   public int getCount()
   {
      int count = 0;
      for (Adapter adapter : mSections.values())
      {
         count += adapter.getCount() + 1;
      }
      return count;
   }

   @Override
   public Object getItem(int position)
   {
      int countDown = position;
      Adapter adapter;
      for (String section : mSections.keySet())
      {
         adapter = mSections.get(section);
         if (countDown == 0)
         {
            return section;
         }
         countDown--;

         if (countDown < adapter.getCount())
         {
            return adapter.getItem(countDown);
         }
         countDown -= adapter.getCount();
      }
      return null;
   }

   @Override
   public long getItemId(int position)
   {
      int countDown = position;
      Adapter adapter;
      for (String section : mSections.keySet())
      {
         adapter = mSections.get(section);
         if (countDown == 0)
         {
            return position;
         }
         countDown--;

         if (countDown < adapter.getCount())
         {
            long id = adapter.getItemId(countDown);
            return id;
         }
         countDown -= adapter.getCount();
      }
      return -1;
   }

   @Override
   public View getView(final int position, View convertView, ViewGroup parent)
   {
      int sectionNumber = 0;
      int countDown = position;
      for (String section : mSections.keySet())
      {
         Adapter adapter = mSections.get(section);
         int size = adapter.getCount() + 1;

         // check if position inside this section
         if (countDown == 0)
         {
            View view = new View(mHeaders.getContext());
            if (!TextUtils.isEmpty(mHeaders.getItem(sectionNumber)))
            {
               view = mHeaders.getView(sectionNumber, convertView, parent);
            }
            return view;
         }
         if (countDown < size)
         {
            return adapter.getView(countDown - 1, convertView, parent);
         }

         // otherwise jump into next section
         countDown -= size;
         sectionNumber++;
      }
      return null;
   }
}
