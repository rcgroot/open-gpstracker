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
