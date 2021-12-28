package space.rymiel.secret.bungee.listener;

import com.mojang.brigadier.tree.RootCommandNode;
import dev.simplix.protocolize.api.Direction;
import dev.simplix.protocolize.api.listener.AbstractPacketListener;
import dev.simplix.protocolize.api.listener.PacketReceiveEvent;
import dev.simplix.protocolize.api.listener.PacketSendEvent;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.protocol.packet.Commands;
import space.rymiel.secret.bungee.SecretBungee;

import java.util.HashSet;
import java.util.UUID;

public class SuggestionPacketListener extends AbstractPacketListener<Commands> implements Listener {
  private static final HashSet<UUID> pendingLoginPlayerList = new HashSet<>();
  private final SecretBungee plugin;

  public SuggestionPacketListener(SecretBungee plugin) {
    super(Commands.class, Direction.UPSTREAM, 0x7FFFFFFE);
    this.plugin = plugin;
  }

  @EventHandler
  public void onConnect(PostLoginEvent postLoginEvent) {
    if (!postLoginEvent.getPlayer().getPendingConnection().isLegacy()) {
      pendingLoginPlayerList.add(postLoginEvent.getPlayer().getUniqueId());
    }
  }

  public void packetSend(PacketSendEvent<Commands> packetSendEvent) {
    RootCommandNode<?> rootCommandNode = packetSendEvent.packet().getRoot();
    ProtocolizePlayer proxiedPlayer = packetSendEvent.player();
    if (proxiedPlayer == null) {
      plugin.async(() -> pendingLoginPlayerList.forEach((uuid) -> {
        var p = ProxyServer.getInstance().getPlayer(uuid);
        if (p != null && p.getPendingConnection().isConnected()) {
          p.getPendingConnection().unsafe().sendPacket(new Commands(rootCommandNode));
        }
      }));
      packetSendEvent.cancelled(true);
      return;
    }
    pendingLoginPlayerList.remove(proxiedPlayer.uniqueId());
    plugin.filterSuggestions("/", rootCommandNode, plugin.groups(proxiedPlayer.handle()));
    packetSendEvent.packet().setRoot(rootCommandNode);
  }

  public void packetReceive(PacketReceiveEvent<Commands> packetReceiveEvent) {
  }
}

