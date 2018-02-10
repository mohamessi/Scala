package upmc.akka.leader

import akka.actor._

abstract class NodeStatus
case class Passive () extends NodeStatus
case class Candidate () extends NodeStatus
case class Dummy () extends NodeStatus
case class Waiting () extends NodeStatus
case class Leader () extends NodeStatus

abstract class LeaderAlgoMessage
case class Initiate () extends LeaderAlgoMessage
case class ALG (list:List[Int], nodeId:Int) extends LeaderAlgoMessage
case class AVS (list:List[Int], nodeId:Int) extends LeaderAlgoMessage
case class AVSRSP (list:List[Int], nodeId:Int) extends LeaderAlgoMessage

case class StartWithNodeList (list:List[Int])

class ElectionActor (val id:Int, val terminaux:List[Terminal]) extends Actor {

     val father = context.parent
     var nodesAlive:List[Int] = List(id)

     var candSucc:Int = -1
     var candPred:Int = -1
     var status:NodeStatus = new Passive ()

     def receive = {

          // Initialisation
          case Start => {
               self ! Initiate
          }

          case StartWithNodeList (list) => {
               if (list.isEmpty) {
                    this.nodesAlive = this.nodesAlive:::List(id)
               }
               else {
                    this.nodesAlive = list
               }

               // Debut de l'algorithme d'election
               self ! Initiate
          }

          case Initiate => 

          case ALG (list, init) =>
               if (status == Passive()){
                    status = Dummy()
                    list.foreach(n => {
                         if( n == (id + 1) % terminaux.length){
                              var remote = context.actorSelection("akka.tcp://LeaderSystem" + terminaux(n).id + "@" + terminaux(n).ip + ":" + terminaux(n).port + "/user/Node/electionActor")
                              remote ! ALG(list, init)
                         }

                    })

               }

               if( status == Candidate()){
                    candPred = init
                    if(id > init) {
                         if (candSucc == -1) {
                              status = Waiting()
                              var remote = context.actorSelection("akka.tcp://LeaderSystem" + terminaux(init).id + "@" + terminaux(init).ip + ":" + terminaux(init).port + "/user/Node/electionActor")
                              remote ! AVS(list, init)
                         }

                         else {
                              var remote = context.actorSelection("akka.tcp://LeaderSystem" + terminaux(candSucc).id + "@" + terminaux(candSucc).ip + ":" + terminaux(candSucc).port + "/user/Node/electionActor")
                              remote ! AVSRSP(list, candPred)
                              status = Dummy()
                         }
                    }
                    if(init == id){
                         status = Leader()
                    }
               }

          case AVS (list, j) => {
               if(status == Passive()){
                    if(candPred == -1 )
                         candSucc = j
                    else{
                         var remote = context.actorSelection("akka.tcp://LeaderSystem" + terminaux(candPred).id + "@" + terminaux(candPred).ip + ":" + terminaux(candPred).port + "/user/Node/electionActor")
                         remote ! AVSRSP(list, j)
                         status = Dummy()
                    }
                    }
               if(status == Waiting()){
                    candSucc = j
               }
          }
          // test
          case AVSRSP (list, k) => {
               if(status == Waiting()){
                    if( id == k)
                         status = Leader()
                    else{
                         candPred = k
                         if(candSucc == -1){
                              if(k < id){
                                   status = Waiting()
                                   var remote = context.actorSelection("akka.tcp://LeaderSystem" + terminaux(k).id + "@" + terminaux(k).ip + ":" + terminaux(k).port + "/user/Node/electionActor")
                                   remote ! AVS(list, id)

                              }
                         }
                         else{
                              status = Dummy()
                              var remote = context.actorSelection("akka.tcp://LeaderSystem" + terminaux(candSucc).id + "@" + terminaux(candSucc).ip + ":" + terminaux(candSucc).port + "/user/Node/electionActor")
                              remote ! AVS(list, k)
                         }
                    }
               }
          }


     }

}
