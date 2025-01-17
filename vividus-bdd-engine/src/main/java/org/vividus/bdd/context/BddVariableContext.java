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

package org.vividus.bdd.context;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vividus.bdd.variable.IVariablesFactory;
import org.vividus.bdd.variable.VariableScope;
import org.vividus.bdd.variable.Variables;
import org.vividus.testcontext.TestContext;

public class BddVariableContext implements IBddVariableContext
{
    private static final int VARIABLE_NAME_GROUP = 1;
    private static final int LIST_INDEX_GROUP = 2;
    private static final int MAP_KEY_GROUP = 3;
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("([^\\[\\].]*)(?:\\[(\\d+)])?(?:\\.(.*))?");
    private static final Logger LOGGER = LoggerFactory.getLogger(BddVariableContext.class);

    private TestContext testContext;
    private IVariablesFactory variablesFactory;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String variableKey)
    {
        Variables variables = getVariables();
        return (T) Stream.of(VariableScope.STEP, VariableScope.SCENARIO, VariableScope.STORY,
                VariableScope.NEXT_BATCHES, VariableScope.GLOBAL)
                .map(scope -> getVariable(variables.getVariables(scope), variableKey))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(System.getProperty(variableKey));
    }

    @Override
    public void putVariable(Set<VariableScope> variableScopes, String variableKey, Object variableValue)
    {
        variableScopes.forEach(s -> putVariable(s, variableKey, variableValue));
    }

    @Override
    public void putVariable(VariableScope variableScope, String variableKey, Object variableValue)
    {
        if (variableScope == VariableScope.GLOBAL)
        {
            throw new IllegalArgumentException("Setting of GLOBAL variables is forbidden");
        }
        LOGGER.info("Saving a value '{}' into the '{}' variable '{}'", variableValue, variableScope, variableKey);
        if (variableScope == VariableScope.NEXT_BATCHES)
        {
            variablesFactory.addNextBatchesVariable(variableKey, variableValue);
        }
        else
        {
            getVariables().getVariables(variableScope).put(variableKey, variableValue);
        }
    }

    @Override
    public void initVariables()
    {
        getVariables();
    }

    @Override
    public void clearVariables(VariableScope variableScope)
    {
        getVariables().getVariables(variableScope).clear();
    }

    @SuppressWarnings("unchecked")
    private Object getVariable(Map<String, Object> variables, String key)
    {
        Matcher variableMatcher = VARIABLE_PATTERN.matcher(key);
        if (!variableMatcher.find())
        {
            return variables.get(key);
        }
        String variableKey = variableMatcher.group(VARIABLE_NAME_GROUP);
        Object variable = variables.get(variableKey);
        if (variable != null)
        {
            String listIndex = variableMatcher.group(LIST_INDEX_GROUP);
            if (listIndex != null)
            {
                List<?> listVariable = (List<?>) variable;
                int elementIndex = Integer.parseInt(listIndex);
                variable = elementIndex < listVariable.size() ? listVariable.get(elementIndex) : null;
            }
            String mapKey = variableMatcher.group(MAP_KEY_GROUP);
            if (mapKey != null && variable instanceof Map)
            {
                variable = ((Map<String, ?>) variable).get(mapKey);
            }
        }
        return variable;
    }

    private Variables getVariables()
    {
        return testContext.get(Variables.class, variablesFactory::createVariables);
    }

    public void setTestContext(TestContext testContext)
    {
        this.testContext = testContext;
    }

    public void setVariablesFactory(IVariablesFactory variablesFactory)
    {
        this.variablesFactory = variablesFactory;
    }
}
