
#include "RCSwitch.h"
#include <SPI.h>
#include <Ethernet.h>
#include "PubSubClient.h"
#include <math.h>

#define WIFIPIN 9
char CLIENT_ID[20] = "158293-433MhzBridge";
char SWITCH_ID[20] = "158293-433MhzSwitch";


struct _Ws{int number; char group[5+1]; struct _Ws* next;};
typedef struct _Ws Ws ;

void subscribe();
void publishRetained(char* topic, char* payload);
void mqttReceive(char* topic, byte* rawPayload, unsigned int length);
void setWifi(Ws* s, char* state);
void addWifiSwitch(char* topic, char* payload);
void listSwitches();
int switchNumberFromTopic(char* topic);
void connectToMqttServer();

Ws* findSwitch(int id);

// Settings 
byte mac[]    = { 0xDE, 0xAD, 0xBE, 0xEF, 0xFF, 0xFF };
byte ip[]     = { 192, 168, 8, 4 };
byte mqttServer[] = { 192, 168, 8, 2 };
Ws* firstSwitch = NULL;

RCSwitch wifiTransmitter = RCSwitch();
EthernetClient ethClient;
PubSubClient client(mqttServer, 1883, mqttReceive, ethClient);

unsigned int connCtr = 0;


void setup() {
  Serial.begin(9600);
  Serial.println("starting");
  pinMode(WIFIPIN, OUTPUT);
  wifiTransmitter.enableTransmit(WIFIPIN);

  Ethernet.begin(mac, ip);
   connectToMqttServer();
}


void connectToMqttServer() {
  Serial.println("Connecting to mqtt server");
    // clear switches
      Ws *next = firstSwitch;
      Ws *tmp;
       while(next) {
         tmp = next;
         next = next->next;
         free(tmp);  
       }
       firstSwitch = NULL;
   
  
  
    if (client.connect(CLIENT_ID)) {
    int s = sizeof(CLIENT_ID)/sizeof(char)+5+2;
    char sys[s];
    snprintf(sys, s, "/sys/%s/#",CLIENT_ID);
    client.subscribe(sys);
  }
}


void loop() {
  client.loop();  // Mqtt loop
  if (connCtr % 65000 == 0) {
    if (!client.connected()) {
      Serial.println("not connected");
connectToMqttServer();
}
    
    connCtr = 0;
  }
  connCtr++;
  
}

void publishRetained(char* topic, char* payload) {
  client.publish(topic, (uint8_t*)payload, strlen((const char*)payload), true);
}


void mqttReceive(char* topic, byte* rawPayload, unsigned int length) {  
  char  payload[length+1];
  memcpy(payload, rawPayload, length);
  payload[length] = '\0';
  
  //char * splitTopic = strtok(topic, "/");
  Serial.println("Received something"); 
  Serial.println(topic); 
  Serial.println(payload); 

  if (strncmp(topic,"/sys", 4) == 0) { // Cheap parsing here since there is just one function for /sys on this device
  
    if((strcmp(payload, "") == 0) || (rawPayload == NULL)) {
      removeWifiSwitch(topic, payload);
      return;    
    }
  
    addWifiSwitch(topic, payload);
    return;  
  } 

  if (strncmp(topic+9+sizeof(SWITCH_ID)/sizeof(char)-1+1+2,"/controls/Power/on", 18) == 0) { 
   int switchNumber;
   sscanf(topic+9+sizeof(SWITCH_ID)/sizeof(char)-1, "-%d/controls/Power/on", &switchNumber);
  
    if (switchNumber && switchNumber < 100) {
      Ws* wifiSwitch = findSwitch(switchNumber);
      setWifi(wifiSwitch, payload);
      
      
     char t[48+1];
     snprintf(t, 48,"/devices/%s-%02d/controls/Power", SWITCH_ID, switchNumber);
     t[48]= '\0';
     
     publishRetained(t, payload);


    }
  }
  
}

void removeWifiSwitch(char* topic, char* payload) {
  int switchNumber = switchNumberFromTopic(topic);
  Ws *targetSwitch = findSwitch(switchNumber);

  Serial.println("Removing switch with id:");
  Serial.println(switchNumber);

  if (targetSwitch != NULL) {
     
     char t[52];
     snprintf(t, 51,"/devices/%s-%02d/controls/Power/type", SWITCH_ID, switchNumber);
     publishRetained(t, "");

     memset(t, NULL, 52);

     snprintf(t, 51,"/devices/%s-%02d/controls/Power", SWITCH_ID, switchNumber);
     publishRetained(t, "");

     memset(t, NULL, 52);
  
     snprintf(t, 51,"/devices/%s-%02d/meta/room", SWITCH_ID, switchNumber);
     publishRetained(t, "");

     memset(t, NULL, 52);

     snprintf(t, 51,"/devices/%s-%02d/meta/name", SWITCH_ID, switchNumber);
     publishRetained(t, "");


    if (targetSwitch == firstSwitch) {
      firstSwitch = targetSwitch->next;      
    } else {
      Ws* before = firstSwitch;    
      while(before->next != NULL && before->next != targetSwitch)
      {before = before->next;}        
      before->next = targetSwitch->next;      
    }
    free(targetSwitch);
    
  }
}


int switchNumberFromTopic(char* topic) {
   int switchNumber;
   sscanf(topic+sizeof(CLIENT_ID)/sizeof(char)+5+15, "%d", &switchNumber);
   return switchNumber;
}

void addWifiSwitch(char* topic, char* payload) {
   //Serial.println("Adding new device"); 
   //Serial.println(topic); 
   //Serial.println(payload); 
   
   // Strip /sys/158293-433MhzBridge/devices/Switch- from string to obtain switch number   
   int switchNumber = switchNumberFromTopic(topic);
   if (switchNumber > 99) {
      return; 
   }

   Ws *newSwitch = findSwitch(switchNumber);
   if (newSwitch == NULL) {
    
     newSwitch = (Ws*)malloc(sizeof(Ws));
     newSwitch -> number = switchNumber;
     newSwitch->next  = NULL;   
     strncpy(newSwitch->group, payload, 5);
     newSwitch->group[5] = '\0';
     
     if (firstSwitch == NULL) {
       firstSwitch = newSwitch;
     } else {
       Ws *last = firstSwitch;
       while(last->next != NULL) {
         last = last->next;  
       }
   
       last->next = newSwitch; 
     }

     
     char topic[53+1];
     snprintf(topic, 53,"/devices/%s-%02d/controls/Power/type", SWITCH_ID, switchNumber);
     topic[53]= '\0';
     
     publishRetained(topic, "switch");

     char sub[50+1];  
     snprintf(sub, 50,"/devices/%s-%02d/controls/Power/on", SWITCH_ID, switchNumber);
     //Serial.println(sub);
     sub[50]= '\0';
  
     client.subscribe(sub);
  
   }  
   
   //listSwitches();
   
}

Ws* findSwitch(int id) {
    Ws* ptr = firstSwitch;
    while(ptr != NULL)
    {
        if(ptr->number == id)
        {
            return ptr;
        }
        ptr = ptr->next;
    }  
    return ptr;
}

void listSwitches() {
     Ws* ptr = firstSwitch;
    while(ptr != NULL)
    {
        //Serial.println("Switch:");
        //Serial.println(ptr->number);
        //Serial.println(ptr->group);
        ptr = ptr->next;

    } 
}





void setWifi(Ws* s, char* state) {
  if (strcmp(state, "1") == 0) {
    wifiTransmitter.switchOn(s->group, s->number);
  } else if (strcmp(state, "0") == 0)  {  
    wifiTransmitter.switchOff(s->group, s->number); 
  } 
}


