#include <SPI.h>
#include "RCSwitch.h"
#include <Ethernet.h>
#include "PubSubClient.h" // Included in misc/libraries/arduino/pubsubclient

#define WIFIPIN 9
#define CLIENTID "158212-Socket"

struct _Sk{char id[5+1]; char group[5+1]; int type; struct _Sk* next;};
typedef struct _Sk Socket;

 
// Prototypes
void connect();
void cleanup();
void connect();
void receive(char topic, byte* rawPayload, unsigned int length);
void subscribe(char* topic);
void publish(char* topic, char* payload);
void setSocket(Socket* socket, char* payload);
void removeSocket(char* id, char* group);
void addSocket(char* id, char* group, int type);
Socket* getSocket(char* id, char* group);

// Settings 
byte mac[]    = { 0xDE, 0xAD, 0xBE, 0xEF, 0xFF, 0x02 };
byte broker[] = { 192, 168, 8, 2 };
unsigned int connectCtr = 0;
Socket* firstSocket = NULL; 
Socket* lastSocket = NULL;
char* clientId = CLIENTID; 
int clientIdSize = strlen(clientId);

RCSwitch transmitter = RCSwitch();
EthernetClient ethernetClient;
PubSubClient mqttClient = PubSubClient(broker, 1883, receive, ethernetClient);


void loop() {
  mqttClient.loop();  // Mqtt loop
  if (connectCtr % 65000 == 0) {
    if (!mqttClient.connected())
      connect();
    connectCtr = 0;
  }
  connectCtr++;
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
  
  if (mqttClient.connect(CLIENTID)) {
      char buffer[clientIdSize+9];
      snprintf(buffer, clientIdSize+9, "/sys/%s/#",clientId);
      mqttClient.subscribe(buffer);
  }
}


void receive(char* topic, byte* rawPayload, unsigned int length) {  
  // 0 Terminate payload
  char  payload[length+1];
  memset(payload, 0, length+1);
  memcpy(payload, rawPayload, length); 
  
  Serial.print("Received: ");
  Serial.print(topic);
  Serial.print(":");
  Serial.println(payload);
  // T is: /sys/158212-Socket/11011-01000:typeA
  //      /devices/158212-Socket-11011-01000/controls/Power/on:1
  // Info starts is the beginning of: 11011-01000
  // Delimiter is the beginning of 01000
  char id[5+1];
  char  group[5+1];
  memset(id, 0, 6);
  memset(group, 0, 6);

  char* infoStarts = strstr(topic, clientId)+clientIdSize+1;
  char* delimiter = strrchr(infoStarts, '-');
  
  
  if(strncmp(topic,"/sys", 4) == 0) {
    strncpy(group, infoStarts, delimiter-infoStarts);
    strcpy(id, delimiter+1);

    Serial.print("ID: ");
    Serial.println(id);
    Serial.print("G: ");
    Serial.println(group);
    if((strcmp(topic, "") == 0) || (topic == NULL))
      removeSocket(id, group);
    else 
     addSocket(id, group, payload);
  } else {
    char* infoEnd = strstr(delimiter, "/");
    strncpy( group, infoStarts, delimiter-infoStarts);
    strncpy(id, delimiter+1,  infoEnd-(delimiter+1));

    Serial.print("ID: ");
    Serial.println(id);
    Serial.print("G: ");
    Serial.println(group);
    setSocket(getSocket(id, group), payload);
    
    // Echo back state
    char buffer[strlen(topic)];
    memset(buffer, 0, strlen(topic));
    snprintf(buffer, strlen(topic),"/devices/%s-%s-%s/controls/Power", clientId, group, id);
    publish(buffer, payload); 
  }
}

void publish(char* topic, char* payload) {
  Serial.println(strlen((const char*)payload));
    mqttClient.publish(topic, (uint8_t*)payload, strlen((const char*)payload), true);
}


Socket* getSocket(char* id, char* group) {
  Serial.println("--getSocket");

  Socket* s = firstSocket;
  while(s != NULL) {
      if(strcmp(s->id, id) == 0 && strcmp(s->group, group) == 0)
      {
          Serial.println("  found");
          return s;
      }
      s = s->next;
  }  
      Serial.println("  not found");

  return NULL;

};






void addSocket(char* id, char*  group, char* type) {
  Socket* socket = getSocket(id, group);

  if(socket != NULL) {
    Serial.println("  already there");
    return;
  } 

  socket = (Socket*) malloc(sizeof(struct _Sk));
  memset(socket, 0, sizeof(struct _Sk));
  strncpy(socket->group, group, 6);
  strncpy(socket->id, id, 6);

  if(strcmp(type, "typeB")) {
    socket->type = 2; 
  } else {
    socket->type = 1;
  } 
  socket->type=0; // TODO: Fixme
  socket->next = NULL;
  
  if (firstSocket == NULL) {
    Serial.println("  First one");
    firstSocket = socket;
    lastSocket = socket;
  } else {
    Serial.println("  Adding at back");

    lastSocket->next = socket;
    lastSocket = socket;
  }


  char buffer[9+clientIdSize+1+strlen(group)+1+strlen(id)+26+1];
 memset(buffer, 0, 9+clientIdSize+1+strlen(group)+1+strlen(id)+26+1);

  snprintf(buffer, 9+clientIdSize+1+strlen(group)+1+strlen(id)+26,"/devices/%s-%s-%s/controls/Power/meta/type", clientId, group, id);
  Serial.print("  Publishing meta:");
  Serial.println(buffer);
  publish(buffer, "switch");

  memset(buffer, 0, 9+clientIdSize+1+strlen(group)+1+strlen(id)+26+1);
  
  snprintf(buffer, 9+clientIdSize+1+strlen(group)+1+strlen(id)+19 ,"/devices/%s-%s-%s/controls/Power/on", clientId, group, id);
  Serial.print("  Subscribing to: ");
  Serial.println(buffer);
  mqttClient.subscribe(buffer);
  

}



void removeSocket(char* id, char* group) {
// TODO
//  //Topic cleaup   
//  String base = "/devices/"+clientId+"-"+group+"-"+id;
//  unsubscribe(base+"controls/Power/on");
//  publish(base+"controls/Power", "");
//  publish(base+"controls/Power/on", "");
//  publish(base+"meta/room", "");
//  publish(base+"meta/name", "");
    

//  // Memory cleanup
//  // TODO: FIXME
//  Socket* socket = getSocket(id, group);  
//  if(socket != NULL) {
//    socket->id = socket->next->id;
//    socket->group = socket->next->group;
//    socket->type = socket->next->type;
        
//   Socket* tmp = socket->next->next;
//   free(socket->next);    
//   node->Next = temp;

//   before = before->next;

//  if(before->next == socket) {
//    before->next = socket->next;    
//    if(socket == firstSocket)
//      firstSocket =socket->next;
             
//    if(socket == lastSocket) 
//      lastSocket = before;
  
//  }
}

void setSocket(Socket* socket, char* payload) {
  if(socket == NULL)
      return;

  if(strcmp(payload, "0") == 0) {// Switch off
    switch(socket->type) {
      case 2: // TypeB off
        transmitter.switchOff(atoi(socket->group), atoi(socket->id));
        return;
      default: // TypeA off
        transmitter.switchOff(socket->group, socket->id);
        return;
    } 
  } else {// Switch On
     switch(socket->type) {
      case 2:   // TypeB on
        transmitter.switchOn(atoi(socket->group), atoi(socket->id));
        return;
      default: // TypeA on
        transmitter.switchOn(socket->group, socket->id);
        return;
    } 
  }
}



