#include <SPI.h>
#include "RCSwitch.h"
#include <Ethernet.h>
#include "PubSubClient.h"
#include <String>
#define AMBILIGHTBLUEEPIN 6
#define AMBILIGHTREDPIN 5
#define AMBILIGHTGREENPIN 3



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
String clientId = "465632-Ambilight";

unsigned int connectCtr = 0;
unsigned int fadeCtr = 0;
double ambilightValue = 1.0;
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
  char buffer[6];
  float val =  (1.0*ambilightHue*(255.0/359.0));
  dtostrf(val,3,2,buffer);
  publish("/devices/"+clientId+"/controls/Color/on", buffer);
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
  
    if (mqttClient.connect(broker)) {
			 publish("/devices/"+clientId+"/controls/Fading/meta/type", "switch");
			 subscribe("/devices/"+clientId+"/controls/meta/Fading/on");

			 publish("/devices/"+clientId+"/controls/Color/meta/type", "range");
       publish("/devices/"+clientId+"/controls/Brightness/meta/max", "360");
			 subscribe("/devices/"+clientId+"/controls/Color/on");

			 publish("/devices/"+clientId+"/controls/Brightness/meta/type", "range");
       publish("/devices/"+clientId+"/controls/Brightness/meta/type", "360");

			 subscribe("/devices/"+clientId+"/controls/Brightness/on");
    }
}


void receive(char* topic, byte* payload, unsigned int length) {  
  String t = String(topic);
  String p = String((char*)payload);
  Serial.println("Received " + t + ":" + p); 

  if (topic == "/devices/"+clientId+"/controls/Brightness/on") {
    int value;
    sscanf(payload, "%d", &value);
    ambilightValue = (1.0*value)/255.0;
    setLedColorHSV(ambilightHue, ambilightValue);
    publish(t.substring(0, t.length()-3), p);

  } else if (topic == "/devices/"+clientId+"/controls/Color/on")) {
    int hue;
    sscanf(payload, "%d", &hue);
    ambilightHue = round(1.0*hue*(359.0/255.0)) ;
    setLedColorHSV(ambilightHue, ambilightValue);
    publish(t.substring(0, t.length()-3), p);

  } else if(topic == "/devices/"+clientId+"/controls/Fading/on")
    fade = p == '1';
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
  char p[payload.length()];
  payload.toCharArray(p, payload.length());
  publish(topic, (uint8_t*)p, payload.length(), true);
}
void publish(String topic, char* payload, int payloadLength) {
   char t[topic.length()];
   topic.toCharArray(t, topic.length());

   mqttClient.publish(t, (uint8_t*)p, payloadLength, true);
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
