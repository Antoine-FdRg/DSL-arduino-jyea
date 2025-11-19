
//Wiring code generated from an ArduinoML model
// Application name: RedButton

long debounce = 200;
enum STATE {off, on};

STATE currentState = off;

bool b1BounceGuard = false;
long b1LastDebounceTime = 0;

            

bool b2BounceGuard = false;
long b2LastDebounceTime = 0;

            

	void setup(){
		pinMode(12, OUTPUT); // red_led [Actuator]
		pinMode(8, INPUT); // b1 [Sensor]
		pinMode(9, INPUT); // b2 [Sensor]
	}
	void loop() {
			switch(currentState){

				case off:
					digitalWrite(12,LOW);
					b1BounceGuard = millis() - b1LastDebounceTime > debounce;

				if( ( ( digitalRead(8) == HIGH && b1BounceGuard ) ) ) {
					b1LastDebounceTime = millis();
					currentState = on;
				}

				break;
				case on:
					digitalWrite(12,HIGH);
					b1BounceGuard = millis() - b1LastDebounceTime > debounce;

				if( ( ( digitalRead(8) == HIGH && b1BounceGuard ) ) ) {
					b1LastDebounceTime = millis();
					currentState = off;
				}

				break;
		}
	}
	
