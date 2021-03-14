package com.jakubtomana.minecraft.serverstresstool;


import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientTeleportConfirmPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Stresser {

    //Final variables
    private final int threadsNum;
    private final String serverAdress;
    private final int port;
    private final String nick;
    private final int delay;
    private final ScheduledExecutorService e = Executors.newScheduledThreadPool(3);

    /**
     * Creates new stresser object (with register and login)
     */
    public Stresser(String serverAdress, int port, int threadsNum, String nick, int delay) {
        this.serverAdress = serverAdress;
        this.port = port;
        this.nick = nick;
        this.threadsNum = threadsNum;
        this.delay = delay;
    }

    /**
     * Start stressing server
     */
    public void startStressTest() {
        for (int i = 0; i < threadsNum; i++) {
            System.out.println("Started new bot:" + i);
            joinserver(i);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void joinserver(int id) {
        MinecraftProtocol protocol = new MinecraftProtocol(nick + id);
        Client client = new Client(serverAdress, port, protocol, new TcpSessionFactory());
        client.getSession().addListener(new SessionAdapter() {

            float x = 0;
            float z = 0;

            @Override
            public void packetReceived(PacketReceivedEvent event) {
                if (event.getPacket() instanceof ServerPlayerPositionRotationPacket) {
                    final ServerPlayerPositionRotationPacket packet = event.getPacket();
                    System.out.println("Connected");
                    x = (float) packet.getX();
                    z = (float) packet.getZ();


                    event.getSession().send(new ClientTeleportConfirmPacket(packet.getTeleportId()));
                    e.scheduleAtFixedRate(() -> {
                        x += ThreadLocalRandom.current().nextBoolean() ? 0.25f : -0.25f;
                        z += ThreadLocalRandom.current().nextBoolean() ? 0.25f : -0.25f;
                        event.getSession().send(new ClientPlayerPositionPacket(false, x, 70, z));
                    }, 0, 200, TimeUnit.MILLISECONDS);
                }
            }

            @Override
            public void disconnected(DisconnectedEvent event) {
                System.out.println("Disconnected: " + event.getReason());
                if (event.getCause() != null) {
                    event.getCause().printStackTrace();
                }
            }
        });

        client.getSession().connect(true);
    }

}
