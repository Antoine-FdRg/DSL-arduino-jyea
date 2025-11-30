package io.github.mosser.arduinoml.embedded.java.dsl.demo;

import io.github.mosser.arduinoml.kernel.App;
import io.github.mosser.arduinoml.kernel.generator.ToWiring;
import io.github.mosser.arduinoml.kernel.generator.Visitor;

import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.application;
import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.sensor;
import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.actuator;

public class CompositeDemoAlarm {
    public static void main(String[] args) {

        App app =
                application("demo_composite")
                        .uses(sensor("b1", 9))
                        .uses(sensor("b2", 10))
                        .uses(actuator("led", 12))

                        .hasForState("active")
                        .setting("led").toHigh()
                        .endState()

                        .hasForState("idle").initial()
                        .setting("led").toLow()
                        .endState()

                        .beginTransitionTable()

                        .from("idle")
                        .startAnd()
                            .when("b1").isHigh()
                            .startOr()
                                .when("b2").isHigh()
                                .when("b1").isLow()
                            .endOr()
                        .endAnd()
                        .goTo("active")

                        .from("active")
                        .when("b1").isLow()
                        .or()
                        .when("b2").isLow()
                        .goTo("idle")

                        .endTransitionTable()
                        .build();

        Visitor codeGenerator = new ToWiring();
        app.accept(codeGenerator);
        System.out.println(codeGenerator.getResult());
    }
}
