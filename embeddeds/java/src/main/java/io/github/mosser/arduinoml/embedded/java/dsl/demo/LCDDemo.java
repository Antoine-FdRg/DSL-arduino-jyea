package io.github.mosser.arduinoml.embedded.java.dsl.demo;

import io.github.mosser.arduinoml.kernel.App;
import io.github.mosser.arduinoml.kernel.generator.ToWiring;
import io.github.mosser.arduinoml.kernel.generator.Visitor;

import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.*;

public class LCDDemo {
    public static void main(String[] args) {

        App myApp = application("lcd_demo")

                .uses(sensor("button", 8))
                .uses(actuator("led", 9))
                .uses(lcd("screen"))

                .hasForState("idle").initial()
                .setting("led").toLow()
                .displayOn("screen")
                .text("button := ")
                .valueOf("button")
                .endDisplay()
                .endState()

                .hasForState("pressed")
                .setting("led").toHigh()
                .displayOn("screen")
                .text("led := ")
                .valueOf("led")
                .endDisplay()
                .endState()

                .beginTransitionTable()
                .from("idle").when("button").isHigh().goTo("pressed")
                .from("pressed").when("button").isLow().goTo("idle")
                .endTransitionTable()

                .build();

        Visitor codeGenerator = new ToWiring();
        myApp.accept(codeGenerator);
        System.out.println(codeGenerator.getResult());
    }
}
