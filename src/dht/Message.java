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

public class Message {

    public final static int DHT = 0;

    private Type type;
    private int senderId;
    private int targetId;
    private String content;

    public Message(Type type, int senderId, int targetId, String content) {
        this.type = type;
        this.senderId = senderId;
        this.targetId = targetId;
        this.content = content;
    }

    public Type getType() {
        return type;
    }

    public int getSender() {
        return senderId;
    }

    public int getTarget() {
        return targetId;
    }

    public String getContent() {
        return content;
    }
}