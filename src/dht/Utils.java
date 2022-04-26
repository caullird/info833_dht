package dht;

import peersim.core.Network;

public class Utils {

    /**
     * Recupère le noeud associé a une adresse
     * @param address
     * @return node
     */
    public static Node addressToNode(int address) {
        return (Node) Network.get(address).getProtocol(Initializer.getTransportPid());
    }

}
