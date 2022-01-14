package space.rymiel.secret.velocity.listener;

import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.minimessage.MiniMessage;
import space.rymiel.secret.config.CommandEntry;
import space.rymiel.secret.velocity.SecretVelocity;

import java.util.ArrayList;
import java.util.Locale;

public record CommandListener(SecretVelocity plugin) {

  @SuppressWarnings("UnstableApiUsage")
  @Subscribe
  public void onSuggestion(PlayerAvailableCommandsEvent packetSendEvent) {
    RootCommandNode<?> rootCommandNode = packetSendEvent.getRootNode();
    Player player = packetSendEvent.getPlayer();
    plugin.filterSuggestions("/", rootCommandNode, plugin.groups(player));
  }

  @Subscribe
  public void onCommand(CommandExecuteEvent commandEvent) {
    var commandSource = commandEvent.getCommandSource();
    var command = "/" + commandEvent.getCommand().toLowerCase(Locale.ROOT);
    var commandFirstPart = command.split(" ")[0];
    final var plFakeMessage = plugin.config().plFakeMessage();
    if (commandSource instanceof Player player) {
      if (plFakeMessage != null) {
        boolean isPlCommand = false;
        for (var cmd : plugin.config().plCommands()) {
          if (cmd.equals(commandFirstPart)) {
            isPlCommand = true;
            break;
          }
        }
        if (isPlCommand && !player.hasPermission("secret.pl")) {
          player.sendMessage(MiniMessage.miniMessage().deserialize(plFakeMessage));
          commandEvent.setResult(CommandExecuteEvent.CommandResult.denied());
          return;
        }
      }
      var groups = plugin.groups(player);
      var trace = new ArrayList<CommandEntry>();
      if (plugin.isCommandBlocked(command, groups, trace)) {
        final var blockMessage = plugin.config().blockMessage();
        if (blockMessage != null) {
          player.sendMessage(MiniMessage.miniMessage().deserialize(blockMessage));
        }
        if (plugin.config().debug()) {
          plugin.debug(() -> "Command '%s' was denied for '%s'. (Groups: %s; tried %s)".formatted(command, player.getUsername(), groups, trace));
        } else {
          plugin.logger().info("Command '%s' was denied for '%s'. (Last trace: %s)".formatted(command, player.getUsername(), trace.get(trace.size() - 1)));
        }
        plugin.knownBlocked().add(command);
        commandEvent.setResult(CommandExecuteEvent.CommandResult.denied());
      }
    }
  }
}

