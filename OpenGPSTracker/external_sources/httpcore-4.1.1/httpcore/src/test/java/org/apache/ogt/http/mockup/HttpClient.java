/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.ogt.http.mockup;

import java.io.IOException;

import org.apache.ogt.http.ConnectionReuseStrategy;
import org.apache.ogt.http.HttpClientConnection;
import org.apache.ogt.http.HttpException;
import org.apache.ogt.http.HttpHost;
import org.apache.ogt.http.HttpRequest;
import org.apache.ogt.http.HttpRequestInterceptor;
import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.HttpVersion;
import org.apache.ogt.http.impl.DefaultConnectionReuseStrategy;
import org.apache.ogt.http.params.CoreConnectionPNames;
import org.apache.ogt.http.params.CoreProtocolPNames;
import org.apache.ogt.http.params.DefaultedHttpParams;
import org.apache.ogt.http.params.HttpParams;
import org.apache.ogt.http.params.SyncBasicHttpParams;
import org.apache.ogt.http.protocol.BasicHttpContext;
import org.apache.ogt.http.protocol.ExecutionContext;
import org.apache.ogt.http.protocol.HttpContext;
import org.apache.ogt.http.protocol.HttpProcessor;
import org.apache.ogt.http.protocol.HttpRequestExecutor;
import org.apache.ogt.http.protocol.ImmutableHttpProcessor;
import org.apache.ogt.http.protocol.RequestConnControl;
import org.apache.ogt.http.protocol.RequestContent;
import org.apache.ogt.http.protocol.RequestExpectContinue;
import org.apache.ogt.http.protocol.RequestTargetHost;
import org.apache.ogt.http.protocol.RequestUserAgent;

public class HttpClient {

    private final HttpParams params;
    private final HttpProcessor httpproc;
    private final HttpRequestExecutor httpexecutor;
    private final ConnectionReuseStrategy connStrategy;
    private final HttpContext context;

    public HttpClient() {
        super();
        this.params = new SyncBasicHttpParams();
        this.params
            .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
            .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
            .setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1)
            .setParameter(CoreProtocolPNames.USER_AGENT, "TEST-CLIENT/1.1");
        this.httpproc = new ImmutableHttpProcessor(
                new HttpRequestInterceptor[] {
                        new RequestContent(),
                        new RequestTargetHost(),
                        new RequestConnControl(),
                        new RequestUserAgent(),
                        new RequestExpectContinue()
                });
        this.httpexecutor = new HttpRequestExecutor();
        this.connStrategy = new DefaultConnectionReuseStrategy();
        this.context = new BasicHttpContext();
    }

    public HttpParams getParams() {
        return this.params;
    }

    public HttpResponse execute(
            final HttpRequest request,
            final HttpHost targetHost,
            final HttpClientConnection conn) throws HttpException, IOException {
        this.context.setAttribute(ExecutionContext.HTTP_REQUEST, request);
        this.context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, targetHost);
        this.context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);

        request.setParams(new DefaultedHttpParams(request.getParams(), this.params));
        this.httpexecutor.preProcess(request, this.httpproc, this.context);
        HttpResponse response = this.httpexecutor.execute(request, conn, this.context);
        response.setParams(new DefaultedHttpParams(response.getParams(), this.params));
        this.httpexecutor.postProcess(response, this.httpproc, this.context);
        return response;
    }

    public boolean keepAlive(final HttpResponse response) {
        return this.connStrategy.keepAlive(response, this.context);
    }

}
