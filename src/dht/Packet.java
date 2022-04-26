package dht;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface Packet {
    class DiscoveryPacket implements Packet {
        private int id;
        private int address;

        public DiscoveryPacket(int id, int address) {
            this.id = id;
            this.address = address;
        }

        public int getId() {
            return id;
        }

        public int getAddress() {
            return address;
        }
    }

    class WelcomePacket implements Packet {
        int left;
        int right;

        public WelcomePacket(int left, int right) {
            this.left = left;
            this.right = right;
        }
    }

    class SwitchNeighborPacket implements Packet {
        boolean left;
        int address;

        public SwitchNeighborPacket(String position, int address) {
            if (position == "LEFT") this.left = true;
            else if (position == "RIGHT") this.left = false;
            else {
                System.out.println("SwitchNeighborPacket position not valid: " + position);
                return;
            }

            this.address = address;
        }
    }

    class RoutablePacket implements Packet {
        public enum Type {
            MESSAGE,
            UNDELIVERABLE,
            PUT,
            REPLICATION,
            GET,
            GET_RESPONSE
        }

        private Type type;
        private int senderAddress;
        private int sender;
        private int target;
        private String data;

        public RoutablePacket(Type type, int sender, int senderAddress, int target) {
            this.type = type;
            this.senderAddress = senderAddress;
            this.sender = sender;
            this.target = target;
        }

        public RoutablePacket(Type type, int sender, int senderAddress, int target, String data) {
            this.type = type;
            this.senderAddress = senderAddress;
            this.sender = sender;
            this.target = target;
            this.data = data;
        }

        public Type getType() {
            return type;
        }

        public int getSenderAddress() {
            return senderAddress;
        }

        public int getSender() {
            return sender;
        }

        public int getTarget() {
            return target;
        }

        public String getData() {
            return data;
        }
    }
}

