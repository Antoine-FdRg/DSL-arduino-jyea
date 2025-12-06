package io.github.mosser.arduinoml.kernel.samples;

import io.github.mosser.arduinoml.kernel.App;
import io.github.mosser.arduinoml.kernel.behavioral.*;
import io.github.mosser.arduinoml.kernel.generator.ToWiring;
import io.github.mosser.arduinoml.kernel.generator.Visitor;
import io.github.mosser.arduinoml.kernel.structural.*;

import java.util.Arrays;

public class Switch {

	public static void main(String[] args) {

		// Declaring elementary bricks
		Sensor button = new Sensor();
		button.setName("button");
		button.setPin(9);

		Actuator led = new Actuator();
		led.setName("LED");
		led.setPin(12);

		// Declaring states
		State on = new State();
		on.setName("on");

		State off = new State();
		off.setName("off");

		// Creating actions
		SetAction switchTheLightOn = new SetAction();
		switchTheLightOn.setActuator(led);
		switchTheLightOn.setValue(SIGNAL.HIGH);

		SetAction switchTheLightOff = new SetAction();
		switchTheLightOff.setActuator(led);
		switchTheLightOff.setValue(SIGNAL.LOW);

		// Binding actions to states
		on.setActions(Arrays.asList(switchTheLightOn));
		off.setActions(Arrays.asList(switchTheLightOff));

		// Creating transitions
		SignalTransition on2offcondition = new SignalTransition();
		on2offcondition.setSensor(button);
		on2offcondition.setValue(SIGNAL.HIGH);

		SignalTransition off2oncondition = new SignalTransition();
		off2oncondition.setSensor(button);
		off2oncondition.setValue(SIGNAL.HIGH);

		Transition onTransition = new Transition();
		onTransition.setCondition(on2offcondition);
		onTransition.setNext(off);

		Transition offTransition = new Transition();
		offTransition.setCondition(off2oncondition);
		offTransition.setNext(on);
		// Binding transitions to states
		on.setTransition(onTransition);
		off.setTransition(offTransition);

		// Building the App
		App theSwitch = new App();
		theSwitch.setName("Switch!");
		theSwitch.setBricks(Arrays.asList(button, led ));
		theSwitch.setStates(Arrays.asList(on, off));
		theSwitch.setInitial(off);

		// Generating Code
		Visitor codeGenerator = new ToWiring();
		theSwitch.accept(codeGenerator);

		// Printing the generated code on the console
		System.out.println(codeGenerator.getResult());
	}

}
