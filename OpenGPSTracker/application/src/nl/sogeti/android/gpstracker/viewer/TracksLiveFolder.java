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
package nl.sogeti.android.gpstracker.viewer;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.db.GPStracking;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.LiveFolders;

/**
 * Activity to build a data set to be shown in a live folder in a Android desktop
 * 
 * @version $Id$
 * @author rene (c) Mar 22, 2009, Sogeti B.V.
 */
public class TracksLiveFolder extends Activity
{
   @Override
   protected void onCreate( Bundle savedInstanceState )
   {
      this.setVisible( false );
      super.onCreate( savedInstanceState );

      final Intent intent = getIntent();
      final String action = intent.getAction();

      if( LiveFolders.ACTION_CREATE_LIVE_FOLDER.equals( action ) )
      {
         final Intent baseAction = new Intent( Intent.ACTION_VIEW, GPStracking.Tracks.CONTENT_URI );
         
         Uri liveData = Uri.withAppendedPath( GPStracking.CONTENT_URI, "live_folders/tracks" );
         final Intent createLiveFolder = new Intent();
         createLiveFolder.setData( liveData );
         createLiveFolder.putExtra( LiveFolders.EXTRA_LIVE_FOLDER_NAME, getString(R.string.track_list) );
         createLiveFolder.putExtra( LiveFolders.EXTRA_LIVE_FOLDER_ICON, Intent.ShortcutIconResource.fromContext( this, R.drawable.live ) );
         createLiveFolder.putExtra( LiveFolders.EXTRA_LIVE_FOLDER_DISPLAY_MODE, LiveFolders.DISPLAY_MODE_LIST );
         createLiveFolder.putExtra( LiveFolders.EXTRA_LIVE_FOLDER_BASE_INTENT, baseAction );
         setResult( RESULT_OK, createLiveFolder );
      }
      else
      {
         setResult( RESULT_CANCELED );
      }
      finish();
   }
}
