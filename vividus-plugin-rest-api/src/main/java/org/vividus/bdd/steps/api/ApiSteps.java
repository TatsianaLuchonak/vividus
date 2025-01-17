/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.bdd.steps.api;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import javax.inject.Inject;

import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicHeader;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.When;
import org.jbehave.core.model.ExamplesTable;
import org.jbehave.core.steps.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vividus.http.HttpMethod;
import org.vividus.http.HttpRequestBuilder;
import org.vividus.http.HttpTestContext;
import org.vividus.http.client.HttpResponse;
import org.vividus.http.client.IHttpClient;
import org.vividus.http.exception.HttpRequestBuildException;
import org.vividus.softassert.ISoftAssert;

public class ApiSteps
{
    private static final String NAME = "name";
    private static final String VALUE = "value";

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiSteps.class);

    private String apiEndpoint;

    private IHttpClient httpClient;
    @Inject private HttpTestContext httpTestContext;
    @Inject private ISoftAssert softAssert;

    /**
     * Set up request body that will be used while sending request
     * @param content HTTP method request body
     */
    @Given("request body: $content")
    public void request(String content)
    {
        httpTestContext.putRequestEntity(new StringEntity(content, StandardCharsets.UTF_8));
    }

    /**
     * Sets multipart request entity that will be used while sending request
     * <div>Example:</div>
     * <code>
     *   <br>Given multipart request:
     *   <br>|name       |type|value                                     |
     *   <br>|binaryImage|file|/story/bvt/user_avatar.png|
     * </code>
     * <br>
     * <br>where
     * <ul>
     *   <li><code>name</code> is request part name</li>
     *   <li><code>type</code> is one of request types: STRING, FILE</li>
     *   <li><code>value</code> is actual content for STRING request type and file path for FILE request type</li>
     * </ul>
     * @param requestParts HTTP request parts
     *
     */
    @Given("multipart request:$requestParts")
    public void putMultipartRequest(ExamplesTable requestParts)
    {
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        for (Parameters row : requestParts.getRowsAsParameters(true))
        {
            String name = row.valueAs(NAME, String.class);
            String value = row.valueAs(VALUE, String.class);
            String contentType = row.valueAs("contentType", String.class, null);
            RequestPartType requestPartType = row.valueAs("type", RequestPartType.class);
            requestPartType.addPart(multipartEntityBuilder, name, value, Optional.ofNullable(contentType));
        }
        httpTestContext.putRequestEntity(multipartEntityBuilder.build());
    }

    /**
     * Set up request headers that will be used while sending request
     * @param headers ExamplesTable representing list of headers with columns "name" and "value" specifying HTTP header
     * names and values respectively
     */
    @When("I set request headers:$headers")
    public void setUpRequestHeaders(ExamplesTable headers)
    {
        List<Header> requestHeaders = headers.getRowsAsParameters(true).stream()
                .map(row -> new BasicHeader(row.valueAs(NAME, String.class), row.valueAs(VALUE, String.class)))
                .collect(toList());
        httpTestContext.putRequestHeaders(requestHeaders);
    }

    /**
     * Step is sending HTTP request for the given <b>url</b>
     * and saving HTTP response into Scenario-level variable.<br>
     * This step can use the request body that was set before.
     * Request body shouldn't be set for methods that can't contain body (GET, HEAD, OPTIONS, TRACE),
     * and should be set for methods that must contain body (PATCH, POST, PUT).
     * @param httpMethod HTTP method type. Parameter accepts the following HTTP methods:
     * <ul>
     * <li>GET</li>
     * <li>HEAD</li>
     * <li>POST</li>
     * <li>PUT</li>
     * <li>OPTIONS</li>
     * <li>DELETE</li>
     * <li>TRACE</li>
     * <li>PATCH</li>
     * </ul>
     * @param url for example https://www.vividus.org/sitemap.xml
     * @throws IOException If an input or output exception occurred
     */
    @When("I issue a HTTP $httpMethod request for a resource with the URL '$url'")
    public void whenIDoHttpRequest(HttpMethod httpMethod, String url) throws IOException
    {
        executeHttpCallSafely(prepareHttpRequestBuilder(httpMethod, url));
    }

    /**
     * Step is sending HTTP request for the given <b>relative URL</b> and saving HTTP response into Scenario-level
     * variable.<br> This step can use the request body that was set before. Request body shouldn't be set for methods
     * that can't contain body (GET, HEAD, OPTIONS, TRACE), and should be set for methods that must contain body (PATCH,
     * POST, PUT).
     * @param httpMethod HTTP method type. Parameter accepts the following HTTP methods: <ul> <li>GET</li> <li>HEAD</li>
     * <li>POST</li> <li>PUT</li> <li>OPTIONS</li> <li>DELETE</li> <li>TRACE</li> <li>PATCH</li> </ul>
     * @param relativeURL relative URL
     * @return HTTP response
     * @throws IOException If an input or output exception occurred
     */
    @When("I send HTTP $httpMethod to the relative URL '$relativeURL'")
    public HttpResponse whenIDoHttpRequestToRelativeURL(HttpMethod httpMethod, String relativeURL) throws IOException
    {
        return executeHttpCallSafely(prepareHttpRequestBuilder(httpMethod, apiEndpoint).withRelativeUrl(relativeURL));
    }

    /**
     * Step is sending HTTP request for the given <b>relative URL</b>
     * and saving HTTP response into Scenario-level variable.<br>
     * Only methods that can contain body should be used.
     * @param httpMethod HTTP method type. Parameter accepts the following HTTP methods: <ul> <li>POST</li> <li>PUT</li>
     * <li>DELETE</li> <li>PATCH</li> </ul>
     * @param relativeURL relative URL
     * @param content HTTP method request body
     * @return HTTP response
     * @throws IOException If an input or output exception occurred
     */
    @When(value = "I send HTTP $httpMethod to the relative URL '$relativeURL' with content: '$content'", priority = 1)
    public HttpResponse whenIDoHttpRequestToRelativeURL(HttpMethod httpMethod, String relativeURL, String content)
            throws IOException
    {
        request(content);
        return whenIDoHttpRequestToRelativeURL(httpMethod, relativeURL);
    }

    /**
     * Set up custom request configurations
     * @param configItems Table representing list of configuration items with columns "name" and "value"
     *  specifying their names and values respectively.
     * Available configs:
     * <ul>
     * <li>expectContinueEnabled (boolean, default:{@code false})
     *  - determines whether the 'Expect: 100-Continue' handshake is enabled</li>
     * <li>staleConnectionCheckEnabled (boolean, default:{@code false})
     *  - determines whether stale connection check is to be used</li>
     * <li>redirectsEnabled (boolean, default:{@code true})
     *  - determines whether redirects should be handled automatically</li>
     * <li>relativeRedirectsAllowed (boolean, default:{@code true})
     *  - determines whether relative redirects should be rejected</li>
     * <li>circularRedirectsAllowed (boolean, default:{@code false})
     *  - determines whether circular redirects (redirects to the same location) should be allowed</li>
     * <li>authenticationEnabled (boolean, default:{@code true})
     *  - determines whether authentication should be handled automatically</li>
     * <li>contentCompressionEnabled (boolean, default:{@code true})
     *  - determines whether the target server is requested to compress content</li>
     * <li>normalizeUri (boolean, default:{@code true})
     *  - determines whether client should normalize URIs in requests or not</li>
     * <li>maxRedirects (int, default:{@code 50}) - returns the maximum number of redirects to be followed</li>
     * <li>connectionRequestTimeout (int, default:{@code -1})
     *  - returns the timeout in milliseconds used when requesting a connection from the connection manager</li>
     * <li>connectTimeout (int, default:{@code -1})
     *  - determines the timeout in milliseconds until a connection is established</li>
     * <li>socketTimeout (int, default:{@code -1})
     *  - defines the socket timeout ({@code SO_TIMEOUT}) in milliseconds,
     * which is the timeout for waiting for data  or, put differently,
     * a maximum period inactivity between two consecutive data packets</li>
     * <li>cookieSpec (String, default:{@code null})
     *  - determines the name of the cookie specification to be used for HTTP state management</li>
     * </ul>
     * @throws ReflectiveOperationException if any unknown parameter is set in configuration items
     */
    @When("I set HTTP request configuration:$configItems")
    public void setCustomRequestConfig(ExamplesTable configItems) throws ReflectiveOperationException
    {
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
        for (Entry<String, String> entry : configItems.getRow(0).entrySet())
        {
            Field field = requestConfigBuilder.getClass().getDeclaredField(entry.getKey());
            field.setAccessible(true);
            field.set(requestConfigBuilder, castType(field.getType().getName(), entry.getValue()));
        }
        httpTestContext.putRequestConfig(requestConfigBuilder.build());
    }

    private static Object castType(String typeName, String value)
    {
        switch (typeName)
        {
            case "boolean":
                return Boolean.parseBoolean(value);
            case "int":
                return Integer.parseInt(value);
            default:
                return value;
        }
    }

    private HttpRequestBuilder prepareHttpRequestBuilder(HttpMethod httpMethod, String endpoint)
    {
        HttpRequestBuilder httpRequestBuilder = HttpRequestBuilder.create().withHttpMethod(httpMethod)
                .withEndpoint(endpoint).withHeaders(httpTestContext.pullRequestHeaders());
        httpTestContext.pullRequestEntity().ifPresent(httpRequestBuilder::withContent);
        return httpRequestBuilder;
    }

    private HttpResponse executeHttpCallSafely(HttpRequestBuilder builder) throws IOException
    {
        try
        {
            HttpClientContext httpClientContext = new HttpClientContext();
            httpTestContext.getCookieStore().ifPresent(httpClientContext::setCookieStore);
            httpTestContext.getRequestConfig().ifPresent(httpClientContext::setRequestConfig);

            HttpResponse httpResponse = httpClient.execute(builder.build(), httpClientContext);
            LOGGER.info("Response time: {} ms", httpResponse.getResponseTimeInMs());
            return httpResponse;
        }
        catch (HttpRequestBuildException | ConnectionClosedException e)
        {
            softAssert.recordFailedAssertion(e);
            return null;
        }
    }

    public void setApiEndpoint(String apiEndpoint)
    {
        this.apiEndpoint = apiEndpoint;
    }

    public void setHttpClient(IHttpClient httpClient)
    {
        this.httpClient = httpClient;
    }
}
