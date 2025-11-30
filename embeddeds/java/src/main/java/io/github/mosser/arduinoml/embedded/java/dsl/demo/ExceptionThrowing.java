package io.github.mosser.arduinoml.embedded.java.dsl.demo;

import io.github.mosser.arduinoml.kernel.App;
import io.github.mosser.arduinoml.kernel.generator.ToWiring;
import io.github.mosser.arduinoml.kernel.generator.Visitor;

import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.actuator;
import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.application;
import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.sensor;

public class ExceptionThrowing {
    public static void main(String[] args) {

        App myApp =
            application("red_button")
                .uses(sensor("b1", 9))
                .uses(sensor("b2", 10))
                .hasForState("off").initial()
                .endState()
                    .beginTransitionTable()
                        .from("off")
                            .when("b1" ).isHigh()
                            .and()
                            .when("b2" ).isHigh()
                        .error(2)
                    .endTransitionTable()
                .build();


        Visitor codeGenerator = new ToWiring();
        myApp.accept(codeGenerator);
        System.out.println(codeGenerator.getResult());
    }
}
