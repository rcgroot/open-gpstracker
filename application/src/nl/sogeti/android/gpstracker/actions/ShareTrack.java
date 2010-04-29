package nl.sogeti.android.gpstracker.actions;

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.utils.GpxCreator;
import nl.sogeti.android.gpstracker.actions.utils.KmzCreator;
import nl.sogeti.android.gpstracker.viewer.LoggerMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RemoteViews;

public class ShareTrack extends Activity
{

   protected static final int DIALOG_FILENAME = 11;   
   protected static final int PROGRESS_STEPS = 10;

   private RemoteViews mContentView;
   private int barProgress = 0;
   private Notification mNotification;
   private NotificationManager mNotificationManager;


   private EditText mFileNameView;
   private OnClickListener mOnClickListener = new OnClickListener()
      {
         public void onClick( DialogInterface dialog, int which )
         {
            switch( which )
            {
               case Dialog.BUTTON_POSITIVE:
//                  ShareTrack.this.sendFile( mFileNameView.getText().toString() );
                  break;
               case Dialog.BUTTON_NEGATIVE:
                  ShareTrack.this.finish();
                  break;
            }
         }
      };

   @Override
   public void onCreate( Bundle savedInstanceState )
   {
      setVisible( false );
      super.onCreate( savedInstanceState );
      setContentView(  R.layout.filenamedialog );
      mFileNameView = (EditText) findViewById( R.id.fileNameField );
      //showDialog( DIALOG_FILENAME );
   }

   /*
    * (non-Javadoc)
    * @see android.app.Activity#onCreateDialog(int)
    */
   @Override
   protected Dialog onCreateDialog( int id )
   {
      Builder builder;
      switch( id )
      {
         case DIALOG_FILENAME :
            LayoutInflater factory = LayoutInflater.from( this );
            View view = factory.inflate( R.layout.filenamedialog, null );
            
            builder = new AlertDialog.Builder( this )
                           .setTitle( R.string.dialog_filename_title )
                           .setMessage( R.string.dialog_filename_message )
                           .setIcon( android.R.drawable.ic_dialog_alert )
                           .setView( view ).
                           setPositiveButton( R.string.btn_okay, mOnClickListener ).
                           setNegativeButton( R.string.btn_cancel, mOnClickListener );
            Dialog dialog = builder.create();
            dialog.setOwnerActivity( this );
            return dialog;
         default:
            return super.onCreateDialog( id );
      }
   }
   

   protected void exportGpx( String chosenFileName )
   {
      GpxCreator mGpxCreator = new GpxCreator( this, getIntent().getData(), chosenFileName, new ProgressListener() );
      mGpxCreator.start();
      this.finish();
   }
   
   protected void exportKmz( String chosenFileName )
   {
      KmzCreator mKmzCreator = new KmzCreator( this, getIntent().getData(), chosenFileName, new ProgressListener() );
      mKmzCreator.start();
      this.finish();
   }

   public void sendFile( String filePath, String contentType )
   {
      Intent sendActionIntent = new Intent(Intent.ACTION_SEND);
      sendActionIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_body) ); 
      sendActionIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject) );
      sendActionIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+filePath)); 
      sendActionIntent.setType( contentType );
      startActivity(Intent.createChooser(sendActionIntent, getString(R.string.sender_chooser) )); 
   }
   

   public class ProgressListener
   {
      public void startNotification( String fileName )
      {
         String ns = Context.NOTIFICATION_SERVICE;
         mNotificationManager = (NotificationManager) ShareTrack.this.getSystemService( ns );
         int icon = android.R.drawable.ic_menu_save;
         CharSequence tickerText = getString( R.string.ticker_saving )+ "\"" + fileName + "\"";
       
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
      public void endNotification( String filename )
      {
         mNotificationManager.cancel( R.layout.savenotificationprogress );
         //TODO Do it sendFile( filename );
      }
   }
}
