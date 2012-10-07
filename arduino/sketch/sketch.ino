
#include "aJSON.h"
#include "RCSwitch.h"

char buffer[64];
int counter = 0;

int greenPin = 11;
int bluePin = 9;
int redPin = 10;
int wifiPin = 7;
RCSwitch wifiTransmitter = RCSwitch();


void setup() {
  Serial.begin(9600);
  Serial.println("Starting");
  
  pinMode(bluePin, INPUT);
  pinMode(greenPin, INPUT);
  pinMode(redPin, INPUT);
    pinMode(wifiPin, INPUT);

  wifiTransmitter.enableTransmit(wifiPin);  // Using Pin #7

}



// {"w": {"s":1, "g":11001, "s":2, "t":"dip10"}}

void setWifi(int state, char* group, int switchNumber, char* t) {
       wifiTransmitter.switchOn(11011, 1);
}


void setColor(int r, int g, int b) {
    analogWrite(redPin, r);
    analogWrite(greenPin, g);
    analogWrite(bluePin, b);
}

void handleJson(char *b) {
  aJsonObject* root = aJson.parse(b);

  Serial.print("Root: ");
  Serial.println(root->child->name);

  if (*(root->child->name) == 'c') {
    Serial.println("Handling: color");
    setColor(aJson.getObjectItem(root->child, "r")->valueint, aJson.getObjectItem(root->child, "g")->valueint, aJson.getObjectItem(root->child, "b")->valueint);
    return; 
  } 
  
  if (*(root->child->name) == 'w') {
    Serial.println("Handling: wifi");
    setWifi(aJson.getObjectItem(root->child, "s")->valueint, aJson.getObjectItem(root->child, "g")->valuestring, aJson.getObjectItem(root->child, "s")->valueint, aJson.getObjectItem(root->child, "t")->valuestring);
    return; 
  }
}







void loop() {
  char c = 0;

  if (Serial.available())
  {
    buffer[counter] = Serial.read();

    if (buffer[counter] == '\n') {
      Serial.print("Read: ");
      Serial.println(buffer);
    
      handleJson(buffer);
      memset(buffer,0,sizeof(buffer));
      counter = -1;
    } 
    counter++;
  } 
}


