package net.leaderos.auth.velocity.handler;

import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;

public class ValidSessionHandler implements LimboSessionHandler {

    @Override
    public void onSpawn(Limbo server, LimboPlayer player) {
        player.disconnect();
    }

}
