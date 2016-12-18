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

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.ch.FileChannelImpl;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.management.ManagementFactory.getRuntimeMXBean;

/**
 * Low level axcess to OS class.
 */
public enum OS {
    ;
    public static final String TMP = System.getProperty("java.io.tmpdir");
    public static final String TARGET = System.getProperty("project.build.directory", findTarget());
    private static final String HOST_NAME = getHostName0();
    private static final String USER_NAME = System.getProperty("user.name");
    private static final Logger LOG = LoggerFactory.getLogger(OS.class);
    private static final int MAP_RO = 0;
    private static final int MAP_RW = 1;
    private static final int MAP_PV = 2;
    private static final boolean IS64BIT = is64Bit0();
    private static final int PROCESS_ID = getProcessId0();
    private static final Memory MEMORY = getMemory();
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_LINUX = OS.startsWith("linux");
    private static final boolean IS_MAC = OS.contains("mac");
    private static final boolean IS_WIN = OS.startsWith("win");
    private static final boolean IS_WIN10 = OS.equals("windows 10");
    private static final int MAP_ALIGNMENT = isWindows() ? 64 << 10 : pageSize();
    private static final Method UNMAPP0;
    private static final AtomicLong memoryMapped = new AtomicLong();

    /**
     * Unmap a region of memory.
     *
     * @param address of the start of the mapping.
     * @param size    of the region mapped.
     * @throws IOException if the unmap fails.
     */
    static {
        try {
            UNMAPP0 = FileChannelImpl.class.getDeclaredMethod("unmap0", long.class, long.class);
            UNMAPP0.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private static String findTarget() {
        for (File dir = new File(System.getProperty("user.dir")); dir != null; dir = dir.getParentFile()) {
            File target = new File(dir, "target");
            if (target.exists())
                return target.getAbsolutePath();
        }
        return TMP + "/target";
    }

    public static String findDir(String suffix) throws FileNotFoundException {
        for (String s : System.getProperty("java.class.path").split(":")) {
            if (s.endsWith(suffix) && new File(s).isDirectory())
                return s;
        }
        throw new FileNotFoundException(suffix);
    }

    @NotNull
    public static File findFile(String... path) {
        File dir = new File(".").getAbsoluteFile();
        for (int i = 0; i < path.length - 1; i++) {
            File dir2 = new File(dir, path[i]);
            if (dir2.isDirectory())
                dir = dir2;
        }
        return new File(dir, path[path.length - 1]);
    }


    public static String getHostName() {
        return HOST_NAME;
    }

    public static String getUserName() {
        return USER_NAME;
    }

    public static String getTarget() {
        return TARGET;
    }

    private static String getHostName0() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    private static Memory getMemory() {
        Memory memory = null;
        try {
            Class<? extends Memory> java9MemoryClass = Class
                    .forName("software.chronicle.enterprise.core.Java9Memory")
                    .asSubclass(Memory.class);
            Method create = java9MemoryClass.getMethod("create");
            memory = (Memory) create.invoke(null);
        } catch (ClassNotFoundException expected) {
            // expected
        } catch (NoSuchMethodException | InvocationTargetException
                | IllegalAccessException | IllegalArgumentException e) {
            Jvm.warn().on(OS.class, "Unable to load Java9MemoryClass", e);
        }
        if (memory == null)
            memory = UnsafeMemory.INSTANCE;
        return memory;
    }

    /**
     * @return native memory accessor class
     */
    public static Memory memory() {
        return MEMORY;
    }

    /**
     * Align the size to page boundary
     *
     * @param size the size to align
     * @return aligned size
     * @see #pageSize()
     */
    public static long pageAlign(long size) {
        long mask = pageSize() - 1;
        return (size + mask) & ~mask;
    }

    /**
     * @return size of pages
     * @see #pageAlign(long)
     */
    public static int pageSize() {
        return memory().pageSize();
    }

    /**
     * Align an offset of a memory mapping in file based on OS.
     *
     * @param offset to align
     * @return offset aligned
     * @see #mapAlignment()
     */
    public static long mapAlign(long offset) {
        int chunkMultiple = MAP_ALIGNMENT;
        return (offset + chunkMultiple - 1) / chunkMultiple * chunkMultiple;
    }

    /**
     * Returns the alignment of offsets in file, from which memory mapping could start, based on
     * OS.
     *
     * @return granularity of an offset in a file
     * @see #mapAlign(long)
     */
    public static long mapAlignment() {
        return MAP_ALIGNMENT;
    }

    /**
     * @return is the JVM 64-bit
     */
    public static boolean is64Bit() {
        return IS64BIT;
    }

    private static boolean is64Bit0() {
        String systemProp;
        systemProp = System.getProperty("com.ibm.vm.bitmode");
        if (systemProp != null) {
            return "64".equals(systemProp);
        }
        systemProp = System.getProperty("sun.arch.data.model");
        if (systemProp != null) {
            return "64".equals(systemProp);
        }
        systemProp = System.getProperty("java.vm.version");
        return systemProp != null && systemProp.contains("_64");
    }

    public static int getProcessId() {
        return PROCESS_ID;
    }

    private static int getProcessId0() throws NumberFormatException {
        String pid = null;
        final File self = new File("/proc/self");
        try {
            if (self.exists())
                pid = self.getCanonicalFile().getName();
        } catch (IOException ignored) {
            // ignored
        }
        if (pid == null)
            pid = getRuntimeMXBean().getName().split("@", 0)[0];
        if (pid == null) {
            int rpid = new Random().nextInt(1 << 16);
            Jvm.warn().on(OS.class, "Unable to determine PID, picked a random number=" + rpid);
            return rpid;

        } else {
            return Integer.parseInt(pid);
        }
    }

    /**
     * This may or may not be the OS thread id, but should be unique across processes
     *
     * @return a unique tid of up to 48 bits.
     */
/*    public static long getUniqueTid() {
        return getUniqueTid(Thread.currentThread());
    }

    public static long getUniqueTid(Thread thread) {
        // Assume 48 bit for 16 to 24-bit process id and 16 million threads from the start.
        return ((long) getProcessId() << 24) | thread.getId();
    }*/
    public static boolean isWindows() {
        return IS_WIN;
    }

    public static boolean isMacOSX() {
        return IS_MAC;
    }

    public static boolean isLinux() {
        return IS_LINUX;
    }

    /**
     * @return the maximum PID.
     */
    public static long getPidMax() throws NumberFormatException {
        if (isLinux()) {
            File file = new File("/proc/sys/kernel/pid_max");
            if (file.canRead())
                try {
                    return Maths.nextPower2(new Scanner(file).nextLong(), 1);
                } catch (FileNotFoundException e) {
                    Jvm.debug().on(OS.class, e);
                }
        } else if (isMacOSX()) {
            return 1L << 24;
        }
        // the default.
        return IS_WIN10 ? 1L << 32 : 1L << 16;
    }

    /**
     * Map a region of a file into memory.
     *
     * @param fileChannel to map
     * @param mode        of access
     * @param start       offset within a file
     * @param size        of region to map.
     * @return the address of the memory mapping.
     * @throws IOException              if the mapping fails
     * @throws IllegalArgumentException if the arguments are not valid
     */
    public static long map(FileChannel fileChannel, FileChannel.MapMode mode, long start, long size)
            throws IOException, IllegalArgumentException {
        if (isWindows() && size > 4L << 30)
            throw new IllegalArgumentException("Mapping more than 4096 MiB is unusable on Windows, size = " + (size >> 20) + " MiB");
        try {
            return map0(fileChannel, imodeFor(mode), mapAlign(start), pageAlign(size));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            throw asAnIOException(e);
        }
    }

    static long map0(FileChannel fileChannel, int imode, long start, long size)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IllegalArgumentException {
        Method map0 = fileChannel.getClass().getDeclaredMethod("map0", int.class, long.class, long.class);
        map0.setAccessible(true);
        final long invoke;
        try {
            try {
                invoke = (Long) map0.invoke(fileChannel, imode, start, size);
            } catch (InvocationTargetException e) {
                throw Jvm.rethrow(e.getCause());
            }
        } catch (OutOfMemoryError oome) {
            if (oome.getMessage().startsWith("Map failed") && !is64Bit()) {
                throw new OutOfMemoryError("Ran out of virtual memory on a 32-bit JVM, either use a 64-bit JVM or *reduce* your heap size");
            }
            throw oome;
        }
        memoryMapped.addAndGet(size);
        return invoke;
    }

    public static void unmap(long address, long size) throws IOException {
        try {
            final long size2 = pageAlign(size);
            UNMAPP0.invoke(null, address, size2);
            memoryMapped.addAndGet(-size2);
        } catch (Exception e) {
            throw asAnIOException(e);
        }
    }

    public static long memoryMapped() {
        return memoryMapped.get();
    }

    private static IOException asAnIOException(Throwable e) {
        if (e instanceof InvocationTargetException)
            e = e.getCause();
        if (e instanceof IOException)
            return (IOException) e;
        return new IOException(e);
    }

    static int imodeFor(FileChannel.MapMode mode) {
        int imode = -1;
        if (FileChannel.MapMode.READ_ONLY.equals(mode))
            imode = MAP_RO;
        else if (FileChannel.MapMode.READ_WRITE.equals(mode))
            imode = MAP_RW;
        else if (FileChannel.MapMode.PRIVATE.equals(mode))
            imode = MAP_PV;
        assert (imode >= 0);
        return imode;
    }

    /**
     * Get the sapce actually used by a file.
     *
     * @param filename to get the actual size of
     * @return size in bytes.
     */
    public static long spaceUsed(String filename) {
        return spaceUsed(new File(filename));
    }

    private static long spaceUsed(File file) {
        if (!isWindows()) {
            try {
                String du_k = run("du", "-ks", file.getAbsolutePath());
                return Long.parseLong(du_k.substring(0, du_k.indexOf('\t')));
            } catch (IOException | NumberFormatException e) {
                Jvm.warn().on(OS.class, e);
            }
        }
        return file.length();
    }

    private static String run(String... cmds) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(cmds);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringWriter sw = new StringWriter();
        char[] chars = new char[1024];
        try (Reader r = new InputStreamReader(process.getInputStream())) {
            for (int len; (len = r.read(chars)) > 0; ) {
                sw.write(chars, 0, len);
            }
        }
        return sw.toString();
    }

    public static class Unmapper implements Runnable {
        private final long size;

        private volatile long address;

        public Unmapper(long address, long size, ReferenceCounted owner) throws IllegalStateException {

            assert (address != 0);
            this.address = address;
            this.size = size;
        }

        public void run() {
            if (address == 0)
                return;

            try {
                unmap(address, size);
                address = 0;

            } catch (IOException | IllegalStateException e) {
                Jvm.warn().on(OS.class, "Error on unmap and release", e);
            }
        }
    }
}
