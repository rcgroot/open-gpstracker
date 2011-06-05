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

package org.apache.ogt.http.conn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.apache.ogt.http.HttpEntity;
import org.apache.ogt.http.HttpException;
import org.apache.ogt.http.HttpHost;
import org.apache.ogt.http.HttpRequest;
import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.MalformedChunkCodingException;
import org.apache.ogt.http.client.methods.HttpGet;
import org.apache.ogt.http.conn.ClientConnectionRequest;
import org.apache.ogt.http.conn.ConnectionPoolTimeoutException;
import org.apache.ogt.http.conn.ManagedClientConnection;
import org.apache.ogt.http.conn.routing.HttpRoute;
import org.apache.ogt.http.conn.scheme.SchemeRegistry;
import org.apache.ogt.http.entity.BasicHttpEntity;
import org.apache.ogt.http.impl.DefaultHttpServerConnection;
import org.apache.ogt.http.impl.client.DefaultHttpClient;
import org.apache.ogt.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.ogt.http.localserver.ServerTestBase;
import org.apache.ogt.http.protocol.ExecutionContext;
import org.apache.ogt.http.protocol.HttpContext;
import org.apache.ogt.http.protocol.HttpRequestHandler;
import org.apache.ogt.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

public class TestConnectionAutoRelease extends ServerTestBase {

    private ThreadSafeClientConnManager createTSCCM(SchemeRegistry schreg) {
        if (schreg == null)
            schreg = supportedSchemes;
        return new ThreadSafeClientConnManager(schreg);
    }

    @Test
    public void testReleaseOnEntityConsumeContent() throws Exception {
        ThreadSafeClientConnManager mgr = createTSCCM(null);
        mgr.setDefaultMaxPerRoute(1);
        mgr.setMaxTotal(1);

        // Zero connections in the pool
        Assert.assertEquals(0, mgr.getConnectionsInPool());

        DefaultHttpClient client = new DefaultHttpClient(mgr);

        // Get some random data
        HttpGet httpget = new HttpGet("/random/20000");
        HttpHost target = getServerHttp();

        HttpResponse response = client.execute(target, httpget);

        ClientConnectionRequest connreq = mgr.requestConnection(new HttpRoute(target), null);
        try {
            connreq.getConnection(250, TimeUnit.MILLISECONDS);
            Assert.fail("ConnectionPoolTimeoutException should have been thrown");
        } catch (ConnectionPoolTimeoutException expected) {
        }

        HttpEntity e = response.getEntity();
        Assert.assertNotNull(e);
        EntityUtils.consume(e);

        // Expect one connection in the pool
        Assert.assertEquals(1, mgr.getConnectionsInPool());

        // Make sure one connection is available
        connreq = mgr.requestConnection(new HttpRoute(target), null);
        ManagedClientConnection conn = connreq.getConnection(250, TimeUnit.MILLISECONDS);

        mgr.releaseConnection(conn, -1, null);

        mgr.shutdown();
    }

    @Test
    public void testReleaseOnEntityWriteTo() throws Exception {
        ThreadSafeClientConnManager mgr = createTSCCM(null);
        mgr.setDefaultMaxPerRoute(1);
        mgr.setMaxTotal(1);

        // Zero connections in the pool
        Assert.assertEquals(0, mgr.getConnectionsInPool());

        DefaultHttpClient client = new DefaultHttpClient(mgr);

        // Get some random data
        HttpGet httpget = new HttpGet("/random/20000");
        HttpHost target = getServerHttp();

        HttpResponse response = client.execute(target, httpget);

        ClientConnectionRequest connreq = mgr.requestConnection(new HttpRoute(target), null);
        try {
            connreq.getConnection(250, TimeUnit.MILLISECONDS);
            Assert.fail("ConnectionPoolTimeoutException should have been thrown");
        } catch (ConnectionPoolTimeoutException expected) {
        }

        HttpEntity e = response.getEntity();
        Assert.assertNotNull(e);
        ByteArrayOutputStream outsteam = new ByteArrayOutputStream();
        e.writeTo(outsteam);

        // Expect one connection in the pool
        Assert.assertEquals(1, mgr.getConnectionsInPool());

        // Make sure one connection is available
        connreq = mgr.requestConnection(new HttpRoute(target), null);
        ManagedClientConnection conn = connreq.getConnection(250, TimeUnit.MILLISECONDS);

        mgr.releaseConnection(conn, -1, null);

        mgr.shutdown();
    }

    @Test
    public void testReleaseOnAbort() throws Exception {
        ThreadSafeClientConnManager mgr = createTSCCM(null);
        mgr.setDefaultMaxPerRoute(1);
        mgr.setMaxTotal(1);

        // Zero connections in the pool
        Assert.assertEquals(0, mgr.getConnectionsInPool());

        DefaultHttpClient client = new DefaultHttpClient(mgr);

        // Get some random data
        HttpGet httpget = new HttpGet("/random/20000");
        HttpHost target = getServerHttp();

        HttpResponse response = client.execute(target, httpget);

        ClientConnectionRequest connreq = mgr.requestConnection(new HttpRoute(target), null);
        try {
            connreq.getConnection(250, TimeUnit.MILLISECONDS);
            Assert.fail("ConnectionPoolTimeoutException should have been thrown");
        } catch (ConnectionPoolTimeoutException expected) {
        }

        HttpEntity e = response.getEntity();
        Assert.assertNotNull(e);
        httpget.abort();

        // Expect zero connections in the pool
        Assert.assertEquals(0, mgr.getConnectionsInPool());

        // Make sure one connection is available
        connreq = mgr.requestConnection(new HttpRoute(target), null);
        ManagedClientConnection conn = connreq.getConnection(250, TimeUnit.MILLISECONDS);

        mgr.releaseConnection(conn, -1, null);

        mgr.shutdown();
    }

    @Test
    public void testReleaseOnIOException() throws Exception {

        localServer.register("/dropdead", new HttpRequestHandler() {

            public void handle(
                    final HttpRequest request,
                    final HttpResponse response,
                    final HttpContext context) throws HttpException, IOException {
                BasicHttpEntity entity = new BasicHttpEntity() {

                    @Override
                    public void writeTo(
                            final OutputStream outstream) throws IOException {
                        byte[] tmp = new byte[5];
                        outstream.write(tmp);
                        outstream.flush();

                        // do something comletely ugly in order to trigger
                        // MalformedChunkCodingException
                        DefaultHttpServerConnection conn = (DefaultHttpServerConnection)
                            context.getAttribute(ExecutionContext.HTTP_CONNECTION);
                        try {
                            conn.sendResponseHeader(response);
                        } catch (HttpException ignore) {
                        }
                    }

                } ;
                entity.setChunked(true);
                response.setEntity(entity);
            }

        });

        ThreadSafeClientConnManager mgr = createTSCCM(null);
        mgr.setDefaultMaxPerRoute(1);
        mgr.setMaxTotal(1);

        // Zero connections in the pool
        Assert.assertEquals(0, mgr.getConnectionsInPool());

        DefaultHttpClient client = new DefaultHttpClient(mgr);

        // Get some random data
        HttpGet httpget = new HttpGet("/dropdead");
        HttpHost target = getServerHttp();

        HttpResponse response = client.execute(target, httpget);

        ClientConnectionRequest connreq = mgr.requestConnection(new HttpRoute(target), null);
        try {
            connreq.getConnection(250, TimeUnit.MILLISECONDS);
            Assert.fail("ConnectionPoolTimeoutException should have been thrown");
        } catch (ConnectionPoolTimeoutException expected) {
        }

        HttpEntity e = response.getEntity();
        Assert.assertNotNull(e);
        // Read the content
        try {
            EntityUtils.toByteArray(e);
            Assert.fail("MalformedChunkCodingException should have been thrown");
        } catch (MalformedChunkCodingException expected) {

        }

        // Expect zero connections in the pool
        Assert.assertEquals(0, mgr.getConnectionsInPool());

        // Make sure one connection is available
        connreq = mgr.requestConnection(new HttpRoute(target), null);
        ManagedClientConnection conn = connreq.getConnection(250, TimeUnit.MILLISECONDS);

        mgr.releaseConnection(conn, -1, null);

        mgr.shutdown();
    }

}
