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
package nl.sogeti.android.gpstracker.fragment;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.logger.GPSLoggerServiceManager;
import nl.sogeti.android.gpstracker.util.Constants;
import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

/**
 * Empty Activity that pops up the dialog to name the track
 * 
 * @version $Id$
 * @author rene (c) Jul 27, 2010, Sogeti B.V.
 */
public class ControlTrackingFragment extends Fragment
{
   private static final String TAG = "OGT.ControlTracking";

   private Button start;
   private Button pause;
   private Button resume;
   private Button stop;

   private ControlTrackingListener mListener;

   private int mLoggingState;

   public static ControlTrackingFragment newInstance(int index)
   {
      ControlTrackingFragment f = new ControlTrackingFragment();

      // Supply index input as an argument.
      Bundle args = new Bundle();
      args.putInt("state", index);
      f.setArguments(args);

      return f;
   }

   @Override
   public void onAttach(Activity activity)
   {
      super.onAttach(activity);
      try
      {
         mListener = (ControlTrackingListener) activity;
      }
      catch (ClassCastException e)
      {
         throw new ClassCastException(activity.toString() + " must implement OnArticleSelectedListener");
      }
   }

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      mLoggingState = getArguments().getInt("state");
   }

   @Override
   public View onCreateView(LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState)
   {
      View view = null;
      view = inflater.inflate(R.layout.logcontrol, null);
      start = (Button) view.findViewById(R.id.logcontrol_start);
      pause = (Button) view.findViewById(R.id.logcontrol_pause);
      resume = (Button) view.findViewById(R.id.logcontrol_resume);
      stop = (Button) view.findViewById(R.id.logcontrol_stop);
      start.setOnClickListener(mLoggingControlListener);
      pause.setOnClickListener(mLoggingControlListener);
      resume.setOnClickListener(mLoggingControlListener);
      stop.setOnClickListener(mLoggingControlListener);

      updateDialogState();
      
      return view;
   }
   
   public void updateDialogState()
   {
      switch (mLoggingState)
      {
         case Constants.STOPPED:
            start.setEnabled(true);
            pause.setEnabled(false);
            resume.setEnabled(false);
            stop.setEnabled(false);
            break;
         case Constants.LOGGING:
            start.setEnabled(false);
            pause.setEnabled(true);
            resume.setEnabled(false);
            stop.setEnabled(true);
            break;
         case Constants.PAUSED:
            start.setEnabled(false);
            pause.setEnabled(false);
            resume.setEnabled(true);
            stop.setEnabled(true);
            break;
         default:
            Log.w(TAG, String.format("State %d of logging, enabling and hope for the best....", mLoggingState));
            start.setEnabled(false);
            pause.setEnabled(false);
            resume.setEnabled(false);
            stop.setEnabled(false);
            break;
      }
   }

   public interface ControlTrackingListener
   {

      void start();

      void pause();

      void resume();

      void stop();

      void dismiss();

   }

   private final View.OnClickListener mLoggingControlListener = new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            int id = v.getId();
            switch (id)
            {
               case R.id.logcontrol_start:
                  mListener.start();
                  break;
               case R.id.logcontrol_pause:
                  mListener.pause();
                  break;
               case R.id.logcontrol_resume:
                  mListener.resume();
                  break;
               case R.id.logcontrol_stop:
                  mListener.stop();
                  break;
               default:
                  break;
            }
            mListener.dismiss();
         }
      };
}
