grammar ArduinoMl

entry App:
    'app' name=ID
    'initial' 'state' initial=[State:ID]
    ('use' useSerialMonitor=COMPONENT)?
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
    {infer Sensor}
    'Sensor' name=ID ':' inputPin=INT;

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
    actuator=[Actuator:ID] '<=' value=Signal;

SendAction:
    'send' message=STRING
    | lcd=[LCDBrick:ID] 'displays' lcdMessage=LCDMessage;

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
    'receives' (pattern=STRING | any?='any');

Signal:
	value=(HIGH | LOW);
Logic:
	value=(AND | OR);

terminal HIGH: 'HIGH';
terminal LOW: 'LOW';

terminal AND: 'AND';
terminal OR: 'OR';

terminal COMPONENT: 'SerialMonitor';

hidden terminal WS: /\s+/;
terminal ID: /[_a-zA-Z][\w_]*/;
terminal INT returns number: /[0-9]+/;
terminal STRING: /"[^"]*"|'[^']*'/;

hidden terminal ML_COMMENT: /\/\*[\s\S]*?\*\//;
hidden terminal SL_COMMENT: /\/\/[^\n\r]*/;