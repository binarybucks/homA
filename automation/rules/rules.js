var nools = require("nools");
var date = require("datejs")
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

var flow = nools.compile(__dirname + "/homA.nools", null, null, null);
var session = flow.getSession();

function receiveMqtt(topic, payload){
    if (topic in messages) {
        console.log("Modifiying " + topic + ":" + payload);
        var m = messages[topic];
        m.updatePayload(payload);
        session.modify(m);
    } else {
        console.log("Asserting " + topic + ":" + payload);
        var m = new Message(topic, payload);
        messages[topic] = m;
        session.assert(m);
    }
    session.match();
}


receiveMqtt('/test1', '1');
receiveMqtt('/test2', '0');
receiveMqtt('/test2', '1');
receiveMqtt('/test1', '0');
receiveMqtt('/test2', '0');
receiveMqtt('/test1', '1');
receiveMqtt('/test1', '1');

