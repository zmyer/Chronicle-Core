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

package net.openhft.chronicle.core.time;

import java.util.concurrent.atomic.AtomicReference;

/*
 * Created by Peter Lawrey on 10/03/16.
 */
public enum SystemTimeProvider implements TimeProvider {
    INSTANCE;

    static final AtomicReference<TimeProvider> TIME_PROVIDER = new AtomicReference<>(INSTANCE);

    static {
        // warmUp()
        for (int i = 0; i < 1000; i++)
            INSTANCE.currentTimeMicros();
    }

    long delta = 0;

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public long currentTimeMicros() {
        return currentTimeNanos() / 1000;
    }

    @Override
    public long currentTimeNanos() {
        long n0 = System.nanoTime();
        long nowMS = currentTimeMillis() * 1000000;
        long nowNS = n0;
        long estimate = nowNS + delta;

        if (estimate < nowMS) {
            delta = nowMS - nowNS;
            return nowMS;

        } else if (estimate > nowMS + 1000000) {
            nowMS += 1000000;
            delta = nowMS - nowNS;
            return nowMS;
        }
        return estimate;
    }
}
