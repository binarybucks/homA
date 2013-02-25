#!/usr/bin/env node
var nools = require("nools");
var date = require("datejs")
var client = require('homa-mqttjs');
    client.argv = client.argv.argv;

var messages = {};

var Message = function (packet) {
    this.updatePayload = function(packet) {
        this.p_previous = this.p;
        this.p = packet.payload;
        this.changed = this.p_previous != this.p;
        this.retained = packet.retain;
    }
    
    this.changedFromTo = function(from, to) {
        return this.p_previous == from && this.p == to;
    }
    this.updatePayload(packet);
};

var Clock = function(){
    this.date = new Date();

    this.getHours = function() {
        return this.date.getHours();
    }

    this.getMinutes = function() {
        return this.date.getMinutes();
    }

    this.hoursIsBetween = function(a, b) {
        return this.date.getHours() >= a && this.date.getHours() <=b;
    }

    this.step = function(){
        this.date = new Date();
        this.isMorning = this.hoursIsBetween(6, 11);
        this.isNoon = this.hoursIsBetween(12, 14);
        this.isAfternoon = this.hoursIsBetween(15, 17);
        this.isEvening = this.hoursIsBetween(18, 23);
        this.isNight = this.hoursIsBetween(0,5);
        return this;
    }
}

var flow = nools.compile(__dirname + "/ruleset.nools", {define: {Message: Message, publish: client.publish, forget: forget, Clock: Clock}});
var session = flow.getSession();
var clock = new Clock();
session.assert(clock);

client.events.on('connected', function(packet) {
    client.subscribe('#');
});

// It is a good idea to forget knowledge that triggered a rule which publishes things
// Otherwise the rule would fire again if the publish is received and the session is matched, resulting in an infinite loop
function forget(m) {
    if (m.t in messages) {
        console.log("R <= " + m.t + ":" + m.p);
        session.retract(m);
        delete messages[m.t];  
    }
}

client.events.on('receive', function(packet) {
    console.log("retained: " + packet.retain);
    if (packet.topic in messages) {
        var m = messages[packet.topic];
        if(packet.payload) {
            console.log("M => " + packet.topic + ":" + packet.payload);
            m.updatePayload(packet);
            session.modify(m);
        } else {
            forget(m);
        }
    } else {
        if(!packet.payload) {
            return;
        }
        console.log("A => " + packet.topic + ":" + packet.payload);
        var m = new Message(packet);
        messages[packet.topic] = m;
        session.assert(m);
    }

    session.modify(clock.step());
    session.match();
});

(function connect() {
    client.connect();
})();


