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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.annotation.ForceInline;
import net.openhft.chronicle.core.annotation.Java9;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

import static java.lang.Character.toLowerCase;

/*
 * Created by Rob Austin
 */
public enum StringUtils {
    ;

    private static final Field S_VALUE, SB_VALUE, SB_COUNT;
    private static final long S_VALUE_OFFSET, SB_VALUE_OFFSET, SB_COUNT_OFFSET;
    private static final long MAX_VALUE_DIVIDE_10 = Long.MAX_VALUE / 10;

    static {
        try {
            S_VALUE = String.class.getDeclaredField("value");
            S_VALUE.setAccessible(true);
            S_VALUE_OFFSET = OS.memory().getFieldOffset(S_VALUE);
            SB_VALUE = Class.forName("java.lang.AbstractStringBuilder").getDeclaredField("value");
            SB_VALUE.setAccessible(true);
            SB_VALUE_OFFSET = OS.memory().getFieldOffset(SB_VALUE);
            SB_COUNT = Class.forName("java.lang.AbstractStringBuilder").getDeclaredField("count");
            SB_COUNT.setAccessible(true);
            SB_COUNT_OFFSET = OS.memory().getFieldOffset(SB_COUNT);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void setLength(@NotNull StringBuilder sb, int length) {
        try {
            SB_COUNT.set(sb, length);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static void set(@NotNull StringBuilder sb, CharSequence cs) {
        sb.setLength(0);
        sb.append(cs);
    }

    public static boolean endsWith(@NotNull final CharSequence source,
                                   @NotNull final String endsWith) {
        for (int i = 1; i <= endsWith.length(); i++) {
            if (toLowerCase(source.charAt(source.length() - i)) !=
                    toLowerCase(endsWith.charAt(endsWith.length() - i))) {
                return false;
            }
        }

        return true;
    }

    @ForceInline
    public static boolean isEqual(@Nullable CharSequence s, @Nullable CharSequence cs) {
        if (s instanceof StringBuilder) {
            return isEqual((StringBuilder) s, cs);
        }
        if (s == cs)
            return true;
        if (s == null) return false;
        if (cs == null) return false;
        int sLength = s.length();
        int csLength = cs.length();
        if (sLength != csLength) return false;
        for (int i = 0; i < csLength; i++)
            if (s.charAt(i) != cs.charAt(i))
                return false;
        return true;
    }

    @ForceInline
    public static boolean isEqual(@Nullable StringBuilder s, @Nullable CharSequence cs) {
        if (s == cs)
            return true;
        if (s == null) return false;
        if (cs == null) return false;
        int length = cs.length();
        if (s.length() != length) return false;

        if (Jvm.isJava9Plus()) {
            for (int i = 0; i < length; i++)
                // This is not as fast as it could be.
                if (s.charAt(i) != cs.charAt(i))
                    return false;
            return true;
        } else {
            char[] chars = StringUtils.extractChars(s);
            for (int i = 0; i < length; i++)
                if (chars[i] != cs.charAt(i))
                    return false;
            return true;
        }
    }

    @ForceInline
    public static boolean equalsCaseIgnore(@Nullable CharSequence s, @NotNull CharSequence cs) {
        if (s == null) return false;
        if (s.length() != cs.length()) return false;
        for (int i = 0; i < cs.length(); i++)
            if (Character.toLowerCase(s.charAt(i)) !=
                    Character.toLowerCase(cs.charAt(i)))
                return false;
        return true;
    }

    @Nullable
    @ForceInline
    public static String toString(@Nullable Object o) {
        return o == null ? null : o.toString();
    }

    public static char[] extractChars(StringBuilder sb) {
        if (Jvm.isJava9Plus()) {
            final char[] data = new char[sb.length()];
            sb.getChars(0, sb.length(), data, 0);
            return data;
        }

        return OS.memory().getObject(sb, SB_VALUE_OFFSET);
    }

    @Java9
    public static byte getStringCoder(@NotNull String str) {
        return getStringCoderForStringOrStringBuilder(str);
    }

    @Java9
    public static byte getStringCoder(@NotNull StringBuilder str) {
        return getStringCoderForStringOrStringBuilder(str);
    }

    @Java9
    private static byte getStringCoderForStringOrStringBuilder(@NotNull CharSequence charSequence) {
        try {
            return Jvm.getField(charSequence.getClass(), "coder").getByte(charSequence);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    @Java9
    public static byte[] extractBytes(@NotNull StringBuilder sb) {
        ensureJava9Plus();

        return OS.memory().getObject(sb, SB_VALUE_OFFSET);
    }

    public static char[] extractChars(@NotNull String s) {
        if (Jvm.isJava9Plus()) {
            return s.toCharArray();
        }
        return OS.memory().getObject(s, S_VALUE_OFFSET);
    }

    @Java9
    public static byte[] extractBytes(@NotNull String s) {
        ensureJava9Plus();

        return OS.memory().getObject(s, S_VALUE_OFFSET);
    }

    public static void setCount(@NotNull StringBuilder sb, int count) {
        OS.memory().writeInt(sb, SB_COUNT_OFFSET, count);
    }

    @NotNull
    public static String newString(@NotNull char[] chars) {
        if (Jvm.isJava9Plus()) {
            return new String(chars);
        }
        //noinspection RedundantStringConstructorCall
        @NotNull String str = new String();
        try {
            S_VALUE.set(str, chars);
            return str;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Java9
    @NotNull
    public static String newStringFromBytes(@NotNull byte[] bytes) {
        ensureJava9Plus();
        //noinspection RedundantStringConstructorCall
        @NotNull String str = new String();
        try {
            S_VALUE.set(str, bytes);
            return str;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Nullable
    public static String firstLowerCase(@Nullable String str) {
        if (str == null || str.isEmpty())
            return str;
        final char ch = str.charAt(0);
        final char c2 = Character.toLowerCase(ch);
        return ch == c2 ? str : c2 + str.substring(1);
    }

    public static double parseDouble(@NotNull @net.openhft.chronicle.core.annotation.NotNull
                                             CharSequence in) {
        long value = 0;
        int exp = 0;
        boolean negative = false;
        int decimalPlaces = Integer.MIN_VALUE;

        int ch = in.charAt(0);
        int pos = 1;
        switch (ch) {
            case 'N':
                if (compareRest(in, 1, "aN"))
                    return Double.NaN;
                return Double.NaN;
            case 'I':
                //noinspection SpellCheckingInspection
                if (compareRest(in, 1, "nfinity"))
                    return Double.POSITIVE_INFINITY;

                return Double.NaN;
            case '-':
                if (compareRest(in, 1, "Infinity"))
                    return Double.NEGATIVE_INFINITY;
                negative = true;
                ch = in.charAt(pos++);
                break;
        }
        while (true) {
            if (ch >= '0' && ch <= '9') {
                while (value >= MAX_VALUE_DIVIDE_10) {
                    value >>>= 1;
                    exp++;
                }
                value = value * 10 + (ch - '0');
                decimalPlaces++;

            } else if (ch == '.') {
                decimalPlaces = 0;

            } else {
                break;
            }
            if (pos == in.length())
                break;
            ch = in.charAt(pos++);
        }

        return asDouble(value, exp, negative, decimalPlaces);
    }

    private static void ensureJava9Plus() {
        if (!Jvm.isJava9Plus()) {
            throw new UnsupportedOperationException("This method is only supported on Java9+ runtimes");
        }
    }

    private static boolean compareRest(@NotNull @net.openhft.chronicle.core.annotation.NotNull CharSequence in,
                                       int pos, @NotNull @net.openhft.chronicle.core.annotation.NotNull String s) {

        if (s.length() > in.length() - pos)
            return false;

        for (int i = 0; i < s.length(); i++) {
            if (in.charAt(i + pos) != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static double asDouble(long value, int exp, boolean negative, int decimalPlaces) {
        if (decimalPlaces > 0 && value < Long.MAX_VALUE / 2) {
            if (value < Long.MAX_VALUE / (1L << 32)) {
                exp -= 32;
                value <<= 32;
            }
            if (value < Long.MAX_VALUE / (1L << 16)) {
                exp -= 16;
                value <<= 16;
            }
            if (value < Long.MAX_VALUE / (1L << 8)) {
                exp -= 8;
                value <<= 8;
            }
            if (value < Long.MAX_VALUE / (1L << 4)) {
                exp -= 4;
                value <<= 4;
            }
            if (value < Long.MAX_VALUE / (1L << 2)) {
                exp -= 2;
                value <<= 2;
            }
            if (value < Long.MAX_VALUE / (1L << 1)) {
                exp -= 1;
                value <<= 1;
            }
        }
        for (; decimalPlaces > 0; decimalPlaces--) {
            exp--;
            long mod = value % 5;
            value /= 5;
            int modDiv = 1;
            if (value < Long.MAX_VALUE / (1L << 4)) {
                exp -= 4;
                value <<= 4;
                modDiv <<= 4;
            }
            if (value < Long.MAX_VALUE / (1L << 2)) {
                exp -= 2;
                value <<= 2;
                modDiv <<= 2;
            }
            if (value < Long.MAX_VALUE / (1L << 1)) {
                exp -= 1;
                value <<= 1;
                modDiv <<= 1;
            }
            if (decimalPlaces > 1)
                value += modDiv * mod / 5;
            else
                value += (modDiv * mod + 4) / 5;
        }
        final double d = Math.scalb((double) value, exp);
        return negative ? -d : d;
    }

    @Nullable
    public static String toTitleCase(@Nullable String name) {
        if (name == null || name.isEmpty())
            return name;
        @NotNull StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(name.charAt(0)));
        boolean wasUnder = false;
        for (int i = 1; i < name.length(); i++) {
            char ch0 = name.charAt(i);
            char ch1 = i + 1 < name.length() ? name.charAt(i + 1) : ' ';
            if (Character.isLowerCase(ch0)) {
                sb.append(Character.toUpperCase(ch0));
                if (Character.isUpperCase(ch1)) {
                    sb.append('_');
                    wasUnder = true;
                } else {
                    wasUnder = false;
                }
            } else if (Character.isUpperCase(ch0)) {
                if (!wasUnder && Character.isLowerCase(ch1)) {
                    sb.append('_');
                }
                sb.append(ch0);
                wasUnder = false;
            } else {
                sb.append(ch0);
                wasUnder = ch0 == '_';
            }
        }
        return sb.toString();
    }
}
