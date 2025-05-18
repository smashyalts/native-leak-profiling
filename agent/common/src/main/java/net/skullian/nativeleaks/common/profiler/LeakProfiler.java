package net.skullian.nativeleaks.common.profiler;

import me.lucko.spark.common.util.TemporaryFiles;
import net.skullian.nativeleaks.common.AgentPlatform;
import net.skullian.nativeleaks.common.model.PluginInfo;
import net.skullian.nativeleaks.common.model.Version;
import one.profiler.AsyncProfiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

public class LeakProfiler {

    private final AsyncProfiler profiler = AsyncProfiler.getInstance();
    private final TemporaryFiles temporaryFiles;
    private final Logger logger;
    private final Path jfrConverter;

    private Path profilerOutputPath;

    public LeakProfiler(AgentPlatform platform) throws Exception {
        PluginInfo sparkInfo = platform.getInfo("spark");
        if (!sparkInfo.enabled() || Version.fromString(sparkInfo.version()).getPatch() < 133) {
            throw new IllegalStateException("Spark v1.10.133 or higher must be installed!");
        }

        this.temporaryFiles = new TemporaryFiles(platform.getConfigDirectory().resolve("tmp"));
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
                this.profilerOutputPath = this.temporaryFiles.create("agent-", "-leak-data.jfr.tmp");
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
                    "java", "-jar", jfrConverter.toString(), "--total", "--nativemem", "--leak", profilerOutputPath.toString(), "leak-results.html"
                    ).inheritIO();

            int exitCode = processBuilder.start().waitFor();
            logger.info("Process exited with code " + exitCode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse jfr file.", e);
        }
    }

}
