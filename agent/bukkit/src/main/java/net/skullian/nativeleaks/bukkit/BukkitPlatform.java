package net.skullian.nativeleaks.bukkit;

import net.skullian.nativeleaks.common.AgentPlatform;
import net.skullian.nativeleaks.common.profiler.AsyncProfilerAccess;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public class BukkitPlatform extends JavaPlugin implements AgentPlatform {

    private AsyncProfilerAccess profilerAccess;

    @Override
    public void onLoad() {
        try {
            this.profilerAccess = new AsyncProfilerAccess(this);
            this.profilerAccess.onLoad();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize async-profiler.", e);
        }
    }

    @Override
    public void onDisable() {
        this.profilerAccess.onDisable();
    }

    @Override
    public Path dataDirectory() {
        return getDataFolder().toPath();
    }
}
