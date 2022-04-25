package dht;

import peersim.core.Control;
import peersim.core.Network;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static dht.Initializer.getRandomAwakenNode;
public class Controller implements Control {

    private int executedStep = 0;

    private final List<Runnable> steps = new ArrayList<>();

    public Controller(String prefix) {
        // Réveiller tous les noeuds
        for (int i = 1; i < Network.size(); i++) {
            final int index = i;
            this.steps.add(() -> wakeUpNode(index));
        }
        this.steps.add(this::displayRing);

        // Suppresion d'un noeud
        this.steps.add(() -> disconnectRandomNode());
        this.steps.add(this::displayRing);

        // send messages
        this.steps.add(() -> sendMessageRandom("Hello world"));
        this.steps.add(() -> sendMessageRandom("Hello universe"));
        this.steps.add(() -> sendMessageRandom("Hello cosmos"));
    }

    @Override
    public boolean execute() {
        // skip execution if all action have been executed
        if (this.executedStep == this.steps.size()) return false;

        // trigger the next action
        this.steps.get(this.executedStep).run();
        this.executedStep++;

        return false;
    }

    public void wakeUpNode(int nodeIndex) {
        System.out.println(this + "Waking up node " + nodeIndex);
        Node node = (Node) Network.get(nodeIndex).getProtocol(Initializer.getTransportPid());
        node.awake(nodeIndex);
    }

    public void disconnectRandomNode() {
        Node randomNode = getRandomAwakenNode();
        this.disconnectNode(randomNode.getAddress());
    }

    public void disconnectNode(int address) {
        Node node = (Node) Network.get(address).getProtocol(Initializer.getTransportPid());
        System.out.println(this + "Disconnect node " + node.getId());
        node.leave();
    }

    public void displayRing() {
        Node startNode = Initializer.getRandomAwakenNode();
        Node currentNode = startNode.getRight();
        List<Node> nodes = new LinkedList<>();
        nodes.add(startNode);

        while (currentNode != startNode) {
            nodes.add(currentNode);
            currentNode = currentNode.getRight();
        }

        String idsString = nodes.stream()
                .map(node -> String.format("%s", node.getId()))
                .collect(Collectors.joining(" => "));

        System.out.println(this + ConsoleColors.YELLOW_BOLD + "Final ring: " + idsString);
    }

    public void sendMessageRandom(String message) {
        Node randomSender = getRandomAwakenNode();
        Node randomTarget = getRandomAwakenNode();

        System.out.println(this + "Sending `" + message + "` from " + randomSender.getId() + " to " + randomTarget.getId());

        randomSender.sendMessage(randomTarget.getId(), message);
    }

    @Override
    public String toString() {
        return ConsoleColors.GREEN + "[Controller] ";
    }
}
