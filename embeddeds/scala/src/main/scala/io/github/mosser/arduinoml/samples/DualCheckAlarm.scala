package io.github.mosser.arduinoml.samples

import io.github.mosser.arduinoml.dsl.ArduinoML

object DualCheckAlarm extends App with ArduinoML {

  this hasForName "red_button"

  val b1     = declare aSensor()    named "b1"     boundToPin 9
  val b2     = declare aSensor()    named "b2"     boundToPin 10
  val buzzer = declare anActuator() named "buzzer" boundToPin 12

  val on = state named "on" executing (
    buzzer --> high
    )

  val off = state named "off" executing (
    buzzer --> low
    )

  off.isInitial

  transitions {

    off -> on when ((b1 is high) and (b2 is high))

    on  -> off when ((b1 is low) or (b2 is low))
  }

  exportToWiring
}
