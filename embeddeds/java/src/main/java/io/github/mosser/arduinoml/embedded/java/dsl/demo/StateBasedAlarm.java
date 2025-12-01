package io.github.mosser.arduinoml.embedded.java.dsl.demo;

import io.github.mosser.arduinoml.kernel.App;
import io.github.mosser.arduinoml.kernel.generator.ToWiring;
import io.github.mosser.arduinoml.kernel.generator.Visitor;

import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.actuator;
import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.application;
import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.sensor;

public class StateBasedAlarm {
    public static void main(String[] args) {

        App myApp = application("red_button")
                .uses(actuator("red_led", 12))
                .uses(sensor("button", 8))

                .hasForState("off").initial()
                .setting("red_led").toLow()
                .endState()

                .hasForState("on")
                .setting("red_led").toHigh()
                .endState()

                .beginTransitionTable()
                .from("off")
                .when("button").isHigh()
                .goTo("on")

                .from("on")
                .when("button").isHigh()
                .goTo("off")
                .endTransitionTable()

                .build();

        Visitor codeGenerator = new ToWiring();
        myApp.accept(codeGenerator);
        System.out.println(codeGenerator.getResult());
    }
}
