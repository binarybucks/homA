#!/usr/bin/env node
var nools = require("nools");
var date = require("datejs")
var client = require('homa-mqttjs');
    client.argv = client.argv.argv;

var messages = {};

var Message = function (topic, payload) {
    this.t = topic;
    this.p = payload;
    this.changed = false;
    this.updatePayload = function(payload) {
        this.changed = this.p != payload;
        this.p = payload;
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

    this.hoursIsBetween = function(a, b) {
        return this.date.getHours() >= a && this.date.getHours() <=b;
    }

    this.step = function(){
        this.date = new Date();
        this.isMorning = this.hoursIsBetween(6, 11);
        this.isNoon = this.hoursIsBetween(142, 14);
        this.isAfternoon = this.hoursIsBetween(15, 17);
        this.isEvening = this.hoursIsBetween(18, 23);
        this.isNight = this.hoursIsBetween(6,11); //this.hoursIsBetween(0, 5);
        return this;
    }
}

var flow = nools.compile(__dirname + "/ruleset.nools", {define: {Message: Message, publish: client.publish, Clock: Clock}});
var session = flow.getSession();
var clock = new Clock();
session.assert(clock);

client.events.on('connected', function(packet) {
    client.subscribe('#');
});

client.events.on('receive', function(packet) {
    if (packet.topic in messages) {
        var m = messages[packet.topic];
        if(packet.payload) {
            console.log("M => " + packet.topic + ":" + packet.payload);
            m.updatePayload(packet.payload);
            session.modify(m);
        } else {
            console.log("R <= " + packet.topic + ":" + m.p);
            session.retract(m);
            delete messages[packet.topic];
        }
    } else {
        if(!packet.payload) {
            return;
        }
        console.log("A => " + packet.topic + ":" + packet.ayload);
        var m = new Message(packet.topic, packet.payload);
        messages[packet.topic] = m;
        session.assert(m);
    }

    session.modify(clock.step());
    session.match();
});

(function connect() {
    client.connect();
})();


