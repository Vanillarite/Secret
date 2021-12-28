package space.rymiel.secret.bungee.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import space.rymiel.secret.bungee.SecretBungee;
import space.rymiel.secret.config.CommandEntry;

import java.util.ArrayList;

public record CommandListener(SecretBungee plugin) implements Listener {
  @EventHandler(priority=65)
  public void onChat(ChatEvent chatEvent) {
    if (chatEvent.isCancelled()) return;
    if (chatEvent.getSender() instanceof ProxiedPlayer proxiedPlayer) {
      var command = chatEvent.getMessage();
      var audience = plugin.adventure().player(proxiedPlayer);
      if (!command.startsWith("/")) return;
      var commandFirstPart = command.split(" ")[0];
      if (command.equals("/secret reload")) {
        if (proxiedPlayer.hasPermission("secret.reload")) {
          plugin.reloadConfig();
          audience.sendMessage(Component.text("Reloaded config", NamedTextColor.LIGHT_PURPLE));
          chatEvent.setCancelled(true);
          return;
        }
      } else if (plugin.config().plFakeMessage() != null) {
        boolean isPlCommand = false;
        for (var cmd : plugin.config().plCommands()) {
          if (cmd.equals(commandFirstPart)) {
            isPlCommand = true;
            break;
          }
        }
        if (isPlCommand) {
          audience.sendMessage(MiniMessage.miniMessage().parse(plugin.config().plFakeMessage()));
          chatEvent.setCancelled(true);
          return;
        }
      }
      var groups = plugin.groups(proxiedPlayer);
      var trace = new ArrayList<CommandEntry>();
      if (plugin.isCommandBlocked(command, groups, trace)) {
        if (plugin.config().blockMessage() != null) {
          audience.sendMessage(MiniMessage.miniMessage().parse(plugin.config().blockMessage()));
        }
        if (plugin.config().debug()) {
          plugin.debug(() -> "Command '%s' was denied for '%s'. (Groups: %s; tried %s)".formatted(command, proxiedPlayer.getName(), groups, trace));
        } else {
          plugin.getLogger().info("Command '%s' was denied for '%s'. (Last trace: %s)".formatted(command, proxiedPlayer.getName(), trace.get(trace.size() - 1)));
        }
        chatEvent.setCancelled(true);
      }
    }
  }
}
