package net.leaderos.auth.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.leaderos.auth.shared.helpers.Placeholder;
import net.leaderos.auth.velocity.Velocity;
import net.leaderos.auth.velocity.helpers.ChatUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class IpConnectionLimitListener {
    private final Velocity plugin;
    private final Map<String, Integer> ipConnections = new ConcurrentHashMap<>();

    @Subscribe(order = PostOrder.LAST)
    public void onJoin(PreLoginEvent event) {
        // Ignore if max connections per IP is not enabled
        if (plugin.getConfigFile().getSettings().getMaxJoinPerIP() <= 0) return;

        String ip = event.getConnection().getRemoteAddress().getAddress().getHostAddress();
        int current = ipConnections.getOrDefault(ip, 0);

        // Deny the connection if the max connections per IP is reached
        if (current >= plugin.getConfigFile().getSettings().getMaxJoinPerIP()) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                    Component.join(JoinConfiguration.newlines(),
                            ChatUtil.replacePlaceholders(plugin.getLangFile().getMessages().getKickMaxConnectionsPerIP(),
                                    new Placeholder("{prefix}", plugin.getLangFile().getMessages().getPrefix())))
            ));
            return;
        }

        // Increase the connection count for the IP
        ipConnections.put(ip, current + 1);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        // Ignore if max connections per IP is not enabled
        if (plugin.getConfigFile().getSettings().getMaxJoinPerIP() <= 0) return;

        // Decrease the connection count for the IP
        String ip = event.getPlayer().getRemoteAddress().getAddress().getHostAddress();
        ipConnections.computeIfPresent(ip, (k, v) -> v > 1 ? v - 1 : null);
    }
}
