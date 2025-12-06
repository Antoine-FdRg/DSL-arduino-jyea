package io.github.mosser.arduinoml.samples

import io.github.mosser.arduinoml.dsl.ArduinoML

object VerySimpleAlarm extends App with ArduinoML {

  this hasForName "very_simple_alarm"

  val button = declare aSensor()    named "button" boundToPin 9
  val led    = declare anActuator() named "led"    boundToPin 10
  val buzzer = declare anActuator() named "buzzer" boundToPin 11

  val off = state named "off" executing (
    buzzer --> low,
    led    --> low
  )

  val on = state named "on" executing (
    buzzer --> high,
    led    --> high
  )

  off.isInitial

  transitions {
    off -> on when (button is high)
    on  -> off when (button is low)
  }

  exportToWiring
}
