/*
 * Copyright 2010 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.ning.http.client.async;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ProviderConfig;
import com.ning.http.client.providers.NettyAsyncHttpProvider;
import com.ning.http.client.Cookie;
import com.ning.http.client.Headers;
import com.ning.http.client.Part;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Request;
import com.ning.http.client.RequestType;
import com.ning.http.client.Response;
import com.ning.http.client.StringPart;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AsyncProvidersBasicTest extends AbstractBasicTest {

    static class VoidListener implements AsyncHandler<Response> {

        @Override
        public Response onCompleted(Response response) throws IOException{
            return response;
        }

        @Override
        public void onThrowable(Throwable t) {
            t.printStackTrace();
            Assert.fail("Unexpected exception", t);
        }

    }

    @Test(groups = "async")
    public void asyncProviderContentLenghtGETTest() throws Throwable {
        NettyAsyncHttpProvider p = new NettyAsyncHttpProvider(new ProviderConfig(Executors.newScheduledThreadPool(1)));
        final CountDownLatch l = new CountDownLatch(1);
        URL url = new URL(TARGET_URL);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();

        Request request = new Request(RequestType.GET, TARGET_URL);
        p.handle(request, new VoidListener() {

            @Override
            public Response onCompleted(Response response) throws IOException {
                Assert.assertEquals(response.getStatusCode(), 200);
                int contentLenght = -1;
                if (response.getHeader("content-length") != null) {
                    contentLenght = Integer.valueOf(response.getHeader("content-length"));
                }
                int ct = connection.getContentLength();
                Assert.assertEquals(contentLenght, ct);
                l.countDown();
                return response;
            }

            @Override
            public void onThrowable(Throwable t) {
                Assert.fail("Unexpected exception", t);
                l.countDown();
            }


        }).get();

        if (!l.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Timeout out");
        }

        p.close();
    }

    @Test(groups = "async")
    public void asyncContentTypeGETTest() throws Throwable {
        NettyAsyncHttpProvider p = new NettyAsyncHttpProvider(new ProviderConfig(Executors.newScheduledThreadPool(1)));
        
        final CountDownLatch l = new CountDownLatch(1);
        Request request = new Request(RequestType.GET, TARGET_URL);
        p.handle(request, new VoidListener() {

            @Override
            public Response onCompleted(Response response) throws IOException {
                Assert.assertEquals(response.getStatusCode(), 200);
                Assert.assertEquals(response.getContentType(), "text/html; charset=utf-8");
                l.countDown();
                return response;
            }
        }).get();

        if (!l.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Timeout out");
        }
        p.close();
    }

    @Test(groups = "async")
    public void asyncHeaderGETTest() throws Throwable {
        NettyAsyncHttpProvider n = new NettyAsyncHttpProvider(new ProviderConfig(Executors.newScheduledThreadPool(1)));
        final CountDownLatch l = new CountDownLatch(1);
        Request request = new Request(RequestType.GET, TARGET_URL);
        n.handle(request, new VoidListener() {

            @Override
            public Response onCompleted(Response response) throws IOException {
                Assert.assertEquals(response.getStatusCode(), 200);
                Assert.assertEquals(response.getContentType(), "text/html; charset=utf-8");

                l.countDown();
                return response;
            }
        }).get();

        if (!l.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Timeout out");
        }
        
    }

    @Test(groups = "async")
    public void asyncHeaderPOSTTest() throws Throwable {
        final CountDownLatch l = new CountDownLatch(1);
        NettyAsyncHttpProvider n = new NettyAsyncHttpProvider(new ProviderConfig(Executors.newScheduledThreadPool(1)));
        
        Request request = new Request(RequestType.POST, TARGET_URL);
        Headers h = new Headers();
        h.add("Test1", "Test1");
        h.add("Test2", "Test2");
        h.add("Test3", "Test3");
        h.add("Test4", "Test4");
        h.add("Test5", "Test5");
        request.setHeaders(h);

        n.handle(request, new VoidListener() {

            @Override
            public Response onCompleted(Response response) throws IOException {
                System.out.println(">>>>> " + response.getStatusText());
                Assert.assertEquals(response.getStatusCode(), 200);
                for (int i = 1; i < 5; i++) {
                    Assert.assertEquals(response.getHeader("X-Test" + i), "Test" + i);
                }
                l.countDown();
                l.countDown();
                return response;
            }
        }).get();

        if (!l.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Timeout out");
        }
        
    }

    @Test(groups = "async")
    public void asyncParamPOSTTest() throws Throwable {
        NettyAsyncHttpProvider n = new NettyAsyncHttpProvider(new ProviderConfig(Executors.newScheduledThreadPool(1)));
        
        final CountDownLatch l = new CountDownLatch(1);
        Headers h = new Headers();
        h.add("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> m = new HashMap<String, String>();
        for (int i = 0; i < 5; i++) {
            m.put("param_" + i, "value_" + i);
        }
        Request request = new Request(RequestType.POST, TARGET_URL, h, null, m);
        n.handle(request, new VoidListener() {

            @Override
            public Response onCompleted(Response response) throws IOException {

                Assert.assertEquals(response.getStatusCode(), 200);
                for (int i = 1; i < 5; i++) {
                    System.out.println(">>>>> " + response.getHeader("X-param_" + i));
                    Assert.assertEquals(response.getHeader("X-param_" + i), "value_" + i);
                }

                l.countDown();
                return response;
            }
        }).get();

        if (!l.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Timeout out");
        }
        
    }

    @Test(groups = "async")
    public void asyncStatusHEADTest() throws Throwable {
        NettyAsyncHttpProvider n = new NettyAsyncHttpProvider(new ProviderConfig(Executors.newScheduledThreadPool(1)));
        
        final CountDownLatch l = new CountDownLatch(1);
        Request request = new Request(RequestType.HEAD, TARGET_URL);
        n.handle(request, new VoidListener() {

            @Override
            public Response onCompleted(Response response) throws IOException {
                Assert.assertEquals(response.getStatusCode(), 200);
                l.countDown();
                return response;
            }
        }).get();

        if (!l.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Timeout out");
        }
        
    }

    @Test(groups = "async")
    public void asyncDoGetTransferEncodingTest() throws Throwable {
        NettyAsyncHttpProvider n = new NettyAsyncHttpProvider(new ProviderConfig(Executors.newScheduledThreadPool(1)));
        
        AsyncHttpClient c = new AsyncHttpClient(n);
        final CountDownLatch l = new CountDownLatch(1);

        c.doGet(TARGET_URL, new VoidListener() {

            @Override
            public Response onCompleted(Response response) throws IOException {

                Assert.assertEquals(response.getStatusCode(), 200);
                Assert.assertEquals(response.getHeader("Transfer-Encoding"), "chunked");
                l.countDown();
                return response;
            }
        }).get();

        if (!l.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Timeout out");
        }
        
    }

    @Test(groups = "async")
    public void asyncDoGetHeadersTest() throws Throwable {
        NettyAsyncHttpProvider n = new NettyAsyncHttpProvider(new ProviderConfig(Executors.newScheduledThreadPool(1)));

        AsyncHttpClient c = new AsyncHttpClient(n);
        final CountDownLatch l = new CountDownLatch(1);
        Headers h = new Headers();
        h.add("Test1", "Test1");
        h.add("Test2", "Test2");
        h.add("Test3", "Test3");
        h.add("Test4", "Test4");
        h.add("Test5", "Test5");
        c.doGet(TARGET_URL, h, new VoidListener() {

            @Override
            public Response onCompleted(Response response) throws IOException {

                Assert.assertEquals(response.getStatusCode(), 200);
                for (int i = 1; i < 5; i++) {
                    Assert.assertEquals(response.getHeader("X-Test" + i), "Test" + i);
                }
                l.countDown();
                return response;
            }
        }).get();
        if (!l.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Timeout out");
        }
        
    }

    @Test(groups = "async")
    public void asyncDoGetCookieTest() throws Throwable {
        AsyncHttpClient c = new AsyncHttpClient();
        final CountDownLatch l = new CountDownLatch(1);
        Headers h = new Headers();
        h.add("Test1", "Test1");
        h.add("Test2", "Test2");
        h.add("Test3", "Test3");
        h.add("Test4", "Test4");
        h.add("Test5", "Test5");

        Cookie coo = new Cookie("/", "foo", "value", "/", 3000, false);
        ArrayList<Cookie> la = new ArrayList<Cookie>();
        la.add(coo);
        c.doGet(TARGET_URL, h, la, new VoidListener() {

            @Override
            public Response onCompleted(Response response) throws IOException {
                System.out.println(">>>> " + response.getHeader("Set-Cookie"));
                Assert.assertEquals(response.getStatusCode(), 200);
                Assert.assertEquals("foo=value;Path=/;Domain=/", response.getHeader("Set-Cookie"));
                l.countDown();
                return response;
            }
        }).get();
        if (!l.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Timeout out");
        }
    }

    @Test(groups = "async")
    public void asyncDoPostBytesTest() throws Throwable {

        AsyncHttpClient c = new AsyncHttpClient();
        final CountDownLatch l = new CountDownLatch(1);
        Headers h = new Headers();
        h.add("Content-Type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append("param_");
            sb.append(i);
            sb.append("=value_");
            sb.append(i);
            sb.append("&");
        }
        sb.deleteCharAt(sb.length() - 1);

        c.doPost(TARGET_URL, h, sb.toString().getBytes(), new VoidListener() {

            @Override
            public Response onCompleted(Response response) throws IOException {
                Assert.assertEquals(response.getStatusCode(), 200);
                for (int i = 1; i < 5; i++) {
                    System.out.println(">>>>> " + response.getHeader("X-param_" + i));
                    Assert.assertEquals(response.getHeader("X-param_" + i), "value_" + i);

                }
                l.countDown();
                return response;
            }
        }).get();
        if (!l.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Timeout out");
        }
    }

    @Test(groups = "async")
    public void asyncDoPostInputStreamTest() throws Throwable {

        AsyncHttpClient c = new AsyncHttpClient();
        final CountDownLatch l = new CountDownLatch(1);
        Headers h = new Headers();
        h.add("Content-Type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append("param_");
            sb.append(i);
            sb.append("=value_");
            sb.append(i);
            sb.append("&");
        }
        sb.deleteCharAt(sb.length() - 1);
        ByteArrayInputStream is = new ByteArrayInputStream(sb.toString().getBytes());

        c.doPost(TARGET_URL, h, is, new VoidListener() {

            @Override
            public Response onCompleted(Response response) throws IOException {
                Assert.assertEquals(response.getStatusCode(), 200);
                for (int i = 1; i < 5; i++) {
                    System.out.println(">>>>> " + response.getHeader("X-param_" + i));
                    Assert.assertEquals(response.getHeader("X-param_" + i), "value_" + i);

                }
                l.countDown();
                return response;
            }
        }).get();
        if (!l.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Timeout out");
        }
    }

    @Test(groups = "async")
    public void asyncDoPostEntityWriterTest() throws Throwable {

        AsyncHttpClient c = new AsyncHttpClient();
        final CountDownLatch l = new CountDownLatch(1);
        Headers h = new Headers();
        h.add("Content-Type", "application/x-www-form-urlencoded");

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append("param_");
            sb.append(i);
            sb.append("=value_");
            sb.append(i);
            sb.append("&");
        }
        sb.deleteCharAt(sb.length() - 1);
        byte[] bytes = sb.toString().getBytes();
        h.add("Content-Length", String.valueOf(bytes.length));

        c.doPost(TARGET_URL, h, new Request.EntityWriter() {

            @Override
            public void writeEntity(OutputStream out) throws IOException {
                out.write(sb.toString().getBytes());
            }
        }, new VoidListener() {

            @Override
            public Response onCompleted(Response response) throws IOException {
                Assert.assertEquals(response.getStatusCode(), 200);
                for (int i = 1; i < 5; i++) {
                    System.out.println(">>>>> " + response.getHeader("X-param_" + i));
                    Assert.assertEquals(response.getHeader("X-param_" + i), "value_" + i);
                }
                l.countDown();
                return response;
            }
        }).get();
        if (!l.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Timeout out");
        }
    }

    @Test(groups = "async")
    public void asyncDoPostMultiPartTest() throws Throwable {

        AsyncHttpClient c = new AsyncHttpClient();
        final CountDownLatch l = new CountDownLatch(1);

        ArrayList<Part> la = new ArrayList<Part>();
        Part p = new StringPart("foo", "bar");
        la.add(p);

        c.doMultipartPost(TARGET_URL, la, new VoidListener() {

            @Override
            public Response onCompleted(Response response) throws IOException {
                String xContentType = response.getHeader("X-Content-Type");
                String boundary = xContentType.substring(
                        (xContentType.indexOf("boundary") + "boundary".length() + 1));

                String s = response.getResponseBodyExcerpt(boundary.length() + "--".length()).substring("--".length());
                Assert.assertEquals(boundary, s);
                l.countDown();
                return response;
            }
        }).get();
        if (!l.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Timeout out");
        }
    }

    @Test(groups = "async")
    public void asyncDoPostBasicGZIPTest() throws Throwable {

        AsyncHttpClient c = new AsyncHttpClient();
        final CountDownLatch l = new CountDownLatch(1);
        c.setCompressionEnabled(true);
        Headers h = new Headers();
        h.add("Content-Type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append("param_");
            sb.append(i);
            sb.append("=value_");
            sb.append(i);
            sb.append("&");
        }
        sb.deleteCharAt(sb.length() - 1);

        c.doPost(TARGET_URL, h, sb.toString().getBytes(), new VoidListener() {

            @Override
            public Response onCompleted(Response response) throws IOException {
                Assert.assertEquals(response.getStatusCode(), 200);
                Assert.assertEquals(response.getHeader("X-Accept-Encoding"), "gzip");
                l.countDown();
                return response;
            }
        }).get();
        if (!l.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Timeout out");
        }
        
    }

    @Test(groups = "async")
    public void asyncDoPostProxyTest() throws Throwable {

        AsyncHttpClient c = new AsyncHttpClient();
        c.setProxy(new ProxyServer("127.0.0.1", 38080));

        Headers h = new Headers();
        h.add("Content-Type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append("param_");
            sb.append(i);
            sb.append("=value_");
            sb.append(i);
            sb.append("&");
        }
        sb.deleteCharAt(sb.length() - 1);

        Response response = c.doPost(TARGET_URL, h, sb.toString().getBytes(), new AsyncHandler<Response>() {
            @Override
            public Response onCompleted(Response response) {
                return response;
            }

            @Override
            public void onThrowable(Throwable t) {
            }
        }).get();

        

        Assert.assertEquals(response.getStatusCode(), 200);
        Assert.assertEquals(response.getHeader("X-Proxy-Connection"), "keep-alive");
    }


    @Test(groups = "async")
    public void asyncRequestVirtualServerPOSTTest() throws Throwable {
        NettyAsyncHttpProvider n = new NettyAsyncHttpProvider(new ProviderConfig(Executors.newScheduledThreadPool(1)));

        Headers h = new Headers();
        h.add("Content-Type", "application/x-www-form-urlencoded");

        Map<String, String> m = new HashMap<String, String>();
        for (int i = 0; i < 5; i++) {
            m.put("param_" + i, "value_" + i);
        }
        Request request = new Request(RequestType.POST, TARGET_URL, h, null, m);
        request.setVirtualHost("localhost");

        Response response = n.handle(request, new VoidListener()).get();

        Assert.assertEquals(response.getStatusCode(), 200);
        Assert.assertEquals(response.getHeader("X-Host"), "localhost:19999");
        
    }

    @Test(groups = "async")
    public void asyncDoPutTest() throws Throwable {

        AsyncHttpClient c = new AsyncHttpClient();
        Headers h = new Headers();
        h.add("Content-Type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append("param_");
            sb.append(i);
            sb.append("=value_");
            sb.append(i);
            sb.append("&");
        }
        sb.deleteCharAt(sb.length() - 1);

        Response response = c.doPut(TARGET_URL, h, sb.toString().getBytes(), new VoidListener()).get();

        Assert.assertEquals(response.getStatusCode(), 200);
        Assert.assertEquals(response.getHeader("X-param_1"), null);
        
    }

    @Test(groups = "async")
    public void asyncDoPostLatchBytesTest() throws Throwable {

        AsyncHttpClient c = new AsyncHttpClient();
        final CountDownLatch l = new CountDownLatch(1);
        Headers h = new Headers();
        h.add("Content-Type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append("param_");
            sb.append(i);
            sb.append("=value_");
            sb.append(i);
            sb.append("&");
        }
        sb.deleteCharAt(sb.length() - 1);

        /*
         * Use a Latch to simulate asynchronous response.
         */
        final CountDownLatch latch = new CountDownLatch(1);

        c.doPost(TARGET_URL, h, sb.toString().getBytes(), new VoidListener() {

            @Override
            public Response onCompleted(Response response) throws IOException {
                try {
                    Assert.assertEquals(response.getStatusCode(), 200);
                    for (int i = 1; i < 5; i++) {
                        System.out.println(">>>>> " + response.getHeader("X-param_" + i));
                        Assert.assertEquals(response.getHeader("X-param_" + i), "value_" + i);

                    }
                    l.countDown();
                    return response;
                } finally {
                    latch.countDown();
                }
            }
        });

        if (!l.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Timeout out");
        }
        
    }

    @Test(groups = "async")
    public void asyncDoPostDelayCancelTest() throws Throwable {

        AsyncHttpClient c = new AsyncHttpClient();
        Headers h = new Headers();
        h.add("Content-Type", "application/x-www-form-urlencoded");
        h.add("LockThread", "true");
        StringBuilder sb = new StringBuilder();
        sb.append("LockThread=true");

        Future<Response> future = c.doPost(TARGET_URL, h, sb.toString().getBytes(), new VoidListener());
        future.cancel(true);
        Response response = future.get(5, TimeUnit.SECONDS);
        Assert.assertNotNull(response);
        c.close();
    }

    @Test(groups = "async")
    public void asyncDoPostDelayBytesTest() throws Throwable {

        AsyncHttpClient c = new AsyncHttpClient();
        Headers h = new Headers();
        h.add("Content-Type", "application/x-www-form-urlencoded");
        h.add("LockThread", "true");
        StringBuilder sb = new StringBuilder();
        sb.append("LockThread=true");

        try {
            Future<Response> future = c.doPost(TARGET_URL, h, sb.toString().getBytes(),new VoidListener(){
                @Override
                public void onThrowable(Throwable t) {
                    t.printStackTrace();
                }
            });

            future.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            Assert.assertTrue(true);
        } catch (IllegalStateException ex) {
            Assert.assertTrue(false);
        }
        c.close();
    }

    @Test(groups = "async")
    public void asyncDoPostNullBytesTest() throws Throwable {

        AsyncHttpClient c = new AsyncHttpClient();
        Headers h = new Headers();
        h.add("Content-Type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append("param_");
            sb.append(i);
            sb.append("=value_");
            sb.append(i);
            sb.append("&");
        }
        sb.deleteCharAt(sb.length() - 1);

        Future<Response> future = c.doPost(TARGET_URL, h, sb.toString().getBytes(),new VoidListener());

        Response response = future.get();
        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusCode(), 200);

    }

    @Test(groups = "async")
    public void asyncDoPostListenerBytesTest() throws Throwable {
        AsyncHttpClient c = new AsyncHttpClient();
        Headers h = new Headers();
        h.add("Content-Type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append("param_");
            sb.append(i);
            sb.append("=value_");
            sb.append(i);
            sb.append("&");
        }
        sb.deleteCharAt(sb.length() - 1);

        final CountDownLatch latch = new CountDownLatch(1);

        c.doPost(TARGET_URL, h, sb.toString().getBytes(),
                new VoidListener() {

                    @Override
                    public Response onCompleted(Response response) {
                        Assert.assertEquals(response.getStatusCode(), 200);
                        latch.countDown();
                        return response;
                    }
                });

        if (!latch.await(10, TimeUnit.SECONDS)) {
            Assert.fail("Latch time out");
        }
    }

    @Test(groups = "async")
    public void asyncConnectInvalidPortFuture() throws Throwable {

        AsyncHttpClient c = new AsyncHttpClient();
        Headers h = new Headers();
        h.add("Content-Type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append("param_");
            sb.append(i);
            sb.append("=value_");
            sb.append(i);
            sb.append("&");
        }
        sb.deleteCharAt(sb.length() - 1);

        IOException expected = null;
        try {
            c.doPost("http://127.0.0.1:9999/", h, sb.toString().getBytes(), new VoidListener());
        } catch (IOException ex) {
            expected = ex;
        }

        if (expected != null) {
            Assert.assertTrue(true);
        } else {
            Assert.fail("Must have thrown an IOException");
        }
    }

    @Test(groups = "async")
    public void asyncConnectInvalidPort() throws Throwable {
        AsyncHttpClient c = new AsyncHttpClient();
        Headers h = new Headers();
        h.add("Content-Type", "application/x-www-form-urlencoded");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append("param_");
            sb.append(i);
            sb.append("=value_");
            sb.append(i);
            sb.append("&");
        }
        sb.deleteCharAt(sb.length() - 1);

        try{
            c.doPost("http://127.0.0.1:9999/", h, sb.toString().getBytes(), new VoidListener());
            Assert.assertTrue(false);          
        } catch (ConnectException ex){
            Assert.assertTrue(true);
        }
    }

    @Test(groups = "async")
    public void asyncConnectInvalidFuturePort() throws Throwable {
        AsyncHttpClient c = new AsyncHttpClient();

        try {
            c.doGet("http://127.0.0.1:9999/", new VoidListener());
            Assert.fail("No ConnectionException was thrown");
        } catch (ConnectException ex) {
            Assert.assertEquals(ex.getMessage(), "Connection refused: http://127.0.0.1:9999/");
        }

    }

    @Test(groups = "async")
    public void asyncContentLenghtGETTest() throws Throwable {
        AsyncHttpClient c = new AsyncHttpClient();
        Response response = c.doGet(TARGET_URL,  new VoidListener() {

            @Override
            public void onThrowable(Throwable t) {
                Assert.fail("Unexpected exception", t);
            }
        }).get();

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getStatusCode(), 200);
    }


    @Test(groups = "asyncAPI")
    public void asyncAPIContentLenghtGETTest() throws Throwable {
        AsyncHttpClient client = new AsyncHttpClient();

        // Use a latch in case the assert fail
        final CountDownLatch latch = new CountDownLatch(1);

        client.doGet(TARGET_URL,new VoidListener() {

            @Override
            public Response onCompleted(Response response) {
                Assert.assertEquals(response.getStatusCode(), 200);
                latch.countDown();
                return response;
            }

            @Override
            public void onThrowable(Throwable t) {
            }
        });


        if (!latch.await(10, TimeUnit.SECONDS)) {
            Assert.fail("Timed out");
        }
    }

    @Test(groups = "asyncAPI")
    public void asyncAPIHandlerExceptionTest() throws Throwable {
        AsyncHttpClient client = new AsyncHttpClient();

        // Use a latch in case the assert fail
        final CountDownLatch latch = new CountDownLatch(1);

        client.doGet(TARGET_URL,new VoidListener() {
            @Override
            public Response onCompleted(Response response) {
                throw new IllegalStateException("FOO");
            }

            @Override
            public void onThrowable(Throwable t) {
                t.printStackTrace();
                if (t.getMessage() != null) {
                    Assert.assertEquals(t.getMessage(), "FOO");
                    latch.countDown();
                }
            }
        });


        if (!latch.await(10, TimeUnit.SECONDS)) {
            Assert.fail("Timed out");
        }
    }

    @Test(groups = "async")
    public void asyncDoPostDelayHandlerTest() throws Throwable {
        Headers h = new Headers();
        h.add("LockThread", "true");
        AsyncHttpClient client = new AsyncHttpClient();
        client.setRequestTimeout(5 * 1000);

        // Use a latch in case the assert fail
        final CountDownLatch latch = new CountDownLatch(1);

        client.doGet(TARGET_URL,h,new VoidListener() {

            @Override
            public Response onCompleted(Response response) {
                try {
                    Assert.fail("Must not receive a response");
                } finally {
                    latch.countDown();
                }
                return response;
            }

            @Override
            public void onThrowable(Throwable t) {
                try {
                    if (t instanceof TimeoutException) {
                        Assert.assertTrue(true);
                    } else {
                        Assert.fail("Unexpected exception", t);
                    }
                } finally {
                    latch.countDown();
                }
            }
        });

        if (!latch.await(10, TimeUnit.SECONDS)) {
            Assert.fail("Timed out");
        }
    }
}