
// get the digital pin for the LED attached to the TinyDuino Processor Board
int ledPin = 13;
// the setup routine runs once
void setup() {
  // initialize serial communication at 9600 bits per second:
  Serial.begin(9600);
  //The LED pin will onl be used for output
  pinMode(ledPin, OUTPUT);
}

// the loop routine runs over and over again forever:
void loop() {
  // read the input on analog pin 0:
  int sensorValue = analogRead(A0);
  // print out the value you read:
  Serial.println(sensorValue);
  //Blink the LED if the muscle is engaged hard enough for extra feedback
  if(sensorValue > 600) digitalWrite(ledPin, HIGH);
  else digitalWrite(ledPin,LOW);
  
  delay(10); // delay in between sensor reads for stability
}

