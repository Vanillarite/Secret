package space.rymiel.secret;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import org.jetbrains.annotations.Nullable;
import space.rymiel.secret.config.CommandEntry;
import space.rymiel.secret.config.Config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.function.Supplier;

public interface Secret<Player> {
  Config config();
  ArrayList<CommandEntry> entries();
  void reloadConfig();
  void debug(Supplier<String> s);
  HashSet<String> groups(Player player);

  default boolean isCommandBlocked(String command, Collection<String> groups, @Nullable ArrayList<CommandEntry> trace) {
    boolean willBlock = true;
    int bestCandidate = -1;

    for (var i : entries()) {
      if (i.isCommandBlock() && groups.contains(i.owner())) {
        String com = i.command();
        int candidate = i.priority() * 100 + com.length();
        if (candidate > bestCandidate) {
          if (i.matches(command)) {
            willBlock = !i.isWhitelist();
            bestCandidate = candidate;
            if (trace != null) trace.add(i);
          }
        }
      }
    }
    return willBlock;
  }

  default boolean isCompletionBlocked(String command, Collection<String> groups) {
    boolean willBlock = true;
    int bestCandidate = -1;

    for (var i : entries()) {
      if (i.isTabBlock() && groups.contains(i.owner())) {
        String com = i.command();
        int candidate = i.priority() * 100 + com.length();
        if (candidate > bestCandidate) {
          if (i.segmentedMatch(command)) {
            willBlock = !i.isWhitelist();
            bestCandidate = candidate;
          }
        }
      }
    }
    return willBlock;
  }

  default void filterSuggestions(String context, CommandNode<?> rootNode, Collection<String> groups) {
    // this.getLogger().info(rootNode.toString());
    var children = new ArrayList<CommandNode<?>>(rootNode.getChildren());
    for (CommandNode<?> node : children) {
      if (node instanceof ArgumentCommandNode) return;
      String nodeName = context + node.getName().toLowerCase(Locale.ROOT);
      if (isCompletionBlocked(nodeName, groups)) {
        rootNode.getChildren().remove(node);
      } else {
        filterSuggestions(nodeName + ' ', node, groups);
      }
    }
  }
}
