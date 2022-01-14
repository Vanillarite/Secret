package space.rymiel.secret.velocity;

import cloud.commandframework.CommandTree;
import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.arguments.parser.StandardParameters;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.velocity.VelocityCommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.leangen.geantyref.TypeToken;
import org.slf4j.Logger;
import space.rymiel.secret.Secret;
import space.rymiel.secret.velocity.command.SecretCommands;
import space.rymiel.secret.velocity.listener.CommandListener;
import space.rymiel.secret.config.CommandEntry;
import space.rymiel.secret.config.Config;
import space.rymiel.secret.config.ConfigurationManager;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

@Plugin(id = "secret", name = "Secret", version = "@version@", authors = {"rymiel"})
public class SecretVelocity implements Secret<Player> {
  private Config config = null;
  private ConfigurationManager<Config> configManager = null;
  private final Path dataDirectory;
  private final Logger logger;
  private final ProxyServer server;
  private ArrayList<CommandEntry> entries = null;
  private final Set<String> knownBlocked = new HashSet<>();

  public Config config() {
    return config;
  }
  public ConfigurationManager<Config> configManager() { return configManager; }
  public ArrayList<CommandEntry> entries() {
    return entries;
  }
  public Set<String> knownBlocked() {
    return knownBlocked;
  }

  @Inject
  public SecretVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
    this.logger = logger;
    this.dataDirectory = dataDirectory;
    this.server = server;
    reloadConfig();
  }

  @Subscribe
  public void onInitialize(ProxyInitializeEvent e) {
    final Function<CommandTree<CommandSource>, CommandExecutionCoordinator<CommandSource>>
        executionCoordinatorFunction =
        AsynchronousCommandExecutionCoordinator.<CommandSource>newBuilder().build();
    final Function<CommandSource, CommandSource> mapperFunction = Function.identity();
    VelocityCommandManager<CommandSource> manager;
    try {
      manager =
          new VelocityCommandManager<>(
              this.server.getPluginManager().ensurePluginContainer(this), this.server, executionCoordinatorFunction, mapperFunction, mapperFunction);
    } catch (final Exception ex) {
      this.logger.error("Failed to initialize the command manager");
      return;
    }
    final Function<ParserParameters, CommandMeta> commandMetaFunction =
        p ->
            CommandMeta.simple()
                .with(
                    CommandMeta.DESCRIPTION,
                    p.get(StandardParameters.DESCRIPTION, "No description"))
                .build();
    AnnotationParser<CommandSource> annotationParser =
        new AnnotationParser<>(manager, CommandSource.class, commandMetaFunction);
    annotationParser.parse(new SecretCommands(this));

    this.server.getEventManager().register(this, new CommandListener(this));
  }

  @Override
  public void reloadConfig() {
    try {
      Files.createDirectories(this.dataDirectory);
      this.configManager = new ConfigurationManager<>(
          new File(this.dataDirectory.toFile(), "config.conf"),
          TypeToken.get(Config.class),
          new Config()
      );
      this.config = configManager.loadConfig();
      debug(() -> this.config.toString());
      this.entries = new ArrayList<>();
      this.config.groups().forEach((key, value) -> entries.addAll(value.convertEntries(key)));
      this.entries.sort(Comparator.comparing(CommandEntry::owner));
      this.entries.sort(Comparator.comparing(CommandEntry::command));
      this.entries.sort(Comparator.comparingInt(CommandEntry::priority));
      debug(() -> this.entries.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void debug(Supplier<String> s) {
    if (config.debug()) {
      this.logger.info("[DEBUG] %s".formatted(s.get()));
    }
  }

  @Override
  public HashSet<String> groups(Player player) {
    var found = new HashSet<>(List.of("default"));
    config.groups().forEach((k, v) -> {
      if (player.hasPermission("secret.group." + k)) {
        found.add(k);
      }
    });
    return found;
  }

  public Logger logger() {
    return this.logger;
  }
}
