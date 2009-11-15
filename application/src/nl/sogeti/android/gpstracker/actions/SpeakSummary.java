package nl.sogeti.android.gpstracker.actions;

import java.util.Timer;

import nl.sogeti.android.gpstracker.R;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;

public class SpeakSummary extends Activity implements TextToSpeech.OnInitListener
{
   private static final int MY_DATA_CHECK_CODE = 0;
   private Timer timer;
   private TextToSpeech mTts;
   private Uri mTrackUri;
   private final ContentObserver mTrackObserver = new ContentObserver( new Handler() )
      {
         @Override
         public void onChange( boolean selfUpdate )
         {
            SpeakSummary.this.updateSummary();
         }
      };

   @Override
   protected void onCreate( Bundle load )
   {
      super.onCreate( load );
      setContentView( R.layout.speaker );
      this.mTrackUri = this.getIntent().getData();

      ContentResolver resolver = this.getApplicationContext().getContentResolver();
      resolver.registerContentObserver( mTrackUri, false, this.mTrackObserver );

      Intent checkIntent = new Intent();
      checkIntent.setAction( TextToSpeech.Engine.ACTION_CHECK_TTS_DATA );
      startActivityForResult( checkIntent, MY_DATA_CHECK_CODE );

   }

   protected void updateSummary()
   {
      // TODO Auto-generated method stub

   }

   protected void onActivityResult( int requestCode, int resultCode, Intent data )
   {
      if( requestCode == MY_DATA_CHECK_CODE )
      {
         if( resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS )
         {
            // success, create the TTS instance
            mTts = new TextToSpeech( this, this );
         }
         else
         {
            // missing data, install it
            Intent installIntent = new Intent();
            installIntent.setAction( TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA );
            startActivity( installIntent );
         }
      }
   }

   public void onInit( int status )
   {
      // TODO Auto-generated method stub
   }
}
