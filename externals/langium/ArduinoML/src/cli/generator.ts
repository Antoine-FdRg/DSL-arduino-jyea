import fs from 'fs';
import { CompositeGeneratorNode, NL, toString } from 'langium';
import path from 'path';
import { Action, Actuator, App, Sensor, State, TransitionList } from '../language-server/generated/ast';
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
		if (state.transition !== null){
			compileTransition(state.transition, fileNode)
		}
		fileNode.append(`
				break;`)
    }
	

	function compileAction(action: Action, fileNode:CompositeGeneratorNode) {
		fileNode.append(`
					digitalWrite(`+action.actuator.ref?.outputPin+`,`+action.value.value+`);`)
	}

function compileTransition(transition: TransitionList, fileNode: CompositeGeneratorNode) {
	const transitions: any[] = (transition as any).transitions || [];

	const sensors = new Set<string>();
	for (const t of transitions) {
		const name = t.sensor?.ref?.name;
		if (name) sensors.add(name);
	}

	for (const s of Array.from(sensors)) {
		fileNode.append(`
			` + s + `BounceGuard = millis() - ` + s + `LastDebounceTime > debounce;`, NL)
	}

	const parts = transitions.map(t => {
		const pin = t.sensor?.ref?.inputPin;
		const name = t.sensor?.ref?.name;
		const val = t.value?.value;
		return `( digitalRead(${pin}) == ${val} && ${name}BounceGuard )`;
	});

	const op = (transition as any).connector?.value === 'AND' ? ' && ' : ' || ';
	const condition = parts.length > 1 ? `( ` + parts.join(op) + ` )` : (parts[0] || 'false');
	const nextName = (transition as any).next?.ref?.name ? (transition as any).next.ref.name : ((transition as any).errorCode !== undefined ? 'error_' + (transition as any).errorCode : undefined);

	fileNode.append(`
			if( ` + condition + ` ) {
				` + Array.from(sensors).map(s => s + `LastDebounceTime = millis();`).join('\n\t\t\t\t') + `
					currentState = ` + (nextName ? nextName : 'currentState') + `;
				}`)
	}


	function compileErrorLedActuatorCode(fileNode: CompositeGeneratorNode) {
		if (isUsingErrorState(app)) {
			fileNode.append(`
		pinMode(12, OUTPUT); // Onboard LED for error blinking`, NL);
		}
	}

	function isUsingErrorState(app: App): boolean {
		return app.states.some(s => s.transition && (s.transition as any).errorCode !== undefined);
	}

	function getErrorCodes(app: App): number[] {
		return app.states.map(s => s.transition).filter(t => t && (t as any).errorCode !== undefined).map(t => (t as any).errorCode);
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