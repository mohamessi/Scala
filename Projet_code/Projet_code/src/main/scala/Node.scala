package upmc.akka.leader

import akka.actor.{Actor, ActorSelection, Props}

case class Start()

sealed trait SyncMessage

case class Sync(nodes: List[Int]) extends SyncMessage

case class SyncForOneNode(nodeId: Int, nodes: List[Int]) extends SyncMessage

sealed trait AliveMessage

case class IsAlive(id: Int) extends AliveMessage

case class IsAliveLeader(id: Int) extends AliveMessage

class Node(val id: Int, val terminaux: List[Terminal]) extends Actor {

  // Les differents acteurs du systeme
  val electionActor = context.actorOf(Props(new ElectionActor(this.id, terminaux)), name = "electionActor")
  val checkerActor = context.actorOf(Props(new CheckerActor(this.id, terminaux, electionActor)), name = "checkerActor")
  val beatActor = context.actorOf(Props(new BeatActor(this.id)), name = "beatActor")
  val displayActor = context.actorOf(Props[DisplayActor], name = "displayActor")

  var allNodes: List[ActorSelection] = List()

  def receive = {

    // Initialisation
    case Start => {

      displayActor ! Message("Node " + this.id + " is created")
      checkerActor ! Start
      beatActor ! Start

      // Initilisation des autres remote, pour communiquer avec eux
      terminaux.foreach(n => {
        if (n.id != id) {
          val remote = context.actorSelection("akka.tcp://LeaderSystem" + n.id + "@" + n.ip + ":" + n.port + "/user/Node")
          // Mise a jour de la liste des nodes
          this.allNodes = this.allNodes ::: List(remote)
        }
      })
    }

    // Envoi de messages (format texte)
    case Message(content) => {
      displayActor ! Message(content)
    }

    // Messages venant de beatActor (BeatTick) : pour nous dire qui est encore en vie et le notifer aux autres noeuds
    case BeatLeader(nodeId) => {
      System.out.println("Hello i am in NODE -> BeatLeader");
      this.allNodes.foreach(n => {
        n ! IsAliveLeader(nodeId)
      })
      checkerActor ! IsAliveLeader (nodeId)
    }

    // Messages venant de beatActor (BeatTick) a chaque tick: pour nous dire qui est encore en vie et le notifer aux autres noueds
    case Beat(nodeId) => {
      System.out.println("Hello i am in NODE -> Beat");
      this.allNodes.foreach(n => {
        n ! IsAlive(nodeId)
      })
    }

    // Messages venant des autres nodes : pour nous dire qui est encore en vie ou mort
    case IsAlive(id) => {
      displayActor ! Message("Node " + id + " is in live")
      checkerActor ! IsAlive(id)
    }

    case IsAliveLeader(id) => {
      displayActor ! Message("Node " + id + " is the leader and is in live")
      checkerActor ! IsAliveLeader(id)
    }

    // Message indiquant que le leader a change
    case LeaderChanged(nodeId) => {
      displayActor ! Message("The Leader Node " + id + " has changed -> The king is dead, long live the king")
      beatActor ! LeaderChanged(nodeId)
      checkerActor ! LeaderChanged (nodeId)
    }

  }

}
