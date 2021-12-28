package space.rymiel.secret.config;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigSerializable
public record Config(
    @Comment("Verbose console output")
    boolean debug,
    @Comment("Custom server brand on F3")
    @Nullable String serverBrand,
    @Comment("Message to show when a message has been blocked")
    @Nullable String blockMessage,
    ArrayList<String> plCommands,
    @Nullable String plFakeMessage,
    LinkedHashMap<String, GroupNode> groups
) {
  public Config() {
    this(false, null, null, new ArrayList<>(), null, new LinkedHashMap<>(Map.of("default", new GroupNode())));
  }
}
