package dht;

import java.util.UUID;

enum Type {
    Message,
    UndeliverableRoutable,
    Welcome,
    SwitchNeighbor,
    Application,
    Discovery,
    Routable;
}

public class Packet {

    public final static int DHT = 0;

    private Type type;

    private UUID sender;
    private int senderAddress;
    private UUID target;
    private int targetAddress;
    private String content;

    public Packet(Type type, UUID sender, int senderAddress, UUID target, String content) {
        this.type = type;
        this.sender = sender;
        this.senderAddress = senderAddress;
        this.target = target;
        this.content = content;
    }

    public Packet(Type type, UUID sender, int senderAddress, int targetAddress, String content) {
        this.type = type;
        this.sender = sender;
        this.senderAddress = senderAddress;
        this.targetAddress = targetAddress;
        this.content = content;
    }

    public Type getType() {
        return type;
    }

    public UUID getSender() {
        return sender;
    }

    public int getSenderAddress() {
        return senderAddress;
    }

    public UUID getTarget() {
        return target;
    }

    public int getTargetAddress() {
        return targetAddress;
    }

    public String getContent() {
        return content;
    }
}