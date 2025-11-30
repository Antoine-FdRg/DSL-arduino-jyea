package io.github.mosser.arduinoml.kernel.generator;

import io.github.mosser.arduinoml.kernel.App;
import io.github.mosser.arduinoml.kernel.behavioral.*;
import io.github.mosser.arduinoml.kernel.structural.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

	@Override
	public void visit(App app) {
		//first pass, create global vars
		context.put("pass", PASS.ONE);
		w("// Wiring code generated from an ArduinoML model\n");
		w(String.format("// Application name: %s\n", app.getName())+"\n");

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
		for(Brick brick: app.getBricks()){
			brick.accept(this);
		}
		if (app.isUsingErrorState()) {
			w("  pinMode(12, OUTPUT); // Onboard LED for error blinking\n");
		}
		w("}\n");

		w("\nvoid loop() {\n" +
			"\tswitch(currentState){\n");
		for(State state: app.getStates()){
			state.accept(this);
		}
		w("\t}\n" +
			"}");
		generateErrorBlink(app.isUsingErrorState());
	}

	private void generateErrorBlink(boolean useErrorState) {
		if (!useErrorState) {
			return;
		}
		w("\n\n" +
				"long blinkDuration = 200 ;\n" +
				"long pauseDuration = 900;\n" +
				"long currentBlinkNumber = 0;\n" +
				"boolean currentBlinkState = false;\n" +
				"boolean pauseBlink = false;\n" +
				"long currentBlinkDuration = 0;\n" +
				"\n" +
				"void errorBlink(long errorCode){\n" +
				"  if(pauseBlink){\n" +
				"    if(millis() - currentBlinkDuration > pauseDuration){\n" +
				"      pauseBlink = false;\n" +
				"    }\n" +
				"    return;\n" +
				"  }\n" +
				"  if(millis() - currentBlinkDuration > blinkDuration){\n" +
				"      currentBlinkDuration = millis();\n" +
				"      if(currentBlinkState){\n" +
				"        digitalWrite(12,LOW);\n" +
				"        currentBlinkNumber++;      \n" +
				"        if(currentBlinkNumber == errorCode){\n" +
				"          currentBlinkNumber = 0;\n" +
				"          pauseBlink = true;\n" +
				"          currentBlinkDuration = millis();\n" +
				"        }\n" +
				"      }else{\n" +
				"        digitalWrite(12,HIGH);\n" +
				"      }\n" +
				"      currentBlinkState = !currentBlinkState;\n" +
				"  }\n" +
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
	public void visit(State state) {
		if(context.get("pass") == PASS.ONE){
			w(state.getName());
			return;
		}
		if(context.get("pass") == PASS.TWO) {
			w("\t\tcase " + state.getName() + ":\n");

			if(state.getName().startsWith("error_")){
				w(String.format("\t\t\terrorBlink(%s);\n",state.getName().substring(6)));
			} else {
				for (Action action : state.getActions()) {
					action.accept(this);
				}

				if (state.getExpression() != null && state.getNext() != null) {
					generateTransitionCode(state);
				}
			}
			w("\t\t\tbreak;\n");
		}
	}

	private void generateTransitionCode(State state) {
		Expression expr = state.getExpression();
		Set<Sensor> sensors = new HashSet<>();
		collectSensors(expr, sensors);

		for (Sensor s : sensors) {
			String name = s.getName();
			w(String.format("\t\t\t%sBounceGuard = millis() - %sLastDebounceTime > debounce;\n",
					name, name));
		}

		String condition = buildCondition(expr);
		w(String.format("\t\t\tif( %s ) {\n", condition));

		for (Sensor s : sensors) {
			String name = s.getName();
			w(String.format("\t\t\t\t%sLastDebounceTime = millis();\n", name));
		}

		w("\t\t\t\tcurrentState = " + state.getNext().getName() + ";\n");
		w("\t\t\t}\n");
	}

	private void collectSensors(Expression expr, Set<Sensor> sensors) {
		if (expr instanceof SignalTransition) {
			SignalTransition st = (SignalTransition) expr;
			if (st.getSensor() != null) {
				sensors.add(st.getSensor());
			}
		} else if (expr instanceof LogicalExpression) {
			LogicalExpression le = (LogicalExpression) expr;
			for (Expression child : le.getExpressions()) {
				collectSensors(child, sensors);
			}
		}
	}

	private String buildCondition(Expression expr) {
		if (expr instanceof SignalTransition) {
			SignalTransition st = (SignalTransition) expr;
			Sensor sensor = st.getSensor();
			String name = sensor.getName();
			int pin = sensor.getPin();
			String val = st.getValue().toString();

			return String.format("(digitalRead(%d) == %s && %sBounceGuard)",
					pin, val, name);
		} else if (expr instanceof LogicalExpression) {
			LogicalExpression le = (LogicalExpression) expr;
			String op = (le.getOperator() == LOGIC.OR) ? " || " : " && ";

			List<String> parts = le.getExpressions().stream()
					.map(this::buildCondition)
					.collect(Collectors.toList());

			if (parts.size() == 1) {
				return parts.get(0);
			}
			return "(" + String.join(op, parts) + ")";
		}
		return "false";
	}

	@Override
	public void visit(SignalTransition transition) {
	}

	@Override
	public void visit(LogicalExpression logicalExpression) {
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
