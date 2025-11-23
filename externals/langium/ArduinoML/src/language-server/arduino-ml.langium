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

Brick :
	(Actuator | Sensor);

Actuator:
	{infer Actuator}
	'Actuator' name=ID ':' outputPin=INT;

Sensor :
	{infer Sensor}
	'Sensor'  name=ID ':' inputPin=INT;
	
State :
	name=ID '{'
		(actions+=Action)* 
		transition=TransitionList
	'}';


Action :
	actuator=[Actuator:ID] '<=' value=Signal;

TransitionList:
	(
		connector=Logic '{'
			(transitions+=Transition)
			(transitions+=Transition)*
		'}' '=>' (next=[State:ID] | 'error' errorCode=INT)
	) | (
		transitions+=Transition
	) '=>' (next=[State:ID] | 'error' errorCode=INT);


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
