/* Homer */
$(document).ready(function() {
  var mqttSocket = new Mosquitto();
  var queuedMessages = new Array();


	function subscribe() {
		mqttSocket.subscribe('/devices/#', 0);
	}

	function publish(topic, payload) {
		console.log("Publishing "+topic+":"+payload);
		mqttSocket.publish(topic, payload, 0, true);
	}

	function elementsExistForTopic(bareTopic) {
		var elements = $('[data-topic=\'' + bareTopic +'\']');
		if (elements.size() != 0) {
			console.log("Found elements for: " + bareTopic);
			return true;
		} else {
			console.log("Did not find elements for:" + bareTopic)
			return false;
		}
	}

	// topic is /devices/deviceID/attribute/type
	function createControll(bareTopic, type) {
		var deviceId = bareTopic.split("/")[2]
		var device = $("#device-" + deviceId + "");

		if (device.size() == 0) {		// create new device if it does not exists
			device =  $("<div class='appliance' id='device-"+ deviceId + "'> <div class='body'> <div class='powerSwitch' data-topic='/devices/"+deviceId+"/status' data-type='switch' data-value='" + queuedMessages[bareTopic] + "'> </div><div class='name'>" + deviceId +"</div></div> <div class='controlls'> </div></div>").appendTo("#container");
		} 

		// Create controll according to type and append it to device
		if (type == "range") {
			var control =  $("<input type='range' value='" + queuedMessages[bareTopic]+ "' data-topic='"+bareTopic+"' data-type='range'/>").appendTo(device.children(".controlls"));
			positionRangeBackground(control);			
		} else if (type == "switch") {
			// not yet implemented
		}
	}

	function handleTypeMessage(topic, payload) {
		bareTopic = topic.replace('/type', ''); // strip type 
		// check if element for topic exists 
		if (elementsExistForTopic(bareTopic)) {
			return;
		} else {
			createControll(bareTopic, payload);
		}
	}

	function handleDataMessage(topic, payload) {
		if (elementsExistForTopic(topic)) {
			console.log("Seting data");
			$('[data-topic=\'' + topic +'\']').each(function(index) {
			if ($(this).attr("data-type") == "switch") {
				console.log("switch for "+topic+":"+payload);
				setToggle(topic, payload, $(this));
			} else if ($(this).attr("data-type") == "range") {
				console.log("range for "+topic+":"+payload);
				setRange(topic, payload, $(this));
			} else {
			  console.log("handleDataMessage for type ("+ $(this).attr("type") +") is not implemented");
			}
		}); 
		} else {
			console.log("Data message queued until controll created");
			queuedMessages[topic] = payload;
		}
	}
	function receive(topic, payload) {
		console.log("-----------MARK-----------");
		console.log("Receive "+topic+":"+payload);		
	
		// Check if it is type or data message
		if(topic.indexOf("type") != -1) {
			console.log ("Got type message");
			handleTypeMessage(topic, payload);
		} else {
			console.log("Got data message");
			handleDataMessage(topic, payload)
		}





		
	}

	function setRange(topic, payload, domElement) {
			domElement.val(payload);
					positionRangeBackground(domElement);

	}	
	function setToggle(topic, payload, domElement) {
		console.log("here");
						domElement.attr('data-value', payload)
	}

	function publishToggle(domElement) {
		console.log("foo");
		var topic=domElement.data('topic');
		var currentValue=domElement.attr('data-value');
		var payload = currentValue == '1' ? "0" : "1"; // Toggle value
		publish(topic, payload);
	}

	function publishRange(domElement) {
		var topic=domElement.data('topic');
		var payload=domElement.val();
		publish(topic, payload);
	}

 
  mqttSocket.onconnect = function(rc){
  	console.log("Connection established");
  	subscribe();
	};

	mqttSocket.ondisconnect = function(rc){
		console.log("Connection terminated");
		$('#container').html("Connection terminated");
	};

	mqttSocket.onmessage = function(topic, payload, qos){
		receive(topic, payload);
	};

	function attachBindings() {
		$('#settings-save').click(function() {
	    savePreferences();
	  });

		$('#container').delegate("[data-type=\'switch\']", "click",function() {
	    publishToggle($(this));
	  });

		$('#container').delegate("[data-type=\'range\']", "change",function() {
			publishRange($(this));
			positionRangeBackground($(this));
		});
	}

	function connect(server) {	
	 	console.log("Connecting to "+ server);
	 	mqttSocket.connect(server);
 	}

	function positionRangeBackground(domElement) {
		$(domElement).css('background-position-x', (-(400-$(domElement).val()*($(domElement).outerWidth()/100)))+"px");
	}

	function populatePreferences() {
		$('#settings-server').val(localStorage.server);			
	}

	function savePreferences() {
		console.log("saving preferences");
		localStorage.server = $('#settings-server').val();
	}

	if (typeof(Storage) !== "undefined") {
	 	console.log("Client supports local storage");
	 	attachBindings();
	 	populatePreferences();

	 	console.log("Checking for server");
	 	if (localStorage.server) {
	 		connect(localStorage.server);
	 	} else {
	 		console.log("Server not set");
	 	}
	} else {
	 	console.log("No local storage support");
	}
}); 
