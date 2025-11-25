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
    for (const brick of app.bricks) {
		if (brick.$type === "LCDBrick") {
			(brick as any).rs = 10;
			(brick as any).enable = 11;
			(brick as any).d4 = 12;
			(brick as any).d5 = 13;
			(brick as any).d6 = 14;
			(brick as any).d7 = 15;
			(brick as any).d8 = 16;
			(brick as any).columns = 16;
			(brick as any).rows = 2;
		}
    }
    fileNode.append(
	`
//Wiring code generated from an ArduinoML model
// Application name: `+app.name+`

long debounce = 200;
enum STATE {`+app.states.map(s => s.name).join(', ')+`};

STATE currentState = `+app.initial.ref?.name+`;`
    ,NL);

	const lcdBrick = app.bricks.find(b => b.$type === "LCDBrick");
	const lcdName = lcdBrick?.name ?? "lcd";
	const columns = (lcdBrick as any).columns ?? 16;
    const rows = (lcdBrick as any).rows ?? 2;

	if (app.bricks.some(b => "rs" in b)) {
		fileNode.append(`#include <LiquidCrystal.h>
	LiquidCrystal ${lcdName}(10, 11, 12, 13, 14, 15, 16);

	`);
	}
	
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
	for (const brick of app.bricks) {

		if ("inputPin" in brick) {
			compileSensor(brick, fileNode);

		} else if ("outputPin" in brick) {
			compileActuator(brick, fileNode);

		} else if ("rs" in brick) {
		}
	}

	if (app.bricks.some(b => "rs" in b)) {
    	fileNode.append(`
        	${lcdName}.begin(${columns}, ${rows});`);
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
	

function compileAction(action: Action, fileNode: CompositeGeneratorNode) {
    if (action.lcd) { 
        compileLCDAction(action, fileNode);
        return;
    }
    if (action.actuator && action.value) { 
        fileNode.append(`
                        digitalWrite(${action.actuator.ref?.outputPin}, ${action.value.value});`);
    }
}

function compileLCDAction(action: Action, fileNode: CompositeGeneratorNode) {
    if (!action.lcdMessage) return;

    const lcdName = action.lcd?.ref?.name ?? "lcd";

    fileNode.append(`
                        ${lcdName}.setCursor(0, 0);
                        ${lcdName}.print("                ");  // clear line 0
                        ${lcdName}.setCursor(0, 1);
                        ${lcdName}.print("                ");  // clear line 1
    `);

    for (const part of action.lcdMessage.parts) {
        if (part.$type === "ConstantText") {
            fileNode.append(`
                        ${lcdName}.setCursor(0, 0);
                        ${lcdName}.print("${part.value}");
            `);
        }
    }

    for (const part of action.lcdMessage.parts) {
        if (part.$type === "BrickValueRef") {

            const brick = part.brick?.ref;
            if (!brick) continue;

            fileNode.append(`
                        ${lcdName}.setCursor(0, 1);
            `);

            if ("inputPin" in brick) {
                fileNode.append(`
                        ${lcdName}.print((digitalRead(${brick.inputPin}) == HIGH ? "HIGH" : "LOW "));
                `);
            }
            else if ("outputPin" in brick) {
                fileNode.append(`
                        ${lcdName}.print((digitalRead(${brick.outputPin}) == HIGH ? "ON  " : "OFF "));
                `);
            }
        }
    }
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

