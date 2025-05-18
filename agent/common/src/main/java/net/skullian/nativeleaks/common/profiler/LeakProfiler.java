package net.skullian.nativeleaks.common.profiler;

import net.skullian.nativeleaks.common.AgentPlatform;
import net.skullian.nativeleaks.common.model.PluginInfo;
import net.skullian.nativeleaks.common.model.Version;
import one.profiler.AsyncProfiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.logging.Logger;

public class LeakProfiler {

    private final AsyncProfiler profiler = AsyncProfiler.getInstance();
    private final Path configDirectory;
    private final Logger logger;
    private final Path jfrConverter;

    private Path profilerOutputPath;

    public LeakProfiler(AgentPlatform platform) throws Exception {
        PluginInfo sparkInfo = platform.getInfo("spark");
        if (!sparkInfo.enabled() || Version.fromString(sparkInfo.version()).getPatch() < 133) {
            throw new IllegalStateException("Spark v1.10.133 or higher must be installed!");
        }

        this.configDirectory = platform.getConfigDirectory();
        this.logger = platform.getLogger();

        this.jfrConverter = platform.getConfigDirectory().resolve("jfr-converter.jar");
        if (!Files.exists(jfrConverter)) {
            logger.info("Copying embedded jfr-converter.jar to /home/container/plugins/Agent/jfr-converter.jar - This is nothing to worry about.");
            Files.copy(getClass().getClassLoader().getResourceAsStream("assets/jfr-converter.jar"), jfrConverter, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void onLoad() {
        try {
            logger.info("Starting profiler...");

            try {
                this.profilerOutputPath = createTemporaryFile(this.configDirectory.resolve("tmp"), "agent-", "-leak-data.jfr");
            } catch (IOException error) {
                throw new RuntimeException("Unable to create temporary JFR file.", error);
            }

            String response = this.profiler.execute("start,nativemem,nofree,jfr,file=" + this.profilerOutputPath.toString() + ",loglevel=NONE,clock=monotonic,filter");
            if (!response.trim().equalsIgnoreCase("profiling started")) {
                throw new RuntimeException("Unable to start AsyncProfiler - unexpected response given: " + response);
            } else {
                logger.info("Successfully started profiler.");
            }
        } catch (Exception e) {
            try {
                this.profiler.stop();
            } catch (Exception ignored) {}

            throw new RuntimeException("An unexpected error occurred while starting profiling.", e);
        }
    }

    public void onDisable() {
        try {
            logger.info("Stopping profiler...");

            this.profiler.stop();
        } catch (IllegalStateException e) {
            throw new RuntimeException("Failed to disable profiling.");
        } finally {
            process();
        }
    }

    private void process() {
        try {
            logger.info("Parsing JFR file...");

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "java", "-jar", jfrConverter.toString(), "--total", "--nativemem", "--leak", profilerOutputPath.toString(), configDirectory.resolve("leak-results-" + System.currentTimeMillis() + ".html").toString()
                    ).inheritIO();

            int exitCode = processBuilder.start().waitFor();
            logger.info("Process exited with code " + exitCode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse jfr file.", e);
        }
    }

    private Path createTemporaryFile(Path directory, String prefix, String suffix) throws IOException {
        String name = prefix + Long.toHexString(System.nanoTime()) + suffix;
        Path file = Files.createFile(directory.resolve(name), PosixFilePermissions.asFileAttribute(EnumSet.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE
        )));

        file.toFile().deleteOnExit();
        return file;
    }

}
