/*------------------------------------------------------------------------------
 **     Ident: Delivery Center Java
 **    Author: rene
 ** Copyright: (c) Dec 11, 2010 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced  
 ** Distributed Software Engineering |  or transmitted in any form or by any        
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the      
 ** 4131 NJ Vianen                   |  purpose, without the express written    
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 */
package nl.sogeti.android.gpstracker.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.widget.ProgressBar;

/**
 * ????
 *
 * @version $Id$
 * @author rene (c) Dec 11, 2010, Sogeti B.V.
 */
public class ProgressFilterInputStream extends FilterInputStream
{
   ProgressBar mProgressBar;
   long progress = 0;
   
   public ProgressFilterInputStream(InputStream in, ProgressBar bar)
   {
      super( in );
      mProgressBar = bar;
   }

   @Override
   public int read() throws IOException
   {
      int read = super.read();
      if( read >= 0 )
      {
         incrementProgressBy( 1 );
      }
      return read;
   }

   @Override
   public int read( byte[] buffer, int offset, int count ) throws IOException
   {
      int read = super.read( buffer, offset, count );
      if( read >= 0 )
      {
         incrementProgressBy( read );
      }
      return read;
   }
   
   private void incrementProgressBy( int add )
   {
      if( mProgressBar != null )
      {
         mProgressBar.incrementProgressBy( add );
      }
   }
   
}
