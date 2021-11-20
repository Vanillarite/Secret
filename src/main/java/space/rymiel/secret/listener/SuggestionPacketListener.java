package space.rymiel.secret.listener;

import com.mojang.brigadier.tree.RootCommandNode;
import dev.simplix.protocolize.api.Direction;
import dev.simplix.protocolize.api.listener.AbstractPacketListener;
import dev.simplix.protocolize.api.listener.PacketReceiveEvent;
import dev.simplix.protocolize.api.listener.PacketSendEvent;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.protocol.packet.Commands;
import space.rymiel.secret.Secret;

import java.util.HashSet;
import java.util.UUID;

public class SuggestionPacketListener extends AbstractPacketListener<Commands> implements Listener {
  private static final HashSet<UUID> pendingLoginPlayerList = new HashSet<>();
  private final Secret plugin;

  public SuggestionPacketListener(Secret plugin) {
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
    ProxiedPlayer proxiedPlayer = packetSendEvent.player().handle();
    if (proxiedPlayer == null) {
      ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> pendingLoginPlayerList.forEach((uuid) -> {
        var p = ProxyServer.getInstance().getPlayer(uuid);
        if (p != null && p.getPendingConnection().isConnected()) {
          p.getPendingConnection().unsafe().sendPacket(new Commands(rootCommandNode));
        }
      }));
      packetSendEvent.cancelled(true);
      return;
    }
    pendingLoginPlayerList.remove(proxiedPlayer.getUniqueId());
    plugin.filterSuggestions("/", rootCommandNode, plugin.groups(proxiedPlayer));
    packetSendEvent.packet().setRoot(rootCommandNode);
  }

  public void packetReceive(PacketReceiveEvent<Commands> packetReceiveEvent) {
  }
}

