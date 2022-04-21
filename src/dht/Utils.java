package dht;

import peersim.core.Network;
import peersim.core.Node;

import java.util.UUID;

public class Utils {
    public static Transport getTransport(Node node) {
        return ((Transport)node.getProtocol(Initializer.getDhtPid()));
    }

    public static Transport getTransport(int node) {
        return getTransport(Network.get(node));
    }
}
