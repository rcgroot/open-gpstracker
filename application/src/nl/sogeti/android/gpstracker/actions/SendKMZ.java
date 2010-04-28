/*------------------------------------------------------------------------------
 **     Ident: Innovation en Inspiration > Google Android 
 **    Author: rene
 ** Copyright: (c) Jan 22, 2009 Sogeti Nederland B.V. All Rights Reserved.
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

import nl.sogeti.android.gpstracker.R;
import nl.sogeti.android.gpstracker.actions.utils.KmzCreator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Send a GPX file with Android SEND Intent
 * 
 * @version $Id$
 * @author rene (c) Mar 22, 2009, Sogeti B.V.
 */
public class SendKMZ extends SendTrack
{
   protected static final String TAG = "OGT.SendKMZ";
   private KmzCreator mKmzCreator;
   
   @Override
   public void onCreate( Bundle savedInstanceState )
   {
      setVisible( false );
      super.onCreate( savedInstanceState );
      showDialog( DIALOG_FILENAME );
   }
   
   

   protected void exportGPX( String chosenFileName )
   {
      mKmzCreator = new KmzCreator( this, getIntent(), chosenFileName, new ProgressListener() );
      mKmzCreator.start();
      this.finish();
   }

   @Override
   public void sendFile( String filename )
   {
      Intent sendActionIntent = new Intent(Intent.ACTION_SEND);
      sendActionIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_body) ); 
      sendActionIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject) );
      sendActionIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+filename)); 
      sendActionIntent.setType( mKmzCreator.getContentType() );
      startActivity(Intent.createChooser(sendActionIntent, getString(R.string.sender_chooser) )); 
   }
}