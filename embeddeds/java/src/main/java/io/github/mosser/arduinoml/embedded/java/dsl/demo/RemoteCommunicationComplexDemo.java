package io.github.mosser.arduinoml.embedded.java.dsl.demo;

import io.github.mosser.arduinoml.kernel.App;
import io.github.mosser.arduinoml.kernel.generator.ToWiring;
import io.github.mosser.arduinoml.kernel.generator.Visitor;

import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.*;

public class RemoteCommunicationComplexDemo {

    public static void main(String[] args) {
        App myApp =
                application("RemoteComplexApp")
                        .uses(actuator("led", 12))
                        .uses(sensor("button", 11))
                        .uses(serialSensor("keyboard"))
                        .hasForState("off").initial()
                            .setting("led").toLow()
                            .sending("\"Press ON to activate led ...\"")
                        .endState()
                        .hasForState("on")
                            .setting("led").toHigh()
                        .endState()
                            .beginTransitionTable()
                            .from("off")
                                .whenSerial("keyboard").receives("\"ON\"")
                                .and()
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
