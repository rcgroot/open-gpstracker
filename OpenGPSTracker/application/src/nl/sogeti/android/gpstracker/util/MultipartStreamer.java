package nl.sogeti.android.gpstracker.util;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URLConnection;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import android.util.Log;

/**
 * This utility class provides an abstraction layer for sending multipart HTTP POST requests to a web server.
 * 
 * @author www.codejava.net
 */
public class MultipartStreamer implements Closeable
{
   private final String boundary;
   private static final String LINE_FEED = "\r\n";
   private static final String TAG = "MultipartStreamer";
   private String charset;
   private OutputStream outputStream;
   private PrintWriter writer;
   private HttpMultipartMode mode;
   private boolean flushed = false;

   /**
    * This constructor initializes a new HTTP POST request with content type is set to multipart/form-data
    * 
    * @param requestURL
    * @param charset
    * @throws IOException
    * @throws OAuthCommunicationException
    * @throws OAuthExpectationFailedException
    * @throws OAuthMessageSignerException
    */
   public MultipartStreamer(HttpURLConnection httpConnection, HttpMultipartMode mode, OAuthConsumer mConsumer) throws IOException, OAuthMessageSignerException, OAuthExpectationFailedException,
         OAuthCommunicationException
   {
      this.mode = mode;
      this.charset = "UTF-8";
      httpConnection.setDoOutput(true);
      httpConnection.setRequestMethod("POST");

      // creates a unique boundary based on time stamp
      boundary = "===" + System.currentTimeMillis() + "===";
      httpConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
      if (mConsumer != null)
      {
         mConsumer.sign(httpConnection);
      }
      outputStream = httpConnection.getOutputStream();
      writer = new PrintWriter(new OutputStreamWriter(outputStream, charset), true);
   }

   public MultipartStreamer(HttpURLConnection httpConnection, HttpMultipartMode mode) throws IOException
   {
      this.mode = mode;
      this.charset = "UTF-8";
      httpConnection.setDoOutput(true);

      // creates a unique boundary based on time stamp
      boundary = "===" + System.currentTimeMillis() + "===";
      httpConnection.setDoOutput(true); // indicates POST method
      httpConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
      outputStream = httpConnection.getOutputStream();
      writer = new PrintWriter(new OutputStreamWriter(outputStream, charset), true);
   }

   /**
    * Adds a form field to the request
    * 
    * @param name field name
    * @param value field value
    */
   public void addFormField(String name, String value)
   {
      writer.append("--" + boundary).append(LINE_FEED);
      writer.append("Content-Disposition: form-data; name=\"" + name + "\"").append(LINE_FEED);
      if (mode == HttpMultipartMode.STRICT)
      {
         writer.append("Content-Type: text/plain; charset=" + charset).append(LINE_FEED);
      }
      writer.append(LINE_FEED);
      writer.append(value).append(LINE_FEED);
      writer.flush();
   }

   /**
    * Adds a upload file section to the request
    * 
    * @param fieldName
    * @param file
    * @throws IOException
    */
   public void addFilePart(String fieldName, File file) throws IOException
   {
      FileInputStream fileStream = null;
      try
      {
         fileStream = new FileInputStream(file);
         addFilePart(fieldName, file.getName(), fileStream);
      }
      finally
      {
         close(fileStream);
      }
   }

   /**
    * Adds a upload file section to the request
    * 
    * @param fieldName name attribute in <input type="file" name="..." />
    * @paran fileName name to be given as filename to the stream
    * @param inputStream stream of data to upload
    * @throws IOException
    */
   public void addFilePart(String fieldName, String fileName, InputStream inputStream) throws IOException
   {
      writer.append("--" + boundary).append(LINE_FEED);
      writer.append("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"").append(LINE_FEED);
      if (mode == HttpMultipartMode.STRICT)
      {
         writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(fileName)).append(LINE_FEED);
         writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
      }
      writer.append(LINE_FEED);
      writer.flush();

      byte[] buffer = new byte[4096];
      int bytesRead = -1;
      BufferedInputStream bis = new BufferedInputStream(inputStream);
      while ((bytesRead = bis.read(buffer)) != -1)
      {
         outputStream.write(buffer, 0, bytesRead);
      }
      outputStream.flush();
      bis.close();

      writer.append(LINE_FEED);
      writer.flush();
   }

   public void flush()
   {
      writer.append("--" + boundary + "--").append(LINE_FEED).flush();
      writer.close();
      flushed = true;
   }

   @Override
   public void close() throws IOException
   {
      if (!flushed)
      {
         flush();
         writer.close();
      }
   }

   private void close(Closeable connection)
   {
      try
      {
         if (connection != null)
         {
            connection.close();
         }
      }
      catch (IOException e)
      {
         Log.w(TAG, "Failed to close ", e);
      }
   }

   public static enum HttpMultipartMode
   {
      BROWSER_COMPATIBLE, STRICT
   }

   @Override
   protected void finalize() throws Throwable
   {
      super.finalize();
      if (!flushed)
      {
         Log.e(TAG, "Unflushed mime body garbage collected");
      }
   }
}
