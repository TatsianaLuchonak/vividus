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

package org.vividus.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.bdd.resource.ResourceBatch;

@ExtendWith(MockitoExtension.class)
class BatchedPathFinderTests
{
    @Mock
    private PathFinder pathFinder;

    @Mock
    private IBatchStorage batchStorage;

    @InjectMocks
    private BatchedPathFinder batchedPathFinder;

    @Test
    void testFindPaths() throws IOException
    {
        ResourceBatch resourceBatch = new ResourceBatch();
        resourceBatch.setResourceLocation("testLocation");
        resourceBatch.setResourceIncludePatterns("testIncludePattern");
        resourceBatch.setResourceExcludePatterns("testExcludePattern");
        String batchKey = "batch1";
        when(batchStorage.getBatches()).thenReturn(Map.of(batchKey, resourceBatch));
        List<String> testPaths = List.of("testPath");
        when(pathFinder.findPaths(resourceBatch)).thenReturn(testPaths);
        Map<String, List<String>> actual = batchedPathFinder.findPaths();
        Map<String, List<String>> expected = Map.of(batchKey, testPaths);
        assertEquals(expected, actual);
    }
}
