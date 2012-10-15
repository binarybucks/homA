/* Mosquitto MQTT Javascript/Websocket client */
/* Provides complete support for QoS 0. 
 * Will not cause an error on QoS 1/2 packets.
 */

var CONNECT = 0x10;
var CONNACK = 0x20;
var PUBLISH = 0x30;
var PUBACK = 0x40;
var PUBREC = 0x50;
var PUBREL = 0x60;
var PUBCOMP = 0x70;
var SUBSCRIBE = 0x80;
var SUBACK = 0x90;
var UNSUBSCRIBE = 0xA0;
var UNSUBACK = 0xB0;
var PINGREQ = 0xC0;
var PINGRESP = 0xD0;
var DISCONNECT = 0xE0;

function AB2S(buffer) {
	var binary = '';
	var bytes = new Uint8Array(buffer);
	var len = bytes.byteLength;
	for(var i=0; i<len; i++){
		binary += String.fromCharCode(bytes[i]);
	}
	return binary;
}

function Mosquitto()
{
	this.ws = null;
	this.onconnect = null;
	this.ondisconnect = null;
	this.onmessage = null;
}

Mosquitto.prototype = {
	mqtt_ping : function()
	{
		var buffer = new ArrayBuffer(2);
		var i8V = new Int8Array(buffer);
		i8V[0] = PINGREQ;
		i8V[1] = 0;
		if(this.ws.readyState == 1){
			this.ws.send(buffer);
		}else{
			this.queue(buffer);
		}
		setTimeout(function(_this){_this.mqtt_ping();}, 60000, this);
	},

	connect : function(url, keepalive){

		this.url = url;
		this.keepalive = keepalive;
		this.mid = 1;
		this.out_queue = new Array();

		this.ws = new WebSocket(url, 'mqtt');
		this.ws.binaryType = "arraybuffer";
		this.ws.onopen = this.ws_onopen;
		this.ws.onclose = this.ws_onclose;
		this.ws.onmessage = this.ws_onmessage;
		this.ws.m = this;
		this.ws.onerror = function(evt){
			alert(evt.data);
		}
	},

	disconnect : function(){
		if(this.ws.readyState == 1){
			var buffer = new ArrayBuffer(2);
			var i8V = new Int8Array(buffer);

			i8V[0] = DISCONNECT;
			i8V[1] = 0;
			this.ws.send(buffer);
			this.ws.close();
		}
	},

	ws_onopen : function(evt) {
		var buffer = new ArrayBuffer(1+1+12+2+20);
		var i8V = new Int8Array(buffer);

		i=0;
		i8V[i++] = CONNECT;
		i8V[i++] = 12+2+20;
		i8V[i++] = 0;
		i8V[i++] = 6;
		str = "MQIsdp";
		for(var j=0; j<str.length; j++){
			i8V[i++] = str.charCodeAt(j);
		}
		i8V[i++] = 3;
		i8V[i++] = 2;
		i8V[i++] = 0;
		i8V[i++] = 60;
		i8V[i++] = 0;
		i8V[i++] = 20;
		var str = "mjsws/";
		for(var j=0; j<str.length; j++){
			i8V[i++] = str.charCodeAt(j);
		}
		var chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		for(var j=0; j<14; j++){
			i8V[i++] = chars.charCodeAt(Math.floor(Math.random()*chars.length));
		}

		this.send(buffer);
		while(this.m.out_queue.length > 0){
			this.send(this.m.out_queue.pop());
		}
		setTimeout(function(_this){_this.mqtt_ping();}, 60000, this.m);
	},

	ws_onclose : function(evt) {
		if(this.m.ondisconnect){
			this.m.ondisconnect(evt.data);
		}
	},

	ws_onmessage : function(evt) {
		var i8V = new Int8Array(evt.data);
		buffer = evt.data;
		var q=0;
		while(i8V.length > 0 && q < 1000){
			q++;
			switch(i8V[0] & 0xF0){
				case CONNACK:
					var rl = i8V[1];
					var rc = i8V[2];
					if(this.m.onconnect){
						this.m.onconnect(rc);
					}
					buffer = buffer.slice(rl+2);
					i8V = new Int8Array(buffer);
					break;
				case PUBLISH:
					var i=1;
					var mult = 1;
					var rl = 0;
					var count = 0;
					var digit;
					var qos = (i8V[0] & 0x06) >> 1;
					var retain = (i8V[0] & 0x01);
					var mid = 0;
					do{
						count++;
						digit = i8V[i++];
						rl += (digit & 127)*mult;
						mult *= 128;
					}while((digit & 128) != 0);

					var topiclen = i8V[i++]*256 + i8V[i++];
					var atopic = buffer.slice(i, i+topiclen);
					i+=topiclen;
					var topic = AB2S(atopic);
					if(qos > 0){
						mid = i8V[i++]*256 + i8V[i++];
					}
					var apayload = buffer.slice(i, rl+count+1);
					var payload = AB2S(apayload);

					buffer = buffer.slice(rl+1+count);
					i8V = new Int8Array(buffer);

					if(this.m.onmessage){
						this.m.onmessage(topic, payload, qos, retain);
					}
					break;
				case PUBREC:
				case PUBREL:
				case PUBACK:
				case PUBCOMP:
				case SUBACK:
				case UNSUBACK:
				case PINGRESP:
					var rl = i8V[1];
					buffer = buffer.slice(rl+2);
					i8V = new Int8Array(buffer);
					break;
			}
		}
	},

	get_remaining_count : function(remaining_length)
	{
		if(remaining_length >= 0 && remaining_length < 128){
			return 1;
		}else if(remaining_length >= 128 && remaining_length < 16384){
			return 2;
		}else if(remaining_length >= 16384 && remaining_length < 2097152){
			return 3;
		}else if(remaining_length >= 2097152 && remaining_length < 268435456){
			return 4;
		}else{
			return -1;
		}
	},

	generate_mid : function()
	{
		var mid = this.mid;
		this.mid++;
		if(this.mid == 256) this.mid = 0;
		return mid;
	},

	queue : function(buffer)
	{
		this.out_queue.push(buffer);
	},

	send_cmd_with_mid : function(cmd, mid)
	{
		var buffer = new ArrayBuffer(4);
		var i8V = new Int8Array(buffer);
		i8V[0] = cmd;
		i8V[1] = 2;
		i8V[2] = mid%128;
		i8V[3] = mid/128;
		if(this.ws.readyState == 1){
			this.ws.send(buffer);
		}else{
			this.queue(buffer);
		}
	},

	unsubscribe : function(topic)
	{
		var rl = 2+2+topic.length;
		var remaining_count = this.get_remaining_count(rl);
		var buffer = new ArrayBuffer(1+remaining_count+rl);
		var i8V = new Int8Array(buffer);

		var i=0;
		i8V[i++] = UNSUBSCRIBE | 0x02;
		do{
			digit = Math.floor(rl % 128);
			rl = Math.floor(rl / 128);
			if(rl > 0){
				digit = digit | 0x80;
			}
			i8V[i++] = digit;
		}while(rl > 0);
		i8V[i++] = 0;
		i8V[i++] = this.generate_mid();
		i8V[i++] = 0;
		i8V[i++] = topic.length;
		for(var j=0; j<topic.length; j++){
			i8V[i++] = topic.charCodeAt(j);
		}

		if(this.ws.readyState == 1){
			this.ws.send(buffer);
		}else{
			this.queue(buffer);
		}
	},

	subscribe : function(topic, qos)
	{
		if(qos != 0){
			return 1;
		}
		var rl = 2+2+topic.length+1;
		var remaining_count = this.get_remaining_count(rl);
		var buffer = new ArrayBuffer(1+remaining_count+rl);
		var i8V = new Int8Array(buffer);

		var i=0;
		i8V[i++] = SUBSCRIBE | 0x02;
		do{
			digit = Math.floor(rl % 128);
			rl = Math.floor(rl / 128);
			if(rl > 0){
				digit = digit | 0x80;
			}
			i8V[i++] = digit;
		}while(rl > 0);
		i8V[i++] = 0;
		i8V[i++] = this.generate_mid();
		i8V[i++] = 0;
		i8V[i++] = topic.length;
		for(var j=0; j<topic.length; j++){
			i8V[i++] = topic.charCodeAt(j);
		}
		i8V[i++] = qos;

		if(this.ws.readyState == 1){
			this.ws.send(buffer);
		}else{
			this.queue(buffer);
		}
	},

	publish : function(topic, payload, qos, retain){
		if(qos != 0) return 1;
		var rl = 2+topic.length+payload.length;
		var remaining_count = this.get_remaining_count(rl);
		var buffer = new ArrayBuffer(1+remaining_count+rl);
		var i8V = new Int8Array(buffer);

		var i=0;
		retain = retain?1:0;
		i8V[i++] = PUBLISH | (qos<<1) | retain;
		do{
			digit = Math.floor(rl % 128);
			rl = Math.floor(rl / 128);
			if(rl > 0){
				digit = digit | 0x80;
			}
			i8V[i++] = digit;
		}while(rl > 0);
		i8V[i++] = 0;
		i8V[i++] = topic.length;
		for(var j=0; j<topic.length; j++){
			i8V[i++] = topic.charCodeAt(j);
		}
		for(var j=0; j<payload.length; j++){
			i8V[i++] = payload.charCodeAt(j);
		}

		if(this.ws.readyState == 1){
			this.ws.send(buffer);
		}else{
			this.queue(buffer);
		}
	}
}
