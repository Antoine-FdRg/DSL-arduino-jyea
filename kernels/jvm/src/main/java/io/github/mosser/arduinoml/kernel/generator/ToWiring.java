package io.github.mosser.arduinoml.kernel.generator;

import io.github.mosser.arduinoml.kernel.App;
import io.github.mosser.arduinoml.kernel.behavioral.*;
import io.github.mosser.arduinoml.kernel.structural.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Quick and dirty visitor to support the generation of Wiring code
 */
public class ToWiring extends Visitor<StringBuffer> {
	enum PASS {ONE, TWO}


	public ToWiring() {
		this.result = new StringBuffer();
	}

	private void w(String s) {
		result.append(String.format("%s",s));
	}

    private boolean hasSerialCommunication(App app) {
        boolean hasSerialSensor = app.getBricks().stream()
                .anyMatch(brick -> brick instanceof SerialSensor);

        boolean hasSendAction = app.getStates().stream()
                .anyMatch(state -> !state.getSendActions().isEmpty());

        return hasSerialSensor || hasSendAction;
    }

	@Override
	public void visit(App app) {
		//first pass, create global vars
		context.put("pass", PASS.ONE);
		w("// Wiring code generated from an ArduinoML model\n");
		w(String.format("// Application name: %s\n", app.getName())+"\n");

        if (hasSerialCommunication(app)) {
            w("// Serial communication: 9600 baud (Standard Arduino Uno)\n");
        }
        w("\n");

		w("long debounce = 200;\n");
		w("\nenum STATE {");
		String sep ="";
		for(State state: app.getStates()){
			w(sep);
			state.accept(this);
			sep=", ";
		}
		w("};\n");
		if (app.getInitial() != null) {
			w("STATE currentState = " + app.getInitial().getName()+";\n");
		}

		for(Brick brick: app.getBricks()){
			brick.accept(this);
		}

		//second pass, setup and loop
		context.put("pass",PASS.TWO);
		w("\nvoid setup(){\n");

        if (hasSerialCommunication(app)) {
            w("  Serial.begin(9600);\n");
            w("  while(!Serial) { ; } // Wait for serial port\n");
        }

		for(Brick brick: app.getBricks()){
			brick.accept(this);
		}
		w("}\n");

        w("\nvoid loop() {\n");

        if (hasSerialCommunication(app)) {
            w("  String serialInput = \"\";\n");
            w("  if (Serial.available() > 0) {\n");
            w("    serialInput = Serial.readStringUntil('\\n');\n");
            w("    serialInput.trim();\n");
            w("  }\n");
        }

        w("\tswitch(currentState) {\n");

		for(State state: app.getStates()){
			state.accept(this);
		}
		w("\t}\n" +
			"}");
	}

	@Override
	public void visit(Actuator actuator) {
		if(context.get("pass") == PASS.ONE) {
			return;
		}
		if(context.get("pass") == PASS.TWO) {
			w(String.format("  pinMode(%d, OUTPUT); // %s [Actuator]\n", actuator.getPin(), actuator.getName()));
			return;
		}
	}


	@Override
	public void visit(Sensor sensor) {
		if(context.get("pass") == PASS.ONE) {
			w(String.format("\nboolean %sBounceGuard = false;\n", sensor.getName()));
			w(String.format("long %sLastDebounceTime = 0;\n", sensor.getName()));
			return;
		}
		if(context.get("pass") == PASS.TWO) {
			w(String.format("  pinMode(%d, INPUT);  // %s [Sensor]\n", sensor.getPin(), sensor.getName()));
			return;
		}
	}

    @Override
    public void visit(SerialSensor sensor) {
        // SerialSensor has no pinMode to configure
    }

    @Override
    public void visit(SendAction action) {
        String message = action.getMessage();

        if (message.startsWith("\"") && message.endsWith("\"")) {
            message = message.substring(1, message.length() - 1);
        }

        w("\t\t\tSerial.println(\"" + message + "\");\n");
    }

    @Override
    public void visit(SerialTransition transition) {
        if (transition.isMatchAny()) {
            w("( serialInput.length() > 0)");
        } else {
            // Le pattern est stocké sans guillemets, il faut les ajouter pour la génération
            w("( serialInput == \"" + transition.getPattern() + "\" )");
        }
    }

    @Override
	public void visit(State state) {
		if(context.get("pass") == PASS.ONE){
			w(state.getName());
			return;
		}
		if(context.get("pass") == PASS.TWO) {
			w("\t\tcase " + state.getName() + ":\n");
			for (Action action : state.getActions()) {
				action.accept(this);
			}

            for (SendAction action : state.getSendActions()) {
                action.accept(this);
            }

            state.getTransitionList().accept(this);
        }

	}

	@Override
	public void visit(SignalTransition transition) {
		if(context.get("pass") == PASS.ONE) {
			return;
		}
		if(context.get("pass") == PASS.TWO) {
			String sensorName = transition.getSensor().getName();
			w(String.format("\t\t\t\t%sLastDebounceTime = millis();\n", sensorName));
		}
	}

    @Override
    public void visit(TransitionList transitionList) {
        List<SignalTransition> signalTransitions = transitionList.getTransitions().stream()
                .filter(t -> t instanceof SignalTransition)
                .map(t -> (SignalTransition) t)
                .collect(Collectors.toList());

        List<SerialTransition> serialTransitions = transitionList.getTransitions().stream()
                .filter(t -> t instanceof SerialTransition)
                .map(t -> (SerialTransition) t)
                .collect(Collectors.toList());

        List<String> sensorsName = signalTransitions.stream()
                .map(t -> t.getSensor().getName())
                .distinct()
                .collect(Collectors.toList());

        for (String name : sensorsName) {
            w(String.format("\t\t\t%sBounceGuard = millis() - %sLastDebounceTime > debounce;\n",
                    name, name));
        }

        List<String> parts = new java.util.ArrayList<>();

        for (SignalTransition t : signalTransitions) {
            parts.add(String.format("( digitalRead(%d) == %s && %sBounceGuard )",
                    t.getSensor().getPin(),
                    t.getValue(),
                    t.getSensor().getName()));
        }

        for (SerialTransition t : serialTransitions) {
            if (t.isMatchAny()) {
                parts.add("( serialInput.length() > 0)");
            } else {
                // Le pattern est stocké sans guillemets, il faut les ajouter pour la génération
                parts.add("( serialInput == \"" + t.getPattern() + "\" )");
            }
        }

        String connector = transitionList.getConnector() == LOGIC.OR ? " || " : " && ";
        String condition = parts.size() > 1 ? "( " + String.join(connector, parts) + " )" :
                (parts.isEmpty() ? "false" : parts.get(0));

        w(String.format("\t\t\tif( %s ) {\n", condition));

        for (String name : sensorsName) {
            w(String.format("\t\t\t\t%sLastDebounceTime = millis();\n", name));
        }

        w("\t\t\t\tcurrentState = " + transitionList.getNext().getName() + ";\n");
        w("\t\t\t}\n");
        w("\t\t\tbreak;\n");
    }


	@Override
	public void visit(Action action) {
		if(context.get("pass") == PASS.ONE) {
			return;
		}
		if(context.get("pass") == PASS.TWO) {
			w(String.format("\t\t\tdigitalWrite(%d,%s);\n",action.getActuator().getPin(),action.getValue()));
			return;
		}
	}

}
