#include <SPI.h>
#include "RCSwitch.h"
#include <Ethernet.h>
#include "PubSubClient.h"
#include <String>
#define WIFIPIN 9


struct _Sk{String id; String group; int type; struct _Sk* next;};
typedef struct _Sk Socket;

 
// Prototypes
void connect();
void cleanup();
void connect();
void receive(char* topic, byte* payload, unsigned int length);
void subscribe(String topic);
void publish(String topic, String payload);
void setSocket(Socket* socket, String payload);
void removeSocket(String id, String group);
void addSocket(String id, String group, String type);
Socket* getSocket(String id, String group);

// Settings 
byte mac[]    = { 0xDE, 0xAD, 0xBE, 0xEF, 0xFF, 0xFF };
byte broker[] = { 192, 168, 8, 2 };
unsigned int ctr = 0;
Socket* firstSocket = NULL;
Socket* lastSocket = NULL;
String clientId = "158212-Socket";
// Classes
RCSwitch transmitter = RCSwitch();
EthernetClient ethernetClient;
PubSubClient mqttClient = PubSubClient(broker, 1883, receive, ethernetClient);

void loop() {
  mqttClient.loop();  // Mqtt loop
  if (ctr % 65000 == 0) {
    if (!mqttClient.connected())
      connect();
    ctr = 0;
  }
  ctr++;
}


void setup() {
  Serial.begin(9600);
  Serial.println("Starting");
  pinMode(WIFIPIN, OUTPUT);
  transmitter.enableTransmit(WIFIPIN);
  Ethernet.begin(mac);
  connect();
}

void connect() {
  Serial.println("Connecting to mqtt server");
    // clear switches
      Socket *next = firstSocket;
      Socket *tmp;
       while(next) {
         tmp = next;
         next = next->next;
         free(tmp);  
       }
       firstSocket = NULL;
        lastSocket = NULL;
  
    char b[clientId.length()];
    clientId.toCharArray(b, clientId.length());

  
    if (mqttClient.connect(b)) {
      subscribe("/sys/"+clientId+"/#");
    }
}


void receive(char* topic, byte* payload, unsigned int length) {  
  String t = String(topic);
  String p = String((char*)payload);
  Serial.println("Received " + t + ":" + p); 

  //TODO: Parse topic
  String id = String("1");
  String group = String("11011");

  if(t.substring(0, 4) == "/sys") {
    if(p == "")
      removeSocket(id, group);
    else 
      addSocket(id, group, p);
  } else {
      setSocket(getSocket(id, group), p);
      publish(t.substring(0, t.length()-3), p); // Echo back state
  }
}

void publish(String topic, String payload) {
  char t[topic.length()];
  char p[payload.length()];
  
  topic.toCharArray(t, topic.length());
  payload.toCharArray(p, payload.length());

  mqttClient.publish(t, (uint8_t*)p, payload.length(), true);
}
Socket* getSocket(String id, String group) {
  Socket* s = firstSocket;
  while(s != NULL) {
      if(s->id == id && s->group == group)
      {
          return s;
      }
      s = s->next;
  }  
  return NULL;

};

void setSocket(Socket* socket, String payload) {
  // TODO: Add type differentiation
//  if(state == "0")
//    transmitter.switchOff(s->group, s->id);
//  else
//    transmitter.switchOn(s->group, s->id);
}

void addSocket(String id, String group, String type) {
  Socket* socket = getSocket(id, group);
  if(socket != NULL) {
    return;
  } 

  socket = (Socket*) malloc(sizeof(Socket));
  socket->id=id;
  socket->group=group;
  socket->type=0; // TODO: Fixme

  if (firstSocket == NULL) {
    firstSocket = socket;
    lastSocket = socket;
  } else {
    lastSocket->next = socket;
    lastSocket = socket;
  }

  String base = "/devices/"+clientId+"-"+group+"-"+id+"/controls/Power";
  publish(base+"/type","switch");
  subscribe(base+"/on");
}



void removeSocket(String id, String group) {

  // Topic cleaup   
  String base = "/devices/"+clientId+"-"+group+"-"+id;
  unsubscribe(base+"controls/Power/on");
  publish(base+"controls/Power", "");
  publish(base+"controls/Power/on", "");
  publish(base+"meta/room", "");
  publish(base+"meta/name", "");
    

  // Memory cleanup
  // TODO: FIXME
  Socket* socket = getSocket(id, group);  
  Socket* before = firstSocket;    
  while(before->next != NULL && before->next != socket)
    before = before->next;

  if(before->next == socket) {
    before->next = socket->next;    
    if(socket == firstSocket)
      firstSocket =socket->next;
             
    if(socket == lastSocket) 
      lastSocket = before;
  }
  free(socket);    
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
