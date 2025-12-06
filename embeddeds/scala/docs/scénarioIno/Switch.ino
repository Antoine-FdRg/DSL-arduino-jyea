// Wiring code generated from an ArduinoML model
// Application name: Switch!

long debounce = 200;
long stateEnteredTime = 0;
int lastState = -1;
enum STATE {on, off};

STATE currentState = off;

bool buttonBounceGuard = false;
long buttonLastDebounceTime = 0;

void setup() {
    pinMode(9, INPUT); // button [Sensor]
    pinMode(12, OUTPUT); // led [Actuator]
}

void loop() {
    if ((int)currentState != lastState) {
        stateEnteredTime = millis();
        lastState = (int)currentState;
    }

    switch (currentState) {
        case on:
            digitalWrite(12, HIGH);
            buttonBounceGuard = millis() - buttonLastDebounceTime > debounce;
            if ((digitalRead(9) == HIGH && buttonBounceGuard)) {
                buttonLastDebounceTime = millis();
                currentState = off;
            }

            break;
        case off:
            digitalWrite(12, LOW);
            buttonBounceGuard = millis() - buttonLastDebounceTime > debounce;
            if ((digitalRead(9) == HIGH && buttonBounceGuard)) {
                buttonLastDebounceTime = millis();
                currentState = on;
            }

            break;
    }
}