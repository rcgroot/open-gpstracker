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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Constants and utilities for the KW protocol.
 *
 * @author $Author: $
 * @version $Id: Protocol.java,v 1.2 2005/07/22 22:25:20 just Exp $
 */
public class Protocol
{
	/**
	 * AMUSE Protocol version.
	 */
	public static final String PROTOCOL_VERSION = "4.0";

	/**
	 * Postfixes
	 */
	public static final String POSTFIX_REQ = "-req";
	public static final String POSTFIX_RSP = "-rsp";
	public static final String POSTFIX_NRSP = "-nrsp";
	public static final String POSTFIX_IND = "-ind";

	/**
	 * Service id's
	 */
	public static final String SERVICE_SESSION_CREATE = "ses-create";
	public static final String SERVICE_LOGIN = "ses-login";
	public static final String SERVICE_LOGOUT = "ses-logout";
	public static final String SERVICE_SESSION_PING = "ses-ping";
	public static final String SERVICE_QUERY_STORE = "query-store";

	public static final String SERVICE_MAP_AVAIL = "map-avail";
	public static final String SERVICE_MULTI_REQ = "multi-req";


	/**
	 * Common Attributes *
	 */
	public static final String ATTR_DEVID = "devid";
	public static final String ATTR_USER = "user";
	public static final String ATTR_AGENT = "agent";
	public static final String ATTR_AGENTKEY = "agentkey";

	public static final String ATTR_SECTIONS = "sections";

	public static final String ATTR_ID = "id";
	public static final String ATTR_CMD = "cmd";
	public static final String ATTR_ERROR = "error";
	public static final String ATTR_ERRORID = "errorId"; // yes id must be Id !!
	public static final String ATTR_PASSWORD = "password";
	public static final String ATTR_PROTOCOLVERSION = "protocolversion";

	public static final String ATTR_STOPONERROR = "stoponerror";
	public static final String ATTR_T = "t";
	public static final String ATTR_TIME = "time";
	public static final String ATTR_DETAILS = "details";

	public static final String ATTR_NAME = "name";

	/**
	 * Error ids returned in -nrsp as attribute ATTR_ERRORID
	 */
	public final static int
			// 4000-4999 are "user-correctable" errors (user sends wrong input)
			// 5000-5999 are server failures
			ERR4004_ILLEGAL_COMMAND_FOR_STATE = 4003,
			ERR4004_INVALID_ATTR_VALUE = 4004,
			ERR4007_AGENT_LEASE_EXPIRED = 4007,

			//_Portal/Application_error_codes_(4100-4199)
			ERR4100_INVALID_USERNAME = 4100,
			ERR4101_INVALID_PASSWORD = 4101,
			ERR4102_MAX_LOGIN_ATTEMPTS_EXCEEDED = 4102,

			//_General_Server_Error_Codes
			ERR5000_INTERNAL_SERVER_ERROR = 5000;

	/**
	 * Create login protocol request.
	 */
	public static Element createLoginRequest(String aName, String aPassword)
	{
		Element request = createRequest(SERVICE_LOGIN);
		request.setAttribute(ATTR_NAME, aName);
		request.setAttribute(ATTR_PASSWORD, aPassword);
		request.setAttribute(ATTR_PROTOCOLVERSION, PROTOCOL_VERSION);
		return request;
	}

	/**
	 * Create create-session protocol request.
	 */
	public static Element createSessionCreateRequest()
	{
		return createRequest(SERVICE_SESSION_CREATE);
	}


	/**
	 * Create a positive response element.
	 */
	public static Element createRequest(String aService)
	{
		Element element = null;
		try
		{
			DocumentBuilderFactory dFact = DocumentBuilderFactory.newInstance();
			DocumentBuilder build = dFact.newDocumentBuilder();
			Document doc = build.newDocument();
			element = doc.createElement(aService + POSTFIX_REQ);
			doc.appendChild(element);

		} catch (Throwable t)
		{

		}
		return element;
	}


	/**
	 * Return service name for a message tag.
	 *
	 * @param aMessageTag
	 * @return
	 */
	public static String getServiceName(String aMessageTag)
	{
		try
		{
			return aMessageTag.substring(0, aMessageTag.lastIndexOf('-'));
		}
		catch (Throwable t)
		{
			throw new IllegalArgumentException("getServiceName: invalid tag: " + aMessageTag);
		}
	}

	/**
	 * Is message a (negative) response..
	 *
	 * @param message
	 * @return
	 */
	public static boolean isResponse(Element message)
	{
		String tag = message.getTagName();
		return tag.endsWith(POSTFIX_RSP) || tag.endsWith(POSTFIX_NRSP);
	}

	/**
	 * Is message a positive response..
	 *
	 * @param message
	 * @return
	 */
	public static boolean isPositiveResponse(Element message)
	{
		return message.getTagName().endsWith(POSTFIX_RSP);
	}

	/**
	 * Is message a negative response..
	 *
	 * @param message
	 * @return
	 */
	public static boolean isNegativeResponse(Element message)
	{
		return message.getTagName().endsWith(POSTFIX_NRSP);
	}

	public static boolean isPositiveServiceResponse(Element message, String service)
	{
		return isPositiveResponse(message) && service.equals(getServiceName(message.getTagName()));
	}

	public static boolean isNegativeServiceResponse(Element message, String service)
	{
		return isNegativeResponse(message) && service.equals(getServiceName(message.getTagName()));
	}

	public static boolean isService(Element message, String service)
	{
		return service.equals(getServiceName(message.getTagName()));
	}

	protected Protocol()
	{
	}
}
