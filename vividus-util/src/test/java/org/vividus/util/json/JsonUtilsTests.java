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

package org.vividus.util.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.junit.jupiter.api.Test;

class JsonUtilsTests
{
    private static final String JSON_STRING = "{\"id\":\"1\",\"firstName\":\"name\"}";
    private static final String JSON_LIST_STRING = "[{\"id\":\"1\",\"first_name\":\"name1\"},"
            + " {\"id\":\"2\",\"first_name\":\"name2\"}]";

    private static final TestClass TEST_OBJECT;
    static
    {
        TEST_OBJECT = new TestClass();
        TEST_OBJECT.setId("1");
        TEST_OBJECT.setFirstName("name");
    }

    private JsonUtils jsonUtils = new JsonUtils(PropertyNamingStrategy.LOWER_CAMEL_CASE);

    @Test
    void testToJsonSuccessDefault()
    {
        String actaulJson = jsonUtils.toJson(TEST_OBJECT);
        assertEquals(JSON_STRING, actaulJson);
    }

    @Test
    void testToJsonSuccessCamelCase()
    {
        jsonUtils.setNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        String actaulJson = jsonUtils.toJson(TEST_OBJECT);
        assertEquals("{\"id\":\"1\",\"first_name\":\"name\"}", actaulJson);
    }

    @Test
    void testToObjectSuccess()
    {
        TestClass actualObj = jsonUtils.toObject(JSON_STRING, TestClass.class);
        assertEquals(TEST_OBJECT, actualObj);
    }

    @Test
    void testToObjectInputStreamSuccess()
    {
        InputStream jsonStream = new ByteArrayInputStream(JSON_STRING.getBytes(StandardCharsets.UTF_8));
        TestClass actualObj = jsonUtils.toObject(jsonStream, TestClass.class);
        assertEquals(TEST_OBJECT, actualObj);
    }

    @Test
    void testToObjectInvalidClass()
    {
        assertThrows(JsonProcessingException.class, () -> jsonUtils.toObject(JSON_STRING, JsonUtils.class));
    }

    @Test
    void testToObjectInputStreamInvalidClass()
    {
        InputStream jsonStream = new ByteArrayInputStream(JSON_STRING.getBytes(StandardCharsets.UTF_8));
        assertThrows(JsonProcessingException.class, () -> jsonUtils.toObject(jsonStream, JsonUtils.class));
    }

    @Test
    void testToObjectListSuccess()
    {
        jsonUtils = new JsonUtils(PropertyNamingStrategy.SNAKE_CASE);
        int expectedSize = 2;
        List<TestClass> actualList = jsonUtils.toObjectList(JSON_LIST_STRING, TestClass.class);
        assertEquals(actualList.size(), expectedSize);
    }

    @Test
    void testToObjectListInputStreamSuccess()
    {
        InputStream jsonListStream = new ByteArrayInputStream(JSON_LIST_STRING.getBytes(StandardCharsets.UTF_8));
        jsonUtils = new JsonUtils(PropertyNamingStrategy.SNAKE_CASE);
        int expectedSize = 2;
        List<TestClass> actualList = jsonUtils.toObjectList(jsonListStream, TestClass.class);
        assertEquals(actualList.size(), expectedSize);
    }

    @Test
    void testToObjectListNotList()
    {
        assertThrows(JsonProcessingException.class, () -> jsonUtils.toObjectList(JSON_STRING, TestClass.class));
    }

    @Test
    void testToObjectListInputStreamNotList()
    {
        InputStream jsonStream = new ByteArrayInputStream(JSON_STRING.getBytes(StandardCharsets.UTF_8));
        assertThrows(JsonProcessingException.class, () -> jsonUtils.toObjectList(jsonStream, TestClass.class));
    }

    @Test
    void testToObjectListInvalidClass()
    {
        assertThrows(JsonProcessingException.class, () -> jsonUtils.toObjectList(JSON_LIST_STRING, TestClass.class));
    }

    @Test
    void testToObjectListInputStreamInvalidClass()
    {
        InputStream jsonListStream = new ByteArrayInputStream(JSON_LIST_STRING.getBytes(StandardCharsets.UTF_8));
        assertThrows(JsonProcessingException.class, () -> jsonUtils.toObjectList(jsonListStream, TestClass.class));
    }

    @Test
    void testStringToJsonNode()
    {
        assertEquals(JSON_STRING, new JsonUtils().toJson(JSON_STRING).toString());
    }

    @Test
    void testBytesToJsonNode()
    {
        assertEquals(JSON_STRING, new JsonUtils().toJson(JSON_STRING.getBytes(StandardCharsets.UTF_8)).toString());
    }

    @Test
    void testStringToJsonNodeWithException()
    {
        assertThrows(JsonProcessingException.class, () -> jsonUtils.toJson("notJson"));
    }

    @Test
    void testEnableSerializationFeature()
    {
        testSerializationFeature(new JsonUtils(PropertyNamingStrategy.LOWER_CAMEL_CASE,
                Collections.singletonMap(SerializationFeature.WRAP_ROOT_VALUE, Boolean.TRUE)));
    }

    @Test
    void testSetSerializationFeature()
    {
        jsonUtils.setSerializationFeature(SerializationFeature.WRAP_ROOT_VALUE, true);
        testEnableSerializationFeature();
    }

    private static void testSerializationFeature(JsonUtils utils)
    {
        String actualJson = utils.toJson(TEST_OBJECT);
        assertEquals("{\"TestClass\":{\"id\":\"1\",\"firstName\":\"name\"}}", actualJson);
    }

    public static class TestClass
    {
        private String id;
        private String firstName;

        public String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            this.id = id;
        }

        public String getFirstName()
        {
            return firstName;
        }

        public void setFirstName(String firstName)
        {
            this.firstName = firstName;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(id, firstName);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj == null)
            {
                return false;
            }
            if (!(obj instanceof TestClass))
            {
                return false;
            }
            TestClass other = (TestClass) obj;
            return Objects.equals(id, other.id) && Objects.equals(firstName, other.firstName);
        }
    }
}
