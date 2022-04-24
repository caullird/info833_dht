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

    //le numero de noeud
    private int nodeId;

    private boolean isAwaken = false;

    //uuid aléatoire
    private UUID uuid = UUID.randomUUID();

    /**
     * Left node in the ring. A node with an inferior id
     */
    private Node left = null;

    /**
     * Right node in the ring. A node with a greater id
     */
    private Node right = null;

    public Node(String prefix) {
        this.prefix = prefix;
        //initialisation des identifiants a partir du fichier de configuration
        this.transportPid = Configuration.getPid(prefix + ".transport");
        this.mypid = Configuration.getPid(prefix + ".myself");
        this.transport = null;
    }

    public boolean isAwaken() {
        return isAwaken;
    }

    public UUID getUUID() {
        return uuid;
    }

    public Node getLeft() {
        return left;
    }

    public void setLeft(Node left) {
        this.left = left;
    }

    public Node getRight() {
        return right;
    }

    public void setRight(Node right) {
        this.right = right;
    }

    // Initialize this as the first node in the ring
    public void awakeAsInitialNode(int nodeId) {
        if (this.isAwaken()) {
            System.out.println("The node is already awaken");
            return;
        }

        this.setTransportLayer(nodeId);
        this.isAwaken = true;

        this.left = this;
        this.right = this;

        System.out.println(this + "Awaken as initial node");
    }

    // Initialize this as the first node in the ring
    public void awake(int nodeId) {
        if (this.isAwaken()) {
            System.out.println(this + "Already awaken");
            return;
        }

        this.setTransportLayer(nodeId);

        Node randomAwakenNode = Initializer.getRandomAwakenNode();

        Packet discoveryPacket = new Packet(Type.Discovery, this.uuid, this.nodeId, randomAwakenNode.uuid, "");

        System.out.println(this + "Send Discovery Packet to " + randomAwakenNode);
        this.send(randomAwakenNode, discoveryPacket);
    }

    //liaison entre un objet de la couche applicative et un
    //objet de la couche transport situes sur le meme noeud
    public void setTransportLayer(int nodeId) {
        this.nodeId = nodeId;
        this.transport = (Transport) Network.get(this.nodeId).getProtocol(this.transportPid);
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
        // else if (event instanceof RoutablePacket) this.onRoutablePacket((RoutablePacket) packet);
        else throw new IllegalArgumentException("Event not recognized: " + event);
    }

    private void sendWelcomePacket(int address, Node left, Node right) {
        Node node = addressToNode(address);
        Packet welcomePacket = new Packet(Type.Welcome, this.uuid, this.nodeId, address, left.mypid + " " + right.mypid);
        System.out.println(this + "Send Welcome Packet to " + node + " (left: " + left + ", right: " + right + ")");
        this.send(node, welcomePacket);
    }

    private void onDiscoverPacket(Packet packet) {
        Node newNode = addressToNode(packet.getSenderAddress());
        System.out.println(this + "Receive " + packet.getType() + " from " + newNode);

        // are we the unique node in the ring ?
        if (this.left.equals(this.right) && this.left.equals(this)) {
            this.sendWelcomePacket(packet.getSenderAddress(), this, this);
            this.right = newNode;
            this.left = newNode;
        }
        else if (packet.getSender().compareTo(this.uuid) > 0) { // packet.nodeId > this.id
            // the sender has a greater id than the local node
            // should be on the right
            UUID rightId = this.right.uuid;

            if (
                // the node is inferior to our right node
                    packet.getSender().compareTo(rightId) < 0 // packet.nodeId < rightId
                            // Our right node is inferior than the current node. We are the last node in the ring and the new node
                            // is greater than the local node. We add the node at the end of the ring
                            || this.uuid.compareTo(rightId) > 0 // this.id > rightId
            ) {
                // the node should be placed between the right and the local node

                // send back neighbors addresses to the node that joined the cluster
                System.out.println(this + "Welcoming node " + packet.getSenderAddress() + " as my new right node");

                this.sendWelcomePacket(packet.getSenderAddress(), this, this.right);

                // Notify the right node that his left node has changed
                System.out.println(this + "Notifying node " + this.right.mypid + " of their new left node");
                Packet switchNeighborPacket = new Packet(Type.SwitchNeighbor, packet.getSender(), packet.getSenderAddress(), this.right.mypid, "LEFT");
                this.send(this.right, switchNeighborPacket);
                this.right = newNode;
            } else {
                // the node should be placed after the right node
                // we follow the packet to the next node in the ring
                System.out.println(this + "Following discovery of " + packet.getSenderAddress() + " to " + this.right.nodeId);
                this.send(this.right, packet);
            }
        }

        else {
            // the sender has an inferior id than the local node
            // should be on the left
            UUID leftId = this.left.uuid;

            if(
                // the node is greater than the left node
                    packet.getSender().compareTo(leftId) > 0 // packet.nodeId > leftId
                            // Our left node is greater than the current node. We are the first node in the ring and the new node
                            // is less than the local node. We add the node at the beginning of the ring
                            || this.uuid.compareTo(leftId) < 0 // this.id < leftId
            ) {
                // the node should be placed between the left and the local node

                // send back neighbors addresses to the node that joined the cluster
                System.out.println(this + "Welcoming node " + packet.getSenderAddress() + " as my new left node");
                this.sendWelcomePacket(packet.getSenderAddress(), this.left, this);

                // Notify the left node that his right node has changed
                System.out.println(this + "Notifying node " + this.left.mypid + " of their new right node");
                Packet switchNeighborPacket = new Packet(Type.SwitchNeighbor, packet.getSender(), packet.getSenderAddress(), this.left.mypid, "RIGHT");
                this.send(this.left, switchNeighborPacket);
                this.left = newNode;
            } else {
                // the node should be placed after the left node
                // we follow the packet to the next node in the ring
                System.out.println(this + "Following discovery of " + packet.getSenderAddress() + " to " + this.left.nodeId);
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
        this.left = addressToNode(Integer.parseInt(addresses[0]));
        this.right = addressToNode(Integer.parseInt(addresses[1]));
        this.isAwaken = true;
        System.out.println(this + "Joining to form a ring of size " + Initializer.getAwakenNodesCount());
    }

    /**
     * A new node entered the cluster and we must change one of our neighbors
     * @param packet the packet received
     */
    private void onSwitchNeighborPacket(Packet packet) {
        if (packet.getContent() == "LEFT") {
            System.out.println(this + "Switching left neighbor from " + this.left.mypid +" to " + packet.getSenderAddress());
            this.left = addressToNode(packet.getSenderAddress());
        } else {
            System.out.println(this + "Switching right neighbor from " + this.right.mypid +" to " + packet.getSenderAddress());
            this.right = addressToNode(packet.getSenderAddress());
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

    public void sendMessage(UUID target, String msg) {
        this.route(new Packet(Type.Message, this.uuid, this.mypid, target, msg));
    }

    public void route(Packet message) {
        if (!this.isAwaken()) throw new IllegalStateException("Node in idle state");

        if (message.getTarget().compareTo(this.uuid) > 0) { // packet.target > this.id
            // route to right node
            if (message.getTarget().compareTo(this.right.getUUID()) < 0) {
                // the destination node should be placed between us and the right node
                // hence this node is missing, it may have left the ring
                this.nodeNotFoundWhenRouting(message);
            } else {
                System.out.println("Routing packet to right: " + this.right.getUUID());
                this.sendRight(message);
            }
        } else if (message.getTarget().compareTo(this.uuid) < 0) {
            // route to left node
            if (message.getTarget().compareTo(this.left.getUUID()) > 0) {
                // the destination node should be placed between us and the left node
                // hence this node is missing, it may have left the ring
                this.nodeNotFoundWhenRouting(message);
            } else {
                System.out.println("Routing packet to left: " + this.left.getUUID());
                this.sendLeft(message);
            }
        } else {
            this.handleRoutableMessage(message);
        }
    }

    private void nodeNotFoundWhenRouting(Packet message) {
        if (message.getSender() == this.uuid) {
            // no need to forward an error packet, we just notify the console
            System.out.println("Node " + message.getTarget() + " not found");
        } else {
            // route a response to the sender, notifying the node is missing
            this.route(new Packet(Type.UndeliverableRoutable, this.uuid, this.mypid, message.getSenderAddress(), "Node " + message.getTarget() + " not found"));
        }
    }

    private void handleRoutableMessage(Packet message) {
        if (message.getType() == Type.Message) this.onMessage(message);
        if (message.getType() == Type.UndeliverableRoutable) this.onUndeliverableRoutableMessage(message);
    }

    private void onMessage(Packet packet) {
        System.out.println("Received a message from " + packet.getSender() + ": " + packet.getContent());
    }

    private void onUndeliverableRoutableMessage(Packet packet) {
        System.out.println("Was not able to deliver a message: " + packet.getContent());
    }

    //retourne le noeud courant
    private peersim.core.Node getMyNode() {
	    return Network.get(this.nodeId);
    }

    private static Node addressToNode(int address) {
        return (Node) Network.get(address).getProtocol(Initializer.getTransportPid());
    }

    public int getNodeId() {
        return nodeId;
    }

    public String toString() {
	    return ConsoleColors.BLUE + "[Node "+ this.nodeId + "] ";
    }
}