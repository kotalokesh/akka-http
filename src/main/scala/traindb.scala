//import MarshallingJson._

import actor.actorSystem
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.util.Timeout
import spray.json._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.concurrent.duration.DurationInt


case class Train(number: Int, name: String, start: String, end: String)

object Trainenquiry {
  case class getAllTrains()

  case class addTrain(train: Train)

  case class removeTrain(number: Int)

  case class getTraindetails(number: Int)

  case class getTrainsBtwStops(start: String, end: String)

  case object Operationsuccess
}

class RwStation extends Actor with ActorLogging {

  import Trainenquiry._

  var trainMap = Map[Int, Train]()

  override def receive: Receive = {

    case getAllTrains() =>
      sender() ! trainMap.values.toList

    case removeTrain(number) =>
      trainMap = trainMap.-(number)
      sender() ! Operationsuccess

    case addTrain(train) =>
      trainMap = trainMap + (train.number -> train)
      sender() ! Operationsuccess

    case getTraindetails(number) =>
      sender() ! trainMap.get(number)

    case getTrainsBtwStops(start, end) =>
      sender() ! trainMap.values.toList.filter(t => (t.start == start & t.end == end))

  }

}

case class TrainNumber(number: Int)

trait TrainJsonProtocol extends DefaultJsonProtocol {
  implicit val TrainFormat = jsonFormat4(Train)
  implicit val TrainNumberFormat = jsonFormat1(TrainNumber)
}

object Traindb extends App with TrainJsonProtocol with SprayJsonSupport {
  implicit val system = ActorSystem("Marshalling")
  implicit val materalizer = ActorMaterializer()

  import system.dispatcher
  import Trainenquiry._

  implicit val timeout = Timeout(2.seconds)

  val bobbili = system.actorOf(Props(new RwStation), "bobbili")
  val trains = List(
    Train(1, "samatha", "vizag", "delhi"),
    Train(2, "Durg", "vizag", "rayagada"),
    Train(3, "bukaro", "alpi", "dhanbadh"),
    Train(4, "rayagada_pass", "vizag", "rayagada"),
  )
//  println("before add")
  trains.foreach(train => bobbili ! addTrain(train))
//  println("after add")
  val railroute =
    get {
      path("api" / "trains") {
        val getallTrainsFuture = (bobbili ? getAllTrains()).mapTo[List[Train]]
        complete(getallTrainsFuture)
      } ~
        path("api" / "trains" / IntNumber) {
          number => {
            val getTrainDetailsFuture = (bobbili ? getTraindetails(number)).mapTo[Option[Train]]
            complete(getTrainDetailsFuture)
          }
        } ~
        path("api" / "trains" / Segment / Segment) { (start, end) => {
          val getTrainsBtwStationsFuture = (bobbili ? getTrainsBtwStops(start, end)).mapTo[List[Train]]
          complete(getTrainsBtwStationsFuture)
        }
        }
    } ~
      delete {
        path("api" / "trains" / "del") {
          entity(as[TrainNumber]) {
            TrainNumber => {
              //              println(json)
              //              println(json.asJsObject.fields)
              //              println(json.asJsObject.fields.get("number").getOrElse("not-found").isInstanceOf[Int])
              //              println(json.asJsObject.fields.get("number").getOrElse("not-found").isInstanceOf[String])
              //              println(json.asJsObject.fields.get("number").getOrElse("not-found").getClass)
              //              println(json.asJsObject.fields.get("name").getOrElse("no-found").isInstanceOf[String])

              val removetrainFuture = (bobbili ? removeTrain(TrainNumber.number)).map(_ => StatusCodes.OK)
              complete(StatusCodes.OK)
            }
          }
        }
      }

  println("server stared")
  val bindingFuture = Http().newServerAt("localhost", 8080).bind(railroute)


}
