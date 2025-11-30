import fs from 'fs';
import { CompositeGeneratorNode, NL, toString } from 'langium';
import path from 'path';
import { Action, Actuator, App, Condition, Expression, LogicExpression, Sensor, State } from '../language-server/generated/ast';
import { extractDestinationAndName } from './cli-util';

export function generateInoFile(app: App, filePath: string, destination: string | undefined): string {
    const data = extractDestinationAndName(filePath, destination);
    const generatedFilePath = `${path.join(data.destination, data.name)}.ino`;

    const fileNode = new CompositeGeneratorNode();
    compile(app,fileNode)
    
    
    if (!fs.existsSync(data.destination)) {
        fs.mkdirSync(data.destination, { recursive: true });
    }
    fs.writeFileSync(generatedFilePath, toString(fileNode));
    return generatedFilePath;
}


function compile(app:App, fileNode:CompositeGeneratorNode){
    fileNode.append(
	`
//Wiring code generated from an ArduinoML model
// Application name: `+app.name+`

long debounce = 200;
enum STATE {`+app.states.map(s => s.name).join(', '));
		if (isUsingErrorState(app)) {
			const errorCodes = getErrorCodes(app);
			const uniqueErrorCodes = Array.from(new Set(errorCodes));
			for (const code of uniqueErrorCodes) {
				fileNode.append(`, error_` + code);
			};
		}
		fileNode.append(`};
STATE currentState = `+app.initial.ref?.name+`;`
    ,NL);
	
    for(const brick of app.bricks){
        if ("inputPin" in brick){
            fileNode.append(`
bool `+brick.name+`BounceGuard = false;
long `+brick.name+`LastDebounceTime = 0;

            `,NL);
        }
    }
    fileNode.append(`
	void setup(){`);
    for(const brick of app.bricks){
        if ("inputPin" in brick){
       		compileSensor(brick,fileNode);
		}else{
            compileActuator(brick,fileNode);
        }
	}

		compileErrorLedActuatorCode(fileNode);

    fileNode.append(`
	}
	void loop() {
			switch(currentState){`,NL)
			for(const state of app.states){
				compileState(state, fileNode)
			}
		if(isUsingErrorState(app)){
				const errorCodes = getErrorCodes(app);
				const uniqueErrorCodes = Array.from(new Set(errorCodes));
				for(const code of uniqueErrorCodes){
				fileNode.append(`
				case error_`+code+` :
					errorBlink(`+code+`);
					break;`, NL);
				}
		}
	fileNode.append(`
		}
	}
	`,NL);

		if(isUsingErrorState(app)){
			generateErrorMethodCode(fileNode);
		}

	function compileActuator(actuator: Actuator, fileNode: CompositeGeneratorNode) {
        fileNode.append(`
		pinMode(`+actuator.outputPin+`, OUTPUT); // `+actuator.name+` [Actuator]`)
    }

	function compileSensor(sensor:Sensor, fileNode: CompositeGeneratorNode) {
    	fileNode.append(`
		pinMode(`+sensor.inputPin+`, INPUT); // `+sensor.name+` [Sensor]`)
	}

    function compileState(state: State, fileNode: CompositeGeneratorNode) {
        fileNode.append(`
				case `+state.name+`:`)
		for(const action of state.actions){
			compileAction(action, fileNode)
		}
		if (state.expression) {
			compileStateTransition(state, fileNode);
		}
		fileNode.append(`
				break;`)
    }
	

	function compileAction(action: Action, fileNode:CompositeGeneratorNode) {
		fileNode.append(`
					digitalWrite(`+action.actuator.ref?.outputPin+`,`+action.value.value+`);`)
	}

	function compileExpression(expr: Expression): string {

		if ('sensor' in expr) {
			const cond = expr as Condition;
			const pin = cond.sensor?.ref?.inputPin;
			const name = cond.sensor?.ref?.name;
			const val = cond.value?.value; 

			return `(digitalRead(${pin}) == ${val} && ${name}BounceGuard)`;
		}

		if ('expressions' in expr) {
			const logical = expr as LogicExpression;
			const op = logical.logic.value === 'AND' ? ' && ' : ' || ';
			const parts = logical.expressions.map(child => compileExpression(child));
			return `( ${parts.join(op)} )`;
		}

		return 'false';
	}

	function collectSensors(expr: Expression, sensors: Set<string>) {
		if ('sensor' in expr) {
			const cond = expr as Condition;
			const name = cond.sensor?.ref?.name;
			if (name) sensors.add(name);
		}
		if ('expressions' in expr) {
			const logical = expr as LogicExpression;
			logical.expressions.forEach(e => collectSensors(e, sensors));
		}
	}

	function compileStateTransition(state: State, fileNode: CompositeGeneratorNode) {
		const expr = state.expression;

		const sensors = new Set<string>();
		collectSensors(expr, sensors);

		for (const s of sensors) {
			fileNode.append(`
				${s}BounceGuard = millis() - ${s}LastDebounceTime > debounce;`, NL);
		}

		const condition = compileExpression(expr);

		const target = state.next?.ref?.name
			?? (state.errorCode !== undefined ? `error_${state.errorCode}` : 'currentState');

		fileNode.append(`
				if (${condition}) {
					${
						Array.from(sensors)
							.map(s => `${s}LastDebounceTime = millis();`)
							.join('\n\t\t\t\t')
					}
					currentState = ${target};
				}
			`);
	}

	function compileErrorLedActuatorCode(fileNode: CompositeGeneratorNode) {
		if (isUsingErrorState(app)) {
			fileNode.append(`
		pinMode(12, OUTPUT); // Onboard LED for error blinking`, NL);
		}
	}

	function isUsingErrorState(app: App): boolean {
    	return app.states.some(s => s.errorCode !== undefined);
	}

	function getErrorCodes(app: App): number[] {
		return app.states
			.filter(s => s.errorCode !== undefined)
			.map(s => s.errorCode!);
	}

	function generateErrorMethodCode(fileNode: CompositeGeneratorNode) {
		fileNode.append(`

long blinkDuration = 200 ;
long pauseDuration = 900;
long currentBlinkNumber = 0;
boolean currentBlinkState = false;
boolean pauseBlink = false;
long currentBlinkDuration = 0;

void errorBlink(long errorCode){
	if(pauseBlink){
		if(millis() - currentBlinkDuration > pauseDuration){
			pauseBlink = false;
		}
		return;
	}
	if(millis() - currentBlinkDuration > blinkDuration){
			currentBlinkDuration = millis();
			if(currentBlinkState){
				digitalWrite(12,LOW);
				currentBlinkNumber++;      
				if(currentBlinkNumber == errorCode){
					currentBlinkNumber = 0;
					pauseBlink = true;
					currentBlinkDuration = millis();
				}
			}else{
				digitalWrite(12,HIGH);
			}
			currentBlinkState = !currentBlinkState;
	}
}

`, NL);
	}
		
}