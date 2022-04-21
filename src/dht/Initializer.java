package dht;

import peersim.core.*;
import peersim.config.*;

import java.util.Random;
import java.util.stream.IntStream;

/*
  Module d'initialisation de helloWorld: 
  Fonctionnement:
    pour chaque noeud, le module fait le lien entre la couche transport et la couche applicative
    ensuite, il fait envoyer au noeud 0 un message "Hello" a tous les autres noeuds
 */
public class Initializer implements Control {

    private static int dhtPid;

    public Initializer(String prefix) {
		//recuperation du pid de la couche applicative
		this.dhtPid = Configuration.getPid(prefix + ".dhtProtocolPid");
    }

	public static int getDhtPid() {
		if (dhtPid == 0) throw new IllegalStateException("DHT project not yet initialized");
		return dhtPid;
	}

	public static Node getRandomAwakenNode() {
		Node[] nodes = IntStream.range(0, Network.size())
				.mapToObj(Network::get)
				.filter(node -> !((Dht) node.getProtocol(getDhtPid())).isIdle())
				.toArray(Node[]::new);

		return nodes[new Random().nextInt(nodes.length)];
	}

	public static long getAwakenNodesCount() {
		return IntStream.range(0, Network.size())
				.mapToObj(Network::get)
				.filter(node -> !((Dht) node.getProtocol(getDhtPid())).isIdle())
				.count();
	}

	public boolean execute() {
		System.out.println("Start simulation");

		// Minimum 2 nodes in network
		if (Network.size() < 2) {
			System.err.println("Network size must be greater than 2");
			System.exit(1);
		}

		// Initialize the ring
		System.out.println("Initialize the ring");
		Node initialNode = Network.get(0);
		((Dht) initialNode.getProtocol(getDhtPid())).awakeAsInitialNode();

		System.out.println("Initialization completed");
		return false;
    }
}