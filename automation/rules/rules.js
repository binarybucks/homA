#!/usr/bin/env node
var nools = require("nools");
var date = require("datejs")
var mqtt = require('mqttjs')
var argv = require('optimist').usage('Usage: $0 [--brokerHost 127.0.0.1] [--brokerPort 1883]')
                              .default("brokerHost", '127.0.0.1')
                              .default("brokerPort", 1883)
                              .argv;

var mqttClient;
var messages = {};

var Message = function (topic, payload) {
    this.t = topic;
    this.p = payload;
    this.changed = false;
    this.clock = new Clock();
    this.updatePayload = function(payload) {
        this.clock.step();
        this.changed = this.p != payload;
        this.p = payload;
    }
    this.is = function(topic, payload) {
        return false;
    }
};

var Clock = function(){
    this.date = new Date();

    this.getHours = function() {
        return this.date.getHours();
    }

    this.getMinutes = function() {
        return this.date.getMinutes();
    }

    this.between = function(i, a, b) {
        return i >= a && i <=b;
    }

    this.isMorning = function() {
        return this.between(this.getHours(), 6, 11);
    }
    this.isNoon = function() {
        return this.between(this.getHours(), 12, 14);
    }
    this.isAfternoon = function(){
        return this.between(this.getHours(), 15, 17);
    }
    this.isEvening = function() {
        return this.between(this.getHours(), 18, 23);
    }
    this.isNight = function() {
        return bthis.etween(this.getHours(), 0, 5);
    }
    this.step = function(){
        this.date = new Date();
        return this;
    }
}


var flow = nools.compile(__dirname + "/ruleset.nools", {define: {Message: Message, mqttPublish: mqttPublish, Clock: Clock}});
var session = flow.getSession();
var clock = new Clock();
session.assert(clock);
setInterval(function(){session.modify(clock.step());}, 5*1000);




function mqttPublish(topic, payload, retained) {
    mqttClient.publish({ topic: topic.toString(), payload: payload.toString(), qos: 0, retain: retained});
}

function mqttReceive(topic, payload){
    if (topic in messages) {
        var m = messages[topic];

        if(payload !== "" && payload !== undefined) {
            console.log("M => " + topic + ":" + payload);
            m.updatePayload(payload);
            session.modify(m);
        } else {
            console.log("R <= " + topic + ":" + m.p);
            session.retract(m);
            delete messages[topic];
        }
    } else {
        if(payload == "" || payload == undefined) {
            return;
        }
        console.log("A => " + topic + ":" + payload);
        var m = new Message(topic, payload);
        messages[topic] = m;
        session.assert(m);
    }
    session.match();
}

(function mqttConnect() {
    mqtt.createClient(argv.brokerPort, argv.brokerHost, function(err, client) {
        if (err) {
            console.log('MQTT        %s', err);
            process.exit(1);
        }

        mqttClient = client;
        client.connect({keepalive: 3000});

        client.on('connack', function(packet) {
            client.subscribe({topic: '#'});
        });

        client.on('close', function() {
            process.exit(-1);
        });

        client.on('error', function(e) {
            console.log('MQTT        Error: %s', e);
            process.exit(-1);
        });

        client.on('publish', function(packet) {
            process.nextTick(function(){mqttReceive(packet.topic, packet.payload);})
        });
    });
})();

