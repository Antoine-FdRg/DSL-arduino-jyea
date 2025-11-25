grammar ArduinoMl

entry App:
	'app' name=ID 'initial' 'state' initial=[State:ID]
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
	'Sensor'  name=ID ':' inputPin=INT;
	
State :
	name=ID '{'
		actions+=Action
		(actions+=Action)* 
		transition=TransitionList
	'}';

LCDBrick:
    {infer LCDBrick}
    'LCD' name=ID;

Action:
      actuator=[Actuator:ID] '<=' value=Signal
    | lcd=[LCDBrick:ID] 'displays' lcdMessage=LCDMessage
;

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
	(
		connector=Logic '{'
			(transitions+=Transition)
			(transitions+=Transition)*
		'}' '=>' next=[State:ID]
	) | (
		transitions+=Transition
	) '=>' next=[State:ID];


Transition:
	sensor=[Sensor:ID] 'is' value=Signal;

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
