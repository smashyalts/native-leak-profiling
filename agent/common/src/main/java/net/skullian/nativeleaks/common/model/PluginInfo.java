package net.skullian.nativeleaks.common.model;

public record PluginInfo(
        boolean enabled,
        String version,
        Object pluginClass
) {}
