/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Nov 4, 2012 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 */
package nl.sogeti.android.gpstracker.fragment;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.viewer.map.LoggerMapHelper;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * ????
 * 
 * @version $Id:$
 * @author rene (c) Nov 4, 2012, Sogeti B.V.
 */
public abstract class AbstractLoggerFragment extends Fragment implements LoggerMapFragment
{

   protected LoggerMapHelper mHelper;
   private TextView[] mSpeedtexts;
   private TextView mLastGPSSpeedView;
   private TextView mLastGPSAltitudeView;
   private TextView mDistanceView;
   private View mSpeedbar;

   /**
    * Constructor: create a new AbstractLoggerFragment.
    */
   public AbstractLoggerFragment()
   {
   }

   public void didCreateView(View v, Bundle savedInstanceState)
   {
      TextView[] speeds = { (TextView) v.findViewById(R.id.speedview05), (TextView) v.findViewById(R.id.speedview04), (TextView) v.findViewById(R.id.speedview03),
            (TextView) v.findViewById(R.id.speedview02), (TextView) v.findViewById(R.id.speedview01), (TextView) v.findViewById(R.id.speedview00) };
      mSpeedtexts = speeds;
      mLastGPSSpeedView = (TextView) v.findViewById(R.id.currentSpeed);
      mLastGPSAltitudeView = (TextView) v.findViewById(R.id.currentAltitude);
      mDistanceView = (TextView) v.findViewById(R.id.currentDistance);
      mSpeedbar =  v.findViewById(R.id.speedbar);
      mHelper.onCreateView();
   }

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      mHelper = new LoggerMapHelper(this);
      mHelper.onCreate(savedInstanceState);
   }

   @Override
   public void onResume()
   {
      super.onResume();
      mHelper.onResume();
   }

   @Override
   public void onPause()
   {
      mHelper.onPause();
      super.onPause();
   }

   @Override
   public void onStop()
   {
      super.onStop();
   }
   
   @Override
   public void onSaveInstanceState(Bundle save)
   {
      super.onSaveInstanceState(save);
      mHelper.onSaveInstanceState(save);
   }
   
   @Override
   public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
   {
      super.onCreateOptionsMenu(menu, inflater);
      mHelper.onCreateOptionsMenu(menu);
   }
   
   @Override
   public void onPrepareOptionsMenu(Menu menu)
   {
      mHelper.onPrepareOptionsMenu(menu);
      super.onPrepareOptionsMenu(menu);
   }
   
   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      boolean handled = mHelper.onOptionsItemSelected(item);
      if( !handled )
      {
         handled = super.onOptionsItemSelected(item);
      }
      return handled;
   }
   
   @Override
   public TextView[] getSpeedTextViews()
   {
      return mSpeedtexts;
   }

   @Override
   public TextView getAltitideTextView()
   {
      return mLastGPSAltitudeView;
   }

   @Override
   public TextView getSpeedTextView()
   {
      return mLastGPSSpeedView;
   }

   @Override
   public TextView getDistanceTextView()
   {
      return mDistanceView;
   }

   @Override
   public View getSpeedbar()
   {
      return mSpeedbar;
   }
}
