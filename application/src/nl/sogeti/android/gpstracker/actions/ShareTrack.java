package nl.sogeti.android.gpstracker.actions;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.utils.GpxCreator;
import nl.sogeti.android.gpstracker.actions.utils.KmzCreator;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.viewer.LoggerMap;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.Spinner;

public class ShareTrack extends Activity
{

   protected static final int DIALOG_FILENAME = 11;
   protected static final int PROGRESS_STEPS = 10;

   private RemoteViews mContentView;
   private int barProgress = 0;
   private Notification mNotification;
   private NotificationManager mNotificationManager;

   private EditText mFileNameView;
   private Spinner mShareTypeSpinner;
   private Spinner mShareTargetSpinner;
   private Uri mTrackUri;

   @Override
   public void onCreate( Bundle savedInstanceState )
   {
      super.onCreate( savedInstanceState );
      setContentView( R.layout.sharedialog );

      mTrackUri = getIntent().getData();
      mFileNameView = (EditText) findViewById( R.id.fileNameField );
      mFileNameView.setText( resolveTrackName() );

      mShareTypeSpinner = (Spinner) findViewById( R.id.shareTypeSpinner );
      ArrayAdapter<CharSequence> shareTypeAdapter = ArrayAdapter.createFromResource( this, R.array.sharetype_choices, android.R.layout.simple_spinner_item );
      shareTypeAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
      mShareTypeSpinner.setAdapter( shareTypeAdapter );

      mShareTargetSpinner = (Spinner) findViewById( R.id.shareTargetSpinner );
      ArrayAdapter<CharSequence> shareTargetAdapter = ArrayAdapter.createFromResource( this, R.array.sharefiletarget_choices, android.R.layout.simple_spinner_item );
      shareTargetAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
      mShareTargetSpinner.setAdapter( shareTargetAdapter );

      Button okay = (Button) findViewById( R.id.okayshare_button );
      okay.setOnClickListener( new View.OnClickListener()
      {
         public void onClick( View v )
         {
            share();
         }
      } );

      Button cancel = (Button) findViewById( R.id.cancelshare_button );
      cancel.setOnClickListener( new View.OnClickListener()
         {
            public void onClick( View v )
            {
               ShareTrack.this.finish();
            }
         } );
   }

   private void share()
   {
      String chosenFileName = mFileNameView.getText().toString();
      int type = (int) mShareTypeSpinner.getSelectedItemId();
      int target = (int) mShareTargetSpinner.getSelectedItemId();
      switch( type ) 
      {
         case 0: //KMZ
            exportKmz( chosenFileName, target );
            ShareTrack.this.finish();
            break;
         case 1: //GPX
            exportGpx( chosenFileName, target );
            ShareTrack.this.finish();
            break;
         default:
            break;
      }
   }

   protected void exportGpx( String chosenFileName, int target )
   {
      EndJob endJob = null;
      if( target == 1 )
      {
         endJob = new EndJob()
            {
               public void shareFile( Uri fileUri, String contentType )
               {
                  sendFile( fileUri, getString( R.string.email_gpxbody ), contentType );
               }
            };
      }
      GpxCreator mGpxCreator = new GpxCreator( this, mTrackUri, chosenFileName, new ProgressMonitor( chosenFileName, endJob ) );
      mGpxCreator.start();
   }

   protected void exportKmz( String chosenFileName, int target )
   {
      EndJob endJob = null;
      if( target == 1 )
      {
         endJob = new EndJob()
            {
               public void shareFile( Uri fileUri, String contentType )
               {
                  sendFile( fileUri, getString( R.string.email_kmzbody ), contentType );
               }
            };
      }
      KmzCreator mKmzCreator = new KmzCreator( this, mTrackUri, chosenFileName, new ProgressMonitor( chosenFileName, endJob ) );
      mKmzCreator.start();
   }

   public void sendFile( Uri fileUri, String body, String contentType )
   {
      Intent sendActionIntent = new Intent( Intent.ACTION_SEND );
      sendActionIntent.putExtra( Intent.EXTRA_SUBJECT, getString( R.string.email_subject ) );
      sendActionIntent.putExtra( Intent.EXTRA_TEXT, body );
      sendActionIntent.putExtra( Intent.EXTRA_STREAM, fileUri );
      sendActionIntent.setType( contentType );
      startActivity( Intent.createChooser( sendActionIntent, getString( R.string.sender_chooser ) ) );
   }

   private String resolveTrackName()
   {
      ContentResolver resolver = getContentResolver();
      Cursor trackCursor = null;
      String name = null;

      try
      {
         trackCursor = resolver.query( mTrackUri, new String[] { Tracks.NAME }, null, null, null );
         if( trackCursor.moveToFirst() )
         {
            name = trackCursor.getString( 0 );
         }
      }
      finally
      {
         if( trackCursor != null )
         {
            trackCursor.close();
         }
      }
      return name;
   }

   public class ProgressMonitor
   {
      private String mFileName;
      private EndJob mEndJob;

      public ProgressMonitor(String sharename, EndJob endJob)
      {
         mFileName = sharename;
         mEndJob = endJob;
      }

      public void startNotification()
      {
         String ns = Context.NOTIFICATION_SERVICE;
         mNotificationManager = (NotificationManager) ShareTrack.this.getSystemService( ns );
         int icon = android.R.drawable.ic_menu_save;
         CharSequence tickerText = getString( R.string.ticker_saving ) + "\"" + mFileName + "\"";

         mNotification = new Notification();
         PendingIntent contentIntent = PendingIntent.getActivity( ShareTrack.this, 0, new Intent( ShareTrack.this, LoggerMap.class ).setFlags( Intent.FLAG_ACTIVITY_NEW_TASK ),
               PendingIntent.FLAG_UPDATE_CURRENT );

         mNotification.contentIntent = contentIntent;
         mNotification.tickerText = tickerText;
         mNotification.icon = icon;
         mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
         mContentView = new RemoteViews( getPackageName(), R.layout.savenotificationprogress );
         mContentView.setImageViewResource( R.id.icon, icon );
         mContentView.setTextViewText( R.id.progresstext, tickerText );

         mNotification.contentView = mContentView;
      }

      public void updateNotification( int progress, int goal )
      {
         //         Log.d( "TAG", "Progress " + progress + " of " + goal );
         if( progress > 0 && progress < goal )
         {
            if( ( progress * PROGRESS_STEPS ) / goal != barProgress )
            {
               barProgress = ( progress * PROGRESS_STEPS ) / goal;
               mContentView.setProgressBar( R.id.progress, goal, progress, false );
               mNotificationManager.notify( R.layout.savenotificationprogress, mNotification );
            }
         }
         else if( progress == 0 )
         {
            mContentView.setProgressBar( R.id.progress, goal, progress, true );
            mNotificationManager.notify( R.layout.savenotificationprogress, mNotification );
         }
         else if( progress >= goal )
         {
            mContentView.setProgressBar( R.id.progress, goal, progress, false );
            mNotificationManager.notify( R.layout.savenotificationprogress, mNotification );
         }
      }

      public void endNotification( Uri file, String contentType )
      {
         mNotificationManager.cancel( R.layout.savenotificationprogress );
         if( mEndJob != null )
         {
            mEndJob.shareFile( file, contentType );
         }
      }
   }
   
   interface EndJob
   {
      void shareFile( Uri fileUri, String contentType );
   }
}
