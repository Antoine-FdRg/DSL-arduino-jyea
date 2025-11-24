package io.github.mosser.arduinoml.embedded.java.dsl.demo;

import io.github.mosser.arduinoml.kernel.App;
import io.github.mosser.arduinoml.kernel.generator.ToWiring;
import io.github.mosser.arduinoml.kernel.generator.Visitor;

import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.*;

public class VerySimpleAlarm {

    public static void main(String[] args) {
        App myApp =
                application("very_simple_alarm")
                        .uses(sensor("button", 9))
                        .uses(actuator("led", 10))
                        .uses(actuator("buzzer", 11))
                        .hasForState("off").initial()
                            .setting("buzzer").toLow()
                            .setting("led").toLow()
                        .endState()
                        .hasForState("on")
                            .setting("buzzer").toHigh()
                            .setting("led").toHigh()
                        .endState()
                            .beginTransitionTable()
                            .from("off")
                                .when("button").isHigh()
                            .goTo("on")
                            .from("on")
                                .when("button").isLow()
                            .goTo("off")
                            .endTransitionTable()
                        .build();

        Visitor<StringBuffer> codeGenerator = new ToWiring();
        myApp.accept(codeGenerator);
        System.out.println(codeGenerator.getResult());
    }

}
