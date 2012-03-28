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

/**
 * Generic exception wrapper. $Id: ClientException.java,v 1.2 2005/07/22 22:25:20 just Exp $
 *
 * @author $Author: Just van den Broecke$
 * @version $Revision: $
 */
public class ClientException extends Exception
{
	private int errorId;
	String error;

	protected ClientException()
	{
	}

	public ClientException(String aMessage, Throwable t)
	{
		super(aMessage + "\n embedded exception=" + t.toString());
	}

	public ClientException(String aMessage)
	{
		super(aMessage);
	}

	public ClientException(int anErrorId, String anError, String someDetails)
	{
		super(someDetails);
		errorId = anErrorId;
		error = anError;
	}

	public ClientException(Throwable t)
	{
		this("ClientException: ", t);
	}

	public String toString()
	{
		return "ClientException: " + getMessage();
	}
}
