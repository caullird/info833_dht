package dht;

import peersim.core.*;
import peersim.config.*;

/*
  Module d'initialisation de helloWorld: 
  Fonctionnement:
    pour chaque noeud, le module fait le lien entre la couche transport et la couche applicative
    ensuite, il fait envoyer au noeud 0 un message "Hello" a tous les autres noeuds
 */
public class Initializer implements peersim.core.Control {
    
    private int dhtPid;

    public Initializer(String prefix) {
	//recuperation du pid de la couche applicative
	this.dhtPid = Configuration.getPid(prefix + ".dhtProtocolPid");
    }

    public boolean execute() {
	int nodeNb;
	Dht emitter, current;
	Node dest;
	Message helloMsg;

	//recuperation de la taille du reseau
	nodeNb = Network.size();
	//creation du message
	helloMsg = new Message(Message.DHT,"Hello!!");
	if (nodeNb < 1) {
	    System.err.println("Network size is not positive");
	    System.exit(1);
	}

	//recuperation de la couche applicative de l'emetteur (le noeud 0)
	emitter = (Dht)Network.get(0).getProtocol(this.dhtPid);
	emitter.setTransportLayer(0);

	//pour chaque noeud, on fait le lien entre la couche applicative et la couche transport
	//puis on fait envoyer au noeud 0 un message "Hello"
	for (int i = 1; i < nodeNb; i++) {
	    dest = Network.get(i);
	    current = (Dht)dest.getProtocol(this.dhtPid);
	    current.setTransportLayer(i);
	    emitter.send(helloMsg, dest);
	}

	System.out.println("Initialization completed");
	return false;
    }
}