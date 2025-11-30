grammar ArduinoMl

entry App:
    'app' name=ID 
    'initial' 'state' initial=[State:ID]
    '{'
       'bricks'
       bricks+=Brick
       (bricks+=Brick)* 
       
       'states'
       states+=State
       ( states+=State)*
    '}';

Brick :
    (Actuator | Sensor);

Actuator:
    {infer Actuator}
    'Actuator' name=ID ':' outputPin=INT;

Sensor :
    DigitalSensor | SerialSensor;

DigitalSensor:
    {infer DigitalSensor}
    'Sensor' name=ID ':' inputPin=INT;

SerialSensor:
    {infer SerialSensor}
    'SerialSensor' name=ID;
    
State :
    name=ID '{'
       actions+=Action
       (actions+=Action)* 
       transition=TransitionList
    '}';

Action :
    SetAction | SendAction;

SetAction:
    actuator=[Actuator:ID] '<=' value=Signal;

SendAction:
    'send' message=STRING;

TransitionList:
    (
       connector=Logic '{'
          (transitions+=Transition)
          (transitions+=Transition)*
       '}' '=>' next=[State:ID]
    ) | (
       transitions+=Transition
    ) '=>' next=[State:ID];

Transition:
    DigitalTransition | SerialTransition;

DigitalTransition:
    sensor=[DigitalSensor:ID] 'is' value=Signal;

SerialTransition:
    sensor=[SerialSensor:ID] 'receives' (pattern=STRING | any?='any');

Signal:
    value=(HIGH | LOW);
    
Logic:
    value=(AND | OR);

terminal HIGH: 'HIGH';
terminal LOW: 'LOW';

terminal AND: 'AND';
terminal OR: 'OR';

hidden terminal WS: /\s+/;
terminal ID: /[_a-zA-Z][\w_]*/;
terminal INT returns number: /[0-9]+/;
terminal STRING: /"[^"]*"|'[^']*'/;

hidden terminal ML_COMMENT: /\/\*[\s\S]*?\*\//;
hidden terminal SL_COMMENT: /\/\/[^\n\r]*/;