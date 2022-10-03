import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object actor extends App{
  val actorSystem = ActorSystem("firstactor")
  println(actorSystem.name)
  class Person(name:String,rollno :Int,branch :String) extends Actor{
    override def receive: Receive = {
      case "hii" => {
        println(s" $self : message : hii")
        context.sender() ! "hello"
      }
      case "hello" => println(s" $self : message : hello")
      case "name"=> println(s"my name is $name")
      case "roll" => println(s"my roll no is $rollno")
      case "branch" => println(s"my branch is $branch")
      case sayhito(ref) => ref ! "hii"
    }
  }
  val lokesh = actorSystem.actorOf(Props(new Person("lokesh",20,"cse")),"lokesh")
  val simmi = actorSystem.actorOf(Props(new Person("simi",36,"cse")),"simmi")
  case class sayhito(ref : ActorRef)
  simmi ! sayhito(lokesh)
  lokesh ! "hii"

}
