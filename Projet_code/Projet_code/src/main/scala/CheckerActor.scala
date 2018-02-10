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
    case IsAlive(nodeId : Int) =>
      var b = false
      var i = nodesAlive.indexWhere(n => n == nodeId);
      if( i == None){
         nodesAlive = nodeId::nodesAlive
        lastDate = new Date()
        lastDate::datesForChecking

      }
      else {
        lastDate = new Date();
        datesForChecking.patch(i, Seq(lastDate),1);
      }

    case IsAliveLeader(nodeId) => {
      var i = nodesAlive.indexWhere(n => n == nodeId);
      if( i != None && i == leader ){
        lastDate = new Date();
        datesForChecking.patch(i, Seq(lastDate),1);
      }



    }
    // test
    // A chaque fois qu'on recoit un CheckerTick : on verifie qui est mort ou pas
    // Objectif : lancer l'election si le leader est mort
    case CheckerTick =>
      var nodeAux:List[Int] = List()
      for (elem <- nodesAlive) {
        if(datesForChecking(elem).compareTo(lastDate)<time){
          if(elem == leader)
            electionActor ! Start
        }
        else {

          elem::nodeAux
        }
      }
      nodeAux = nodeAux;


  }


}
