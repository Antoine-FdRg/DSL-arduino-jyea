import fs from "fs";
import { CompositeGeneratorNode, NL, toString } from "langium";
import path from "path";
import {
    Action,
    Actuator,
    App,
    Sensor,
    State,
    TransitionList,
} from "../language-server/generated/ast";
import { extractDestinationAndName } from "./cli-util";

export function generateInoFile(
    app: App,
    filePath: string,
    destination: string | undefined
): string {
    const data = extractDestinationAndName(filePath, destination);
    const generatedFilePath = `${path.join(data.destination, data.name)}.ino`;

    const fileNode = new CompositeGeneratorNode();
    compile(app, fileNode);

    if (!fs.existsSync(data.destination)) {
        fs.mkdirSync(data.destination, { recursive: true });
    }
    fs.writeFileSync(generatedFilePath, toString(fileNode));
    return generatedFilePath;
}


function compile(app: App, fileNode: CompositeGeneratorNode) {
    const hasSerial = hasSerialCommunication(app);

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
        `// Wiring code generated from an ArduinoML model
// Application name: ` + app.name + `
` + (hasSerial ? `// Serial communication: 9600 baud (Standard Arduino Uno)
` : ``) + `
long debounce = 200;
long stateEnteredTime = 0;
int lastState = -1;
` + (hasSerial ? `bool notPrint = true;
` : ``) + `enum STATE {` +
        app.states.map((s) => s.name).join(", "));
    if (isUsingErrorState(app)) {
        const errorCodes = getErrorCodes(app);
        const uniqueErrorCodes = Array.from(new Set(errorCodes));
        for (const code of uniqueErrorCodes) {
            fileNode.append(`, error_` + code);
        }
        ;
    }
    fileNode.append(`};

STATE currentState = ` +
        app.initial.ref?.name +
        `;`,
        NL
    );

    const lcdBrick = app.bricks.find(b => b.$type === "LCDBrick");

    let lcdName: string | undefined = undefined;
    let columns = 16;
    let rows = 2;

    if (lcdBrick) {
        lcdName = lcdBrick.name;
        columns = (lcdBrick as any).columns ?? 16;
        rows = (lcdBrick as any).rows ?? 2;
    }

    if (app.bricks.some(b => "rs" in b)) {
        fileNode.append(`
#include <LiquidCrystal.h>
LiquidCrystal ${lcdName}(10, 11, 12, 13, 14, 15, 16);
`);
    }

    for (const brick of app.bricks) {
        if (brick.$type === 'DigitalSensor') {
            fileNode.append(
                `
bool ` +
                brick.name +
                `BounceGuard = false;
long ` +
                brick.name +
                `LastDebounceTime = 0;`,
                NL
            );
        }
    }
    fileNode.append(`

void setup() {`);

    if (hasSerial) {
        fileNode.append(`
    Serial.begin(9600);
    while(!Serial) { ; } // Wait for serial port`);
    }

    for (const brick of app.bricks) {
        if (brick.$type === 'DigitalSensor') {
            compileSensor(brick, fileNode);
        } else if (brick.$type === 'Actuator') {
            compileActuator(brick, fileNode);
        } else if ("rs" in brick) {
        }
    }

    if (app.bricks.some(b => "rs" in b)) {
        fileNode.append(`
    ${lcdName}.begin(${columns}, ${rows});`);
    }

    compileErrorLedActuatorCode(app, fileNode);

    fileNode.append(`
}

void loop() {`);
    if (hasSerial) {
        fileNode.append(`
    String serialInput = "";
    if (Serial.available() > 0) {
        serialInput = Serial.readStringUntil('\\n');
        serialInput.trim();
    }
`, NL);
    }
    fileNode.append(`
    if ((int)currentState != lastState) {
        stateEnteredTime = millis();
        lastState = (int)currentState;
    }

    switch (currentState) {`);

    for (const state of app.states) {
        compileState(state, fileNode, hasSerial);
    }
    if (isUsingErrorState(app)) {
        const errorCodes = getErrorCodes(app);
        const uniqueErrorCodes = Array.from(new Set(errorCodes));
        for (const code of uniqueErrorCodes) {
            fileNode.append(`
        case error_` + code + `:
            errorBlink(` + code + `);
            break;`, NL);
        }
    }
    fileNode.append(
        `
    }
}`, NL);

    if (isUsingErrorState(app)) {
        generateErrorMethodCode(fileNode);
    }
}

function hasSerialCommunication(app: App) {
    const hasSerialSensor = app.bricks.some(b => b.$type === 'SerialSensor');
    const hasSendAction = app.states.some(state => state.actions.some(action => action.$type === 'SendAction'));
    return hasSerialSensor || hasSendAction;
}

function compileActuator(actuator: Actuator, fileNode: CompositeGeneratorNode) {
    fileNode.append(
        `
    pinMode(` +
        actuator.outputPin +
        `, OUTPUT); // ` +
        actuator.name +
        ` [Actuator]`
    );
}

function compileSensor(sensor: Sensor, fileNode: CompositeGeneratorNode) {
    if (sensor.$type === 'DigitalSensor') {
        fileNode.append(`
    pinMode(` + (sensor as any).inputPin + `, INPUT); // ` + sensor.name + ` [Sensor]`)
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

    fileNode.append(`
            break;`);
}


function compileAction(action: Action, fileNode: CompositeGeneratorNode) {
    if (action.$type === 'SetAction') {
        if (action.lcd) {
            compileLCDAction(action, fileNode);
            return;
        }
        fileNode.append(`
            digitalWrite(` + action.actuator?.ref?.outputPin + `, ` + action.value?.value + `);`);
    } else if (action.$type === 'SendAction') {
        let message = action.message;
        if (message.startsWith('"') && message.endsWith('"')) {
            message = message.substring(1, message.length - 1);
        }
        fileNode.append(`
            if (notPrint) {
                Serial.println("` + message + `");
                notPrint = false;
            }`);
    }
}

function compileLCDAction(action: Action, fileNode: CompositeGeneratorNode) {
    if (action.$type !== 'SetAction') return;
    if (!action.lcdMessage) return;

    const lcdName = action.lcd?.ref?.name ?? "lcd";

    fileNode.append(`
            ${lcdName}.setCursor(0, 0);
            ${lcdName}.print("                ");  // clear line 0
            ${lcdName}.setCursor(0, 1);
            ${lcdName}.print("                ");  // clear line 1`);

    for (const part of action.lcdMessage.parts) {
        if (part.$type === "ConstantText") {
            fileNode.append(`
            ${lcdName}.setCursor(0, 0);
            ${lcdName}.print("${part.value}");`);
        }
    }

    for (const part of action.lcdMessage.parts) {
        if (part.$type === "BrickValueRef") {
            const brick = part.brick?.ref;
            if (!brick) continue;

            fileNode.append(`
            ${lcdName}.setCursor(0, 1);`);

            if ("inputPin" in brick) {
                fileNode.append(`
            ${lcdName}.print((digitalRead(${brick.inputPin}) == HIGH ? "HIGH" : "LOW "));`);
            }
            else if ("outputPin" in brick) {
                fileNode.append(`
            ${lcdName}.print((digitalRead(${brick.outputPin}) == HIGH ? "ON  " : "OFF "));`);
            }
        }
    }
}

function compileTransition(
    transition: TransitionList,
    fileNode: CompositeGeneratorNode,
    hasSerial: boolean
) {
    const transitions: any[] = (transition as any).transitions || [];
    // Check if this is a TemporalTransitionList (has delay property)
    if ((transition as any).delay !== undefined) {
        const delay = (transition as any).delay;
        const nextName = (transition as any).next?.ref?.name ? (transition as any).next.ref.name : ((transition as any).errorCode !== undefined ? 'error_' + (transition as any).errorCode : undefined);
        fileNode.append(
            `
            if (millis() - stateEnteredTime > ${delay}) {
                currentState = ` +
            nextName +
            `;
            }`,
            NL
        );
        return;
    }

    const digitalTransitions = transitions.filter(t =>
        (t.$type === 'DigitalTransition' || t.$type === 'SignalTransition') &&
        t.sensor?.ref?.$type === 'DigitalSensor'
    );
    const serialTransitions = transitions.filter(t => t.$type === 'SerialTransition');

    // Handle SignalTransitionList
    const sensors = new Set<string>();
    for (const t of digitalTransitions) {
        const name = t.sensor?.ref?.name;
        if (name) sensors.add(name);
    }

    for (const s of Array.from(sensors)) {
        fileNode.append(
            `
            ` +
            s +
            `BounceGuard = millis() - ` +
            s +
            `LastDebounceTime > debounce;`,
            NL
        );
    }

    const parts = digitalTransitions.map((t) => {
        const pin = t.sensor?.ref?.inputPin;
        const name = t.sensor?.ref?.name;
        const val = t.value?.value;
        return `(digitalRead(${pin}) == ${val} && ${name}BounceGuard)`;
    });


    for (const t of serialTransitions) {
        if (t.any) {
            parts.push(`(serialInput.length() > 0)`);
        } else if (t.pattern) {
            let pattern = t.pattern;
            // Remove quotes if they exist (pattern comes from STRING terminal)
            if ((pattern.startsWith('"') && pattern.endsWith('"')) ||
                (pattern.startsWith("'") && pattern.endsWith("'"))) {
                pattern = pattern.substring(1, pattern.length - 1);
            }
            parts.push(`(serialInput == "` + pattern + `")`)
        }
    }

    const op = (transition as any).connector?.value === 'AND' ? ' && ' : ' || ';
    const condition = parts.length > 1 ? `(` + parts.join(` ` + op + ` `) + `)` : (parts[0] || 'false');
    const nextName = (transition as any).next?.ref?.name ? (transition as any).next.ref.name : ((transition as any).errorCode !== undefined ? 'error_' + (transition as any).errorCode : undefined);

    const debounceCode = sensors.size > 0
        ? `
                ` + Array.from(sensors).map(s => s + `LastDebounceTime = millis();`).join('\n                ')
        : '';

    const notPrintCode = hasSerial ? `
                notPrint = true;` : '';

    fileNode.append(`
            if (` + condition + `) {` + debounceCode + `
                currentState = ` + nextName + `;` + notPrintCode + `
            }`, NL);
}

function compileErrorLedActuatorCode(app: App, fileNode: CompositeGeneratorNode) {
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

long blinkDuration = 200;
long pauseDuration = 900;
long currentBlinkNumber = 0;
boolean currentBlinkState = false;
boolean pauseBlink = false;
long currentBlinkDuration = 0;

void errorBlink(long errorCode) {
    if (pauseBlink) {
        if (millis() - currentBlinkDuration > pauseDuration) {
            pauseBlink = false;
        }
        return;
    }
    if (millis() - currentBlinkDuration > blinkDuration) {
        currentBlinkDuration = millis();
        if (currentBlinkState) {
            digitalWrite(12, LOW);
            currentBlinkNumber++;
            if (currentBlinkNumber == errorCode) {
                currentBlinkNumber = 0;
                pauseBlink = true;
                currentBlinkDuration = millis();
            }
        } else {
            digitalWrite(12, HIGH);
        }
        currentBlinkState = !currentBlinkState;
    }
}
`, NL);
}
