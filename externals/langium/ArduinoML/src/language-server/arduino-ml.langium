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

Brick:
      Actuator
    | Sensor
    | LCDBrick
;

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
		(actions+=Action)*
		transition=TransitionList
	'}';

LCDBrick:
    {infer LCDBrick}
    'LCD' name=ID;

Action :
    SetAction | SendAction;

SetAction:
    actuator=[Actuator:ID] '<=' value=Signal
    | lcd=[LCDBrick:ID] 'displays' lcdMessage=LCDMessage;

SendAction:
    'send' message=STRING;

LCDMessage:
    parts+=LCDMessagePart ('+' parts+=LCDMessagePart)*;

LCDMessagePart:
      ConstantText
    | BrickValueRef
;

ConstantText:
    value=STRING;

BrickValueRef:
    brick=[Brick:ID];

TransitionList:
	SignalTransitionList | TemporalTransitionList;

SignalTransitionList:
	(
		connector=Logic '{'
			(transitions+=SignalTransition)
			(transitions+=SignalTransition)*
		'}' '=>' (next=[State:ID] | 'error' errorCode=INT)
	) | (
		transitions+=SignalTransition
	) '=>' (next=[State:ID] | 'error' errorCode=INT);

TemporalTransitionList:
	'after' delay=INT 'ms' '=>' (next=[State:ID] | 'error' errorCode=INT);

SignalTransition:
	DigitalTransition | SerialTransition;

DigitalTransition:
    sensor=[Sensor:ID] 'is' value=Signal;

SerialTransition:
    sensor=[Sensor:ID] 'receives' (pattern=STRING | any?='any');

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