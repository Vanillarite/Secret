package space.rymiel.secret.velocity.command;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.specifier.Greedy;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurateException;
import space.rymiel.secret.config.CommandEntry;
import space.rymiel.secret.config.GroupNode;
import space.rymiel.secret.velocity.SecretVelocity;

import java.util.ArrayList;
import java.util.List;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

public record SecretCommands(SecretVelocity plugin) {
  public enum ListingMode {
    WHITELIST, BLACKLIST
  }

  @CommandDescription("Register a command for a group to secret")
  @CommandMethod("secret add <mode> <type> <group> <command>")
  @CommandPermission("secret.manage")
  private void commandAdd(
      final @NotNull CommandSource sender,
      final @Argument("mode") @NotNull ListingMode mode,
      final @Argument("type") @NotNull CommandEntry.BlockMode type,
      final @Argument(value = "group", suggestions = "group") @NotNull String groupName,
      final @Argument(value = "command", suggestions = "known") @NotNull @Greedy String command
  ) {
    try {
      final var baseConfig = plugin.configManager().loadConfig();
      final var group = baseConfig.groups().get(groupName);
      if (group == null) {
        sender.sendMessage(Component.empty().color(RED)
            .append(text("No group called \""))
            .append(text(groupName).color(LIGHT_PURPLE))
            .append(text("\" exists!"))
        );
        return;
      }

      GroupNode.GroupListing listingType = switch (type) {
        case BOTH -> group.entries();
        case COMMAND -> group.onlyCommands();
        case TAB -> group.onlyTab();
      };
      if (listingType == null) listingType = new GroupNode.GroupListing(null, null);

      ArrayList<String> listing = switch (mode) {
        case BLACKLIST -> listingType.blacklist();
        case WHITELIST -> listingType.whitelist();
      };
      if (listing == null) listing = new ArrayList<>();

      listing.add(command);

      final var substituteListing = switch (mode) {
        case BLACKLIST -> new GroupNode.GroupListing(listingType.whitelist(), listing);
        case WHITELIST -> new GroupNode.GroupListing(listing, listingType.blacklist());
      };
      final var substituteGroup = switch (type) {
        case BOTH -> new GroupNode(group.priority(), substituteListing, group.onlyCommands(), group.onlyTab());
        case COMMAND -> new GroupNode(group.priority(), group.entries(), substituteListing, group.onlyTab());
        case TAB -> new GroupNode(group.priority(), group.entries(), group.onlyCommands(), substituteListing);
      };
      baseConfig.groups().put(groupName, substituteGroup);
      final var loader = plugin.configManager().loader();
      loader.save(loader.createNode().set(plugin.configManager().type(), baseConfig));
      plugin.reloadConfig();
      sender.sendMessage(Component.empty().color(GREEN)
          .append(text("Success! Added the command \""))
          .append(text(command).color(DARK_GREEN))
          .append(text("\" to the group \""))
          .append(text(groupName).color(DARK_GREEN))
          .append(text("\" to with mode "))
          .append(text(mode.toString()).color(DARK_GREEN))
          .append(Component.space())
          .append(text(type.toString()).color(DARK_GREEN))
          .append(text("!"))
      );
    } catch (ConfigurateException e) {
      e.printStackTrace();
    }
  }

  @CommandDescription("Re-read the config")
  @CommandMethod("secret reload")
  @CommandPermission("secret.reload")
  private void commandReload(
      final @NotNull CommandSource sender
  ) {
    plugin.reloadConfig();
    sender.sendMessage(text("Reloaded config", LIGHT_PURPLE));
  }

  @Suggestions("group")
  public @NotNull List<String> completeGroups(CommandContext<CommandSource> sender, String input) {
    return plugin.config().groups().keySet().stream().filter(i -> i.startsWith(input)).limit(20).toList();
  }

  @Suggestions("known")
  public @NotNull List<String> completeKnownBlocked(CommandContext<CommandSource> sender, String input) {
    return plugin.knownBlocked().stream().filter(i -> i.startsWith(input)).limit(20).toList();
  }
}
