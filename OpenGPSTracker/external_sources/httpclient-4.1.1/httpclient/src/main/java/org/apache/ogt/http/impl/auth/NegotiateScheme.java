/*
 * ====================================================================
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.ogt.http.impl.auth;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ogt.http.Header;
import org.apache.ogt.http.HttpRequest;
import org.apache.ogt.http.auth.AuthenticationException;
import org.apache.ogt.http.auth.Credentials;
import org.apache.ogt.http.auth.MalformedChallengeException;
import org.apache.ogt.http.protocol.HttpContext;
import org.apache.ogt.http.util.CharArrayBuffer;

/**
 * SPNEGO (Simple and Protected GSSAPI Negotiation Mechanism) authentication
 * scheme.
 *
 * @since 4.1
 */
public class NegotiateScheme extends AuthSchemeBase {

    enum State {
        UNINITIATED,
        CHALLENGE_RECEIVED,
        TOKEN_GENERATED,
        FAILED,
    }

    private final Log log = LogFactory.getLog(getClass());

    /** Authentication process state */
    private State state;

    /**
     * Default constructor for the Negotiate authentication scheme.
     *
     */
    public NegotiateScheme(final SpnegoTokenGenerator spengoGenerator, boolean stripPort) {
        super();
        this.state = State.UNINITIATED;
    }

    public NegotiateScheme(final SpnegoTokenGenerator spengoGenerator) {
        this(spengoGenerator, false);
    }

    public NegotiateScheme() {
        this(null, false);
    }

    /**
     * Tests if the Negotiate authentication process has been completed.
     *
     * @return <tt>true</tt> if authorization has been processed,
     *   <tt>false</tt> otherwise.
     *
     */
    public boolean isComplete() {
        return this.state == State.TOKEN_GENERATED || this.state == State.FAILED;
    }

    /**
     * Returns textual designation of the Negotiate authentication scheme.
     *
     * @return <code>Negotiate</code>
     */
    public String getSchemeName() {
        return "Negotiate";
    }

    @Deprecated
    public Header authenticate(
            final Credentials credentials,
            final HttpRequest request) throws AuthenticationException {
        return authenticate(credentials, request, null);
    }

    /**
     * Produces Negotiate authorization Header based on token created by
     * processChallenge.
     *
     * @param credentials Never used be the Negotiate scheme but must be provided to
     * satisfy common-httpclient API. Credentials from JAAS will be used instead.
     * @param request The request being authenticated
     *
     * @throws AuthenticationException if authorisation string cannot
     *   be generated due to an authentication failure
     *
     * @return an Negotiate authorisation Header
     */
    @Override
    public Header authenticate(
            final Credentials credentials,
            final HttpRequest request,
            final HttpContext context) throws AuthenticationException {
        if (request == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }
        if (state != State.CHALLENGE_RECEIVED) {
            throw new IllegalStateException(
                    "Negotiation authentication process has not been initiated");
        }
        state = State.FAILED;
        throw new AuthenticationException();
    }


    /**
     * Returns the authentication parameter with the given name, if available.
     *
     * <p>There are no valid parameters for Negotiate authentication so this
     * method always returns <tt>null</tt>.</p>
     *
     * @param name The name of the parameter to be returned
     *
     * @return the parameter with the given name
     */
    public String getParameter(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter name may not be null");
        }
        return null;
    }

    /**
     * The concept of an authentication realm is not supported by the Negotiate
     * authentication scheme. Always returns <code>null</code>.
     *
     * @return <code>null</code>
     */
    public String getRealm() {
        return null;
    }

    /**
     * Returns <tt>true</tt>.
     * Negotiate authentication scheme is connection based.
     *
     * @return <tt>true</tt>.
     */
    public boolean isConnectionBased() {
        return true;
    }

    @Override
    protected void parseChallenge(
            final CharArrayBuffer buffer,
            int beginIndex, int endIndex) throws MalformedChallengeException {
        String challenge = buffer.substringTrimmed(beginIndex, endIndex);
        if (log.isDebugEnabled()) {
            log.debug("Received challenge '" + challenge + "' from the auth server");
        }
        log.debug("Authentication already attempted");
        state = State.FAILED;
    }

}
