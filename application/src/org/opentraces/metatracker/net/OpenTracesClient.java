/*
 * Copyright (C) 2010  Just Objects B.V.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opentraces.metatracker.net;

import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.opentraces.metatracker.xml.DOMUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic OpenTraces client using XML over HTTP.
 * <p/>
 * Use this class within Android HTTP clients.
 *
 * @author $Author: Just van den Broecke$
 * @version $Revision: 3043 $ $Id: HTTPClient.java 3043 2009-01-17 16:25:21Z just $
 * @see
 */
public class OpenTracesClient extends Protocol
{
	private static final String LOG_TAG = "MT.OpenTracesClient";

	/**
	 * Default KW session timeout (minutes).
	 */
	public static final int DEFAULT_TIMEOUT_MINS = 5;

	/**
	 * Full KW protocol URL.
	 */
	private String protocolURL;

	/**
	 * Debug flag for verbose output.
	 */
	private boolean debug;

	/**
	 * Key gotten on login ack
	 */
	private String agentKey;

	/**
	 * Keyworx session timeout (minutes).
	 */
	private int timeout;

	/**
	 * Saved login request for session restore on timeout.
	 */
	private Element loginRequest;

	/**
	 * Constructor with full protocol URL e.g. http://www.bla.com/proto.srv.
	 */
	public OpenTracesClient(String aProtocolURL)
	{
		this(aProtocolURL, DEFAULT_TIMEOUT_MINS);
	}

	/**
	 * Constructor with protocol URL and timeout.
	 */
	public OpenTracesClient(String aProtocolURL, int aTimeout)
	{
		protocolURL = aProtocolURL;
		if (!protocolURL.endsWith("/proto.srv")) {
			protocolURL += "/proto.srv";		
		}
		timeout = aTimeout;
	}

	/**
	 * Create session.
	 */
	synchronized public Element createSession() throws ClientException
	{
		agentKey = null;

		// Create XML request
		Element request = createRequest(SERVICE_SESSION_CREATE);

		// Execute request
		Element response = doRequest(request);
		handleResponse(request, response);
		throwOnNrsp(response);
		// Returns positive response
		return response;
	}

	public String getAgentKey()
	{
		return agentKey;
	}

	public boolean hasSession()
	{
		return agentKey != null;
	}

	public boolean isLoggedIn()
	{
		return hasSession() && loginRequest != null;
	}

	/**
	 * Login on portal.
	 */
	synchronized public Element login(String aName, String aPassword) throws ClientException
	{
		// Create XML request
		Element request = createLoginRequest(aName, aPassword);

		// Execute request
		Element response = doRequest(request);

		// Filter session-related attrs
		handleResponse(request, response);

		throwOnNrsp(response);

		// Returns positive response
		return response;
	}

	/**
	 * Keep alive service.
	 */
	synchronized public Element ping() throws ClientException
	{
		return service(createRequest(SERVICE_SESSION_PING));
	}

	/**
	 * perform TWorx service request.
	 */
	synchronized public Element service(Element request) throws ClientException
	{
		throwOnInvalidSession();

		// Execute request
		Element response = doRequest(request);

		// Check for session timeout
		response = redoRequestOnSessionTimeout(request, response);

		// Throw exception on negative response
		throwOnNrsp(response);

		// Positive response: return wrapped handler response
		return response;
	}

	/**
	 * perform TWorx multi-service request.
	 */
	synchronized public List<Element> service(List<Element> requests, boolean stopOnError) throws ClientException
	{
		// We don't need a valid session as one of the requests
		// may be a login or create-session request.

		// Create multi-req request with individual requests as children
		Element request = createRequest(SERVICE_MULTI_REQ);
		request.setAttribute(ATTR_STOPONERROR, stopOnError + "");

		for (Element req : requests)
		{
			request.appendChild(req);
		}

		// Execute request
		Element response = doRequest(request);

		// Check for session timeout
		response = redoRequestOnSessionTimeout(request, response);

		// Throw exception on negative response
		throwOnNrsp(response);

		// Filter child responses for session-based responses
		NodeList responseList = response.getChildNodes();
		List<Element> responses = new ArrayList(responseList.getLength());

		for (int i = 0; i < responseList.getLength(); i++)
		{
			handleResponse(requests.get(i), (Element) responseList.item(i));
			responses.add((Element) responseList.item(i));
		}

		// Positive multi-req response: return child responses
		return responses;
	}

	/**
	 * Logout from portal.
	 */
	synchronized public Element logout() throws ClientException
	{
		throwOnInvalidSession();

		// Create XML request
		Element request = createRequest(SERVICE_LOGOUT);

		// Execute request
		Element response = doRequest(request);

		handleResponse(request, response);

		// Throw exception or return positive response
		// throwOnNrsp(response);
		return response;
	}

	/*
	http://brainflush.wordpress.com/2008/10/17/talking-to-web-servers-via-http-in-android-10/
	 */

	public void uploadFile(String fileName)
	{
		try
		{
			DefaultHttpClient httpclient = new DefaultHttpClient();
			File f = new File(fileName);

			HttpPost httpost = new HttpPost("http://local.geotracing.com/tland/media.srv");
			MultipartEntity entity = new MultipartEntity();
			entity.addPart("myIdentifier", new StringBody("somevalue"));
			entity.addPart("myFile", new FileBody(f));
			httpost.setEntity(entity);

			HttpResponse response;

			response = httpclient.execute(httpost);

			Log.d(LOG_TAG, "Upload result: " + response.getStatusLine());

			if (entity != null)
			{
				entity.consumeContent();
			}

			httpclient.getConnectionManager().shutdown();

		} catch (Throwable ex)
		{
			Log.d(LOG_TAG, "Upload failed: " + ex.getMessage() + " Stacktrace: " + ex.getStackTrace());
		}

	}

	public void setDebug(boolean b)
	{
		debug = b;
	}

	/**
	 * Filter responses for session-related requests.
	 */
	private void handleResponse(Element request, Element response) throws ClientException
	{
		if (isNegativeResponse(response))
		{
			return;
		}

		String service = Protocol.getServiceName(request.getTagName());

		if (service.equals(SERVICE_LOGIN))
		{
			// Save for later session restore
			loginRequest = request;

			// Save session key
			agentKey = response.getAttribute(ATTR_AGENTKEY);
//			if (response.hasAttribute(ATTR_TIME)) {
//				DateTimeUtils.setTime(response.getLongAttr(ATTR_TIME));
//			}
		} else if (service.equals(SERVICE_SESSION_CREATE))
		{
			// Save session key
			agentKey = response.getAttribute(ATTR_AGENTKEY);

		} else if (service.equals(SERVICE_LOGOUT))
		{
			loginRequest = null;
			agentKey = null;
		}
	}

	/**
	 * Throw exception on negative protocol response.
	 */
	private void throwOnNrsp(Element anElement) throws ClientException
	{
		if (isNegativeResponse(anElement))
		{
			String details = "no details";
			if (anElement.hasAttribute(ATTR_DETAILS))
			{
				details = anElement.getAttribute(ATTR_DETAILS);
			}
			throw new ClientException(Integer.parseInt(anElement.getAttribute(ATTR_ERRORID)),
					anElement.getAttribute(ATTR_ERROR), details);
		}
	}

	/**
	 * Throw exception when not logged in.
	 */
	private void throwOnInvalidSession() throws ClientException
	{
		if (agentKey == null)
		{
			throw new ClientException("Invalid tworx session");
		}
	}

	/**
	 * .
	 */
	private Element redoRequestOnSessionTimeout(Element request, Element response) throws ClientException
	{
		// Check for session timeout
		if (isNegativeResponse(response) && Integer.parseInt(response.getAttribute(ATTR_ERRORID)) == Protocol.ERR4007_AGENT_LEASE_EXPIRED)
		{
			p("Reestablishing session...");

			// Reset session
			agentKey = null;

			// Do login if already logged in
			if (loginRequest != null)
			{
				response = doRequest(loginRequest);
				throwOnNrsp(response);
			} else
			{
				response = createSession();
				throwOnNrsp(response);
			}

			// Save session key
			agentKey = response.getAttribute(ATTR_AGENTKEY);

			// Re-issue service request and return new response
			return doRequest(request);
		}

		// No session timeout so same response
		return response;
	}

	/**
	 * Do XML over HTTP request and retun response.
	 */
	private Element doRequest(Element anElement) throws ClientException
	{

		// Create URL to use
		String url = protocolURL;
		if (agentKey != null)
		{
			url = url + "?agentkey=" + agentKey;
		} else
		{
			// Must be login
			url = url + "?timeout=" + timeout;
		}
		p("doRequest: " + url + " req=" + anElement.getTagName());

		// Perform request/response
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(url);


		Element replyElement = null;

		try
		{
			// Make sure the server knows what kind of a response we will accept
			httpPost.addHeader("Accept", "text/xml");

			// Also be sure to tell the server what kind of content we are sending
			httpPost.addHeader("Content-Type", "application/xml");

			String xmlString = DOMUtil.dom2String(anElement.getOwnerDocument());

			StringEntity entity = new StringEntity(xmlString, "UTF-8");
			entity.setContentType("application/xml");
			httpPost.setEntity(entity);

			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httpPost);

			// Parse response
			Document replyDoc = DOMUtil.parse(response.getEntity().getContent());
			replyElement = replyDoc.getDocumentElement();
			p("doRequest: rsp=" + replyElement.getTagName());
		}
		catch (Throwable t)
		{
			throw new ClientException("Error in doRequest: " + t);
		}
		finally
		{

		}

		return replyElement;

	}


	/**
	 * Util: print.
	 */
	private void p(String s)
	{
		if (debug)
		{
			Log.d(LOG_TAG, s);
		}
	}

	/**
	 * Util: warn.
	 */
	private void warn(String s)
	{
		warn(s, null);
	}

	/**
	 * Util: warn with exception.
	 */
	private void warn(String s, Throwable t)
	{
		Log.e(LOG_TAG, s + " ex=" + t, t);
	}


}

