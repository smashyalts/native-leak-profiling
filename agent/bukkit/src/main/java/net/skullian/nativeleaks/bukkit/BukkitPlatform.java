package net.skullian.nativeleaks.bukkit;

import net.skullian.nativeleaks.common.AgentPlatform;
import net.skullian.nativeleaks.common.model.PluginInfo;
import net.skullian.nativeleaks.common.profiler.LeakProfiler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public class BukkitPlatform extends JavaPlugin implements AgentPlatform {

    private LeakProfiler leakProfiler;

    @Override
    public void onEnable() {
        try {
            this.leakProfiler = new LeakProfiler(this);
            this.leakProfiler.onLoad();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise LeakProfiler.", e);
        }
    }

    @Override
    public void onDisable() {
        this.leakProfiler.onDisable();
    }

    @Override
    public Path getConfigDirectory() {
        return getDataFolder().toPath();
    }

    @Override
    public PluginInfo getInfo(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin != null) {
            return new PluginInfo(true, plugin.getDescription().getVersion(), plugin);
        }

        return new PluginInfo(false, null, null);
    }
}
