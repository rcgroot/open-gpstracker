/*
 * Copyright (C) 2011 Ievgenii Nazaruk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.sogeti.android.gpstracker.googlemapfragment;

import nl.sogeti.android.gpstracker.fragment.AbstractLoggerFragment;
import nl.sogeti.android.gpstracker.fragment.GoogleMapFragment;
import android.app.LocalActivityManager;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;

/**
 * This is a fragment that will be used during transition from activities to fragments.
 */
@SuppressWarnings("deprecation")
public abstract class LocalActivityManagerFragment extends AbstractLoggerFragment
{

   private static final String TAG = LocalActivityManagerFragment.class.getSimpleName();
   private static final String KEY_STATE_BUNDLE = "localActivityManagerState";

   private LocalActivityManager mLocalActivityManager;

   protected LocalActivityManager getLocalActivityManager()
   {
      return mLocalActivityManager;
   }

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      Log.d(TAG, "onCreate(): " + getClass().getSimpleName());

      Bundle state = null;
      if (savedInstanceState != null)
      {
         state = savedInstanceState.getBundle(KEY_STATE_BUNDLE);
      }

      mLocalActivityManager = new LocalActivityManager(getActivity(), true);
      mLocalActivityManager.dispatchCreate(state);
   }

   @Override
   public void onSaveInstanceState(Bundle outState)
   {
      super.onSaveInstanceState(outState);
      outState.putBundle(KEY_STATE_BUNDLE, mLocalActivityManager.saveInstanceState());
   }

   @Override
   public void onResume()
   {
      super.onResume();
      Log.d(TAG, "onResume(): " + getClass().getSimpleName());
      mLocalActivityManager.dispatchResume();
   }

   @Override
   public void onPause()
   {
      super.onPause();
      Log.d(TAG, "onPause(): " + getClass().getSimpleName());
      mLocalActivityManager.dispatchPause(getActivity().isFinishing());
   }

   @Override
   public void onStop()
   {
      super.onStop();
      Log.d(TAG, "onStop(): " + getClass().getSimpleName());
      mLocalActivityManager.dispatchStop();
   }

   @Override
   public void onDestroy()
   {
      super.onDestroy();
      Log.d(TAG, "onDestroy(): " + getClass().getSimpleName());
      mLocalActivityManager.dispatchDestroy(getActivity().isFinishing());
   }
}
