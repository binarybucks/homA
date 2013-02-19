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
    this.changed = true;
    this.updatePayload = function(payload) {
        this.changed = this.p != payload;
        this.p = payload;
    }
};

var flow = nools.compile(__dirname + "/ruleset.nools", {define: {Message: Message, mqttPublish: mqttPublish}});
var session = flow.getSession();

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
            process.nextTick(function(){receiveMqtt(packet.topic, packet.payload);})
        });
    });
})();

function mqttPublish(topic, payload, retained) {
    mqttClient.publish({ topic: topic.toString(), payload: payload.toString(), qos: 0, retain: retained});
}

function receiveMqtt(topic, payload){
    if (topic in messages) {
        var m = messages[topic];

        if(payload !== "" && payload !== undefined) {
            console.log("M => " + topic + ":" + payload);
            m.updatePayload(payload);
            session.modify(m);
        } else {
            session.retract(m);
            delete messages[topic];
        }
    } else {
        console.log("A => " + topic + ":" + payload);
        var m = new Message(topic, payload);
        messages[topic] = m;
        session.assert(m);
    }
    session.match();
}

