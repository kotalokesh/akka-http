
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
// step 1
import spray.json._

import scala.concurrent.duration._

case class Student(name: String, roll: Int, branch: String, gpa: Double)

object schoolmap {
  case object GetAllStudents

  case class GetStudent(name: String)

  case class GetStudentByroll(roll: Int)

  case class AddStudent(student: Student)

  case class RemoveStudent(student: Student)

  case object OperationSuccess

}

class School extends Actor with ActorLogging {

  import schoolmap._

  var students = Map[String, Student]()

  override def receive: Receive = {

    case GetAllStudents =>
      log.info("Getting all the students")
      sender() ! students.values.toList

    case GetStudent(name) =>
      log.info("getting student by th name")
      sender() ! students.get(name)

    case GetStudentByroll(roll) =>
      log.info("getting student by roll")
      sender() ! students.values.toList.filter(_.roll == roll)

    case AddStudent(student) =>
      log.info(s"Trying to add ${student}")
      students = students + (student.name -> student)
      sender() ! OperationSuccess

    case RemoveStudent(student) =>
      log.info(s"Trying to remove ${student}")
      students = students.-(student.name)
      sender() ! OperationSuccess

  }

}

// step 2
trait StudentJsonProtocol extends DefaultJsonProtocol {
  implicit val studentFormat = jsonFormat4(Student)
}

object studentdb extends App
  with StudentJsonProtocol
  with SprayJsonSupport {

  import schoolmap._

  implicit val system = ActorSystem("MarshallingJson")
  implicit val materalizer = ActorMaterializer()

  import system.dispatcher

  val nit = system.actorOf(Props(new School), "nit")
  val students = List(
    Student("lokesh", 20, "cse", 7.82),
    Student("simmi", 36, "cse", 7.82),
    Student("santosh", 56, "ece", 7.08),
    Student("karthik", 72, "eee", 7.48),
  )
  students.foreach(student => nit ! AddStudent(student))
  //   - GET /api/student , returns all the students int the map, as JSON
  //  - GET /api/student/(name) , return the student with the particular name
  //  - GET /api/student?name , does the same as above
  //  - GET /api/student/roll/(rollno) , with particular roll number
  //  -POST /api/student with JSON payload adds the student to the student map
  //  -DELETE /api/student with JSON payload deletes the student from the student map
  implicit val timeout = Timeout(2.seconds)
  val nitroute =
    pathPrefix("api" / "student") {
      get {
        path("roll" / IntNumber) {
          roll => {
            val studentByRollFuture = (nit ? GetStudentByroll(roll)).mapTo[List[Student]]
            complete(studentByRollFuture)
          }
        } ~
          (path(Segment) | parameter("name")) {
            name => {
              val studentByNameFuture = (nit ? GetStudent(name)).mapTo[Option[Student]]
              complete(studentByNameFuture)
            }
          } ~
          pathEndOrSingleSlash {
            val getAllStudentsFuture = (nit ? GetAllStudents).mapTo[List[Student]]
            complete(getAllStudentsFuture)
          }
      } ~
        post {
          entity(as[Student]) {
            student =>
              complete((nit ? AddStudent(student)).map(_ => StatusCodes.OK))
          }
        } ~
        delete {
          entity(as[Student]) {
            student =>
              complete((nit ? RemoveStudent(student)).map(_ => StatusCodes.OK))
          }
        }
    }

  println("server stared")
  val bindingFuture = Http().newServerAt("localhost", 8080).bind(nitroute)

}
