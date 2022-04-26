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

        // Déconnexion d'un noeud aléatoire
        this.steps.add(() -> disconnectRandomNode());
        this.steps.add(this::displayRing);

        // Envoie de plusieurs messages aléatoires
        this.steps.add(() -> sendMessageRandom("Hey bro"));
        this.steps.add(() -> sendMessageRandom("It's me"));
        this.steps.add(() -> sendMessageRandom("Coucou"));

        // Sauvegarde et récupération d'une donnée
        int randomKey = (int)(Math.random()*(1000));
        this.steps.add(() -> put(randomKey, "Les fleurs sont jolies"));
        this.steps.add(() -> get(randomKey));
    }

    @Override
    public boolean execute() {
        if (this.executedStep == this.steps.size()) return false;

        this.steps.get(this.executedStep).run();
        this.executedStep++;

        return false;
    }

    /**
     * Reveiller le noeud passé en parametre
     * @param nodeIndex
     */
    public void wakeUpNode(int nodeIndex) {
        Node node = (Node) Network.get(nodeIndex).getProtocol(Initializer.getTransportPid());
        System.out.println(this + "Waking up node " + node.getId());
        node.awake(nodeIndex);
    }

    /**
     * Déconnecter un noeud aléatoire
     */
    public void disconnectRandomNode() {
        Node randomNode = getRandomAwakenNode();
        this.disconnectNode(randomNode.getAddress());
    }

    /**
     * Deconnecter le noeud passé en parametre
     * @param address
     */
    public void disconnectNode(int address) {
        Node node = (Node) Network.get(address).getProtocol(Initializer.getTransportPid());
        System.out.println(this + "Disconnect node " + node.getId());
        node.leave();
    }

    /**
     * Afficher l'anneau créer par l'interconnexion de chaque noeud
     */
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

    /**
     * Envoie d'un message d'un noeud aléatoire à un autre noeud aléatoire
     * @param message
     */
    public void sendMessageRandom(String message) {
        Node randomSender = getRandomAwakenNode();
        Node randomTarget = getRandomAwakenNode();

        System.out.println(this + "Sending `" + message + "` from " + randomSender.getId() + " to " + randomTarget.getId());

        randomSender.sendMessage(randomTarget.getId(), message);
    }

    /**
     * Sauvegarder d'une donnée a l'aide d'une clé
     * @param key
     * @param data
     */
    public void put(int key, String data) {
        System.out.println(this + "Inserting key/data: " + key + "/" + data);

        Node randomNode = getRandomAwakenNode();
        randomNode.put(key, data);
    }

    /**
     * Récupération de la donnée liée a la clé
     * @param key
     */
    public void get(int key) {
        System.out.println(this + "Get " + key + " from the DHT");

        Node randomNode = getRandomAwakenNode();
        randomNode.get(key).thenAccept(data -> System.out.println(this + "Data associate to " + key + " is '" + data + "'"));
    }

    @Override
    public String toString() {
        return ConsoleColors.GREEN + "[Controller] ";
    }
}
