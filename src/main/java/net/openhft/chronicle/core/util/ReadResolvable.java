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

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/*
 * Created by Peter Lawrey on 23/06/15.
 */
@FunctionalInterface
public interface ReadResolvable<T> {
    @SuppressWarnings("unchecked")
    static <T> T readResolve(Object o) {
        return (T) (o instanceof ReadResolvable
                ? ((ReadResolvable) o).readResolve()
                : o instanceof Serializable
                ? ObjectUtils.readResolve(o)
                : o);
    }

    /**
     * Post deserialization step
     *
     * @return the object to use instead.
     */
    @NotNull
    T readResolve();
}
