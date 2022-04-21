package dht;

public class Message {

    public final static int DHT = 0;

    private int type;
    private String content;

    public Message(int type, String content) {
	this.type = type;
	this.content = content;
    }

    public String getContent() {
	return this.content;
    }

    public int getType() {
	return this.type;
    }
    
}