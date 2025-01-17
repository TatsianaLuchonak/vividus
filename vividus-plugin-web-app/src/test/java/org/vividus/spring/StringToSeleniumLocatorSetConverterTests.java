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

package org.vividus.spring;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

class StringToSeleniumLocatorSetConverterTests
{
    private final StringToSeleniumLocatorSetConverter converter = new StringToSeleniumLocatorSetConverter();

    @Test
    void testEmptyPropertyValueIsConvertedInEmptySet()
    {
        assertThat(converter.convert(""), instanceOf(Set.class));
    }

    @Test
    void testPropertyValueIsConvertedInSet()
    {
        Set<By> expected = new HashSet<>();
        String xpath = "//div[contains(@class,'pane-bv-reviews-product-reviews')";
        expected.add(By.xpath(xpath));
        String className = "breadcrumb";
        expected.add(By.className(className));
        Set<By> actual = converter.convert("By.xpath(" + xpath + "), By.className(" + className + ")");
        assertEquals(expected, actual);
    }

    @Test
    void testExceptionIsThrownWhileConvertingInvalidPropertyValue()
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> converter.convert("By.unknown(something)"));
        assertThat(exception.getMessage(), containsString("java.lang.NoSuchMethodException"));
    }
}
