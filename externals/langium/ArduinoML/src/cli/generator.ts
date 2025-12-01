import fs from 'fs';
import { CompositeGeneratorNode, NL, toString } from 'langium';
import path from 'path';
import { Action, Actuator, App, Sensor, State, TransitionList } from '../language-server/generated/ast';
import { extractDestinationAndName } from './cli-util';

export function generateInoFile(app: App, filePath: string, destination: string | undefined): string {
	const data = extractDestinationAndName(filePath, destination);
	const generatedFilePath = `${path.join(data.destination, data.name)}.ino`;

	const fileNode = new CompositeGeneratorNode();
	compile(app, fileNode)


	if (!fs.existsSync(data.destination)) {
		fs.mkdirSync(data.destination, { recursive: true });
	}
	fs.writeFileSync(generatedFilePath, toString(fileNode));
	return generatedFilePath;
}


function compile(app: App, fileNode: CompositeGeneratorNode) {
	const hasSerial = hasSerialCommunication(app);

	fileNode.append(
		`
// Wiring code generated from an ArduinoML model
// Application name: `+ app.name + `
` + (hasSerial ? `// Serial communication: 9600 baud (Standard Arduino Uno)` : ``) + `

long debounce = 200;
` + (hasSerial ? `bool notPrint = true;` : ``) + `
enum STATE {`+ app.states.map(s => s.name).join(', ') + `};

STATE currentState = `+ app.initial.ref?.name + `;`, NL);

	for (const brick of app.bricks) {
		if (brick.$type === 'DigitalSensor') {
			fileNode.append(`
bool `+ brick.name + `BounceGuard = false;
long `+ brick.name + `LastDebounceTime = 0;
			`, NL);
		}
	}

	fileNode.append(`
void setup(){`);

	if(hasSerial) {
		fileNode.append(`
	Serial.begin(9600);
	while(!Serial) { ; } // Wait for serial port`);
	}

	for (const brick of app.bricks) {
		if(brick.$type === 'DigitalSensor') {
			compileSensor(brick, fileNode);
		} else if (brick.$type === 'Actuator') {
			compileActuator(brick, fileNode);
		}
	}

	fileNode.append(`
}
	
void loop() {`);

	if(hasSerial) {
		fileNode.append(`
	String serialInput = "";
	if (Serial.available() > 0) {
		serialInput = Serial.readStringUntil('\\n');
		serialInput.trim();
	}
`, NL);
	}
	
	fileNode.append(`
	switch(currentState){`);

	for (const state of app.states) {
		compileState(state, fileNode, hasSerial);
	}

	fileNode.append(`
	}
}
`, NL);
}

function hasSerialCommunication(app: App) {
	const hasSerialSensor = app.bricks.some(b => b.$type === 'SerialSensor');
	const hasSendAction = app.states.some(state => state.actions.some(action => action.$type === 'SendAction'));
	return hasSerialSensor || hasSendAction;
}

function compileActuator(actuator: Actuator, fileNode: CompositeGeneratorNode) {
	fileNode.append(`
	pinMode(`+ actuator.outputPin + `, OUTPUT); // ` + actuator.name + ` [Actuator]`)
}

function compileSensor(sensor: Sensor, fileNode: CompositeGeneratorNode) {
	if (sensor.$type === 'DigitalSensor') {
		fileNode.append(`
	pinMode(` + (sensor as any).inputPin + `, INPUT); // ` + sensor.name + `[Sensor]`)
	}
}

function compileState(state: State, fileNode: CompositeGeneratorNode, hasSerial: boolean) {
	fileNode.append(`
		case `+ state.name + `:`);

	for (const action of state.actions) {
		compileAction(action, fileNode);
	}

	if (state.transition !== null) {
		compileTransition(state.transition, fileNode, hasSerial);
	}

	fileNode.append(`		break;`);
}


function compileAction(action: Action, fileNode: CompositeGeneratorNode) {
	if (action.$type === 'SetAction') {
		fileNode.append(`
			digitalWrite(` + action.actuator.ref?.outputPin + `,` + action.value.value + `);`);
	} else if (action.$type === 'SendAction') {
		let message = action.message;
		if (message.startsWith('"') && message.endsWith('"')) {
			message = message.substring(1, message.length - 1);
		}
		fileNode.append(`
			if(notPrint) {
				Serial.println("` + message + `");
				notPrint = false;
			}`);
	}
}

function compileTransition(transition: TransitionList, fileNode: CompositeGeneratorNode, hasSerial: boolean) {
	const transitions: any[] = (transition as any).transitions || [];

	const digitalTransitions = transitions.filter(t => t.$type === 'DigitalTransition');
	const serialTransitions = transitions.filter(t => t.$type === 'SerialTransition');

	const sensors = new Set<string>();
	for (const t of digitalTransitions) {
		const name = t.sensor?.ref?.name;
		if (name) sensors.add(name);
	}

	for (const s of Array.from(sensors)) {
		fileNode.append(`
			` + s + `BounceGuard = millis() - ` + s + `LastDebounceTime > debounce;`, NL);
	}

	const parts: string[] = [];

	for (const t of digitalTransitions) {
		const pin = t.sensor?.ref?.inputPin;
		const name = t.sensor?.ref?.name;
		const val = t.value?.value;
		parts.push(`( digitalRead(${pin}) == ${val} && ${name}BounceGuard )`);
	}

	for (const t of serialTransitions) {
		if (t.any) {
			parts.push(`( serialInput.length() > 0)`);
		} else if (t.pattern) {
			parts.push(`( serialInput == "` + t.pattern + `" )`)
		}
	}

	const op = (transition as any).connector?.value === 'AND' ? ' && ' : ' || ';
	const condition = parts.length > 1 ? `( ` + parts.join(op) + ` )` : (parts[0] || 'false');

	const debounceCode = sensors.size > 0
        ? `
                ` + Array.from(sensors).map(s => s + `LastDebounceTime = millis();`).join('\n\t\t\t\t')
        : '';

	const notPrintCode = hasSerial ? `
				notPrint = true;` : '';

    fileNode.append(`
			if( ` + condition + ` ) {` + debounceCode + `
				currentState = ` + (transition as any).next.ref?.name + `;` + notPrintCode + `
			}`, NL);
}