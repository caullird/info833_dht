package dht;

import peersim.edsim.*;
import peersim.core.*;
import peersim.config.*;

import java.util.UUID;

public class Node implements EDProtocol {

    //prefixe de la couche (nom de la variable de protocole du fichier de config)
    private String prefix;

    //identifiant de la couche transport
    private int transportPid;

    //objet couche transport
    private Transport transport;

    //identifiant de la couche courante (la couche applicative)
    private int mypid;

    //adresse de la couche transport
    private int address;

    private boolean isAwaken = false;

    //id aléatoire
    private int id = (int)(Math.random()*(1000));

    private Node left = null;
    private Node right = null;

    public Node(String prefix) {
        this.prefix = prefix;
        this.transportPid = Configuration.getPid(prefix + ".transport");
        this.mypid = Configuration.getPid(prefix + ".myself");
        this.transport = null;
    }

    public boolean isAwaken() {
        return isAwaken;
    }

    // Initialize this as the first node in the ring
    public void awakeAsInitialNode(int address) {
        if (this.isAwaken()) {
            System.out.println("The node is already awaken");
            return;
        }

        this.setTransportLayer(address);
        this.isAwaken = true;

        this.left = this;
        this.right = this;

        System.out.println(this + "Awaken as initial node");
    }

    // Initialize this as the first node in the ring
    public void awake(int address) {
        if (this.isAwaken()) {
            System.out.println(this + "Already awaken");
            return;
        }

        this.setTransportLayer(address);

        Node randomAwakenNode = Initializer.getRandomAwakenNode();

        Packet discoveryPacket = new Packet(Type.Discovery, this.id, this.address, randomAwakenNode.getId(), "");

        System.out.println(this + "Send Discovery Packet to " + randomAwakenNode);
        this.send(randomAwakenNode, discoveryPacket);
    }

    //  Leave the ring and notify the its left and right neighbors that their respective right and left neighbor have change
    public void leave() {
        if(!this.isAwaken()) System.out.println("Cannot leave as the node is not part of the ring");

        System.out.println(this + "Leaving the ring (notifying neighbors)");

        this.isAwaken = false;

        Packet switchLeftNeighborPacket = new Packet(Type.SwitchNeighbor, this.right.getId(), this.right.getAddress(), this.left.getAddress(), "RIGHT");
        Packet switchRightNeighborPacket = new Packet(Type.SwitchNeighbor, this.left.getId(), this.left.getAddress(), this.right.getAddress(), "LEFT");

        this.send(this.left, switchLeftNeighborPacket);
        this.send(this.right, switchRightNeighborPacket);
        this.left = null;
        this.right = null;
    }

    //liaison entre un objet de la couche applicative et un
    //objet de la couche transport situes sur le meme noeud
    public void setTransportLayer(int address) {
        this.address = address;
        this.transport = (Transport) Network.get(this.address).getProtocol(this.transportPid);
    }

    //methode necessaire pour la creation du reseau (qui se fait par clonage d'un prototype)
    public Object clone() {
        Node dolly = new Node(this.prefix);
        return dolly;
    }

    @Override
    public void processEvent(peersim.core.Node node, int pid, Object event) {
        Packet packet = (Packet) event;
        if (packet.getType() == Type.Discovery) this.onDiscoverPacket(packet);
        else if (packet.getType() == Type.Welcome) this.onWelcomePacket(packet);
        else if (packet.getType() == Type.SwitchNeighbor) this.onSwitchNeighborPacket(packet);
        else if (packet.getType() == Type.Message || packet.getType() == Type.UndeliverableRoutable) this.route(packet);
        else throw new IllegalArgumentException("Event not recognized: " + event);
    }

    private void sendWelcomePacket(Node node, Node left, Node right) {
        Packet welcomePacket = new Packet(
                Type.Welcome, this.getId(),
                this.getAddress(),
                node.getId(),
                left.getAddress() + " " + right.getAddress());
        System.out.println(this + "Send Welcome Packet to " + node + " (left: " + left + ", right: " + right + ")");
        this.send(node, welcomePacket);
    }

    private void onDiscoverPacket(Packet packet) {
        Node newNode = Utils.addressToNode(packet.getSenderAddress());
        System.out.println(this + "Receive " + packet.getType() + " from " + newNode);

        // are we the unique node in the ring ?
        if (this.left.equals(this.right) && this.left.equals(this)) {
            this.sendWelcomePacket(newNode, this, this);
            this.right = newNode;
            this.left = newNode;
            System.out.println(this + "New Neighbor " + "(left: " + newNode + "- right: " + newNode + ")");
        }
        else if (packet.getSenderId() > this.getId()) { // packet.sender > this
            // the sender has a greater id than the local node
            // should be on the right
            int rightId = this.right.getId();

            if (
                // the node is inferior to our right node
                    packet.getSenderId() < rightId // packet.nodeId < rightId
                            // Our right node is inferior than the current node. We are the last node in the ring and the new node
                            // is greater than the local node. We add the node at the end of the ring
                            || this.getId()> rightId // this.id > rightId
            ) {
                // the node should be placed between the right and the local node

                // send back neighbors addresses to the node that joined the cluster
                System.out.println(this + "Welcoming " + newNode + "as my new right node");

                this.sendWelcomePacket(newNode, this, this.right);

                // Notify the right node that his left node has changed
                System.out.println(this + "Notifying node " + this.right + "of their new left node");
                Packet switchNeighborPacket = new Packet(Type.SwitchNeighbor, packet.getSenderId(), packet.getSenderAddress(), this.right.address, "LEFT");
                this.send(this.right, switchNeighborPacket);
                this.right = newNode;
            } else {
                // the node should be placed after the right node
                // we follow the packet to the next node in the ring
                System.out.println(this + "Following discovery of " + packet.getSenderAddress() + " to " + this.right.address);
                this.send(this.right, packet);
            }
        }

        else {
            // the sender has an inferior id than the local node
            // should be on the left
            int leftId = this.left.getId();

            if(
                // the node is greater than the left node
                    packet.getSenderId() > leftId // packet.nodeId > leftId
                            // Our left node is greater than the current node. We are the first node in the ring and the new node
                            // is less than the local node. We add the node at the beginning of the ring
                            || this.getId() < leftId // this.id < leftId
            ) {
                // the node should be placed between the left and the local node

                // send back neighbors addresses to the node that joined the cluster
                System.out.println(this + "Welcoming " + newNode + "as my new left node");
                this.sendWelcomePacket(newNode, this.left, this);

                // Notify the left node that his right node has changed
                System.out.println(this + "Notifying node " + this.left + "of their new right node");
                Packet switchNeighborPacket = new Packet(Type.SwitchNeighbor, packet.getSenderId(), packet.getSenderAddress(), this.left.address, "RIGHT");
                this.send(this.left, switchNeighborPacket);
                this.left = newNode;
            } else {
                // the node should be placed after the left node
                // we follow the packet to the next node in the ring
                System.out.println(this + "Following discovery of " + packet.getSenderAddress() + " to " + this.left.address);
                this.send(this.left, packet);
            }
        }
    }

    /**
     * We got an answer from the ring and we now know the addresses of our neighbors
     * @param packet the packet received
     */
    private void onWelcomePacket(Packet packet) {
        String[] addresses = packet.getContent().split("\\s");
        this.left = Utils.addressToNode(Integer.parseInt(addresses[0]));
        this.right = Utils.addressToNode(Integer.parseInt(addresses[1]));
        this.isAwaken = true;
        System.out.println(this + "Joining to form a ring of size " +
                Initializer.getAwakenNodesCount() +
                " (left: " + this.left + "- right: " + this.right + ")");
    }

    /**
     * A new node entered the cluster and we must change one of our neighbors
     * @param packet the packet received
     */
    private void onSwitchNeighborPacket(Packet packet) {
        if (packet.getContent() == "LEFT") {
            System.out.println(this + "Switching left neighbor from " + this.left.getId() +" to " + packet.getSenderId());
            this.left = Utils.addressToNode(packet.getSenderAddress());
        } else {
            System.out.println(this + "Switching right neighbor from " + this.right.getId() +" to " + packet.getSenderId());
            this.right = Utils.addressToNode(packet.getSenderAddress());
        }
    }

    public void send(Node dest, Packet packet) {
        this.transport.send(getMyNode(), dest.getMyNode(), packet, this.mypid);
    }

    public void sendLeft(Packet packet) {
        this.send(this.left, packet);
    }

    public void sendRight(Packet packet) {
        this.send(this.right, packet);
    }

    public void sendMessage(int targetAddress, String msg) {
        this.route(new Packet(Type.Message, this.getId(), this.getAddress(), targetAddress, msg));
    }

    public void route(Packet packet) {
        if (!this.isAwaken()) throw new IllegalStateException("Node in idle state");

        if (packet.getTargetId() > this.getId()) { // packet.target > this.id
            // route to right node
            if (packet.getTargetId() < this.right.getId()) {
                // the destination node should be placed between us and the right node
                // hence this node is missing, it may have left the ring
                this.nodeNotFoundWhenRouting(packet);
            } else {
                System.out.println(this + "Routing " + packet.getType() + " to right: " + this.right);
                this.sendRight(packet);
            }
        } else if (packet.getTargetId() < this.getId()) {
            // route to left node
            if (packet.getTargetId() > this.left.getId()) {
                // the destination node should be placed between us and the left node
                // hence this node is missing, it may have left the ring
                this.nodeNotFoundWhenRouting(packet);
            } else {
                System.out.println(this + "Routing " + packet.getType() + " to left: " + this.left);
                this.sendLeft(packet);
            }
        } else {
            this.handleRoutableMessage(packet);
        }
    }

    private void nodeNotFoundWhenRouting(Packet message) {
        if (message.getSenderId() == this.getId()) {
            // no need to forward an error packet, we just notify the console
            System.out.println("Node " + message.getTargetId() + " not found");
        } else {
            // route a response to the sender, notifying the node is missing
            this.route(new Packet(Type.UndeliverableRoutable, this.getId(), this.address, message.getSenderAddress(), "Node " + message.getTargetId() + " not found"));
        }
    }

    private void handleRoutableMessage(Packet message) {
        if (message.getType() == Type.Message) this.onMessage(message);
        if (message.getType() == Type.UndeliverableRoutable) this.onUndeliverableRoutableMessage(message);
    }

    private void onMessage(Packet packet) {
        System.out.println(this + "Received a message from " + packet.getSenderId() + ": " + packet.getContent());
    }

    private void onUndeliverableRoutableMessage(Packet packet) {
        System.out.println("Was not able to deliver a message: " + packet.getContent());
    }

    private peersim.core.Node getMyNode() {
	    return Network.get(this.address);
    }

    public int getAddress() {
        return address;
    }

    public int getId() { return id; }

    public Node getLeft() {
        return left;
    }

    public Node getRight() {
        return right;
    }

    public String toString() {
	    return ConsoleColors.BLUE + "[Node "+ this.getId() + "] ";
    }
}