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

package org.vividus.bdd.report.allure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vividus.bdd.report.allure.model.ScenarioExecutionStage;
import org.vividus.bdd.report.allure.model.StoryExecutionStage;
import org.vividus.testcontext.SimpleTestContext;
import org.vividus.testcontext.TestContext;

import io.qameta.allure.model.Label;

class AllureRunContextTests
{
    private static final String STORY_LABELS = "allureStoryLabels";
    private static final String GIVEN_STORY_LABELS = "allureGivenStoryLabels";

    private final TestContext testContext = new SimpleTestContext();
    private final AllureRunContext allureRunContext = new AllureRunContext();

    @BeforeEach
    void beforeEach()
    {
        allureRunContext.setTestContext(testContext);
    }

    @Test
    void testCreateNewStoryLabelsForRootStory()
    {
        List<Label> storyLabels = allureRunContext.createNewStoryLabels(false);
        assertEquals(testContext.get(STORY_LABELS), storyLabels);
    }

    @Test
    void testCreateNewStoryLabelsForGivenStory()
    {
        List<Label> storyLabels = allureRunContext.createNewStoryLabels(true);
        assertEquals(testContext.get(GIVEN_STORY_LABELS, Deque.class).peek(), storyLabels);
    }

    @Test
    void testGetCurrentStoryLabelsWhenNoGivenStory()
    {
        List<Label> storyLabels = allureRunContext.getCurrentStoryLabels();
        assertEquals(testContext.get(STORY_LABELS), storyLabels);
    }

    @Test
    void testGetCurrentStoryLabelsWhenGivenStory()
    {
        List<Label> expected = new ArrayList<>();
        testContext.get(GIVEN_STORY_LABELS, LinkedList::new).push(expected);
        List<Label> storyLabels = allureRunContext.getCurrentStoryLabels();
        assertEquals(expected, storyLabels);
    }

    @Test
    void testGetRootStoryLabelsWhenNotInitialized()
    {
        List<Label> rootStoryLabels = allureRunContext.getRootStoryLabels();
        assertEquals(testContext.get(STORY_LABELS), rootStoryLabels);
    }

    @Test
    void testGetRootStoryLabelsWhenInitialized()
    {
        List<Label> rootStoryLabels1 = allureRunContext.getRootStoryLabels();
        List<Label> rootStoryLabels2 = allureRunContext.getRootStoryLabels();
        assertEquals(rootStoryLabels1, rootStoryLabels2);
    }

    @Test
    void testResetCurrentStoryLabelsForRootStory()
    {
        allureRunContext.resetCurrentStoryLabels(false);
        assertNull(testContext.get(STORY_LABELS));
    }

    @Test
    void testResetCurrentStoryLabelsForGivenStory()
    {
        testContext.get(GIVEN_STORY_LABELS, LinkedList::new).push(new ArrayList<>());
        allureRunContext.resetCurrentStoryLabels(true);
        assertEquals(0, testContext.get(GIVEN_STORY_LABELS, Deque.class).size());
    }

    @Test
    void initExecutionStages()
    {
        allureRunContext.initExecutionStages();
        assertNull(allureRunContext.getStoryExecutionStage());
        assertNull(allureRunContext.getScenarioExecutionStage());
    }

    @Test
    void setStoryExecutionStage()
    {
        allureRunContext.initExecutionStages();
        allureRunContext.setStoryExecutionStage(StoryExecutionStage.BEFORE_SCENARIO);
        assertEquals(StoryExecutionStage.BEFORE_SCENARIO, allureRunContext.getStoryExecutionStage());
    }

    @Test
    void setScenarioExecutionStage()
    {
        assertNull(allureRunContext.getScenarioExecutionStage());
        allureRunContext.initExecutionStages();
        allureRunContext.setScenarioExecutionStage(ScenarioExecutionStage.IN_PROGRESS);
        assertEquals(ScenarioExecutionStage.IN_PROGRESS, allureRunContext.getScenarioExecutionStage());
    }

    @Test
    void resetScenarioExecutionStage()
    {
        allureRunContext.initExecutionStages();
        allureRunContext.setScenarioExecutionStage(ScenarioExecutionStage.IN_PROGRESS);
        allureRunContext.resetScenarioExecutionStage();
        assertNull(allureRunContext.getScenarioExecutionStage());
    }

    @Test
    void setStoryExecutionStageForGivenStory()
    {
        allureRunContext.initExecutionStages();
        allureRunContext.setStoryExecutionStage(StoryExecutionStage.BEFORE_SCENARIO);
        allureRunContext.initExecutionStages();
        allureRunContext.setStoryExecutionStage(StoryExecutionStage.AFTER_SCENARIO);
        assertEquals(StoryExecutionStage.AFTER_SCENARIO, allureRunContext.getStoryExecutionStage());
        allureRunContext.resetExecutionStages();
        assertEquals(StoryExecutionStage.BEFORE_SCENARIO, allureRunContext.getStoryExecutionStage());
    }

    @Test
    void setScenarioExecutionStageForGivenStory()
    {
        allureRunContext.initExecutionStages();
        allureRunContext.setScenarioExecutionStage(ScenarioExecutionStage.IN_PROGRESS);
        allureRunContext.initExecutionStages();
        allureRunContext.setScenarioExecutionStage(ScenarioExecutionStage.BEFORE_STEPS);
        assertEquals(ScenarioExecutionStage.BEFORE_STEPS, allureRunContext.getScenarioExecutionStage());
        allureRunContext.resetExecutionStages();
        assertEquals(ScenarioExecutionStage.IN_PROGRESS, allureRunContext.getScenarioExecutionStage());
    }
}
