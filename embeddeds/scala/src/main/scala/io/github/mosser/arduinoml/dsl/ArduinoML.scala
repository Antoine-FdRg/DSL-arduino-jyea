package io.github.mosser.arduinoml.dsl

import io.github.mosser.arduinoml.kernel.App
import io.github.mosser.arduinoml.kernel.generator.ToWiring
import io.github.mosser.arduinoml.kernel.structural._
import io.github.mosser.arduinoml.kernel.behavioral._
import collection.JavaConversions._

trait ArduinoML {


  protected def hasForName(n: String): Unit = app.setName(n)

  protected def declare: StructureBuilder = {
    flush()
    currentBrick = Some(StructureBuilder())
    currentBrick.get
  }

  protected def state: StateBuilder = {
    flush()
    currentState = Some(StateBuilder())
    currentState.get
  }

  protected def transitions(transitionSystem: => Unit): Unit = {
    flush()
    transitionSystem
  }

  protected def exportToWiring: Unit = {
    flush()
    val codeGen = new ToWiring()
    app.accept(codeGen)
    println(codeGen.getResult)
  }

  private[this] val app: App = {
    val tmp = new App()
    tmp.setBricks(List[Brick]())
    tmp.setStates(List[State]())
    tmp
  }

  private def flush(): Unit = {
    if (currentBrick.isDefined) {
      app.setBricks(app.getBricks :+ currentBrick.get.toBrick)
      currentBrick = None
    }
    if (currentState.isDefined) {
      app.setStates(app.getStates :+ currentState.get.toState)
      currentState = None
    }
  }

  private var currentBrick: Option[StructureBuilder] = None
  private var currentState: Option[StateBuilder]   = None

  private def getBrickByName(n: String): Brick =
    (app.getBricks find (_.getName == n)).get

  private def getStateByName(n: String): State =
    (app.getStates find (_.getName == n)).get


  protected object Bricks extends Enumeration {
    val UNKNOWN, SENSOR, ACTUATOR = Value
  }

  protected case class StructureBuilder(
     kind: Bricks.Value = Bricks.UNKNOWN,
     pin: Int = -1,
     name: String = "" ) {

    def aSensor(): StructureBuilder = {
      currentBrick = Some(this.copy(kind = Bricks.SENSOR))
      currentBrick.get
    }

    def anActuator(): StructureBuilder = {
      currentBrick = Some(this.copy(kind = Bricks.ACTUATOR))
      currentBrick.get
    }

    def boundToPin(p: Int): StructureBuilder = {
      currentBrick = Some(this.copy(pin = p))
      currentBrick.get
    }

    def named(n: String): StructureBuilder = {
      currentBrick = Some(this.copy(name = n))
      currentBrick.get
    }

    def toBrick: Brick = {
      val brick: Brick = currentBrick.get.kind match {
        case Bricks.SENSOR   => new Sensor()
        case Bricks.ACTUATOR => new Actuator()
        case _ => throw new UnsupportedOperationException("Undefined brick type")
      }
      brick.setName(currentBrick.get.name)
      brick.setPin(currentBrick.get.pin)
      brick
    }

    // actionneur (ex : led --> high)
    def -->(signal: Signal): ActionBuilder =
      ActionBuilder(this, signal)

    // capteur (ex : button is high)
    def is(signal: Signal): ConditionBuilder =
      ConditionBuilder(Seq((this, signal)), LOGIC.AND)
  }

  protected case class StateBuilder(
     name: String = "",
     actions: Seq[io.github.mosser.arduinoml.kernel.behavioral.Action] = Seq()) {

    def named(n: String): StateBuilder = {
      currentState = Some(this.copy(name = n))
      currentState.get
    }

    def executing(actions: ActionBuilder*): StateBuilder = {
      val acts = actions.map { b =>
        val a = new SetAction()
        a.setActuator(getBrickByName(b.actuator.name).asInstanceOf[Actuator])
        a.setValue(b.signal.asSignal)
        a
      }
      currentState = Some(this.copy(actions = acts))
      currentState.get
    }

    def isInitial: Unit = {
      flush()
      app.setInitial(getStateByName(this.name))
    }

    def toState: State = {
      val s = new State()
      s.setName(this.name)
      s.setActions(this.actions)
      s
    }
    def ->(next: StateBuilder): TransitionBuilder =
      TransitionBuilder(this, next)
  }

  // ---------- BUILDERS ACTIONS / CONDITIONS / TRANSITIONS ----------
  protected case class ActionBuilder(actuator: StructureBuilder, signal: Signal)

  protected case class ConditionBuilder(
   parts: Seq[(StructureBuilder, Signal)],
   connector: LOGIC) {

    def and(other: ConditionBuilder): ConditionBuilder =
      ConditionBuilder(this.parts ++ other.parts, LOGIC.AND)

    def or(other: ConditionBuilder): ConditionBuilder =
      ConditionBuilder(this.parts ++ other.parts, LOGIC.OR)
  }

  protected case class TransitionBuilder(from: StateBuilder, to: StateBuilder) {

    def when(cond: ConditionBuilder): Unit = {
      val condList = new ConditionList()

      condList.setConnector(cond.connector)

      cond.parts.foreach { case (brickBuilder, sig) =>
        val sensor = getBrickByName(brickBuilder.name).asInstanceOf[Sensor]
        val expr = new SignalTransition()
        expr.setSensor(sensor)
        expr.setValue(sig.asSignal)
        condList.getExpressions.add(expr)
      }

      val trans = new Transition()
      trans.setCondition(condList)
      trans.setNext(getStateByName(to.name))

      getStateByName(from.name).setTransition(trans)
    }
  }

  trait Signal { val asSignal: SIGNAL }
  object high extends Signal { val asSignal = SIGNAL.HIGH }
  object low  extends Signal { val asSignal = SIGNAL.LOW  }
}
