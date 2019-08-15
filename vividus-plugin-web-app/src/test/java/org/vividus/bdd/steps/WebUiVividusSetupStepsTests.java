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

package org.vividus.bdd.steps;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.selenium.IBrowserWindowSizeProvider;
import org.vividus.selenium.IWebDriverProvider;
import org.vividus.selenium.manager.IWebDriverManager;
import org.vividus.ui.web.action.IWindowsActions;

@ExtendWith(MockitoExtension.class)
class WebUiVividusSetupStepsTests
{
    @Mock
    private IWebDriverProvider webDriverProvider;

    @Mock
    private IBrowserWindowSizeProvider browserWindowSizeProvider;

    @Mock
    private IWebDriverManager webDriverManager;

    @Mock
    private IWindowsActions windowsActions;

    @InjectMocks
    private WebUiVividusSetupSteps webUiSetupSteps;

    @Test
    void testAfterScenario()
    {
        when(webDriverProvider.isWebDriverInitialized()).thenReturn(true);
        webUiSetupSteps.setWindowsStrategy(WindowsStrategy.CLOSE_ALL_EXCEPT_ONE);
        webUiSetupSteps.afterScenario();
        verify(windowsActions).closeAllWindowsExceptOne();
    }
}