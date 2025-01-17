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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.jbehave.core.embedder.EmbedderControls;
import org.jbehave.core.embedder.EmbedderMonitor;
import org.jbehave.core.embedder.MetaFilter;
import org.jbehave.core.embedder.PerformableTree;
import org.jbehave.core.embedder.StoryManager;
import org.jbehave.core.embedder.StoryTimeouts.TimeoutParser;
import org.jbehave.core.failures.BatchFailures;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.bdd.context.BddRunContext;
import org.vividus.bdd.spring.Configuration;

@ExtendWith(MockitoExtension.class)
class BatchedEmbedderTests
{
    private static final String PATH = "path1";
    private static final String BATCH = "batch1";

    @Mock
    private EmbedderMonitor embedderMonitor;

    @Mock
    private IEmbedderControlsProvider embedderControlsProvider;

    @Mock
    private BddRunContext bddRunContext;

    @InjectMocks
    private BatchedEmbedder embedder;

    @Test
    void testRunStoriesAsPathsIgnoreFailureInBatchesTrue()
    {
        embedder.setIgnoreFailureInBatches(false);
        BatchedEmbedder spy = Mockito.spy(embedder);
        EmbedderControls mockedEmbedderControls = mockEmbedderControls(spy);
        when(mockedEmbedderControls.ignoreFailureInStories()).thenReturn(true);
        when(mockedEmbedderControls.threads()).thenReturn(1);
        StoryManager storyManager = mock(StoryManager.class);
        doReturn(storyManager).when(spy).storyManager();
        MetaFilter mockedFilter = mock(MetaFilter.class);
        doReturn(mockedFilter).when(spy).metaFilter();
        ExecutorService executorService = mock(ExecutorService.class);
        spy.executorService();
        spy.useExecutorService(executorService);
        List<String> testStoryPaths = List.of(PATH);
        String key = "key";
        Throwable throwable = mock(Throwable.class);
        doNothing().when(storyManager).runStoriesAsPaths(eq(testStoryPaths), eq(mockedFilter), argThat(failures ->
        {
            failures.put(key, throwable);
            return true;
        }));
        Map<String, List<String>> batches = new LinkedHashMap<>();
        batches.put(BATCH, testStoryPaths);
        batches.put("batch2", List.of("path2"));
        spy.runStoriesAsPaths(batches);
        InOrder inOrder = inOrder(spy, embedderMonitor, storyManager, bddRunContext, executorService);
        inOrder.verify(spy).processSystemProperties();
        inOrder.verify(embedderMonitor).usingControls(mockedEmbedderControls);
        inOrder.verify(bddRunContext).putRunningBatch(BATCH);
        inOrder.verify(storyManager).runStoriesAsPaths(eq(testStoryPaths), eq(mockedFilter), argThat(
            failures -> failures.size() == 1 && failures.containsKey(key) && failures.containsValue(throwable)));
        inOrder.verify(bddRunContext).removeRunningBatch();
        inOrder.verify(executorService).shutdownNow();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void testRunStoriesAsPaths()
    {
        embedder.setIgnoreFailureInBatches(true);
        BatchedEmbedder spy = Mockito.spy(embedder);
        EmbedderControls mockedEmbedderControls = mockEmbedderControls(spy);
        when(mockedEmbedderControls.threads()).thenReturn(1);
        StoryManager storyManager = mock(StoryManager.class);
        doReturn(storyManager).when(spy).storyManager();
        MetaFilter mockedFilter = mock(MetaFilter.class);
        doReturn(mockedFilter).when(spy).metaFilter();
        ExecutorService executorService = mock(ExecutorService.class);
        spy.executorService();
        spy.useExecutorService(executorService);
        List<String> testStoryPaths = List.of(PATH);
        spy.runStoriesAsPaths(Map.of(BATCH, testStoryPaths));
        InOrder inOrder = inOrder(spy, embedderMonitor, storyManager, bddRunContext, executorService);
        inOrder.verify(spy).processSystemProperties();
        inOrder.verify(embedderMonitor).usingControls(mockedEmbedderControls);
        inOrder.verify(bddRunContext).putRunningBatch(BATCH);
        inOrder.verify(storyManager).runStoriesAsPaths(eq(testStoryPaths), eq(mockedFilter), any(BatchFailures.class));
        inOrder.verify(bddRunContext).removeRunningBatch();
        inOrder.verify(executorService).shutdownNow();
    }

    @Test
    void testRunStoriesAsPathsSkip()
    {
        embedder.setIgnoreFailureInBatches(true);
        BatchedEmbedder spy = Mockito.spy(embedder);
        EmbedderControls mockedEmbedderControls = mockEmbedderControls(spy);
        when(mockedEmbedderControls.skip()).thenReturn(true);
        List<String> testStoryPaths = List.of(PATH);
        spy.runStoriesAsPaths(Map.of(BATCH, testStoryPaths));
        verify(spy).processSystemProperties();
        verify(embedderMonitor).usingControls(mockedEmbedderControls);
        verify(embedderMonitor).storiesSkipped(testStoryPaths);
    }

    @Test
    void testStoryManager()
    {
        Configuration mockedConfiguration = mock(Configuration.class);
        ExtendedStoryReporterBuilder mockedExtendedStoryReporterBuilder = mock(ExtendedStoryReporterBuilder.class);
        when(mockedConfiguration.storyReporterBuilder()).thenReturn(mockedExtendedStoryReporterBuilder);
        BatchedEmbedder spy = Mockito.spy(embedder);
        spy.useConfiguration(mockedConfiguration);
        InjectableStepsFactory mockedInjectableStepsFactory = mock(InjectableStepsFactory.class);
        spy.useStepsFactory(mockedInjectableStepsFactory);
        EmbedderControls mockedEmbedderControls = mockEmbedderControls(spy);
        ExecutorService mockedExecutorService = mock(ExecutorService.class);
        spy.useExecutorService(mockedExecutorService);
        PerformableTree mockedPerformableTree = mock(BatchedPerformableTree.class);
        PerformableTree.PerformableRoot mockedPerformableRoot = mock(PerformableTree.PerformableRoot.class);
        when(mockedPerformableTree.getRoot()).thenReturn(mockedPerformableRoot);
        spy.usePerformableTree(mockedPerformableTree);
        TimeoutParser mockedTimeoutParser = mock(TimeoutParser.class);
        TimeoutParser[] timeoutParsers = { mockedTimeoutParser };
        spy.useTimeoutParsers(timeoutParsers);
        StoryManager expected = new StoryManager(mockedConfiguration, mockedInjectableStepsFactory,
                mockedEmbedderControls, embedderMonitor, mockedExecutorService, mockedPerformableTree, timeoutParsers);
        StoryManager actual = spy.storyManager();
        assertEquals(expected.performableRoot(), actual.performableRoot());
    }

    @Test
    void testPerformableTree()
    {
        BatchedPerformableTree mockedPerformableTree = mock(BatchedPerformableTree.class);
        embedder.usePerformableTree(mockedPerformableTree);
        embedder.performableTree();
        verify(mockedPerformableTree).setReportBeforeStories(true);
        verify(mockedPerformableTree).setReportAfterStories(false);
    }

    @Test
    void testEmbedderControls()
    {
        embedder.embedderControls();
        verify(embedderControlsProvider).getDefault();
    }

    @Test
    void testSetPerformableTree()
    {
        PerformableTree mockedPerformableTree = mock(BatchedPerformableTree.class);
        BatchedEmbedder spy = Mockito.spy(embedder);
        spy.setPerformableTree(mockedPerformableTree);
        verify(spy).usePerformableTree(mockedPerformableTree);
    }

    private EmbedderControls mockEmbedderControls(BatchedEmbedder spy)
    {
        EmbedderControls mockedEmbedderControls = mock(EmbedderControls.class);
        doReturn(mockedEmbedderControls).when(spy).embedderControls();
        return mockedEmbedderControls;
    }
}
