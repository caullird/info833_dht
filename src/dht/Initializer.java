package dht;

import peersim.core.*;
import peersim.config.*;

import java.util.Random;
import java.util.stream.IntStream;

/*
  Module d'initialisation:
  Fonctionnement:
    pour chaque noeud, le module fait le lien entre la couche transport et la couche applicative
 */
public class Initializer implements Control {

    private static int TRANSPORT_PID = -1;

    public Initializer(String prefix) {
		//recuperation du pid de la couche applicative
		this.TRANSPORT_PID = Configuration.getPid(prefix + ".transport");
    }

	public static int getTransportPid() {
		if (TRANSPORT_PID == -1) throw new IllegalStateException("DHT project not yet initialized");
		return TRANSPORT_PID;
	}

	public boolean execute() {
		System.out.println(this + "Start simulation");

		// Minimum 2 nodes in network
		if (Network.size() < 2) {
			System.err.println(this + "Network size must be greater than 2");
			System.exit(1);
		}

		// Initialize the ring by waking the first node
		System.out.println(this + "Initialize the ring");

		Node initialNode = (Node) Network.get(0).getProtocol(TRANSPORT_PID);
		initialNode.awakeAsInitialNode(0);

		System.out.println(this + "Initialization completed");
		return false;
	}

	public static Node getRandomAwakenNode() {
		Node[] nodes = IntStream.range(0, Network.size())
				.mapToObj(Network::get)
				.map(x -> (Node) x.getProtocol(getTransportPid()))
				.filter(node -> node.isAwaken())
				.toArray(Node[]::new);

		return nodes[new Random().nextInt(nodes.length)];
	}

	public static long getAwakenNodesCount() {
		return IntStream.range(0, Network.size())
				.mapToObj(Network::get)
				.filter(node -> ((Node) node.getProtocol(getTransportPid())).isAwaken())
				.count();
	}

	@Override
	public String toString() {
		return ConsoleColors.PURPLE + "[Initialize] ";
	}
}