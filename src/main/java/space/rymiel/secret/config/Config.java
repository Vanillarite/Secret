package space.rymiel.secret.config;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigSerializable
public record Config(
    @Comment("Verbose console output")
    boolean debug,
    @Comment("Custom server brand on F3")
    @Nullable String serverBrand,
    LinkedHashMap<String, GroupNode> groups
) {
  public Config() {
    this(false, null, new LinkedHashMap<>(Map.of("default", new GroupNode())));
  }
}
