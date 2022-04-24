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

        // Afficher le ring
        this.steps.add(this::displayRing);


        /*// send messages
        this.steps.add(() -> sendMessageRandom("Hello world"));
        this.steps.add(() -> sendMessageRandom("Hello universe"));
        this.steps.add(() -> sendMessageRandom("Hello cosmos"));*/
    }

    @Override
    public boolean execute() {
        // skip execution if all action have been executed
        if (this.executedStep == this.steps.size()) return false;

        // trigger the next action
        System.out.println(this + "Lancement de l'étape " + (this.executedStep + 1));
        this.steps.get(this.executedStep).run();
        this.executedStep++;

        return false;
    }

    public void wakeUpNode(int nodeIndex) {
        System.out.println(this + "Waking up node " + nodeIndex);
        Node node = (Node) Network.get(nodeIndex).getProtocol(Initializer.getTransportPid());
        node.awake(nodeIndex);
    }

    /*public void disconnectNode(int nodeIndex) {
        System.out.println("Killing node " + nodeIndex);
        Node node = Network.get(nodeIndex);
        Transport transport = (Transport) node.getProtocol(Initializer.getTransportPid());
        transport.leave();
    }*/

    public void displayRing() {
        Node initialNode = (Node) Network.get(0).getProtocol(Initializer.getTransportPid());
        Node currentNode = initialNode.getRight();
        List<Node> nodes = new LinkedList<>();
        nodes.add(initialNode);

        while (currentNode != initialNode) {
            nodes.add(currentNode);
            currentNode = currentNode.getRight();
        }

        nodes.add(initialNode);

        String idsString = nodes.stream()
                .map(node -> String.format("%s", node.getNodeId()))
                .collect(Collectors.joining(" => "));

        System.out.println("Final ring: " + idsString);
    }

    public void sendMessageRandom(String message) {
        Node randomSender = getRandomAwakenNode();
        Node randomTarget = getRandomAwakenNode();

        System.out.println("Sending `" + message + "` from " + randomSender.getUUID() + " to " + randomTarget.getUUID());

        randomSender.sendMessage(((Node) randomTarget).getUUID(), message);
    }

    @Override
    public String toString() {
        return ConsoleColors.GREEN + "[Controller] ";
    }
}
