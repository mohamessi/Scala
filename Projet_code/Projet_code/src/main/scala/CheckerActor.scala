package upmc.akka.leader

import java.util
import java.util.{Date, GregorianCalendar}

import akka.actor._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class Tick

case class CheckerTick() extends Tick

class CheckerActor(val id: Int, val terminaux: List[Terminal], electionActor: ActorRef) extends Actor {

  var time: Int = 200
  val father = context.parent

  var nodesAlive: List[Int] = List()
  var datesForChecking: List[Date] = List()
  var lastDate: Date = null

  var leader: Int = -1

  def receive = {

    // Initialisation
    case Start => {
      self ! CheckerTick
    }

    // A chaque fois qu'on recoit un Beat : on met a jour la liste des nodes
    case IsAlive(nodeId : Int) => {
      System.out.println(datesForChecking);
      if(lastDate.compareTo(datesForChecking(nodeId)) > time) {
        datesForChecking.drop(nodeId);
      }
      else{ // s'il n'est pas mort
        lastDate.setTime(new Date().getTime);
        datesForChecking(nodeId).setTime(new Date().getTime);
      }
      System.out.println(datesForChecking);
    }

    case IsAliveLeader(nodeId) => {
      lastDate.setTime(new Date().getTime);
          }

    // A chaque fois qu'on recoit un CheckerTick : on verifie qui est mort ou pas
    // Objectif : lancer l'election si le leader est mort
    case CheckerTick =>
      new Thread(() => {
        while (true) {
          datesForChecking = datesForChecking.filter(d => lastDate.compareTo(d) < time);
        }
      }).start();
  }


}
