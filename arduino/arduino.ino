
#include "RCSwitch.h"
#include <SPI.h>
#include <Ethernet.h>
#include "PubSubClient.h"
#include <math.h>

#define WIFIPIN 9

#define AMBILIGHTBLUEEPIN 6
#define AMBILIGHTREDPIN 5
#define AMBILIGHTGREENPIN 3
#define THERMISTORPIN A0       
#define THERMISTORNOMINAL 2200  // resistance at 25 degrees C
#define TEMPERATURENOMINAL 25   // temp. for nominal resistance (almost always 25 C)
#define BCOEFFICIENT 4791       // The beta coefficient of the thermistor (usually 3000-4000)
#define SERIESRESISTOR 2200     // the value of the 'other' resistor


#define DEVICE_1_SUBSCRIBE"/devices/321995-ambilight/controls/#"
#define DEVICE_1_POWER "/devices/321995-ambilight/controls/power"
#define DEVICE_1_POWER_TYPE "/devices/321995-ambilight/controls/power/type"
#define DEVICE_1_FADING "/devices/321995-ambilight/controls/fading"
#define DEVICE_1_FADING_TYPE "/devices/321995-ambilight/controls/fading/type"
#define DEVICE_1_COLOR "/devices/321995-ambilight/controls/color"
#define DEVICE_1_COLOR_TYPE "/devices/321995-ambilight/controls/color/type"

#define DEVICE_2_SUBSCRIBE "/devices/862671-wirelessSwitch/controls/#"
#define DEVICE_2_POWER "/devices/862671-wirelessSwitch/controls/power"
#define DEVICE_2_POWER_TYPE "/devices/862671-wirelessSwitch/controls/power/type"

#define DEVICE_3_SUBSCRIBE "/devices/558426-wirelessSwitch/controls/#"
#define DEVICE_3_POWER "/devices/558426-wirelessSwitch/controls/power"
#define DEVICE_3_POWER_TYPE "/devices/558426-wirelessSwitch/controls/power/type"

#define SENSORS_1_TEMPERATURE "/devices/482031-sensors/controls/temp"
#define SENSORS_1_TEMPERATURE_TYPE "/devices/482031-sensors/controls/temp/type"

void subscribe();
void publishDeviceMetaInformation();

union u_color { 
	// first representation (member of union) 
	struct s_color { 
		uint8_t r, g, b;
	} rgb;
 
	// second representation (member of union) 
	uint32_t hex; 
};


// Prototypes

void setWifi(char* state, char* group, int switchNumber);
void setColor();
float getTemp();
void mqttReceive(char* topic, byte* payload, unsigned int length);
void publishRetained(char* topic, char* payload);



// Settings 
byte mac[]    = { 0xDE, 0xAD, 0xBE, 0xEF, 0xFF, 0xFF };
byte ip[]     = { 192, 168, 8, 4 };
byte mqttServer[] = { 192, 168, 8, 2 };
char* mqttClientId = "homeduino";
char* wifiSwitchHomeGroup = "11011";


int fadecounter = 0;
int sensorcounter = 0;
int currentr = 0;
int currentg = 255;
int currentb = 0;


// Global variables
RCSwitch wifiTransmitter = RCSwitch();
boolean publishSensorsFlag = false;
boolean fadeStepFlag = false;
boolean fading = false;
EthernetClient ethClient;
PubSubClient client(mqttServer, 1883, mqttReceive, ethClient);




void setWifi(char* state, char* group, int switchNumber) {
Serial.println("setting wifi switch");  
  if (strcmp(state, "1") == 0) {
    wifiTransmitter.switchOn(group, switchNumber);
    delay(500);
    wifiTransmitter.switchOn(group, switchNumber);  

  } else if (strcmp(state, "0") == 0)  {  
    wifiTransmitter.switchOff(group, switchNumber); 
    delay(500);
    wifiTransmitter.switchOff(group, switchNumber); 
  } 
}

void setColor() {    
  analogWrite(AMBILIGHTREDPIN, currentr);
  analogWrite(AMBILIGHTGREENPIN, currentg);
  analogWrite(AMBILIGHTBLUEEPIN, currentb);

}

void publishColor() {
  char buffer[6];
  sprintf(buffer, "%.2X%.2X%.2X", currentr, currentg, currentb);
  Serial.println("hex is: ");
  Serial.println(buffer);
  publishRetained("/devices/321995-ambilight/controls/color", buffer);
}

void fadeStep () {


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
  publishColor();
  setColor();
}


float getTemp() {
  float temp;
  temp = analogRead(THERMISTORPIN); 
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



//
//
// MQTT 
//
//

void mqttReceive(char* topic, byte* rawPayload, unsigned int length) {  
  char  payload[length+1];
  memcpy(payload, rawPayload, length);
  payload[length] = '\0';
  
  if(strcmp(topic, DEVICE_1_COLOR) == 0) {
    sscanf(payload, "%2x", &currentr);
    sscanf(&payload[2], "%2x", &currentg);
    sscanf(&payload[4], "%2x", &currentb);
    setColor();
  } else if(strcmp(topic, DEVICE_1_POWER) == 0) {
    setWifi((char*)payload, wifiSwitchHomeGroup, 1);
  } else if(strcmp(topic, DEVICE_1_FADING)  == 0 ) {
    fading  = !(*payload == '0');
  } else if(strcmp(topic, DEVICE_2_POWER)  == 0) {
    setWifi((char*)payload, wifiSwitchHomeGroup, 2);
  }else if(strcmp(topic,DEVICE_3_POWER)  == 0) {
    setWifi((char*)payload, wifiSwitchHomeGroup, 3);
  }
}

void publishRetained(char* topic, char* payload) {
//  Serial.print("tx t: ");
//  Serial.print(topic);
//  Serial.print(", tx p: ");
//  Serial.println((char*)payload);
//  Serial.println("");

  client.publish(topic, (uint8_t*)payload, strlen((const char*)payload), true);
}



//
//
// Init
//
//



void setup() {
  Serial.begin(9600);
  Serial.println("starting");

  pinMode(AMBILIGHTREDPIN, OUTPUT);
  pinMode(AMBILIGHTGREENPIN, OUTPUT);
  pinMode(AMBILIGHTBLUEEPIN, OUTPUT);
  pinMode(WIFIPIN, OUTPUT);
  
//  setColor();

  Ethernet.begin(mac, ip);
  if (client.connect(mqttClientId)) {
    subscribe();
    publishDeviceMetaInformation();
  }

  wifiTransmitter.enableTransmit(WIFIPIN);
  setupInterrupts();
}


void setupInterrupts() {
      // initialize Timer1
      cli();             // disable global interrupts
      TCCR1A = 0;        // set entire TCCR1A register to 0
      TCCR1B = 0;
   
      // Overflow interrupt with CLK/1024 Prescaler (~4s) for Sensor readings
      TIMSK1 |= (1 << TOIE1); 
      TCCR1B |= (1 << CS10);
      TCCR1B |= (1 << CS12);
      
      // enable global interrupts:
      sei();
}

ISR(TIMER1_OVF_vect)
{

  publishSensorsFlag = true;
}


void loop() {
  client.loop();

  if (sensorcounter % 65000 == 0) {
    publishSensors();
    sensorcounter= 0;
  }
    
  if (fading && ((fadecounter % 1000) == 0)) {
    fadeStep(); 
    fadecounter = 0;
  }
  
  fadecounter++;
  sensorcounter++;
}



void publishDeviceMetaInformation() {
 publishRetained(DEVICE_1_POWER_TYPE, "switch");
 publishRetained(DEVICE_1_FADING_TYPE, "switch");
 publishRetained(DEVICE_1_COLOR_TYPE, "range");
 publishRetained(DEVICE_2_POWER_TYPE, "switch");
 publishRetained(DEVICE_3_POWER_TYPE, "switch");
 publishRetained(SENSORS_1_TEMPERATURE_TYPE, "text");
}

void subscribe() {
 client.subscribe(DEVICE_1_SUBSCRIBE);
 client.subscribe(DEVICE_2_SUBSCRIBE);
 client.subscribe(DEVICE_3_SUBSCRIBE);  
}

void publishSensors () {
    char tempStr[5];
    dtostrf(getTemp(),2,2,tempStr);
    publishRetained(SENSORS_1_TEMPERATURE, tempStr);
}

