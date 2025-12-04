package io.github.mosser.arduinoml.embedded.java.dsl;

import io.github.mosser.arduinoml.kernel.behavioral.BooleanExpression;
import io.github.mosser.arduinoml.kernel.structural.Sensor;

public interface IBooleanExpression {
    IBooleanExpression saveExpression(BooleanExpression be);
    Sensor findSensor(String sensorName);
}
