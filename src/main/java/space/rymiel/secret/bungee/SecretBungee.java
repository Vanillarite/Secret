package space.rymiel.secret.bungee;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import dev.simplix.protocolize.api.Protocolize;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import space.rymiel.secret.Secret;
import space.rymiel.secret.config.CommandEntry;
import space.rymiel.secret.config.Config;
import space.rymiel.secret.config.ConfigurationManager;
import space.rymiel.secret.bungee.listener.CommandListener;
import space.rymiel.secret.bungee.listener.SuggestionPacketListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class SecretBungee extends Plugin implements Secret<ProxiedPlayer> {
  private Config config = null;
  private ArrayList<CommandEntry> entries = null;
  private BungeeAudiences adventure = null;

  public Config config() {
    return config;
  }
  public BungeeAudiences adventure() {
    return adventure;
  }
  public ArrayList<CommandEntry> entries() {
    return entries;
  }
  public void reloadConfig() {
    this.getDataFolder().mkdirs();
    var configManager = new ConfigurationManager<>(
        new File(getDataFolder(), "config.conf"),
        TypeToken.get(Config.class),
        new Config()
    );
    try {
      this.config = configManager.loadConfig();
      debug(() -> this.config.toString());
      this.entries = new ArrayList<>();
      this.config.groups().forEach((key, value) -> entries.addAll(value.convertEntries(key)));
      this.entries.sort(Comparator.comparing(CommandEntry::owner));
      this.entries.sort(Comparator.comparing(CommandEntry::command));
      this.entries.sort(Comparator.comparingInt(CommandEntry::priority));
      debug(() -> this.entries.toString());
    } catch (ConfigurateException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onEnable() {
    reloadConfig();
    this.adventure = BungeeAudiences.create(this);
    this.getProxy().getPluginManager().registerListener(this, new CommandListener(this));
    this.getProxy().getPluginManager().registerListener(this, new SuggestionPacketListener(this));
    Protocolize.listenerProvider().registerListener(new SuggestionPacketListener(this));
  }

  public void debug(Supplier<String> s) {
    if (config.debug()) {
      this.getLogger().info("[DEBUG] %s".formatted(s.get()));
    }
  }

  @Override
  public void async(Runnable runnable) {
    ProxyServer.getInstance().getScheduler().runAsync(this, runnable);
  }

  public HashSet<String> groups(ProxiedPlayer player) {
    var found = new HashSet<>(List.of("default"));
    config.groups().forEach((k, v) -> {
      if (player.hasPermission("secret.group." + k)) {
        found.add(k);
      }
    });
    return found;
  }

  @Override
  public void onDisable() {
    adventure.close();
  }

}
