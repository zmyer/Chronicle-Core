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

package net.openhft.chronicle.core.threads;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public enum ThreadLocalHelper {
    ;

    @NotNull
    public static <T> T getTL(@NotNull ThreadLocal<WeakReference<T>> threadLocal, @NotNull Supplier<T> supplier) {
        @Nullable WeakReference<T> ref = threadLocal.get();
        @Nullable T ret = null;
        if (ref != null) ret = ref.get();
        if (ret == null) {
            ret = supplier.get();
            ref = new WeakReference<>(ret);
            threadLocal.set(ref);
        }
        return ret;
    }

    @NotNull
    public static <T, A> T getTL(@NotNull ThreadLocal<WeakReference<T>> threadLocal, A a, @NotNull Function<A, T> function) {
        return getTL(threadLocal, a, function, null, null);
    }

    @NotNull
    public static <T, A> T getTL(@NotNull final ThreadLocal<WeakReference<T>> threadLocal, final A a,
                                 @NotNull final Function<A, T> function,
                                 @Nullable final ReferenceQueue<T> referenceQueue,
                                 @Nullable final Consumer<WeakReference<T>> refConsumer) {
        @Nullable WeakReference<T> ref = threadLocal.get();
        T ret = null;
        if (ref != null) ret = ref.get();
        if (ret == null) {
            ret = function.apply(a);
            if (referenceQueue != null && refConsumer != null) {
                ref = new WeakReference<>(ret, referenceQueue);
                refConsumer.accept(ref);
            } else {
                ref = new WeakReference<>(ret);
            }
            threadLocal.set(ref);
        }
        return ret;
    }
}
