
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

#define DEVICE_COMMAND_WILDCARD "/devices/+/controls/+/on"    // Incomming values and commands for device

#define DEVICE_1_POWER_INTERFACE "/devices/321995-ambilight/controls/Power"
#define DEVICE_1_POWER_COMMAND "/devices/321995-ambilight/controls/Power/on"
#define DEVICE_1_POWER_TYPE "/devices/321995-ambilight/controls/Power/type"
#define DEVICE_1_FADING_INTERFACE "/devices/321995-ambilight/controls/Fading"
#define DEVICE_1_FADING_COMMAND "/devices/321995-ambilight/controls/Fading/on"
#define DEVICE_1_FADING_TYPE "/devices/321995-ambilight/controls/Fading/type"
#define DEVICE_1_VALUE_INTERFACE "/devices/321995-ambilight/controls/Brightness"
#define DEVICE_1_VALUE_COMMAND"/devices/321995-ambilight/controls/Brightness/on"
#define DEVICE_1_VALUE_TYPE "/devices/321995-ambilight/controls/Brightness/type"
#define DEVICE_1_HUE_INTERFACE "/devices/321995-ambilight/controls/Color"
#define DEVICE_1_HUE_COMMAND "/devices/321995-ambilight/controls/Color/on"
#define DEVICE_1_HUE_TYPE "/devices/321995-ambilight/controls/Color/type"
#define DEVICE_2_POWER_INTERFACE "/devices/862671-wirelessSwitch/controls/Power"
#define DEVICE_2_POWER_COMMAND "/devices/862671-wirelessSwitch/controls/Power/on"
#define DEVICE_2_POWER_TYPE "/devices/862671-wirelessSwitch/controls/Power/type"
#define DEVICE_3_POWER_INTERFACE "/devices/558426-wirelessSwitch/controls/Power"
#define DEVICE_3_POWER_COMMAND "/devices/558426-wirelessSwitch/controls/Power/on"
#define DEVICE_3_POWER_TYPE "/devices/558426-wirelessSwitch/controls/Power/type"

#define DEVICE_5_POWER_INTERFACE "/devices/E-wirelessSwitch/controls/Power"
#define DEVICE_5_POWER_COMMAND "/devices/E-wirelessSwitch/controls/Power/on"
#define DEVICE_5_POWER_TYPE "/devices/E-wirelessSwitch/controls/Power/type"


#define SENSORS_1_TEMPERATURE_INTERFACE"/devices/482031-sensors/Controls/temp"
#define SENSORS_1_TEMPERATURE_TYPE "/devices/482031-sensors/controls/Temp/type"

// Prototypes
void fadeLoop();
void sensorLoop();
void subscribe();
void publishRetained(char* topic, char* payload);
void publishDeviceMetaInformation();
void mqttReceive(char* topic, byte* rawPayload, unsigned int length);
void setWifi(char* state, char* group, int switchNumber);
void setLedColorHSV(int h, double v);
void setLedColorHSV(int h, double s, double v);
float getTemp();
void setLedColor(int red, int green, int blue);


// Settings 
byte mac[]    = { 0xDE, 0xAD, 0xBE, 0xEF, 0xFF, 0xFF };
byte ip[]     = { 192, 168, 8, 4 };
byte mqttServer[] = { 192, 168, 8, 2 };
char* mqttClientId = "homeduino";
char* wifiSwitchHomeGroup = "11011";

RCSwitch wifiTransmitter = RCSwitch();
EthernetClient ethClient;
PubSubClient client(mqttServer, 1883, mqttReceive, ethClient);
boolean fadeStepFlag = false;
boolean publishSensorsFlag = false;
int fadecounter = 0;
int sensorcounter = 0;
double ambilightValue = 1.0;
int ambilightHue = 359;


void setup() {
  Serial.begin(9600);
  Serial.println("Starting");

  pinMode(AMBILIGHTREDPIN, OUTPUT);
  pinMode(AMBILIGHTGREENPIN, OUTPUT);
  pinMode(AMBILIGHTBLUEEPIN, OUTPUT);
  pinMode(WIFIPIN, OUTPUT);
  
  wifiTransmitter.enableTransmit(WIFIPIN);

  Ethernet.begin(mac, ip);
  if (client.connect(mqttClientId)) {
    subscribe();
    publishDeviceMetaInformation();
  }
}


void loop() {
  // Mqtt loop
  client.loop();

  if (sensorcounter % 65000 == 0) {
    sensorLoop();
    sensorcounter= 0;
  }
    
  if (fadeStepFlag && ((fadecounter % 2000) == 0)) {
    fadeLoop();
    fadecounter = 0;
  }
  
  fadecounter++;
  sensorcounter++;
}

// Continuously Fades values on HSV spectrum
void fadeLoop() {
  ambilightHue = ambilightHue < 359 ? ambilightHue+1 : 0;
  setLedColorHSV(ambilightHue, 1, ambilightValue);
  char buffer[6];
  float val =  (1.0*ambilightHue*(255.0/359.0));
  dtostrf(val,3,2,buffer);
  publishRetained(DEVICE_1_HUE_INTERFACE, buffer);
}

void sensorLoop(){
  char tempStr[5];
  dtostrf(getTemp(),3,2,tempStr);
  publishRetained(SENSORS_1_TEMPERATURE_INTERFACE, tempStr);
}




void subscribe() {
 client.subscribe(DEVICE_COMMAND_WILDCARD);
 //client.subscribe(DEVICE_2_COMMAND_WILDCARD);
 //client.subscribe(DEVICE_3_COMMAND_WILDCARD);   
}


void publishRetained(char* topic, char* payload) {
  client.publish(topic, (uint8_t*)payload, strlen((const char*)payload), true);
}


void publishDeviceMetaInformation() {
 publishRetained(DEVICE_1_POWER_TYPE, "switch");
 publishRetained(DEVICE_1_FADING_TYPE, "switch");
 publishRetained(DEVICE_1_HUE_TYPE, "range");
 publishRetained(DEVICE_1_VALUE_TYPE, "range"); 
 publishRetained(DEVICE_2_POWER_TYPE, "switch");
 publishRetained(DEVICE_3_POWER_TYPE, "switch");
  publishRetained(DEVICE_5_POWER_TYPE, "switch");

 publishRetained(SENSORS_1_TEMPERATURE_TYPE, "text");
}

void mqttReceive(char* topic, byte* rawPayload, unsigned int length) {  
  char  payload[length+1];
  memcpy(payload, rawPayload, length);
  payload[length] = '\0';
  //Serial.print("Received MQTT message:");
  //Serial.println(payload);
   
// TODO: PUBLISH VALUES TO OTHER SIDE OF RAMP
  if (strcmp(topic, DEVICE_1_VALUE_COMMAND) == 0) {
    int value;
    sscanf(payload, "%d", &value);
    ambilightValue = (1.0*value)/255.0;
    setLedColorHSV(ambilightHue, ambilightValue);
    publishRetained(DEVICE_1_VALUE_INTERFACE, payload);

  } else if (strcmp(topic, DEVICE_1_HUE_COMMAND) == 0) {
    int hue;
    sscanf(payload, "%d", &hue);
    ambilightHue = round(1.0*hue*(359.0/255.0)) ;
    setLedColorHSV(ambilightHue, ambilightValue);
    publishRetained(DEVICE_1_HUE_INTERFACE, payload);

  } else if(strcmp(topic, DEVICE_1_POWER_COMMAND) == 0) {
    setWifi((char*)payload, wifiSwitchHomeGroup, 1);
    publishRetained(DEVICE_1_POWER_INTERFACE, payload);
  } else if(strcmp(topic, DEVICE_1_FADING_COMMAND)  == 0 ) {
    fadeStepFlag  = !(*payload == '0');
    publishRetained(DEVICE_1_FADING_INTERFACE, payload);
  } else if(strcmp(topic, DEVICE_2_POWER_COMMAND)  == 0) {
    setWifi((char*)payload, wifiSwitchHomeGroup, 2);
    publishRetained(DEVICE_2_POWER_INTERFACE, payload);
  }else if(strcmp(topic,DEVICE_3_POWER_COMMAND)  == 0) {
    setWifi((char*)payload, wifiSwitchHomeGroup, 3);
    publishRetained(DEVICE_3_POWER_INTERFACE, payload);
  } else if(strcmp(topic,DEVICE_5_POWER_COMMAND)  == 0) {
    setWifi((char*)payload, wifiSwitchHomeGroup, 5);
    publishRetained(DEVICE_5_POWER_INTERFACE, payload);
  }
  
}

void setWifi(char* state, char* group, int switchNumber) {
  if (strcmp(state, "1") == 0) {
    wifiTransmitter.switchOn(group, switchNumber);
      wifiTransmitter.enableTransmit(WIFIPIN);

    delay(150);
    wifiTransmitter.switchOn(group, switchNumber);  
    delay(350);
    wifiTransmitter.switchOn(group, switchNumber);  


  } else if (strcmp(state, "0") == 0)  {  
    wifiTransmitter.switchOff(group, switchNumber); 
    delay(150);
    wifiTransmitter.switchOff(group, switchNumber);  
    delay(350);
    wifiTransmitter.switchOff(group, switchNumber);  

  } 
}

void setLedColor(int red, int green, int blue) {  

  
  
  
  
//  Serial.println("r");
//Serial.println(red);  
  //Serial.println("G");
//Serial.println(green);  
 // Serial.println("B");
//Serial.println(blue);  
    
//  Serial.println("g");  

//Serial.println(green);  
  //Serial.println("b");  
//Serial.println(b);  
  analogWrite(AMBILIGHTREDPIN, red);
  analogWrite(AMBILIGHTGREENPIN, green);
  analogWrite(AMBILIGHTBLUEEPIN, blue);
}

void setLedColorHSV(int h, double v) {
  setLedColorHSV(h,1.0, v);
}


//Convert a given HSV (Hue Saturation Value) to RGB(Red Green Blue) and set the led to the color
//  h is hue value, integer between 0 and 360
//  s is saturation value, double between 0 and 1
//  v is value, double between 0 and 1
//http://splinter.com.au/blog/?p=29
void setLedColorHSV(int h, double s, double v) {
  //this is the algorithm to convert from RGB to HSV
  double r=0; 
  double g=0; 
  double b=0;

  double hf=h/60.0;

  int i=(int)floor(h/60.0);
  double f = h/60.0 - i;
  double pv = v * (1 - s);
  double qv = v * (1 - s*f);
  double tv = v * (1 - s * (1 - f));

  switch (i)
  {
  case 0: //rojo dominante
    r = v;
    g = tv;
    b = pv;
    break;
  case 1: //verde
    r = qv;
    g = v;
    b = pv;
    break;
  case 2: 
    r = pv;
    g = v;
    b = tv;
    break;
  case 3: //azul
    r = pv;
    g = qv;
    b = v;
    break;
  case 4:
    r = tv;
    g = pv;
    b = v;
    break;
  case 5: //rojo
    r = v;
    g = pv;
    b = qv;
    break;
  }

  //set each component to a integer value between 0 and 255
  int red=constrain((int)255*r,0,255);
  int green=constrain((int)255*g,0,255);
  int blue=constrain((int)255*b,0,255);

  setLedColor(red,green,blue);
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
