/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.core.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/*
 * Created by Peter Lawrey on 29/11/16.
 */
@RunWith(Parameterized.class)
public class ObjectUtilsConvertToTest {

    private final Object converted;
    private final String input;

    public ObjectUtilsConvertToTest(Object converted, String input) {
        this.converted = converted;
        this.input = input;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Boolean.TRUE, "Y"},
                {Boolean.TRUE, "yes"},
                {Boolean.FALSE, "N"},
                {Boolean.FALSE, "no"},
        });
    }

    @Test
    public void convertTo() throws Exception {
        assertEquals(converted, ObjectUtils.convertTo(converted.getClass(), input));
    }

}