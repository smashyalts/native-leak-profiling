package net.skullian.nativeleaks.common.profiler;

import com.google.common.io.ByteStreams;
import net.skullian.nativeleaks.common.AgentPlatform;
import one.profiler.AsyncProfiler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Locale;

public class AsyncProfilerAccess {

    private final AsyncProfiler profiler;
    private final long pid = ProcessHandle.current().pid();

    public AsyncProfilerAccess(AgentPlatform platform) throws Exception {
        this.profiler = getProfiler(platform);
    }

    public AsyncProfiler getProfiler(AgentPlatform platform) throws Exception {
        String osArch = String.format("%s-%s",
                System.getProperty("os.name").toLowerCase(Locale.ROOT).replace(" ", ""),
                System.getProperty("os.arch").toLowerCase(Locale.ROOT)
        );

        System.out.println("OS ARCH: " + osArch);

        URL resource = AsyncProfiler.class.getClassLoader().getResource("profiler-native/" + osArch + "/libasyncProfiler.so");
        if (resource == null) {
            throw new IllegalStateException("You are running an unsupported OS and/or arch.");
        }

        Path libPath = createTemporaryFile(platform.dataDirectory().resolve("tmp"));
        try (InputStream inputStream = resource.openStream();
             OutputStream outputStream = Files.newOutputStream(libPath)) {
            ByteStreams.copy(inputStream, outputStream);
        }

        try {
            return AsyncProfiler.getInstance(libPath.toAbsolutePath().toString());
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("Failed to load async-profiler.", e);
        }
    }

    public void onLoad() {
        try {
            profiler.execute("start,--nativemem=0,--nofree,-f,profiling.jfr,%p");
        } catch (Exception e) {
            throw new RuntimeException("Failed to start data profiling.");
        }
    }

    public void onDisable() {
        try {
            profiler.execute("stop," + pid);

            new ProcessBuilder()
                    .command("jfrconv,--total,--nativemem","--leak","profiling.jfr","leak-details.html")
                    .start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop and analyse profiled data.");
        }
    }

    private static Path createTemporaryFile(Path dataDirectory) throws IOException {
        dataDirectory.toFile().mkdirs();

        String fileName = "agent-" + Long.toHexString(System.nanoTime()) + "-libasyncProfiler.so.tmp";
        Path file = Files.createFile(dataDirectory.resolve(fileName), PosixFilePermissions.asFileAttribute(EnumSet.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE
        )));

        file.toFile().deleteOnExit();
        return file;
    }
}
