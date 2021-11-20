package space.rymiel.secret.config;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.List;

import static space.rymiel.secret.config.CommandEntry.BlockMode.BOTH;
import static space.rymiel.secret.config.CommandEntry.BlockMode.COMMAND;
import static space.rymiel.secret.config.CommandEntry.BlockMode.TAB;

@ConfigSerializable
public record GroupNode(
    @Comment("The group with the highest permission will also be used for the \"blocked\" message, for example")
    int priority,
    @Comment("Command and tab-completion entries to be blocked/permitted")
    GroupListing entries,
    @Comment("Commands to be blocked/permitted")
    @Nullable GroupListing onlyCommands,
    @Comment("Tab-completions to be blocked/permitted")
    @Nullable GroupListing onlyTab
) {
  public GroupNode() {
    this(0, new GroupListing(new ArrayList<>(List.of("/")), new ArrayList<>()), null, null);
  }

  @ConfigSerializable
  public record GroupListing(
      @Nullable ArrayList<String> whitelist,
      @Nullable ArrayList<String> blacklist
  ) {
    public GroupListing() {
      this(new ArrayList<>(), new ArrayList<>());
    }
  }

  public ArrayList<CommandEntry> convertEntries(String ownerName) {
    var a = new ArrayList<CommandEntry>();
    applyEntries(ownerName, a, this.entries, BOTH);
    applyEntries(ownerName, a, this.onlyCommands, COMMAND);
    applyEntries(ownerName, a, this.onlyTab, TAB);
    return a;
  }

  private void applyEntries(String ownerName, ArrayList<CommandEntry> a, @Nullable GroupListing listing, CommandEntry.BlockMode blockMode) {
    if (listing == null) return;
    if (listing.whitelist != null) listing.whitelist.forEach((i) -> a.add(CommandEntry.fromConfig(ownerName, i, this.priority, true, blockMode)));
    if (listing.blacklist != null) listing.blacklist.forEach((i) -> a.add(CommandEntry.fromConfig(ownerName, i, this.priority, false, blockMode)));
  }
}
