# 🔆 INFO833 - DHT

Le but de ce projet est d'utiliser peersim, un simulateur de communication peer to peer, pour créer un DHT composé de différents nœuds.

🛠️ **Objectif du projet au niveau de la mise en œuvre technique**

    ✅ Le noeud peut rejoindre / quitter l'anneau

    ✅ Envoyer/remettre des messages à/depuis d'autres noeuds

    ✅ Mettre/obtenir des données de la DHT

    ✅ Routage avancé

## 🛠️ **How to use** 

1️⃣ **Cloner le Git**

2️⃣ **Assurez-vous que vous avez les paquets suivants** 
* djep-1.0.0.jar
* jep-2.3.0.jar
* peersim-1.0.5.jar
* peersim-doclet.jar

2️⃣ **Ensuite, exécutez le projet**

Exécutez *peersim.Simulator.java* en ajoutant *config_file.cfg* comme argument.

## 👀 Aperçu rapide de notre projet 

### Presentation
La DHT est un simple anneau où chaque noeud connait uniquement ses voisins immédiats.
Chaque noeud a un identifiant (aléatoire), les noeuds sont rangés dans l'anneau en fonction de leur identifiant.
Pour faire simple un noeud initial va se réveiller, en suite les autres noeuds du Network vont se réveiller un par un et vont trouver leur place dans le cercle en parcourant de gauche à droite (gauche = plus petit, droite = plus grand).

### Composition d'un noeud :
- Une couche **transport** pour permettre l'envoie de packets entre les noeuds
- Une couche **applicative** qui correspond au action que le noeud doit effectuer

### Les différents types de packets
- **DiscoveryPacket** : le noeud envoie ce packet qui va se propager à tous les autres noeuds dans l'anneau pour trouver sa place dans l'anneau
- **WelcomePacket** : packet envoyé au noeud qui vient de se réveiller pour lui dire sa place dans l'anneau
- **SwitchingNeighborPacket** : packet envoyé pour prevenir un noeud qu'il est en train de changer de voisin
- **RoutablePacket** : packet qui va se diffuser dans l'anneau pour être reçu par le noeud le plus proche

### Les différents types de RoutablePacket
- **Message** : le noeud envoie ce packet pour envoyer un message à un autre noeud
- **Undeliverable** : le noeud envoie ce packet pour signaler qu'un message n'a pas pu être envoyé
- **PUT** : le noeud envoie ce packet pour demander à un autre noeud de stocker une donnée
- **REPLCIATION** : le noeud envoie ce packet pour demander à un autre noeud de stocker une donnée
- **GET** : le noeud envoie ce packet pour demander à un autre noeud de récupérer une donnée
- **GET_RESPONSE** : le noeud envoie ce packet pour répondre à une demande de récupération de donnée

To find out more and try out all the features, it's best to try out our application! 

## 📜 **Résultats avec les logs**

### Le reveils des noeuds
```
[Controller] Waking up node 722
[Node 722] Send Discovery Packet to 128
[Node 128] Receive discovery from 722
[Node 128] Send Welcome Packet to 722 (left: 128, right: 128)
[Node 128] New Neighbor (left: [Node 722] - right: [Node 722] )
[Node 722] Joining to form a ring of size 2 (left: [Node 128] - right: [Node 128] )
[Controller] Waking up node 705
[Node 705] Send Discovery Packet to 722
[Node 722] Receive discovery from 705
[Node 722] Welcoming [Node 705] as my new left node
[Node 722] Send Welcome Packet to 705 (left: 128, right: 722)
[Node 722] Notifying node [Node 128] of their new right node
[Node 705] Joining to form a ring of size 3 (left: [Node 128] - right: [Node 722] )
[Node 128] Switching right neighbor from 722 to 705
[Controller] Waking up node 176
[Node 176] Send Discovery Packet to 128
[Node 128] Receive discovery from 176
[Node 128] Welcoming 176 as my new right node
[Node 128] Send Welcome Packet to 176 (left: 128, right: 705)
[Node 128] Notifying node [Node 705] of their new left node
[Node 705] Switching left neighbor from 128 to 176
[Node 176] Joining to form a ring of size 4 (left: [Node 128] - right: [Node 705] )
[Controller] Waking up node 767
[Node 767] Send Discovery Packet to 705
[Node 705] Receive discovery from 767
[Node 705] Following discovery of 4 to 1
[Node 722] Receive discovery from 767
[Node 722] Welcoming 767 as my new right node
[Node 722] Send Welcome Packet to 767 (left: 722, right: 128)
[Node 722] Notifying node [Node 128] of their new left node
[Node 128] Switching left neighbor from 722 to 767
[Node 767] Joining to form a ring of size 5 (left: [Node 722] - right: [Node 128] )

[...]

[Controller] Final ring: 128 => 176 => 223 => 233 => 392 => 705 => 722 => 731 => 767 => 793
```

### Deconnexion d'un noeud
```
[Controller] Disconnect node 176
[Node 176] Leaving the ring (notifying neighbors)
[Node 223] Switching left neighbor from 176 to 128
[Node 128] Switching right neighbor from 176 to 223
[Controller] Final ring: 233 => 392 => 705 => 722 => 731 => 767 => 793 => 128 => 223
```

### Envoi de messages
```
[Controller] Sending `Hey bro` from 767 to 722
[Node 767] Routing MESSAGE to left: 731
[Node 731] Routing MESSAGE to left: 722
[Node 722] Received a message from 767: Hey bro
[Controller] Sending `It's me` from 233 to 722
[Node 233] Routing MESSAGE to right: 392
[Node 392] Routing MESSAGE to right: 705
[Node 705] Routing MESSAGE to right: 722
[Node 722] Received a message from 233: It's me
[Controller] Sending `Coucou` from 128 to 128
[Node 128] Received a message from 128: Coucou
```

### Sauvegarde / Recuperation de données
```
[Controller] Inserting key/data: 540/Les fleurs sont jolies
[Node 731] PUT 540 to left 
[Node 722] PUT 540 to left 
[Node 705] PUT 540 to left 
[Node 392] Stored `Les fleurs sont jolies` (hash: 540)
[Node 392] Replicate to [Node 233] & [Node 705] 
[Node 233] Replicated storage for (hash: 540)
[Node 705] Replicated storage for (hash: 540)
[Controller] Get 540 from the DHT
[Node 793] GET 540 to right 
[Node 767] GET 540 to right 
[Node 731] GET 540 to right 
[Node 722] GET 540 to right 
[Node 793] GET_RESPONSE for (hash: 540)
[Controller] Data associate to 540 is 'Les fleurs sont jolies'
```

## 🏗️ **Developed with**

* [Java](https://www.java.com/fr/)
* [Peersim](http://peersim.sourceforge.net/)
* [IntelliJ IDEA](https://www.jetbrains.com/idea/)

## 💪 **Authors of this project**

* **PERROLLAZ Maverick** _alias_ [@M4verickFr](https://github.com/M4verickFr)
* **CAULLIREAU Dorian** _alias_ [@caullird](https://github.com/caullird)
