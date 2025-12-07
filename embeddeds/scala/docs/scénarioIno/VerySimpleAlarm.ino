// Wiring code generated from an ArduinoML model
// Application name: very_simple_alarm

long debounce = 200;
long stateEnteredTime = 0;
int lastState = -1;
enum STATE {off, on};

STATE currentState = off;

bool buttonBounceGuard = false;
long buttonLastDebounceTime = 0;

void setup() {
    pinMode(9, INPUT); // button [Sensor]
    pinMode(10, OUTPUT); // led [Actuator]
    pinMode(11, OUTPUT); // buzzer [Actuator]
}

void loop() {
    if ((int)currentState != lastState) {
        stateEnteredTime = millis();
        lastState = (int)currentState;
    }

    switch (currentState) {
        case off:
            digitalWrite(11, LOW);
            digitalWrite(10, LOW);
            buttonBounceGuard = millis() - buttonLastDebounceTime > debounce;
            if ((digitalRead(9) == HIGH && buttonBounceGuard)) {
                buttonLastDebounceTime = millis();
                currentState = on;
            }

            break;
        case on:
            digitalWrite(11, HIGH);
            digitalWrite(10, HIGH);
            buttonBounceGuard = millis() - buttonLastDebounceTime > debounce;
            if ((digitalRead(9) == LOW && buttonBounceGuard)) {
                buttonLastDebounceTime = millis();
                currentState = off;
            }

            break;
    }
}
