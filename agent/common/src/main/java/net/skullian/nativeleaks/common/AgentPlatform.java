package net.skullian.nativeleaks.common;

import net.skullian.nativeleaks.common.model.PluginInfo;

import java.nio.file.Path;
import java.util.logging.Logger;

public interface AgentPlatform {

    Path getConfigDirectory();

    PluginInfo getInfo(String pluginName);

    Logger getLogger();

}
