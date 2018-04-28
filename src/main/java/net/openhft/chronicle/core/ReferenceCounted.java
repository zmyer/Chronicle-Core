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

package net.openhft.chronicle.core;

import net.openhft.chronicle.core.io.Closeable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * A resource which is reference counted and freed when the refCount drop to 0.
 */
public interface ReferenceCounted {
    static void releaseAll(@NotNull List<WeakReference<ReferenceCounted>> refCounts) {
        for (@Nullable WeakReference<? extends ReferenceCounted> refCountRef : refCounts) {
            if (refCountRef == null)
                continue;
            @Nullable ReferenceCounted refCounted = refCountRef.get();
            if (refCounted != null) {
                try {
                    refCounted.release();
                } catch (IllegalStateException e) {
                    LoggerFactory.getLogger(Closeable.class).debug("", e);
                }
            }
        }
    }

    /**
     * release a reference counted object
     *
     * @param o to release if ReferenceCounted
     */
    static void release(Object o) {
        if (o instanceof ReferenceCounted) {
            @NotNull ReferenceCounted rc = (ReferenceCounted) o;
            try {
                rc.release();
            } catch (IllegalStateException e) {
                LoggerFactory.getLogger(Closeable.class).debug("", e);
            }
        }
    }

    /**
     * reserve a resource
     *
     * @throws IllegalStateException if the resource has already been freed.
     */
    void reserve() throws IllegalStateException;

    /**
     * release a resource
     *
     * @throws IllegalStateException if the resource has already been freed.
     */
    void release() throws IllegalStateException;

    long refCount();

    default boolean tryReserve() {
        try {
            if (refCount() > 0) {
                reserve();
                return true;
            }

        } catch (IllegalStateException ignored) {
            // expected
        }

        return false;
    }
}