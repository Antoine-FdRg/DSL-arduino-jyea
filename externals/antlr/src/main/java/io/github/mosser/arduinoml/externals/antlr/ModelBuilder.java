package io.github.mosser.arduinoml.externals.antlr;

import io.github.mosser.arduinoml.externals.antlr.grammar.*;


import io.github.mosser.arduinoml.kernel.App;
import io.github.mosser.arduinoml.kernel.behavioral.Action;
import io.github.mosser.arduinoml.kernel.behavioral.SignalTransition;
import io.github.mosser.arduinoml.kernel.behavioral.State;
import io.github.mosser.arduinoml.kernel.behavioral.Transition;
import io.github.mosser.arduinoml.kernel.behavioral.TransitionList;
import io.github.mosser.arduinoml.kernel.behavioral.LOGIC;
import io.github.mosser.arduinoml.kernel.structural.Actuator;
import io.github.mosser.arduinoml.kernel.structural.SIGNAL;
import io.github.mosser.arduinoml.kernel.structural.Sensor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModelBuilder extends ArduinomlBaseListener {

    /********************
     ** Business Logic **
     ********************/

    private App theApp = null;
    private boolean built = false;

    public App retrieve() {
        if (built) { return theApp; }
        throw new RuntimeException("Cannot retrieve a model that was not created!");
    }

    /*******************
     ** Symbol tables **
     *******************/

    private final Map<String, Sensor>   sensors   = new HashMap<>();
    private final Map<String, Actuator> actuators = new HashMap<>();
    private final Map<String, State>    states  = new HashMap<>();

    private final Map<String, List<SignalTransition>> transitionLists = new HashMap<>();
    private final Map<String, String> transitionConnectors = new HashMap<>(); // AND / OR
    private final Map<String, String> transitionNext = new HashMap<>(); // next state name
    private final Map<String, Integer> transitionErrorCode = new HashMap<>(); // error code if 'error INT'


    private State currentState = null;

    /**************************
     ** Listening mechanisms **
     **************************/

    @Override
    public void enterRoot(ArduinomlParser.RootContext ctx) {
        built = false;
        theApp = new App();
    }

    @Override public void exitRoot(ArduinomlParser.RootContext ctx) {
        transitionLists.forEach((stateName, transitions) -> {
            TransitionList transitionList = new TransitionList();

            String conn = transitionConnectors.get(stateName);
            if (conn != null) {
                if (conn.equals("AND")) transitionList.setConnector(LOGIC.AND);
                else transitionList.setConnector(LOGIC.OR);
            }
            transitionList.setTransitions((transitions.stream().map(t-> (Transition) t).collect(Collectors.toList())));
            if (transitionErrorCode.containsKey(stateName)) {
                int code = transitionErrorCode.get(stateName);
                String errName = "error_" + code;
                if (!states.containsKey(errName)) {
                    State err = new State();
                    err.setName(errName);
                    states.put(errName, err);
                    this.theApp.getStates().add(err);
                    this.theApp.useErrorState();
                }
                transitionList.setNext(states.get(errName));
            } else if (transitionNext.containsKey(stateName)) {
                String next = transitionNext.get(stateName);
                transitionList.setNext(states.get(next));
            }
            // attach to the source state
            states.get(stateName).setTransitionList(transitionList);
        });
        this.built = true;
    }

    @Override
    public void enterDeclaration(ArduinomlParser.DeclarationContext ctx) {
        theApp.setName(ctx.name.getText());
    }

    @Override
    public void enterSensor(ArduinomlParser.SensorContext ctx) {
        Sensor sensor = new Sensor();
        sensor.setName(ctx.location().id.getText());
        sensor.setPin(Integer.parseInt(ctx.location().port.getText()));
        this.theApp.getBricks().add(sensor);
        sensors.put(sensor.getName(), sensor);
    }

    @Override
    public void enterActuator(ArduinomlParser.ActuatorContext ctx) {
        Actuator actuator = new Actuator();
        actuator.setName(ctx.location().id.getText());
        actuator.setPin(Integer.parseInt(ctx.location().port.getText()));
        this.theApp.getBricks().add(actuator);
        actuators.put(actuator.getName(), actuator);
    }

    @Override
    public void enterState(ArduinomlParser.StateContext ctx) {
        State localState = new State();
        localState.setName(ctx.name.getText());
        this.currentState = localState;
        this.states.put(localState.getName(), localState);
    }

    @Override
    public void exitState(ArduinomlParser.StateContext ctx) {
        this.theApp.getStates().add(this.currentState);
        this.currentState = null;
    }

    @Override
    public void enterAction(ArduinomlParser.ActionContext ctx) {
        Action action = new Action();
        action.setActuator(actuators.get(ctx.receiver.getText()));
        action.setValue(SIGNAL.valueOf(ctx.value.getText()));
        currentState.getActions().add(action);
    }

    @Override
    public void enterTransition(ArduinomlParser.TransitionContext ctx) {
        SignalTransition transition = new SignalTransition();
        transition.setSensor(sensors.get(ctx.trigger.getText()));
        transition.setValue(SIGNAL.valueOf(ctx.value.getText()));
        String currentStateName = currentState.getName();
        transitionLists.computeIfAbsent(currentStateName, k -> new ArrayList<>()).add(transition);
    }

    @Override
    public void exitTransitionList(ArduinomlParser.TransitionListContext ctx) {
        String stateName = currentState.getName();
        if (ctx.connector != null) {
            transitionConnectors.put(stateName, ctx.connector.getText());
        }
        if (ctx.next != null) {
            transitionNext.put(stateName, ctx.next.getText());
        } else if (ctx.errorCode != null) {
            transitionErrorCode.put(stateName, Integer.parseInt(ctx.errorCode.getText()));
        }
    }

    @Override
    public void enterInitial(ArduinomlParser.InitialContext ctx) {
        this.theApp.setInitial(this.currentState);
    }

}

