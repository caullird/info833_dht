package dht;

import peersim.edsim.*;
import peersim.core.*;
import peersim.config.*;

import java.util.UUID;

public class Dht implements EDProtocol {

    //identifiant de la couche transport
    private int transportPid;

    //objet couche transport
    private Transport transport;

    //identifiant de la couche courante (la couche applicative)
    private int mypid;

    //le numero de noeud
    private int nodeId;

    //prefixe de la couche (nom de la variable de protocole du fichier de config)
    private String prefix;

    //voisins du noeud
    public Dht leftNode;
    public Dht rightNode;

    public UUID uuid;

    //état du noeud
    public boolean connected = false;

    public Dht(String prefix) {
        this.prefix = prefix;
        //initialisation des identifiants a partir du fichier de configuration
        this.transportPid = Configuration.getPid(prefix + ".transport");
        this.mypid = Configuration.getPid(prefix + ".myself");
        this.transport = null;
        this.uuid = UUID.randomUUID();
    }

    //methode appelee lorsqu'un message est recu par le protocole HelloWorld du noeud
    public void processEvent(Node node, int pid, Object event) {
        this.receive((Message) event);
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

    //envoi d'un message (l'envoi se fait via la couche transport)
    public void send(Message msg, Node dest) {
        this.transport.send(getMyNode(), dest, msg, this.mypid);
    }

    //affichage a la reception
    private void receive(Message msg) {
        System.out.println(this + ": Received " + msg.getContent());
    }

    //retourne le noeud courant
    private Node getMyNode() {
        return Network.get(this.nodeId);
    }

    public String toString() {
        return "Node " + this.nodeId;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public void connectAsInitialNode() {
        this.setConnected(true);
        this.leftNode = this;
        this.rightNode = this;
        this.setTransportLayer(0);
    }

    public UUID getUuid() {
        return uuid;
    }

    public void join(Dht node) {
        node.setConnected(true);

        // plus grand que moi et plus petit que mon voisin de droite
        boolean c1 = this.uuid.compareTo(node.uuid) == 1  && this.rightNode.uuid.compareTo(node.uuid) == -1;

        boolean c2 = this.uuid.compareTo(node.uuid) == -1  && this.leftNode.uuid.compareTo(node.uuid) == 1;


        if () {

        } else if (this.uuid.compareTo(node.uuid) == 1  && this.rightNode.uuid.compareTo(node.uuid) == -1) {

        }





        if (c1) {
            node.leftNode = this;
            node.rightNode = this.rightNode;

            this.rightNode.leftNode = node;
            this.rightNode = node;

            return true;
        } else if (c2) {
            node.leftNode = this.leftNode;
            node.rightNode = this;

            this.leftNode.rightNode = node;
            this.leftNode = node;

            return true;
        } else if (c3) {
            return this.rightNode.join(node);
        } else if (c4) {
            return this.leftNode.join(node);
        } else {
            return false;
        }


        //cas ou la place du noeud est entre le noeud actuel et son voisin de droite
        boolean c1 = ((this.uuid < node.uuid) && (node.uuid <= this.rightNode.uuid)) || ((this.uuid < node.uuid) && (this.uuid >= this.rightNode.uuid));

        //cas ou la place du noeud est entre le noeud actuel et son voisin de gauche
        boolean c2 = ((this.uuid >= node.uuid) && (node.uuid > this.leftNode.uuid)) || ((this.uuid >= node.uuid) && (this.uuid <= this.leftNode.uuid));

        //cas ou la place du noeud est aprés le voisin de droite du noeud actuel
        boolean c3 = ((this.uuid <= node.uuid) && (node.uuid >= this.rightNode.uuid));

        //cas ou la place du noeud est aprés le voisin de gauche du noeud actuel
        boolean c4 = ((this.uuid >= node.uuid) && (node.uuid <= this.leftNode.uuid));




        this.setTransportLayer(0);
    }

    public void leave() {
        this.setConnected(false);
        this.leftNode.rightNode = this.rightNode;
        this.rightNode.leftNode = this.leftNode;
    }

}