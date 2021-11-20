package space.rymiel.secret.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import space.rymiel.secret.Secret;
import space.rymiel.secret.config.CommandEntry;

import java.util.ArrayList;

public record CommandListener(Secret plugin) implements Listener {
  @EventHandler(priority=65)
  public void onChat(ChatEvent chatEvent) {
    if (chatEvent.isCancelled()) return;
    if (chatEvent.getSender() instanceof ProxiedPlayer proxiedPlayer) {
      var command = chatEvent.getMessage();
      var audience = plugin.adventure().player(proxiedPlayer);
      if (!command.startsWith("/")) return;
      if (command.equals("/secret reload")) {
        if (proxiedPlayer.hasPermission("secret.reload")) {
          plugin.reloadConfig();
          audience.sendMessage(Component.text("Reloaded config", NamedTextColor.LIGHT_PURPLE));
          chatEvent.setCancelled(true);
          return;
        }
      }
      var groups = plugin.groups(proxiedPlayer);
      var trace = new ArrayList<CommandEntry>();
      if (plugin.isCommandBlocked(command, groups, trace)) {
        var feedback = "Unknown command. Type \"/help\" for help.";
        audience.sendMessage(Component.text(feedback));
        plugin.debug(() -> "Command '%s' was denied for '%s'. (Groups: %s; tried %s)".formatted(command, proxiedPlayer.getName(), groups, trace));
        chatEvent.setCancelled(true);
      }
    }
  }
}
