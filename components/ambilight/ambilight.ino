#include <SPI.h>
#include "RCSwitch.h"
#include <Ethernet.h>
#include "PubSubClient.h" // Included in misc/libraries/arduino/pubsubclient
#define AMBILIGHTBLUEEPIN 6
#define AMBILIGHTREDPIN 5
#define AMBILIGHTGREENPIN 3
#define CLIENTID "465632-Ambilight" 

// Prototypes
void connect();
void cleanup();
void connect();
void receive(char* topic, byte* payload, unsigned int length);
void subscribe(String topic);
void publish(String topic, String payload);
void setLedColorHSV(int h, double v);
void setLedColorHSV(int h, double s, double v);
void setLedColor(int red, int green, int blue);

// Settings 
byte mac[]    = { 0xDE, 0xAD, 0xBE, 0xEF, 0x00, 0x01 };
byte broker[] = { 192, 168, 8, 2 };
String clientId = CLIENTID;

unsigned int connectCtr = 0;
unsigned int fadeCtr = 0;
double ambilightBrightness = 1.0;
int ambilightColor = 359;
double ambilightSaturation = 1.0;

boolean fade = false;

// Classes
EthernetClient ethernetClient;
PubSubClient mqttClient = PubSubClient(broker, 1883, receive, ethernetClient);

void loop() {
  mqttClient.loop();  // Mqtt loop

  if (connectCtr % 65000 == 0) {
    if (!mqttClient.connected())  
      connect();
    connectCtr = 0;
  }

  if (fade && ((fadeCtr % 2000) == 0)) {
    fadeLoop();
    fadeCtr = 0;
  }
  
  fadeCtr++;
  connectCtr++;
}


// Continuously Fades values on HSV spectrum
void fadeLoop() {
  ambilightColor = ambilightColor < 359 ? ambilightColor+1 : 0;
  setLedColorHSV(ambilightColor, ambilightSaturation, ambilightBrightness);  
  publish("/devices/"+clientId+"/controls/Color/on", String(ambilightColor));
}


void setup() {
  Serial.begin(9600);
  Serial.println("Starting");
  pinMode(AMBILIGHTREDPIN, OUTPUT);
  pinMode(AMBILIGHTGREENPIN, OUTPUT);
  pinMode(AMBILIGHTBLUEEPIN, OUTPUT);

  Ethernet.begin(mac);
  connect();
}

void connect() {
  Serial.println("Connecting to mqtt server");
  
    if (mqttClient.connect(CLIENTID)) {
     
			 publish("/devices/"+clientId+"/controls/Fading/meta/type", "switch");
			 subscribe("/devices/"+clientId+"/controls/Fading/on");

			 publish("/devices/"+clientId+"/controls/Color/meta/type", "range");
                         publish("/devices/"+clientId+"/controls/Color/meta/max", "359");
			 subscribe("/devices/"+clientId+"/controls/Color/on");

			 publish("/devices/"+clientId+"/controls/Brightness/meta/type", "range");
			 subscribe("/devices/"+clientId+"/controls/Brightness/on");

			 publish("/devices/"+clientId+"/controls/Saturation/meta/type", "range");
			 subscribe("/devices/"+clientId+"/controls/Saturation/on");
    }
}


void receive(char* topic, byte* rawPayload, unsigned int length) {  
  char  payload[length+1];
  memset(payload, 0, length+1);
  memcpy(payload, rawPayload, length);

  String t = String(topic);
  String p = String((char*)payload);
  //Serial.println("Received " + t + ":" + p); 


 if (t == "/devices/"+clientId+"/controls/Color/on") {
    sscanf((char*)payload, "%d", &ambilightColor);
    Serial.println(ambilightColor);
    setLedColorHSV(ambilightColor, ambilightSaturation, ambilightBrightness);
 } else if (t == "/devices/"+clientId+"/controls/Brightness/on") {    
    int brightness;
    sscanf(payload, "%d", &brightness);
    ambilightBrightness = (1.0*brightness)/255.0;
    setLedColorHSV(ambilightColor, ambilightSaturation, ambilightBrightness);
  } else if (t == "/devices/"+clientId+"/controls/Saturation/on") {   
    int saturation;
    sscanf(payload, "%d", &saturation);
    ambilightSaturation = (1.0*saturation)/255.0;
    setLedColorHSV(ambilightColor, ambilightSaturation, ambilightBrightness);
  } else if(t == "/devices/"+clientId+"/controls/Fading/on") {
    fade = p == "1";
  }
  
  publish(t.substring(0, t.length()-3), p);
}


void setLedColorHSV(int h, double s, double v) {
	//Convert a given HSV (Hue Saturation Value) to RGB(Red Green Blue) and set the led to the color
	//  h is hue value, integer between 0 and 360
	//  s is saturation value, double between 0 and 1
	//  v is value, double between 0 and 1
	//http://splinter.com.au/blog/?p=29

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

  analogWrite(AMBILIGHTREDPIN, constrain((int)255*r,0,255));
  analogWrite(AMBILIGHTGREENPIN, constrain((int)255*g,0,255));
  analogWrite(AMBILIGHTBLUEEPIN, constrain((int)255*b,0,255));
}


void publish(String topic, String payload) {
  char p[payload.length()+1];
  memset(p, 0, payload.length()+1);
  payload.toCharArray(p, payload.length()+1);
  publish(topic, p);
}
void publish(String topic, char* payload) {
   char t[topic.length()+1];
   memset(t, 0, topic.length()+1);
   topic.toCharArray(t, topic.length()+1);
   mqttClient.publish(t, (uint8_t*)payload, strlen(payload), true);
} 

void subscribe(String topic) {
  char t[topic.length()+1];
  memset(t, 0, topic.length()+1);
  topic.toCharArray(t, topic.length()+1);
  mqttClient.subscribe(t);
}
