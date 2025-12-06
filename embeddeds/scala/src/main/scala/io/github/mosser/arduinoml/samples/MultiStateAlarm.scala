package io.github.mosser.arduinoml.samples

import io.github.mosser.arduinoml.dsl.ArduinoML

object MultiStateAlarm extends App with ArduinoML {

  this hasForName "red_button"

  val button = declare aSensor()    named "button"  boundToPin 8
  val redLed = declare anActuator() named "red_led" boundToPin 12
  val buzzer = declare anActuator() named "buzzer"  boundToPin 9

  val ready = state named "ready" executing (
    redLed --> low,
    buzzer --> low
  )

  val buzzing = state named "buzzing" executing (
    redLed --> low,
    buzzer --> high
  )

  val ledOn = state named "led_on" executing (
    redLed --> high,
    buzzer --> low
  )

  ready.isInitial

  transitions {
    ready   -> buzzing when (button is high)
    buzzing -> ledOn  when (button is high)
    ledOn   -> ready  when (button is high)
  }

  exportToWiring
}
