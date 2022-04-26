package dht;

import peersim.edsim.*;
import peersim.core.*;
import peersim.config.*;
import dht.Packet.*;
import dht.Packet.RoutablePacket.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static dht.Utils.addressToNode;

public class Node implements EDProtocol {
    //prefixe de la couche (nom de la variable de protocole du fichier de config)
    private String prefix;

    //identifiant de la couche transport
    private int transportPid;

    //objet couche transport
    private Transport transport;

    //identifiant de la couche courante (la couche applicative)
    private int mypid;

    //adresse de la couche transport
    private int address;

    //etat du noeud
    private boolean isAwaken = false;

    //id aléatoire
    private int id = (int)(Math.random()*(1000));

    //noeuds voisins
    private Node left;
    private Node right;

    //hash table
    private Map<Integer,String> storage = new HashMap<>();
    private Map<Integer, CompletableFuture<String>> pendingGets = new HashMap<Integer, CompletableFuture<String>>();

    public Node(String prefix) {
        this.prefix = prefix;
        this.transportPid = Configuration.getPid(prefix + ".transport");
        this.mypid = Configuration.getPid(prefix + ".myself");
        this.transport = null;
    }

    public boolean isAwaken() {
        return isAwaken;
    }

    public int getAddress() {
        return address;
    }

    public int getId() { return id; }

    public Node getLeft() {
        return left;
    }

    public Node getRight() {
        return right;
    }

    public boolean isFirst() { return this.getId() < this.getLeft().getId(); }

    public boolean isLast() { return  this.getId() > this.getRight().getId(); }

    private peersim.core.Node getMyNode() {
        return Network.get(this.getAddress());
    }

    /**
     * methode necessaire pour la creation du reseau (qui se fait par clonage d'un prototype)
     * @return
     */
    public Object clone() {
        Node dolly = new Node(this.prefix);
        return dolly;
    }

    /**
     * liaison entre un objet de la couche applicative et un objet la couche transport situes sur le meme noeud
     * @param address
     */
    public void setTransportLayer(int address) {
        this.address = address;
        this.transport = (Transport) Network.get(address).getProtocol(this.transportPid);
    }

    /**
     * Initialise le noeud comme premier noeud du ring
     * @param address
     */
    public void awakeAsInitialNode(int address) {
        if (this.isAwaken()) {
            System.out.println(this + "Already awaken");
            return;
        }

        this.setTransportLayer(address);
        this.isAwaken = true;

        this.left = this;
        this.right = this;

        System.out.println(this + "Awaken as initial node");
    }

    /**
     * Connecte le noeud au ring
     * @param address
     */
    public void awake(int address) {
        if (this.isAwaken()) {
            System.out.println(this + "Already awaken");
            return;
        }

        this.setTransportLayer(address);

        Node randomAwakenNode = Initializer.getRandomAwakenNode();

        Packet discoveryPacket = new Packet.DiscoveryPacket(this.getId(), this.getAddress());

        System.out.println(this + "Send Discovery Packet to " + randomAwakenNode.getId());
        this.send(randomAwakenNode, discoveryPacket);
    }

    /**
     * Quitte le ring et avertis ses voisins
     */
    public void leave() {
        if(!this.isAwaken()) System.out.println(this + "Cannot leave as the node is not part of the ring");

        System.out.println(this + "Leaving the ring (notifying neighbors)");

        this.isAwaken = false;

        Packet switchLeftNeighborPacket = new SwitchNeighborPacket("LEFT", this.left.getAddress());
        Packet switchRightNeighborPacket = new SwitchNeighborPacket("RIGHT", this.right.getAddress());

        this.sendRight(switchLeftNeighborPacket);
        this.sendLeft(switchRightNeighborPacket);
        this.left = null;
        this.right = null;

        this.storage.forEach((k,v)->{
            RoutablePacket packet = new RoutablePacket(Type.PUT, this.getId(), this.getAddress(), k, v);
            if (k > this.getId()) { this.sendLeft(packet); } else { this.sendRight(packet);}
        });
    }

    public void send(peersim.core.Node dest, Packet packet) {
        this.transport.send(getMyNode(), dest, packet, this.mypid);
    }

    public void send(Node dest, Packet packet) {
        this.send(Network.get(dest.getAddress()), packet);
    }

    public void sendLeft(Packet packet) {
        this.send(this.left, packet);
    }

    public void sendRight(Packet packet) {
        this.send(this.right, packet);
    }

    public void sendMessage(int targetAddress, String msg) {
        this.route(new RoutablePacket(Type.MESSAGE, this.getId(), this.getAddress(), targetAddress, msg));
    }

    private void sendWelcomePacket(Node newNode, Node left, Node right) {
        Packet welcomePacket = new Packet.WelcomePacket(left.getAddress(), right.getAddress());

        System.out.println(this + "Send Welcome Packet to " + newNode.getId() + " (left: " + left.getId() + ", right: " + right.getId() + ")");
        this.send(newNode, welcomePacket);
    }

    public void put(int key, String data) {
        RoutablePacket packet = new RoutablePacket(Type.PUT, this.getId(), this.getAddress(), key, data);
        this.onPut(packet);
    }

    public CompletableFuture<String> get(int key) {
        RoutablePacket packet = new RoutablePacket(Type.GET, this.getId(), this.getAddress(), key);
        CompletableFuture<String> future = new CompletableFuture<>();

        this.pendingGets.put(key, future);
        this.onGet(packet);
        return future;
    }

    @Override
    public void processEvent(peersim.core.Node node, int pid, Object event) {
        if (event instanceof Packet.DiscoveryPacket) this.onDiscoveryPacket((DiscoveryPacket)event);
        else if (event instanceof Packet.WelcomePacket) this.onWelcomePacket((WelcomePacket)event);
        else if (event instanceof Packet.SwitchNeighborPacket) this.onSwitchNeighborPacket((SwitchNeighborPacket)event);
        else if (event instanceof Packet.RoutablePacket) this.route((RoutablePacket)event);
        else throw new IllegalArgumentException("Event not recognized: " + event);
    }

    /**
     * Quand un noeud souhaite rejoindre l'anneau
     * @param packet
     */
    private void onDiscoveryPacket(DiscoveryPacket packet) {
        System.out.println(this + "Receive discovery from " + packet.getId());
        Node newNode = addressToNode(packet.getAddress());

        if (this.left.equals(this.right) && this.left.equals(this)) {
            this.sendWelcomePacket(newNode, this, this);
            this.right = newNode;
            this.left = newNode;
            System.out.println(this + "New Neighbor " + "(left: " + newNode + "- right: " + newNode + ")");
        }
        else if (packet.getId() > this.getId()) {
            int rightId = this.right.getId();

            if (packet.getId() < rightId || this.isLast()) {
                /**
                 * Placement du nouveau noeud entre moi et mon voisin de droite
                 */
                System.out.println(this + "Welcoming " + newNode.getId() + " as my new right node");

                this.sendWelcomePacket(newNode, this, this.right);

                // Notify the right node that his left node has changed
                System.out.println(this + "Notifying node " + this.right + "of their new left node");

                Packet switchNeighborPacket = new SwitchNeighborPacket("LEFT", packet.getAddress());
                this.send(this.right, switchNeighborPacket);
                this.right = newNode;
            } else {
                // the node should be placed after the right node
                // we follow the dht.packet to the next node in the ring
                System.out.println(this + "Following discovery of " + packet.getAddress() + " to " + this.right.address);
                this.send(this.right, packet);
            }
        }

        else {
            // the sender has an inferior id than the local node
            // should be on the left
            int leftId = this.left.getId();

            if(packet.getId() > leftId || this.isFirst()) {
                // the node should be placed between the left and the local node

                // send back neighbors addresses to the node that joined the cluster
                System.out.println(this + "Welcoming " + newNode + "as my new left node");
                this.sendWelcomePacket(newNode, this.left, this);

                // Notify the left node that his right node has changed
                System.out.println(this + "Notifying node " + this.left + "of their new right node");
                Packet switchNeighborPacket = new SwitchNeighborPacket("RIGHT", packet.getAddress());
                this.send(this.left, switchNeighborPacket);
                this.left = newNode;
            } else {
                // the node should be placed after the left node
                // we follow the dht.packet to the next node in the ring
                System.out.println(this + "Following discovery of " + packet.getAddress() + " to " + this.left.address);
                this.send(this.left, packet);
            }
        }
    }

    /**
     * Quand le noeud reçoit la position dans l'anneau
     * @param packet
     */
    private void onWelcomePacket(WelcomePacket packet) {
        this.left = addressToNode(packet.left);
        this.right = addressToNode(packet.right);
        this.isAwaken = true;
        System.out.println(this + "Joining to form a ring of size " +
                Initializer.getAwakenNodesCount() +
                " (left: " + this.left + "- right: " + this.right + ")");
    }

    /**
     * Quand il un noeud doit changer de voisin (nouvelle connexion / deconnexion)
     * @param packet the dht.packet received
     */
    private void onSwitchNeighborPacket(SwitchNeighborPacket packet) {
        Node newNeighbor = addressToNode(packet.address);
        if (packet.left) {
            System.out.println(this + "Switching left neighbor from " + this.left.getId() +" to " + newNeighbor.getId());
            this.left = newNeighbor;
        } else {
            System.out.println(this + "Switching right neighbor from " + this.right.getId() +" to " + newNeighbor.getId());
            this.right = newNeighbor;
        }
    }

    /**
     * Quand le noeud reçoit un packet routable
     * @param packet
     */
    public void route(RoutablePacket packet) {
        if (!this.isAwaken()) throw new IllegalStateException("Node in idle state");

        if (packet.getType() == Type.REPLICATION || packet.getType() == Type.GET_RESPONSE
            || packet.getType() == Type.PUT || packet.getType() == Type.GET) {
            this.handleRoutableMessage(packet);
            return;
        }

        if (packet.getTarget() > this.getId()) { // dht.packet.target > this.id
            // route to right node
            if (packet.getTarget() < this.right.getId()) {
                if (packet.getType() == Type.GET || packet.getType() == Type.PUT) {
                    this.handleRoutableMessage(packet);
                } else {
                    this.nodeNotFoundWhenRouting(packet);
                }
            } else {
                System.out.println(this + "Routing " + packet.getType() + " to right: " + this.right.getId());
                this.sendRight(packet);
            }
        } else if (packet.getTarget() < this.getId()) {
            // route to left node
            if (packet.getTarget() > this.left.getId()) {
                if (packet.getType() == Type.GET || packet.getType() == Type.PUT) {
                    this.handleRoutableMessage(packet);
                } else {
                    this.nodeNotFoundWhenRouting(packet);
                }
            } else {
                System.out.println(this + "Routing " + packet.getType() + " to left: " + this.left.getId());
                this.sendLeft(packet);
            }
        } else {
            this.handleRoutableMessage(packet);
        }
    }

    /**
     * Impossible de trouver le noeud
     * @param packet
     */
    public void nodeNotFoundWhenRouting(RoutablePacket packet) {
        if (packet.getSender() == this.getId()) {
            System.out.println(this + "Node " + packet.getTarget() + " not found");
        } else {
            RoutablePacket undeliverablePacket = new RoutablePacket(Type.UNDELIVERABLE, this.getId(), this.getAddress(), packet.getSender());

            Node sender = addressToNode(packet.getSenderAddress());
            this.send(sender, undeliverablePacket);
        }
    }

    /**
     * Quand le routablePacket est arrivé a destination
     * @param packet
     */
    private void handleRoutableMessage(RoutablePacket packet) {
        if (packet.getType() == Type.MESSAGE) this.onMessage(packet);
        if (packet.getType() == Type.UNDELIVERABLE) this.onUndeliverable(packet);
        if (packet.getType() == Type.PUT) this.onPut(packet);
        if (packet.getType() == Type.REPLICATION) this.onReplication(packet);
        if (packet.getType() == Type.GET) this.onGet(packet);
        if (packet.getType() == Type.GET_RESPONSE) this.onGetResponse(packet);
    }

    private void onMessage(RoutablePacket packet) {
        System.out.println(this + "Received a message from " + packet.getSender() + ": " + packet.getData());
    }

    private void onUndeliverable(RoutablePacket packet) {
        System.out.println("Was not able to deliver packet: " + packet.getData());
    }

    private void onPut(RoutablePacket packet) {
        if (this.isFirst() && packet.getTarget() < this.getId() || this.isLast() && packet.getTarget() > this.getId()
            || this.isFirst() && packet.getTarget() < this.getId() + (this.right.getId() - this.getId()) / 2
            || this.isLast() && packet.getTarget() > this.getId() + (this.left.getId() - this.getId()) / 2) {
            this.store(packet);
        }
        else if (!this.isLast() && packet.getTarget() > this.getId() + (this.right.getId() - this.getId()) / 2 || this.isFirst())  {
            System.out.println(this + "PUT " + packet.getTarget() + " to right ");
            this.sendRight(packet);
        }
        else if (!this.isFirst() && packet.getTarget() < this.getId() + (this.left.getId() - this.getId()) / 2 || this.isLast()) {
            System.out.println(this + "PUT " + packet.getTarget() + " to left ");
            this.sendLeft(packet);
        }
        else {
            this.store(packet);
        }
    }

    public void store(RoutablePacket packet) {
        System.out.println(this + "Stored `" + packet.getData() + "` (hash: " + packet.getTarget() + ")");
        this.storage.put(packet.getTarget(), packet.getData());

        System.out.println(this + "Replicate to " + this.getLeft() + "& " + this.getRight());
        RoutablePacket replicationPacket = new RoutablePacket(Type.REPLICATION, this.getId(), this.getAddress(), packet.getTarget(), packet.getData());
        this.send(this.getLeft(), replicationPacket);
        this.send(this.getRight(), replicationPacket);
    }

    private void onReplication(RoutablePacket packet) {
        this.storage.put(packet.getTarget(), packet.getData());
        System.out.println(this + "Replicated storage for (hash: " + packet.getTarget() + ")");
    }

    private void onGet(RoutablePacket packet) {
        if (packet.getTarget() > this.right.getId() && !this.isLast()) {
            System.out.println(this + "GET " + packet.getTarget() + " to right ");
            this.sendRight(packet);
        }
        else if (packet.getTarget() < this.left.getId() && !this.isFirst()) {
            System.out.println(this + "GET " + packet.getTarget() + " to right ");
            this.sendLeft(packet);
        }
        else {
            // We should have the data or a replication of the data
            String data = this.storage.get(packet.getTarget());

            RoutablePacket responsePacket = new RoutablePacket(
                    Type.GET_RESPONSE,
                    this.getId(), this.getAddress(),
                    packet.getTarget(), data
            );

            this.send(addressToNode(packet.getSenderAddress()), responsePacket);
        }
    }

    private void onGetResponse(RoutablePacket packet) {
        System.out.println(this + "GET_RESPONSE for (hash: " + packet.getTarget() + ")");
        this.pendingGets.get(packet.getTarget()).complete(packet.getData());
    }

    public String toString() {
	    return ConsoleColors.BLUE + "[Node "+ this.getId() + "] ";
    }
}