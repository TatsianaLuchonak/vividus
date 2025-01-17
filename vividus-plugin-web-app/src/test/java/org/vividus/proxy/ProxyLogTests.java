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

package org.vividus.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.http.HttpMethod;

import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarContent;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarLog;
import net.lightbody.bmp.core.har.HarNameVersion;
import net.lightbody.bmp.core.har.HarRequest;
import net.lightbody.bmp.core.har.HarResponse;

@ExtendWith(MockitoExtension.class)
class ProxyLogTests
{
    private static final String URL = "url";
    private static final String TEXT = "text";

    @Mock
    private Har har;

    @InjectMocks
    private ProxyLog proxyLog;

    @Test
    void testClear()
    {
        HarNameVersion creator = new HarNameVersion("name", "version");
        HarLog oldHarLog = new HarLog(creator);
        Har har = new Har();
        har.setLog(oldHarLog);
        ProxyLog proxyLog = new ProxyLog(har);
        proxyLog.clear();
        HarLog newHarLog = har.getLog();
        assertNotEquals(oldHarLog, newHarLog);
    }

    @Test
    void testGetRequestUrls()
    {
        mockHarLog(getRequest(), null);
        List<String> requestUrls = proxyLog.getRequestUrls();
        assertEquals(Collections.singletonList(URL), requestUrls);
    }

    @Test
    void testGetRequestUrlsMatchPattern()
    {
        mockHarLog(getRequest(), null);
        List<String> requestUrls = proxyLog.getRequestUrls(URL);
        assertEquals(Collections.singletonList(URL), requestUrls);
    }

    @Test
    void testGetRequestUrlsDontMatchPattern()
    {
        mockHarLog(getRequest(), null);
        List<String> requestUrls = proxyLog.getRequestUrls(TEXT);
        assertTrue(requestUrls.isEmpty());
    }

    @Test
    void testGetRequestUrl()
    {
        mockHarLog(getRequest(), null);
        String requestUrl = proxyLog.getRequestUrl(URL);
        assertEquals(URL, requestUrl);
    }

    @Test
    void testGetLogEntries()
    {
        HarEntry harEntry = mockHarLog(getRequest(), null);
        List<HarEntry> actualHarEntries = proxyLog.getLogEntries(URL);
        assertEquals(Collections.singletonList(harEntry), actualHarEntries);
    }

    @Test
    void testGetLogEntriesMatchingHttpMethodAndUrl()
    {
        HttpMethod httpMethod = HttpMethod.POST;
        HarEntry harEntry = mockHarLog(getRequest(), null);
        harEntry.getRequest().setMethod(httpMethod.toString());
        List<HarEntry> actualHarEntries = proxyLog.getLogEntries(httpMethod, URL);
        assertEquals(Collections.singletonList(harEntry), actualHarEntries);
    }

    @Test
    void testGetLogEntriesDontMatchPattern()
    {
        mockHarLog(getRequest(), null);
        List<HarEntry> actualHarEntries = proxyLog.getLogEntries(TEXT);
        assertTrue(actualHarEntries.isEmpty());
    }


    @Test
    void testGetLogEntriesNonMatchingHttpMethodButMatchingUrl()
    {
        HarEntry harEntry = mockHarLog(getRequest(), null);
        harEntry.getRequest().setMethod(HttpMethod.GET.toString());
        List<HarEntry> actualHarEntries = proxyLog.getLogEntries(HttpMethod.POST, URL);
        assertTrue(actualHarEntries.isEmpty());
    }

    @Test
    void testGetLogEntriesMatchingHttpMethodButNonMatchingUrl()
    {
        HttpMethod httpMethod = HttpMethod.POST;
        HarEntry harEntry = mockHarLog(getRequest(), null);
        harEntry.getRequest().setMethod(httpMethod.toString());
        List<HarEntry> actualHarEntries = proxyLog.getLogEntries(httpMethod, TEXT);
        assertTrue(actualHarEntries.isEmpty());
    }

    @Test
    void testGetResponsesMatchUrlPattern()
    {
        mockHarLog(getRequest(), mockGetResponse(URL, TEXT));
        List<String> actualresponses = proxyLog.getResponses(URL);
        assertEquals(Collections.singletonList(URL), actualresponses);
    }

    @Test
    void testGetResponsesDontMatchUrlPattern()
    {
        HarResponse response = Mockito.mock(HarResponse.class);
        mockHarLog(getRequest(), response);
        List<String> actualresponses = proxyLog.getResponses(TEXT);
        assertTrue(actualresponses.isEmpty());
    }

    @Test
    void testGetResponseIllegalArgumentException()
    {
        HarResponse response = Mockito.mock(HarResponse.class);
        mockHarLog(getRequest(), response);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> proxyLog.getResponse(TEXT));
        assertEquals("Response is not found by URL pattern: " + TEXT, exception.getMessage());
    }

    @Test
    void testGetResponse()
    {
        mockHarLog(getRequest(), mockGetResponse(URL, TEXT));
        String actual = proxyLog.getResponse(URL);
        assertEquals(URL, actual);
    }

    @Test
    void testGetResponses()
    {
        mockHarLog(null, mockGetResponse(URL, TEXT));
        List<String> actualresponses = proxyLog.getResponses();
        assertEquals(Collections.singletonList(URL), actualresponses);
    }

    @Test
    void testGetRequestUrlIllegalArgumentException()
    {
        mockHarLog(getRequest(), null);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> proxyLog.getRequestUrl(TEXT));
        assertEquals("Request URL is not found by pattern: " + TEXT, exception.getMessage());
    }

    private HarResponse mockGetResponse(String contentText, String contentMimeType)
    {
        HarContent content = Mockito.mock(HarContent.class);
        when(content.getText()).thenReturn(contentText);
        when(content.getMimeType()).thenReturn(contentMimeType);
        HarResponse response = Mockito.mock(HarResponse.class);
        when(response.getContent()).thenReturn(content);
        return response;
    }

    private HarRequest getRequest()
    {
        HarRequest request = new HarRequest();
        request.setUrl(URL);
        return request;
    }

    private HarEntry mockHarLog(HarRequest request, HarResponse response)
    {
        HarEntry harEntry = new HarEntry();
        harEntry.setRequest(request);
        harEntry.setResponse(response);
        HarLog harLog = new HarLog();
        harLog.addEntry(harEntry);
        when(har.getLog()).thenReturn(harLog);
        return harEntry;
    }
}
