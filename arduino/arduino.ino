
#include "aJSON.h"
#include "RCSwitch.h"
#include <SPI.h>
#include <Ethernet.h>
#include "PubSubClient.h"
#include <math.h>

#define WIFIPIN 10
#define TEMPPIN A0



#define AMBILIGHTBLUEEPIN 6
#define AMBILIGHTREDPIN 5
#define AMBILIGHTGREENPIN 3
#define THERMISTORPIN A0       
#define THERMISTORNOMINAL 2200  // resistance at 25 degrees C
#define TEMPERATURENOMINAL 25    // temp. for nominal resistance (almost always 25 C)
#define BCOEFFICIENT 4791  //4791         // The beta coefficient of the thermistor (usually 3000-4000)
#define SERIESRESISTOR 2200  // the value of the 'other' resistor


// Prototypes

void setWifi(char* state, char* group, int switchNumber);
void setColor(int r, int g, int b);
float getTemp();
void mqttReceive(char* topic, byte* payload, unsigned int length);
void publishRetained(char* topic, char* payload);



// Settings 
byte mac[]    = { 0xDE, 0xAD, 0xBE, 0xEF, 0xFF, 0xFF };
byte ip[]     = { 192, 168, 8, 4 };
byte mqttServer[] = { 192, 168, 8, 2 };
char* mqttClientId = "homeduino";

int currentr = 255;
int currentg = 0;
int currentb = 0;






// Global variables
RCSwitch wifiTransmitter = RCSwitch();
boolean publishSensorsFlag = false;
boolean fading = true;
EthernetClient ethClient;
PubSubClient client(mqttServer, 1883, mqttReceive, ethClient);







void mqttReceive(char* topic, byte* payload, unsigned int length) {  
  char  buffer[length+1];
  memcpy(buffer, payload, length);
  buffer[length] = '\0';
  
  Serial.println("Callback");
  Serial.print("Topic:");
  Serial.println(topic);
  Serial.print("Length:");
  Serial.println(length);
  Serial.print("Payload:");
  Serial.print(buffer);
  Serial.println();
  





   if(strcmp(topic, "/devices/321995-ambilight/controls/power") == 0) {
    Serial.println("ambi power");
    char *group = "11011";
    setWifi((char*)buffer, group, 1);
   } else if(strcmp(topic, "/devices/321995-ambilight/controls/fading")  == 0 ) {
    Serial.println("ambi fading");
   } else if(strcmp(topic, "/devices/862671-wirelessSwitch/controls/power")  == 0) {
    Serial.println("862671-wirelessSwitch power");

   }else if(strcmp(topic, "/devices/558426-wirelessSwitch/controls/power")  == 0) {
    Serial.println("558426-wirelessSwitch power");

   } else {
     Serial.println("Unrecognized topic");
   }
   
////  Serial.println("");
}





void setWifi(char* state, char* group, int switchNumber) {
  Serial.print("DEBUG: Setting wifi " + String(group) + " " + String(switchNumber)) ;
  Serial.print("Value: ");
  Serial.println(state);
  
  if (strcmp(state, "1") == 0) {
    wifiTransmitter.switchOn(group, switchNumber);   
  } else if (strcmp(state, "0") == 0)  {  
    wifiTransmitter.switchOff(group, switchNumber);   
  } else {
    Serial.println("unrecognized state value");
  }  

}

void setColor(int r, int g, int b) {    
  analogWrite(AMBILIGHTREDPIN, r);
  analogWrite(AMBILIGHTGREENPIN, g);
  analogWrite(AMBILIGHTBLUEEPIN, b);
}




float getTemp() {
  float temp;
  temp = analogRead(TEMPPIN); 
  temp = 1023 / temp - 1;
  temp = SERIESRESISTOR / temp;  
  temp = temp / THERMISTORNOMINAL;                 // (R/Ro)
  temp = log(temp);                                // ln(R/Ro)
  temp /= BCOEFFICIENT;                            // 1/B * ln(R/Ro)
  temp += 1.0 / (TEMPERATURENOMINAL + 273.15);     // + (1/To)
  temp = 1.0 / temp;                             // Invert
  temp -= 273.15;                                  // convert to C
  return temp;
}


void subscribe() {
 client.subscribe("/devices/321995-ambilight/controls/#");
 client.subscribe("/devices/862671-wirelessSwitch/controls/#");
 client.subscribe("/devices/558426-wirelessSwitch/controls/#");  
}


void publishDeviceMetaInformation() {
 publishRetained("/devices/321995-ambilight/controls/power/type", "switch");
 publishRetained("/devices/321995-ambilight/controls/fading/type", "switch");
 publishRetained("/devices/862671-wirelessSwitch/controls/power/type", "switch");
 publishRetained("/devices/558426-wirelessSwitch/controls/power/type", "switch");

}

//
//
// AUXILARY METHODS
//
//

void publishRetained(char* topic, char* payload) {
  Serial.print("tx t: ");
  Serial.print(topic);
  Serial.print(", tx p: ");
  Serial.println((char*)payload);
  Serial.println("");

  client.publish(topic, (uint8_t*)payload, strlen((const char*)payload), true);
}


void setup() {
  Serial.begin(9600);
  Serial.println("Starting");

  pinMode(AMBILIGHTREDPIN, OUTPUT);
  pinMode(AMBILIGHTGREENPIN, OUTPUT);
  pinMode(AMBILIGHTBLUEEPIN, OUTPUT);
  pinMode(WIFIPIN, OUTPUT);


  analogWrite(AMBILIGHTREDPIN, 255);
  analogWrite(AMBILIGHTGREENPIN, 0);
  analogWrite(AMBILIGHTBLUEEPIN, 0);
  delay(2000);
  analogWrite(AMBILIGHTREDPIN, 0);
  analogWrite(AMBILIGHTGREENPIN, 255);
  analogWrite(AMBILIGHTBLUEEPIN, 0);
  delay(2000);
   analogWrite(AMBILIGHTREDPIN, 0);
  analogWrite(AMBILIGHTGREENPIN, 0);
  analogWrite(AMBILIGHTBLUEEPIN, 255);
  delay(2000);
  Serial.println("lights checked");

  Ethernet.begin(mac, ip);
  if (client.connect(mqttClientId)) {
    subscribe();
    publishDeviceMetaInformation();
  }

  wifiTransmitter.enableTransmit(WIFIPIN);
  setupInterrupts();
}

void publishSensors () {
    char tempStr[5];
    dtostrf(getTemp(),2,2,tempStr);

     publishRetained("/sensors/579761-temperature", tempStr);

}

void setupInterrupts() {
      // initialize Timer1
      cli();             // disable global interrupts
      TCCR1A = 0;        // set entire TCCR1A register to 0
      TCCR1B = 0;
   
      // enable Timer1 overflow interrupt (TIMSK1 = Timer/Counter1 Interrupt Mask Register):
      TIMSK1 = (1 << TOIE1); 
      // Set CLK/1024 presscaler
      // Triggers interrupt every 1/(16*10^6 / 1024) = 4.194s
      TCCR1B |= (1 << CS10);
      TCCR1B |= (1 << CS12);
      // enable global interrupts:
      sei();
}

ISR(TIMER1_OVF_vect)
{
  publishSensorsFlag = true;
}

void fade () {
      Serial.println("fading");
      
        if (currentr == 255 && currentg == 0  && currentb < 255) {
          currentb++;
        } else if (currentb == 255 && currentg == 0 && currentr > 0) {
          currentr--;
        } else if (currentr ==0 && currentb == 255  && currentg < 255){
          currentg++;
        } else if (currentr == 0 && currentg == 255 && currentb > 0){
          currentb--;
        } else if (currentg == 255 && currentb == 0 && currentr < 255){
          currentr++;
        } else if (currentr == 255 && currentb == 0 && currentg > 0){
          currentg--;
        }
              Serial.println(currentr);
      Serial.println(currentg);
      Serial.println(currentb);

        setColor(currentr, currentg, currentb);
 
}

void loop() {
  client.loop();

    if (publishSensorsFlag) {
      publishSensors();
      publishSensorsFlag = false;
    }
    
    if (fading) {
      fade(); 
    }

}


