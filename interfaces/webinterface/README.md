# HomA - Webinterface
This provides a webinterface for components that integrate into the HomA framework. 

### Installation
As the interface relies entirely on client-side Javascript no webserver installation is required. 
It can, however, be delivered by a webserver in the same manner than any other html page. 

The only required component is a websocket server that translates the interface's websocket to something the broker can understand. A compatible websocket server can be started by running ``` websocketServer/./WSS_Static_ARCH [--brokerHost localhost] [--brokerPort 1883]```


### Start
Open the ```index.html``` with a browser of your choice and enter the adress of the websocket server in the interface settings. Settings are saved locally in the browser. 

The browser establishes an own connection to the websocketServer that relays it to the broker. Thus, no security problem arises even when the interface is exposed to the internet, as long as the websocket server cannot be reached. 
