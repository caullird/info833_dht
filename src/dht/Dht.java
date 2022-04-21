package dht;

import peersim.edsim.*;
import peersim.core.*;
import peersim.config.*;

import java.util.UUID;

public class Dht implements EDProtocol {

    //prefixe de la couche (nom de la variable de protocole du fichier de config)
    private String prefix;

    //identifiant de la couche transport
    private int transportPid;

    //reveillé et en attente de connexion
    private boolean idle = true;







    //objet couche transport
    private Transport transport;

    //identifiant de la couche courante (la couche applicative)
    private int mypid;

    //le numero de noeud
    private int nodeId;






    // Noeud a gauche avec un uuid inférieur
    private Node left = null;

    // Noeud a droite avec un uuid supérieur
    private Node right = null;








    public Dht(String prefix) {
        this.prefix = prefix;
        //initialisation des identifiants a partir du fichier de configuration
        this.transportPid = Configuration.getPid(prefix + ".transport");
        this.mypid = Configuration.getPid(prefix + ".myself");
        this.transport = null;
    }

    public boolean isIdle() {
        return idle;
    }

    public void send(Message msg, Node dest) {
        this.transport.send(getMyNode(), dest, msg, this.mypid);
    }

    public void sendLeft(Message msg) {
        this.send(msg, this.left);
    }

    public void sendRight(Message msg) {
        this.send(msg, this.right);
    }

    public void route(Message message) {
        if (this.isIdle()) throw new IllegalStateException("Node in idle state");

        if (message.getTarget() > this.nodeId) { // packet.target > this.id
            // route to right node
            if (message.getTarget() < this.right.getID()) {
                // the destination node should be placed between us and the right node
                // hence this node is missing, it may have left the ring
                this.nodeNotFoundWhenRouting(message);
            } else {
                System.out.println("Routing packet to right: " + this.right.getID());
                this.sendRight(message);
            }
        } else if (message.getTarget() < this.nodeId) {
            // route to left node
            if (message.getTarget() > this.left.getID()) {
                // the destination node should be placed between us and the left node
                // hence this node is missing, it may have left the ring
                this.nodeNotFoundWhenRouting(message);
            } else {
                System.out.println("Routing packet to left: " + this.left.getID());
                this.sendLeft(message);
            }
        } else {
            this.handleRoutablePacket(message);
        }
    }

    public void sendMessage(int target, String message) {
        this.route(new Message(Type.Message, nodeId, target, message));
    }

    private void nodeNotFoundWhenRouting(Message message) {
        if (message.getSender() == this.mypid) {
            // no need to forward an error packet, we just notify the console
            System.out.println("Node " + message.getTarget() + " not found");
        } else {
            // route a response to the sender, notifying the node is missing
            this.route(new Message(Type.UndeliverableRoutable, this.mypid, message.getSender(), "Node " + message.getTarget() + " not found"));
        }
    }

    private void handleRoutablePacket(Message message) {
        if (message.getType() == Type.Message) this.onMessagePacket(message);
        if (message.getType() == Type.UndeliverableRoutable) this.onUndeliverableRoutablePacket(message);
        // if (message.getType() == Type.Application) this.sendToApplication(message);
    }

    private void onMessagePacket(Message message) {
        System.out.println("Received a message from " + message.getSender() + ": " + message.getContent());
    }

    private void onUndeliverableRoutablePacket(Message message) {
        System.out.println("Was not able to deliver a message: " + message.getContent());
    }



















    @Override
    public void processEvent(Node node, int pid, Object event) {
        Message message = (Message) event;
        System.out.println("Received packet: " + message);

        //if (event.getType() == Type.Discovery) this.onDiscoverPacket(event);
        //else if (event.getType() == Type.Welcome) this.onWelcomePacket(event);
        //else if (event.getType() == Type.SwitchNeighbor) this.onSwitchNeighborPacket(event);
        //else if (event.getType() == Type.Routable) this.onRoutablePacket(event);
        //else if (event.getType() == Type.Application) this.sendToApplication(event);
        //else throw new IllegalArgumentException("Event not recognized: " + event);
    }































    //methode necessaire pour la creation du reseau (qui se fait par clonage d'un prototype)
    public Object clone() {
        Dht dolly = new Dht(this.prefix);
        return dolly;
    }

    //liaison entre un objet de la couche applicative et un 
    //objet de la couche transport situes sur le meme noeud
    public void setTransportLayer(int nodeId) {
        this.nodeId = nodeId;
        this.transport = (Transport) Network.get(this.nodeId).getProtocol(this.transportPid);
    }


















    public void awake(Node node) {
        if (this.isIdle()) {
            System.out.println("The node is already awaken");
            return;
        }

        this.mypid = node.getIndex();

        Node target = Initializer.getRandomAwakenNode();
        Message message = new Message(Type.Discovery, this.mypid, target.getIndex(), "");

        System.out.println("Starting discovery, contacting node " + target.getIndex() + " and waiting for response");

        this.send(message, target);
    }

    /**
     * Initialize this as the first node in the ring
     */
    public void awakeAsInitialNode() {
        if (this.isIdle()) {
            System.out.println("The node is already awaken");
            return;
        }

        this.left = this.getMyNode();
        this.right = this.getMyNode();
        this.idle = false;

        System.out.println("Awaken as initial node");
    }

















































    //retourne le noeud courant
    private Node getMyNode() {
	return Network.get(this.nodeId);
    }

    public String toString() {
	return "Node "+ this.nodeId;
    }
}