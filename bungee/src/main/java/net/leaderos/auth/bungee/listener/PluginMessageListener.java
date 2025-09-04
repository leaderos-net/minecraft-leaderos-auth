package net.leaderos.auth.bungee.listener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import lombok.RequiredArgsConstructor;
import net.leaderos.auth.bungee.Bungee;
import net.leaderos.auth.shared.Shared;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

@RequiredArgsConstructor
public class PluginMessageListener implements Listener {

    private final Bungee plugin;

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals("BungeeCord")) return;
        if (!(event.getSender() instanceof Server)) return;

        final ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String channel = in.readUTF();
        if (!channel.equals("Forward")) return;

        in.readUTF();
        String subChannel = in.readUTF();
        if (!subChannel.equals("losauth:status")) return;

        final short dataLength = in.readShort();
        final byte[] dataBytes = new byte[dataLength];
        in.readFully(dataBytes);
        final ByteArrayDataInput dataIn = ByteStreams.newDataInput(dataBytes);
        String playerName = dataIn.readUTF();
        boolean isAuthenticated = dataIn.readBoolean();

        Shared.getDebugAPI().send("Received auth status for player " + playerName + ": " + isAuthenticated, false);
        plugin.getAuthenticatedPlayers().put(playerName, isAuthenticated);
    }

}
