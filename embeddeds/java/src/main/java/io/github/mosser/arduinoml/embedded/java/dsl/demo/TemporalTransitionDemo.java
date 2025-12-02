package io.github.mosser.arduinoml.embedded.java.dsl.demo;

import io.github.mosser.arduinoml.kernel.App;
import io.github.mosser.arduinoml.kernel.generator.ToWiring;
import io.github.mosser.arduinoml.kernel.generator.Visitor;

import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.actuator;
import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.application;
import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.sensor;

/**
 * Demonstration of temporal transitions.
 * Alan wants to define a state machine where LED1 is switched on
 * after a push on button B1 and switched off 800ms after,
 * waiting again for a new push on B1.
 */
public class TemporalTransitionDemo {
    public static void main(String[] args) {

        App myApp =
            application("temporal_transition_demo")
                .uses(sensor("b1", 9))
                .uses(actuator("led1", 12))
                .hasForState("off").initial()
                    .setting("led1").toLow()
                .endState()
                .hasForState("on")
                    .setting("led1").toHigh()
                .endState()
                    .beginTransitionTable()
                    .from("off")
                        .when("b1").isHigh()
                    .goTo("on")
                    .from("on")
                        .waitFor(800)
                    .goTo("off")
                    .endTransitionTable()
                .build();


        Visitor codeGenerator = new ToWiring();
        myApp.accept(codeGenerator);
        System.out.println(codeGenerator.getResult());
    }
}
