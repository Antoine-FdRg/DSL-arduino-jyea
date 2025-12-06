// Wiring code generated from an ArduinoML model
// Application name: red_button

long debounce = 200;
long stateEnteredTime = 0;
int lastState = -1;
enum STATE {ready, buzzing, led_on};

STATE currentState = ready;

bool buttonBounceGuard = false;
long buttonLastDebounceTime = 0;

void setup() {
    pinMode(8, INPUT); // button [Sensor]
    pinMode(12, OUTPUT); // red_led [Actuator]
    pinMode(9, OUTPUT); // buzzer [Actuator]
}

void loop() {
    if ((int)currentState != lastState) {
        stateEnteredTime = millis();
        lastState = (int)currentState;
    }

    switch (currentState) {
        case ready:
            digitalWrite(12, LOW);
            digitalWrite(9, LOW);
            buttonBounceGuard = millis() - buttonLastDebounceTime > debounce;
            if ((digitalRead(8) == HIGH && buttonBounceGuard)) {
                buttonLastDebounceTime = millis();
                currentState = buzzing;
            }

            break;
        case buzzing:
            digitalWrite(12, LOW);
            digitalWrite(9, HIGH);
            buttonBounceGuard = millis() - buttonLastDebounceTime > debounce;
            if ((digitalRead(8) == HIGH && buttonBounceGuard)) {
                buttonLastDebounceTime = millis();
                currentState = led_on;
            }

            break;
        case led_on:
            digitalWrite(12, HIGH);
            digitalWrite(9, LOW);
            buttonBounceGuard = millis() - buttonLastDebounceTime > debounce;
            if ((digitalRead(8) == HIGH && buttonBounceGuard)) {
                buttonLastDebounceTime = millis();
                currentState = ready;
            }

            break;
    }
}
