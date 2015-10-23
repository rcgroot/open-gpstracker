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
package nl.sogeti.android.gpstracker.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import nl.sogeti.android.gpstracker.actions.tasks.GpxParser;
import nl.sogeti.android.gpstracker.actions.tasks.GpxParser.ProgressAdmin;

/**
 * ????
 *
 * @version $Id$
 * @author rene (c) Dec 11, 2010, Sogeti B.V.
 */
public class ProgressFilterInputStream extends FilterInputStream
{
   GpxParser mAsyncTask;
   long progress = 0;
   private ProgressAdmin mProgressAdmin;

   public ProgressFilterInputStream(InputStream is, ProgressAdmin progressAdmin)
   {
      super( is );
      mProgressAdmin = progressAdmin;
   }

   @Override
   public int read() throws IOException
   {
      int read = super.read();
      incrementProgressBy( 1 );
      return read;
   }

   @Override
   public int read( byte[] buffer, int offset, int count ) throws IOException
   {
      int read = super.read( buffer, offset, count );
      incrementProgressBy( read );
      return read;
   }   
   
   private void incrementProgressBy( int bytes )
   {
      if( bytes > 0 )
      {
         mProgressAdmin.addBytesProgress(bytes);
      }
   }
   
}
