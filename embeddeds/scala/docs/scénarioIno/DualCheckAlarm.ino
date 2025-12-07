// Wiring code generated from an ArduinoML model
// Application name: red_button

long debounce = 200;
long stateEnteredTime = 0;
int lastState = -1;
enum STATE {on, off};

STATE currentState = off;

bool b1BounceGuard = false;
long b1LastDebounceTime = 0;

bool b2BounceGuard = false;
long b2LastDebounceTime = 0;

void setup() {
    pinMode(9, INPUT); // b1 [Sensor]
    pinMode(10, INPUT); // b2 [Sensor]
    pinMode(12, OUTPUT); // buzzer [Actuator]
}

void loop() {
    if ((int)currentState != lastState) {
        stateEnteredTime = millis();
        lastState = (int)currentState;
    }

    switch (currentState) {
        case on:
            digitalWrite(12, HIGH);
            b1BounceGuard = millis() - b1LastDebounceTime > debounce;
            b2BounceGuard = millis() - b2LastDebounceTime > debounce;
            if (((digitalRead(9) == LOW && b1BounceGuard)  ||  (digitalRead(10) == LOW && b2BounceGuard))) {
                b1LastDebounceTime = millis();
                b2LastDebounceTime = millis();
                currentState = off;
            }

            break;
        case off:
            digitalWrite(12, LOW);
            b1BounceGuard = millis() - b1LastDebounceTime > debounce;
            b2BounceGuard = millis() - b2LastDebounceTime > debounce;
            if (((digitalRead(9) == HIGH && b1BounceGuard)  &&  (digitalRead(10) == HIGH && b2BounceGuard))) {
                b1LastDebounceTime = millis();
                b2LastDebounceTime = millis();
                currentState = on;
            }

            break;
    }
}
