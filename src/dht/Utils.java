package dht;

import peersim.core.Network;

public class Utils {

    public static Node addressToNode(int address) {
        return (Node) Network.get(address).getProtocol(Initializer.getTransportPid());
    }

}
