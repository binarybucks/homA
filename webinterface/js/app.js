/* Homer */
$(document).ready(function() {
  var mqttSocket = new Mosquitto();
  var queuedMessages = new Array();


  /*
   * 
   * INITIALIZATION 
   *
   */ 
  

  function bootstrap () {
		if (typeof(Storage) !== "undefined") {
		 	console.log("Client supports local storage");
		 	attachBindings();
		 	populatePreferences();
		 	connect();
		} else {
		 	console.log("No local storage support");
		}  	
  }

  function attachBindings() {
		$('#settings-save').click(function() {
	    savePreferences();
	  });

		$('#connect').click(function() {
	    connect();
	  });

		$('#container').delegate("[data-type=\'switch\']", "click",function() {
	    publishToggle($(this));
	  });

		$('#container').delegate("[data-type=\'range\']", "change",function() {
			publishRange($(this));
			positionRangeBackground($(this));
		});
	}

	function populatePreferences() {
		$('#settings-server').val(localStorage.server);			
	}


	/* 
	 *
	 * CONNECTION HANDLING AND MESSAGE DISPATCHING
	 *
	 */ 


	function connect() {	
		console.log("Checking for server");
	 	if (!localStorage.server) {
	 		console.log("Server not set");
	 		return;
	 	}
	 	
	 	console.log("Connecting to "+ localStorage.server);
	 	mqttSocket.connect(localStorage.server);
 	}

  mqttSocket.onconnect = function(rc){
  	console.log("Connection established");
  	$('#disconnected').css('display', 'none')
  	  	$('#settings').css('display', 'none')

  	subscribe();
	};

	mqttSocket.ondisconnect = function(rc){
		console.log("Connection terminated");
		  	  	$('#settings').css('display', 'block')

  	$('#disconnected').css('display', 'block')
	};

	mqttSocket.onmessage = function(topic, payload, qos){
		receive(topic, payload);
	};


	/*
	 *
	 * INTERFACE CREATION AND MESSAGE HANDING
	 *
	 */


	function createControll(bareTopic, type) {
		var deviceId = bareTopic.split("/")[2]
		var device = $("#device-" + deviceId + "");
		var attribute = bareTopic.split("/")[3]
		
		if (device.size() == 0) {		// create new device if it does not exists
			console.log("Creating new device");
			// device =  $("<div class='appliance' id='device-"+ deviceId + "'> <div class='body'> <div class='powerSwitch' data-topic='/devices/"+deviceId+"/status' data-type='switch' data-value='" + queuedMessages[bareTopic] + "'> </div><div class='name'>" + deviceId +"</div></div> <div class='controlls'> </div></div>").appendTo("#container");
			device = $("<div class='appliance' id='device-"+ deviceId + "'> <div class='name'>"+ deviceId +"</div></div>").appendTo('#container');
		} 



		// Create controll according to type and append it to device
		if (type == "range") {
			console.log("Creating new range control");
			var control =  $("<input type='range' value='" + queuedMessages[bareTopic]+ "' data-topic='"+bareTopic+"' data-type='range'/>").prependTo(device);
			positionRangeBackground(control);			
		} else if (type == "switch") {
			console.log("Creating new switch control");

			var control = $("<div data-topic='"+ bareTopic +"' data-type='switch' data-value='" + queuedMessages[bareTopic] + "' ></div>").prependTo(device);
			if (attribute == "power") {
				control.addClass("power");
			}
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
			$('[data-topic=\'' + topic +'\']').each(function(index) {
				if ($(this).attr("data-type") == "switch") {
					setToggle(topic, payload, $(this));
				} else if ($(this).attr("data-type") == "range") {
					setRange(topic, payload, $(this));
				} else {
				  console.log("handleDataMessage for type ("+ $(this).attr("type") +") is not implemented");
				}
			}); 
		} else {
			console.log("Data message queued");
			queuedMessages[topic] = payload;
		}
	}

	function receive(topic, payload) {
		console.log("-----------RECEIVE-----------");
		console.log("Receive "+topic+":"+payload);		
	
		// Check if it is type or data message
		if(topic.indexOf("type") != -1) {
			console.log ("Got type message");
			handleTypeMessage(topic, payload);
		} else {
			console.log("Got data message");
			handleDataMessage(topic, payload)
		}
		console.log("-----------/ RECEIVE-----------");
	}

	function setRange(topic, payload, domElement) {
		domElement.val(payload);
		positionRangeBackground(domElement);
	}

	function setToggle(topic, payload, domElement) {
		domElement.attr('data-value', payload)
	}

	function publishToggle(domElement) {
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


	/* 
	 * 
	 * HELPER 
	 *
	 */


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
			console.log("Control exists for: " + bareTopic);
			return true;
		} else {
			console.log("Control does not exist for:" + bareTopic)
			return false;
		}
	}

	function positionRangeBackground(domElement) {
		$(domElement).css('background-position-x', (-(400-$(domElement).val()*($(domElement).outerWidth()/100)))+"px");
	}
	
	function savePreferences() {
		console.log("saving preferences");
		localStorage.server = $('#settings-server').val();
	}

	bootstrap();
}); 
