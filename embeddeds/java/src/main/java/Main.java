import io.github.mosser.arduinoml.kernel.App;
import io.github.mosser.arduinoml.kernel.generator.ToWiring;
import io.github.mosser.arduinoml.kernel.generator.Visitor;

import static io.github.mosser.arduinoml.embedded.java.dsl.AppBuilder.*;

public class Main {


    public static void main (String[] args) {

        App myApp =
                application("red_button")
                        .uses(sensor("b1", 9))
                        .uses(sensor("b2", 10))
                        .uses(actuator("led", 12))
                        .hasForState("on")
                            .setting("led").toHigh()
                        .endState()
                        .hasForState("off").initial()
                            .setting("led").toLow()
                        .endState()
//                        .beginTransitionTable()
//                            .from("on").when("b1").isHigh().goTo("off")
//                            .from("off").when("b1").isHigh().goTo("on")
//                        .endTransitionTable()
                        .beginTransitionTable()
                            .from("off")
                                .when("b1" ).isHigh()
                                .and()
                                .when("b2" ).isHigh()
                                .or()
                                .when("b2" ).isLow()
                            .goTo("on")
                            .from("on")
                                .when("b1").isLow()
                                .or()
                                .when("b2").isLow()
                            .goTo("off")
                        .endTransitionTable()
                .build();


        Visitor codeGenerator = new ToWiring();
        myApp.accept(codeGenerator);
        System.out.println(codeGenerator.getResult());
    }

}
