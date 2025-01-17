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

package org.vividus.bdd.spring;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jbehave.core.configuration.Keywords;
import org.jbehave.core.embedder.StoryControls;
import org.jbehave.core.model.ExamplesTableFactory;
import org.jbehave.core.model.ExamplesTableProperties;
import org.jbehave.core.model.TableTransformers.TableTransformer;
import org.jbehave.core.parsers.RegexCompositeParser;
import org.jbehave.core.parsers.RegexStoryParser;
import org.jbehave.core.steps.DelegatingStepMonitor;
import org.jbehave.core.steps.ParameterConverters.ParameterConverter;
import org.jbehave.core.steps.StepMonitor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.vividus.bdd.IPathFinder;
import org.vividus.bdd.resource.ResourceBatch;
import org.vividus.bdd.steps.ExpressionAdaptor;
import org.vividus.bdd.steps.ParameterAdaptor;
import org.vividus.bdd.steps.ParameterConvertersDecorator;

@RunWith(PowerMockRunner.class)
public class ConfigurationTests
{
    private static final String SEPARATOR = "|";

    @Mock
    private IPathFinder pathFinder;

    @Mock
    private ParameterAdaptor parameterAdaptor;

    @Mock
    private ExpressionAdaptor expressionAdaptor;

    @InjectMocks
    private Configuration configuration;

    @Before
    public void before()
    {
        MockitoAnnotations.initMocks(this);
        configuration.setCustomConverters(List.of());
        configuration.setCustomTableTransformers(Map.of());
        configuration.setExamplesTableHeaderSeparator(SEPARATOR);
        configuration.setExamplesTableValueSeparator(SEPARATOR);
    }

    @Test
    @PrepareForTest(Configuration.class)
    public void testInit() throws Exception
    {
        String compositePathPatterns = "**/*.steps";
        List<String> compositePaths = List.of("/path/to/composite.steps");
        when(pathFinder.findPaths(equalToCompositeStepsBatch(compositePathPatterns))).thenReturn(compositePaths);

        Configuration spy = spy(configuration);
        Keywords keywords = mock(Keywords.class);
        PowerMockito.whenNew(Keywords.class).withArguments(Keywords.defaultKeywords()).thenReturn(keywords);
        ExamplesTableFactory examplesTableFactory = mock(ExamplesTableFactory.class);
        PowerMockito.whenNew(ExamplesTableFactory.class).withArguments(spy).thenReturn(examplesTableFactory);
        RegexStoryParser regexStoryParser = mock(RegexStoryParser.class);
        PowerMockito.whenNew(RegexStoryParser.class).withArguments(keywords, examplesTableFactory)
                .thenReturn(regexStoryParser);
        RegexCompositeParser regexCompositeParser = mock(RegexCompositeParser.class);
        PowerMockito.whenNew(RegexCompositeParser.class).withArguments(keywords).thenReturn(regexCompositeParser);
        ParameterConvertersDecorator parameterConverters = mock(ParameterConvertersDecorator.class);
        PowerMockito.whenNew(ParameterConvertersDecorator.class).withArguments(spy, parameterAdaptor, expressionAdaptor)
                .thenReturn(parameterConverters);
        @SuppressWarnings("rawtypes")
        List<ParameterConverter> parameterConverterList = List.of();
        when(parameterConverters.addConverters(parameterConverterList)).thenReturn(parameterConverters);
        StoryControls storyControls = mock(StoryControls.class);
        spy.setCustomConverters(parameterConverterList);
        spy.setCompositePaths(compositePathPatterns);
        spy.setStoryControls(storyControls);
        List<StepMonitor> stepMonitors = List.of(mock(StepMonitor.class));
        spy.setStepMonitors(stepMonitors);
        spy.init();
        verify(spy).useKeywords(keywords);
        verify(spy).useCompositePaths(new HashSet<>(compositePaths));
        verify(spy).useStoryParser(regexStoryParser);
        verify(spy).useCompositeParser(regexCompositeParser);
        verify(spy).useParameterConverters(parameterConverters);
        verify(spy).useStoryControls(storyControls);
        verifyStepMonitor(spy, stepMonitors.get(0));
    }

    private static void verifyStepMonitor(Configuration spy, StepMonitor expectedStepMonitorDelegate)
    {
        StepMonitor actualStepMonitor = spy.stepMonitor();
        assertThat(actualStepMonitor, instanceOf(DelegatingStepMonitor.class));
        String step = "step";
        boolean dryRun = true;
        Method method = null;
        actualStepMonitor.beforePerforming(step, dryRun, method);
        verify(expectedStepMonitorDelegate).beforePerforming(step, dryRun, method);
    }

    @Test
    public void testSetCustomTableTransformers() throws IOException
    {
        String name = "customTableTransformer";
        TableTransformer tableTransformer = mock(TableTransformer.class);
        configuration.setCustomTableTransformers(Map.of(name, tableTransformer));
        configuration.init();
        String tableAsString = "tableAsString";
        ExamplesTableProperties examplesTableProperties = new ExamplesTableProperties(new Properties());
        configuration.tableTransformers().transform(name, tableAsString, examplesTableProperties);
        verify(tableTransformer).transform(tableAsString, examplesTableProperties);
    }

    private static ResourceBatch equalToCompositeStepsBatch(String compositePathPatterns)
    {
        List<String> resourceInludePatterns = List.of(compositePathPatterns);
        return argThat(batch -> "/".equals(batch.getResourceLocation())
                && resourceInludePatterns.equals(batch.getResourceIncludePatterns())
                && List.of().equals(batch.getResourceExcludePatterns()));
    }

    @Test
    public void testSetDryRun() throws IOException
    {
        StoryControls storyControls = new StoryControls();
        storyControls.doDryRun(true);
        configuration.setStoryControls(storyControls);
        configuration.init();
        assertTrue(configuration.storyControls().dryRun());
    }
}
