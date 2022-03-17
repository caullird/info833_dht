package dht;

import peersim.core.*;
import peersim.config.*;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*
  Module d'initialisation d'un DHT
  Fonctionnement:
    pour chaque noeud, le module fait le lien entre la couche transport et la couche applicative
    ensuite, il fait envoyer au noeud 0 un message "Hello" a tous les autres noeuds
 */
public class Initializer implements Control {

    private int dhtPid;
    private ArrayList<Dht> nodes = new ArrayList<>();

    public Initializer(String prefix) {
        //recuperation du pid de la couche applicative
        this.dhtPid = Configuration.getPid(prefix + ".dhtProtocolPid");

        // Initialisation du premier noeud
        Dht initialNode = (Dht) Network.get(0).getProtocol(this.dhtPid);
        initialNode.connectAsInitialNode();
        nodes.add(initialNode);
        System.out.println("Fisrt Node in the ring	| UUID : " + initialNode.getUuid());
    }

    public boolean execute() {
        if (Network.size() < 1) {
            System.err.println("Network size is not positive");
            System.exit(1);
        }

        //creation de l'anneau en ajoutant 8 nouveaux noeuds
        this.createRing(8);

        System.out.println("Initialization completed");

        return false;
    }

    public Dht getRandomNode() {
        Random rand = new Random();
        // Get Random Index from the List
        int randomIndex = rand.nextInt(nodes.size());
        // Get the element on the random index in the list
        return this.nodes.get(randomIndex);
    }

    public void createRing(int nbOfNodes) {
        for (int i = 1; i <= nbOfNodes; i++) {
            Dht node = (Dht) Network.get(i).getProtocol(this.dhtPid);
            Dht randomNode = this.getRandomNode();
            randomNode.join(node);
            System.out.println("New node in the ring !");
            System.out.println("Node UUID : " + node.uuid + "	|Left UUID : " + node.leftNode.uuid + "	|Right UUID : " + node.rightNode.uuid);
        }
    }
}