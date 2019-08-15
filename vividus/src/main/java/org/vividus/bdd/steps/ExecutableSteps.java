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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.jbehave.core.annotations.Alias;
import org.jbehave.core.annotations.When;
import org.jbehave.core.model.ExamplesTable;
import org.vividus.bdd.context.IBddVariableContext;

public class ExecutableSteps
{
    public static final int EXECUTIONS_NUMBER_THRESHOLD = 50;

    @Inject private ISubStepExecutorFactory subStepExecutorFactory;
    @Inject private IBddVariableContext bddVariableContext;

    /**
     * Steps designed to perform steps if condition is true
     * <b>if</b> condition is true
     * Performs all steps. No steps will be performed
     * in case of condition is false
     * <br> Usage example:
     * <code>
     * <br>When the condition 'true' is true I do
     * <br>|step|
     * <br>|When I compare against baseline with name 'test_composit1_step'|
     * <br>|When I click on all elements by xpath './/a[@title='Close']'|
     * </code>
     * @param condition verifiable condition
     * @param stepsToExecute examples table with steps to execute if condition is true
     */
    @SuppressWarnings("MagicNumber")
    @When(value = "the condition `$condition` is true I do$stepsToExecute", priority = 5)
    @Alias(value = "the condition '$condition' is true I do$stepsToExecute")
    public void performAllStepsIfConditionIsTrue(boolean condition, ExamplesTable stepsToExecute)
    {
        if (condition)
        {
            ISubStepExecutor subStepExecutor = subStepExecutorFactory.createSubStepExecutor(stepsToExecute);
            subStepExecutor.execute(Optional.empty());
        }
    }

    /** If the variable with name is not set into context executes steps.
     * <b>Example:</b>
     * <br> When variable 'token' is not set perform:
     * <br> |When I login|
     * @param name variable name to check
     * @param stepsToExecute steps to execute
     */
    @When("variable '$name' is not set I do:$stepsToExecute")
    public void ifVariableNotSetPerformSteps(String name, ExamplesTable stepsToExecute)
    {
        if (bddVariableContext.getVariable(name) == null)
        {
            subStepExecutorFactory.createSubStepExecutor(stepsToExecute).execute(Optional.empty());
        }
    }

    /**
     * Steps designed to perform steps <b>number</b> times.
     * Executions number must be in the range from 0 to 50.
     * <br> Usage example:
     * <code>
     * <br>When I `2` times do:
     * <br>|step|
     * <br>|When I enter `text` in field located `By.xpath(//*[@id="identifierId"])`|
     * </code>
     * @param number executions number
     * @param stepsToExecute examples table with steps to execute <b>number</b> times
     */
    @When("I `$number` times do:$stepsToExecute")
    public void performStepsNumberTimes(int number, ExamplesTable stepsToExecute)
    {
        int minimum = 0;
        checkArgument(minimum <= number && number <= EXECUTIONS_NUMBER_THRESHOLD,
            "Please, specify executions number in the range from %s to %s", minimum, EXECUTIONS_NUMBER_THRESHOLD);
        ISubStepExecutor subStepExecutor = subStepExecutorFactory.createSubStepExecutor(stepsToExecute);
        IntStream.range(0, number).forEach(i -> subStepExecutor.execute(Optional.empty()));
    }
}