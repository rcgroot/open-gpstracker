package nl.sogeti.android.gpstracker.actions;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.utils.GpxCreator;
import nl.sogeti.android.gpstracker.actions.utils.KmzCreator;
import nl.sogeti.android.gpstracker.actions.utils.StatisticsCalulator;
import nl.sogeti.android.gpstracker.db.GPStracking.Tracks;
import nl.sogeti.android.gpstracker.util.UnitsI18n;
import nl.sogeti.android.gpstracker.viewer.LoggerMap;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class ShareTrack extends Activity
{

   protected static final int DIALOG_FILENAME = 11;
   protected static final int PROGRESS_STEPS = 10;
   private static final int DIALOG_INSTALL_TWIDROID = 34;
   private static final String TAG = "OGT.ShareTrack";

   private RemoteViews mContentView;
   private int barProgress = 0;
   private Notification mNotification;
   private NotificationManager mNotificationManager;

   private EditText mFileNameView;
   private EditText mTweetView;
   private Spinner mShareTypeSpinner;
   private Spinner mShareTargetSpinner;
   private Uri mTrackUri;
   private StatisticsCalulator calculator;
   private OnClickListener mTwidroidDialogListener = new DialogInterface.OnClickListener()
   {
      public void onClick( DialogInterface dialog, int which )
      {
         Uri twidroidUri = Uri.parse( "market://details?id=com.twidroid" );
         Intent getTwidroid = new Intent( Intent.ACTION_VIEW, twidroidUri );
         try
         {
            startActivity( getTwidroid );
         }
         catch (ActivityNotFoundException e)
         {
            twidroidUri = Uri.parse( "http://twidroid.com/download/" );
            getTwidroid = new Intent( Intent.ACTION_VIEW, twidroidUri );
            startActivity( getTwidroid );
         }
      }
   };

   @Override
   public void onCreate( Bundle savedInstanceState )
   {
      super.onCreate( savedInstanceState );
      setContentView( R.layout.sharedialog );

      mTrackUri = getIntent().getData();
      mFileNameView = (EditText) findViewById( R.id.fileNameField );
      mTweetView = (EditText) findViewById( R.id.tweetField );
      
      mShareTypeSpinner = (Spinner) findViewById( R.id.shareTypeSpinner );
      ArrayAdapter<CharSequence> shareTypeAdapter = ArrayAdapter.createFromResource( this, R.array.sharetype_choices, android.R.layout.simple_spinner_item );
      shareTypeAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
      mShareTypeSpinner.setAdapter( shareTypeAdapter );

      mShareTargetSpinner = (Spinner) findViewById( R.id.shareTargetSpinner );
      mShareTypeSpinner.setOnItemSelectedListener( new OnItemSelectedListener()
         {
            public void onItemSelected( AdapterView< ? > arg0, View arg1, int position, long arg3 )
            {
               switch( position )
               {
                  case 0: //KMZ
                     setXmlExportTargets();
                     mFileNameView.setVisibility( View.VISIBLE );
                     mTweetView.setVisibility( View.GONE );
                     break;
                  case 1: //GPX
                     setXmlExportTargets();
                     mFileNameView.setVisibility( View.VISIBLE );
                     mTweetView.setVisibility( View.GONE );
                     break;
                  case 2: //Line of text
                     setTextLineExportTargets();
                     mFileNameView.setVisibility( View.GONE );
                     mTweetView.setVisibility( View.VISIBLE );
                  default:
                     break;
               }
            }
            public void onNothingSelected( AdapterView< ? > arg0 )
            { /* NOOP */
            }
         } );

      setXmlExportTargets();

      calculator = new StatisticsCalulator( this, new UnitsI18n( this, null ) );
      mFileNameView.setText( createFileName() );
      mTweetView.setText( createTweetText() );

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

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onCreateDialog(int)
    */
   @Override
   protected Dialog onCreateDialog( int id )
   {
      Dialog dialog = null;
      Builder builder = null;
      switch( id )
      {
         case DIALOG_INSTALL_TWIDROID:
            builder = new AlertDialog.Builder( this );
            builder
               .setTitle( R.string.dialog_notwidroid )
               .setMessage( R.string.dialog_notwidroid_message )
               .setIcon( android.R.drawable.ic_dialog_alert )
               .setPositiveButton( R.string.btn_install, mTwidroidDialogListener )
               .setNegativeButton( R.string.btn_cancel, null );
            dialog = builder.create();
            return dialog;
         default:
            return super.onCreateDialog( id );
      }
   }

   private void setXmlExportTargets()
   {
      ArrayAdapter<CharSequence> shareTargetAdapter = ArrayAdapter.createFromResource( this, R.array.sharefiletarget_choices, android.R.layout.simple_spinner_item );
      shareTargetAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
      mShareTargetSpinner.setAdapter( shareTargetAdapter );
   }

   private void setTextLineExportTargets()
   {
      ArrayAdapter<CharSequence> shareTargetAdapter = ArrayAdapter.createFromResource( this, R.array.sharetexttarget_choices, android.R.layout.simple_spinner_item );
      shareTargetAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
      mShareTargetSpinner.setAdapter( shareTargetAdapter );
   }
   
   private void share()
   {
      String chosenFileName = mFileNameView.getText().toString();
      String textLine       = mTweetView.getText().toString();
      int type              = (int) mShareTypeSpinner.getSelectedItemId();
      int target            = (int) mShareTargetSpinner.getSelectedItemId();
      switch( type )
      {
         case 0: //KMZ
            Log.d( TAG, "share KMZ: "+chosenFileName );
            exportKmz( chosenFileName, target );
            ShareTrack.this.finish();
            break;
         case 1: //GPX
            Log.d( TAG, "share GPX: "+chosenFileName );
            exportGpx( chosenFileName, target );
            ShareTrack.this.finish();
            break;
         case 2: //Line of text
            exportTextLine( textLine, target );
         default:
            break;
      }
   }

   protected void exportKmz( String chosenFileName, int target )
   {
      EndJob endJob = null;
      switch( target )
      {
         case 1: 
            endJob = new EndJob()
            {
               public void shareFile( Uri fileUri, String contentType )
               {
                  sendFile( fileUri, getString( R.string.email_kmzbody ), contentType );
               }
            };
            break;
         case 2:
            endJob = new EndJob()
            {
               public void shareFile( Uri fileUri, String contentType )
               {
                  CharSequence text = "Saved "+fileUri+" of type "+contentType;
                  Toast toast = Toast.makeText( ShareTrack.this.getApplicationContext(), text, Toast.LENGTH_LONG );
                  toast.show();
               }
            };
            break;
         default:
            break;
      }
      KmzCreator kmzCreator = new KmzCreator( this, mTrackUri, chosenFileName, new ProgressMonitor( chosenFileName, endJob ) );
      kmzCreator.start();
   }

   protected void exportGpx( String chosenFileName, int target )
   {
      EndJob endJob = null;
      switch( target )
      {
         case 1: 
            endJob = new EndJob()
            {
               public void shareFile( Uri fileUri, String contentType )
               {
                  sendFile( fileUri, getString( R.string.email_gpxbody ), contentType );
               }
            };
            break;
         case 2:
            endJob = new EndJob()
            {
               public void shareFile( Uri fileUri, String contentType )
               {
                  CharSequence text = "Saved "+fileUri+" of type "+contentType;
                  Toast toast = Toast.makeText( ShareTrack.this.getApplicationContext(), text, Toast.LENGTH_LONG );
                  toast.show();
               }
            };
            break;
         default:
            break;
      }
      GpxCreator gpxCreator = new GpxCreator( this, mTrackUri, chosenFileName, new ProgressMonitor( chosenFileName, endJob ) );
      gpxCreator.start();
   }

   protected void exportTextLine( String message, int target )
   {
      String subject = "Open GPS Tracker";
      switch( target )
      {
         case 0:
            sendTwidroidTweet( message );
            break;
         case 1:
            sendSMS( message );
            ShareTrack.this.finish();
            break;
         case 2:
            sentGenericText( subject, message );
            ShareTrack.this.finish();
            break;
      }

   }

   private void sendTwidroidTweet( String tweet )
   {
      final Intent intent = new Intent( "com.twidroid.SendTweet" );
      intent.putExtra( "com.twidroid.extra.MESSAGE", tweet );
      intent.setType( "application/twitter" );
      try
      {
         startActivity( intent );
         ShareTrack.this.finish();
      }
      catch( ActivityNotFoundException e )
      {
         showDialog( DIALOG_INSTALL_TWIDROID );
      }
   }

   private void sendFile( Uri fileUri, String body, String contentType )
   {
      Intent sendActionIntent = new Intent( Intent.ACTION_SEND );
      sendActionIntent.putExtra( Intent.EXTRA_SUBJECT, getString( R.string.email_subject ) );
      sendActionIntent.putExtra( Intent.EXTRA_TEXT, body );
      sendActionIntent.putExtra( Intent.EXTRA_STREAM, fileUri );
      sendActionIntent.setType( contentType );
      startActivity( Intent.createChooser( sendActionIntent, getString( R.string.sender_chooser ) ) );
   }

   private void sendSMS( String msg )
   {
      final Intent intent = new Intent( Intent.ACTION_VIEW );
      intent.setType( "vnd.android-dir/mms-sms" );
      intent.putExtra( "sms_body", msg );
      startActivity( intent );
   }

   private void sentGenericText( String subject, String msg )
   {
      final Intent intent = new Intent( Intent.ACTION_SEND );
      intent.setType( "text/plain" );
      intent.putExtra( Intent.EXTRA_SUBJECT, subject );
      intent.putExtra( Intent.EXTRA_TEXT, msg );
      startActivity( intent );
   }

   private String createTweetText()
   {
      calculator.updateCalculations( mTrackUri );
      String name = createFileName();
      String distString = calculator.getDistanceText();
      String avgSpeed = calculator.getAvgSpeedText();
      String duration = calculator.getDurationText();
      return String.format( getString( R.string.tweettext, name, distString, avgSpeed, duration ) );
   }

   private String createFileName()
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
