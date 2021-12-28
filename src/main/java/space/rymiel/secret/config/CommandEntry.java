package space.rymiel.secret.config;

public record CommandEntry(
    String owner,
    String command,
    int priority,
    MatchMode mode,
    BlockMode block,
    boolean isWhitelist
) {
  public static CommandEntry fromConfig(
      String owner,
      String command,
      int priority,
      boolean isWhitelist,
      BlockMode block
  ) {
    if (command.startsWith("R/")) {
      return new CommandEntry(owner, command.substring(1), priority, MatchMode.REGEX, block, isWhitelist);
    } else if (command.endsWith("*")) {
      return new CommandEntry(owner, command.substring(0, command.length() - 1), priority, MatchMode.GLOB, block, isWhitelist);
    }
    return new CommandEntry(owner, command, priority, MatchMode.REGULAR, block, isWhitelist);
  }

  @Override
  public String toString() {
    return "{" + priority + ":'" +
        command + "':'" + owner + '\'' +
        (isWhitelist ? " Wl " : " Bl ") +
        block.toString() + ' ' +
        mode.toString() +
        '}';
  }

  public boolean matches(String command) {
    return switch (this.mode) {
      case REGEX -> command.matches(this.command);
      case GLOB -> command.startsWith(this.command) && !command.split(" ")[0].contains(":");
      case REGULAR -> (command.equals(this.command) || command.startsWith(this.command + ' '));
    };
  }

  public boolean segmentedMatch(String command) {
    if (this.matches(command)) return true;
    if (this.mode == MatchMode.REGEX) return false;
    if (!this.command.contains(" ")) return false;
    var baseSegments = command.split(" ");
    var comparisonSegments = this.command.split(" ");
    var minLength = Math.min(baseSegments.length, comparisonSegments.length);
    for (int i = 0; i < minLength; i++) {
      if (this.mode == MatchMode.GLOB && i == (minLength - 1)) return baseSegments[i].startsWith(comparisonSegments[i]);
      if (!baseSegments[i].equals(comparisonSegments[i])) return false;
    }
    return true;
  }

  public boolean isCommandBlock() {
    return this.block == BlockMode.BOTH || this.block == BlockMode.COMMAND;
  }

  public boolean isTabBlock() {
    return this.block == BlockMode.BOTH || this.block == BlockMode.TAB;
  }

  public enum BlockMode {
    COMMAND, TAB, BOTH
  }

  public enum MatchMode {
    REGEX, GLOB, REGULAR
  }
}
