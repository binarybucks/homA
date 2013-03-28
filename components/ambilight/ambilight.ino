#include <SPI.h>
#include "RCSwitch.h"
#include <Ethernet.h>
#include "PubSubClient.h"
#include <String>
#define AMBILIGHTBLUEEPIN 6
#define AMBILIGHTREDPIN 5
#define AMBILIGHTGREENPIN 3
#define CLIENTID "465632-Ambilight" 


struct _Sk{String id; String group; int type; struct _Sk* next;};
typedef struct _Sk Socket;

 
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
int ambilightValue = 1;
int ambilightHue = 359;
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
  ambilightHue = ambilightHue < 359 ? ambilightHue+1 : 0;
  setLedColorHSV(ambilightHue, 1, ambilightValue);  
  publish("/devices/"+clientId+"/controls/Color/on", String(ambilightHue));
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
			 subscribe("/devices/"+clientId+"/controls/meta/Fading/on");

			 publish("/devices/"+clientId+"/controls/Color/meta/type", "range");
                         publish("/devices/"+clientId+"/controls/Brightness/meta/max", "359");
			 subscribe("/devices/"+clientId+"/controls/Color/on");

			 publish("/devices/"+clientId+"/controls/Brightness/meta/type", "range");
                         publish("/devices/"+clientId+"/controls/Brightness/meta/type", "359");
			 subscribe("/devices/"+clientId+"/controls/Brightness/on");
    }
}


void receive(char* topic, byte* payload, unsigned int length) {  
  String t = String(topic);
  String p = String((char*)payload);
  Serial.println("Received " + t + ":" + p); 

  if (t == "/devices/"+clientId+"/controls/Brightness/on") {
    sscanf((char*)payload, "%d", &ambilightValue);
    setLedColorHSV(ambilightHue, ambilightValue);
    publish(t.substring(0, t.length()-3), p);

  } else if (t == "/devices/"+clientId+"/controls/Color/on") {
    sscanf((char*)payload, "%d", &ambilightHue);
    setLedColorHSV(ambilightHue, ambilightValue);
    publish(t.substring(0, t.length()-3), p);
  } else if(t == "/devices/"+clientId+"/controls/Fading/on") {
    fade = p == "1";
    publish(t.substring(0, t.length()-3), p);
  }
}


void setLedColor(int red, int green, int blue) {  
  analogWrite(AMBILIGHTREDPIN, red);
  analogWrite(AMBILIGHTGREENPIN, green);
  analogWrite(AMBILIGHTBLUEEPIN, blue);
}

void setLedColorHSV(int h, double v) {
  setLedColorHSV(h,1.0, v);
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

  //set each component to a integer value between 0 and 255
  int red=constrain((int)255*r,0,255);
  int green=constrain((int)255*g,0,255);
  int blue=constrain((int)255*b,0,255);

  setLedColor(red,green,blue);
}


void publish(String topic, String payload) {
  char p[payload.length()+1];
  payload.toCharArray(p, payload.length()+1);
  publish(topic, p);
}
void publish(String topic, char* payload) {
   char t[topic.length()+1];
   topic.toCharArray(t, topic.length()+1);

   mqttClient.publish(t, (uint8_t*)payload, strlen(payload), true);
} 

void subscribe(String topic) {
  char t[topic.length()];
  topic.toCharArray(t, topic.length());
  mqttClient.subscribe(t);
}

void unsubscribe(String topic) {
  char t[topic.length()];
  topic.toCharArray(t, topic.length());
  mqttClient.unsubscribe(t);
}
