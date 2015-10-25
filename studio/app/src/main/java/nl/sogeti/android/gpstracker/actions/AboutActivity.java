/*------------------------------------------------------------------------------
 **     Ident: Sogeti Smart Mobile Solutions
 **    Author: rene
 ** Copyright: (c) 2015 Sogeti Nederland B.V. All Rights Reserved.
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
package nl.sogeti.android.gpstracker.actions;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import nl.sogeti.android.gpstracker.BuildConfig;
import nl.sogeti.android.gpstracker.R;

import static nl.sogeti.android.gpstracker.BuildConfig.BUILD_NUMBER;
import static nl.sogeti.android.gpstracker.BuildConfig.GIT_COMMIT;

public class AboutActivity extends AppCompatActivity
{

   @Override
   protected void onResume()
   {
      super.onResume();
      FragmentManager fm = getSupportFragmentManager();
      AboutDialogFragment aboutDialog = new AboutDialogFragment();
      aboutDialog.setListener(this);
      aboutDialog.show(fm, "fragment_about_dialog");
   }

   public void onDismiss(AboutDialogFragment aboutDialogFragment)
   {
      finish();
   }

   public static class AboutDialogFragment extends DialogFragment
   {
      interface Listener
      {
         void onDismiss(AboutDialogFragment aboutDialogFragment);
      }

      AboutActivity listener;

      public void setListener(AboutActivity listener)
      {
         this.listener = listener;
      }

      @Nullable
      @Override
      public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
      {
         getDialog().setTitle(R.string.menu_about);

         View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_about, container, false);
         Button button = (Button) view.findViewById(R.id.button_ok);
         button.setOnClickListener(new View.OnClickListener()
         {
            @Override
            public void onClick(View v)
            {
               dismiss();
            }
         });
         WebView webView = (WebView) view.findViewById(R.id.fragment_about_webview);
         webView.loadUrl("file:///android_asset/about.html");
         TextView version = (TextView) view.findViewById(R.id.fragment_about_version);
         if (BUILD_NUMBER > 0)
         {
            String shortHash = GIT_COMMIT.substring(0, Math.min(GIT_COMMIT.length(), 7));
            version.setText(String.format("Version %s build %d", shortHash, BUILD_NUMBER));
         }
         else
         {
            version.setText(String.format("Version %s", BuildConfig.VERSION_NAME));
         }

         return view;
      }

      @Override
      public void onDismiss(DialogInterface dialog)
      {
         super.onDismiss(dialog);
         listener.onDismiss(this);
      }
   }
}