package dev._2lstudios.advancedparties.messaging;

import com.google.gson.Gson;

import dev._2lstudios.advancedparties.AdvancedParties;
import dev._2lstudios.advancedparties.messaging.packets.Packet;
import dev._2lstudios.advancedparties.messaging.packets.PartyDisbandPacket;
import dev._2lstudios.advancedparties.messaging.packets.PartyInvitePacket;
import dev._2lstudios.advancedparties.messaging.packets.PartyJoinPacket;
import dev._2lstudios.advancedparties.messaging.packets.PartyKickPacket;
import dev._2lstudios.advancedparties.messaging.packets.PartySendPacket;
import dev._2lstudios.advancedparties.messaging.packets.PartyUpdatePacket;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class RedisPubSub {
    private RedisHandler handler;
    private Gson gson;
    private Jedis suscriber;
    private Jedis publisher;
    private JedisPubSub pubsub;

    private String[] channels = new String[] {
        RedisChannel.PARTY_INVITE,
        RedisChannel.PARTY_KICK,
        RedisChannel.PARTY_JOIN,
        RedisChannel.PARTY_UPDATE,
        RedisChannel.PARTY_DISBAND,
        RedisChannel.PARTY_SEND
    };

    public RedisPubSub(AdvancedParties plugin, String redisURI) {
        this.handler = new RedisHandler(plugin);
        this.gson = new Gson();
        this.suscriber = new Jedis(redisURI);
        this.publisher = new Jedis(redisURI);
        this.pubsub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                process(channel, message);
            }
        };

        new Thread(() -> {
            try {
                suscriber.subscribe(pubsub, channels);
            } catch (JedisConnectionException ignored) { }
        }).start();
    }

    private void process(String channel, String message) {
        if (channel.equalsIgnoreCase(RedisChannel.PARTY_INVITE)) {
            handler.handle(gson.fromJson(message, PartyInvitePacket.class));
        }
        else if (channel.equalsIgnoreCase(RedisChannel.PARTY_KICK)) {
            handler.handle(gson.fromJson(message, PartyKickPacket.class));
        }
        else if (channel.equalsIgnoreCase(RedisChannel.PARTY_JOIN)) {
            handler.handle(gson.fromJson(message, PartyJoinPacket.class));
        }
        else if (channel.equalsIgnoreCase(RedisChannel.PARTY_UPDATE)) {
            handler.handle(gson.fromJson(message, PartyUpdatePacket.class));
        }
        else if (channel.equalsIgnoreCase(RedisChannel.PARTY_DISBAND)) {
            handler.handle(gson.fromJson(message, PartyDisbandPacket.class));
        }
        else if (channel.equalsIgnoreCase(RedisChannel.PARTY_SEND)) {
            handler.handle(gson.fromJson(message, PartySendPacket.class));
        }
    }

    public void publish(Packet packet) {
        this.publisher.publish(packet.getChannel(), gson.toJson(packet));
    }

    public void disconnect() {
        this.pubsub.unsubscribe(channels);
        this.publisher.close();
        this.suscriber.close();
    }
}
