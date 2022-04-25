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

    private int senderId;
    private int senderAddress;
    private int targetId;

    private String content;

    public Packet(Type type, int senderId, int senderAddress, int targetId, String content) {
        this.type = type;
        this.senderId = senderId;
        this.senderAddress = senderAddress;
        this.targetId = targetId;
        this.content = content;
    }

    public Type getType() {
        return type;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getSenderAddress() {
        return senderAddress;
    }

    public int getTargetId() {
        return targetId;
    }

    public String getContent() {
        return content;
    }
}