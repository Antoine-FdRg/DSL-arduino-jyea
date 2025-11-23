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
enum STATE {`+app.states.map(s => s.name).join(', ')+`};

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


    fileNode.append(`
	}
	void loop() {
			switch(currentState){`,NL)
			for(const state of app.states){
				compileState(state, fileNode)
            }
	fileNode.append(`
		}
	}
	`,NL);




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

	fileNode.append(`
			if( ` + condition + ` ) {
				` + Array.from(sensors).map(s => s + `LastDebounceTime = millis();`).join('\n\t\t\t\t') + `
				currentState = ` + (transition as any).next.ref?.name + `;
			}`, NL)
}

