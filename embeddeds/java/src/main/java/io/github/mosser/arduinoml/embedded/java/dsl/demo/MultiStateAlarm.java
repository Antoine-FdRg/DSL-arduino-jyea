package io.github.mosser.arduinoml.embedded.java.dsl.demo;

import io.github.mosser.arduinoml.kernel.App;
import io.github.mosser.arduinoml.kernel.generator.ToWiring;
import io.github.mosser.arduinoml.kernel.generator.Visitor;

import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.actuator;
import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.application;
import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.sensor;

public class MultiStateAlarm {
    public static void main(String[] args) {

        App myApp = application("red_button")
                .uses(sensor("button", 8))
                .uses(actuator("red_led", 12))
                .uses(actuator("buzzer", 9))

                .hasForState("ready").initial()
                .setting("red_led").toLow()
                .setting("buzzer").toLow()
                .endState()

                .hasForState("buzzing")
                .setting("red_led").toLow()
                .setting("buzzer").toHigh()
                .endState()

                .hasForState("led_on")
                .setting("red_led").toHigh()
                .setting("buzzer").toLow()
                .endState()

                .beginTransitionTable()
                .from("ready")
                .when("button").isHigh()
                .goTo("buzzing")

                .from("buzzing")
                .when("button").isHigh()
                .goTo("led_on")

                .from("led_on")
                .when("button").isHigh()
                .goTo("ready")
                .endTransitionTable()

                .build();

        Visitor codeGenerator = new ToWiring();
        myApp.accept(codeGenerator);
        System.out.println(codeGenerator.getResult());
    }
}