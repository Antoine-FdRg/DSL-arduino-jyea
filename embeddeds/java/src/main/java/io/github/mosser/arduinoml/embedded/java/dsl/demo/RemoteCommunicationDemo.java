package io.github.mosser.arduinoml.embedded.java.dsl.demo;

import io.github.mosser.arduinoml.kernel.App;
import io.github.mosser.arduinoml.kernel.generator.ToWiring;
import io.github.mosser.arduinoml.kernel.generator.Visitor;

import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.*;

public class RemoteCommunicationDemo {
    public static void main(String[] args) {

        App myApp =
            application("RemoteCommunicationApp")
                .useSerialMonitor()
                .uses(actuator("led", 12))
                .hasForState("off").initial()
                    .setting("led").toLow()
                    .sending("\"LED is OFF\"")
                .endState()
                .hasForState("on")
                    .setting("led").toHigh()
                    .sending("\"LED is ON\"")
                .endState()
                    .beginTransitionTable()
                    .from("off")
                        .receives("\"ON\"")
                    .goTo("on")
                    .from("on")
                        .receivesAny()
                    .goTo("off")
                    .endTransitionTable()
                .build();

        Visitor codeGenerator = new ToWiring();
        myApp.accept(codeGenerator);
        System.out.println(codeGenerator.getResult());
    }
}