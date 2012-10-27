/* Homer */
$(document).ready(function() {
  var mqttSocket = new Mosquitto();
  var queuedMessages = new Array();
  var rooms = new Array();
  bootstrap();

  /*
   * 
   * INITIALIZATION 
   *
   */

  function bootstrap () {
		if (typeof(Storage) == "undefined") {
		 	console.log("Browser does not support local storage");
		 	return;
		}

	 	if (!localStorage.server) {
	 		console.log("Server not set");
	 		return;
	 	}

	 	prepareCallbacks();
		connect();
  }

  function prepareCallbacks() {
		$('#settings-save').click(function() {
	    savePreferences();
	  });

		$('#connect').click(function() {
	    connect();
	  });

		$('#rooms').delegate("[data-type=\'switch\']", "click",function() {
	    publishToggle($(this));
	  });

		$('#rooms').delegate("input[data-type=\'range\']", "change",function() {
			publishRange($(this));
			positionRangeBackground($(this));
		});

		$('#room-list').delegate("a.forward", "click",function(e) {
			e.preventDefault();
			showRoom($(this).attr("data-room"));
			slideTo($("#"+ $(this).attr("data-room")), "#!/room-"+$(this).attr("data-room"));
		});


		$('#room-details').delegate("a.back", "click",function(e) {
			e.preventDefault();
		 	slideTo($("#room-list"), "#!/");
		});

		$(".device .forward").on("click", function() {
			console.log(e);
		});




		mqttSocket.onconnect = function(rc){
  		console.log("Connection established");
  		subscribe();
		};

		mqttSocket.ondisconnect = function(rc){ 
			console.log("Connection terminated");
		};

		mqttSocket.onmessage = function(topic, payload, qos){
			receive(topic, payload);
		};





	}
	function connect() {		 	
	 	console.log("Connecting to "+ localStorage.server);
	 	mqttSocket.connect(localStorage.server);
 	}

  /*
   * 
   * MQTT MESSAGE PARSING 
   *
   */

	function receive(topic, payload) {
		console.log("-----------RECEIVE-----------");
		console.log("Received: "+topic+":"+payload);		
	
		var splitTopic = topic.split("/");
		/*
		 * splitTopic[0] = undefined
		 * splitTopic[1] = {devices}
		 * splitTopic[2] = {uniqueDeviceId}
		 * splitTopic[3] = {controls, meta,}
		 * splitTopic[4] = {controllName, room, name}
		 * splitTopic[3] = {type}
		 */

		ensureDevieExists(topic); 		// creates device if it does not exist
		


		if(topic.indexOf("type") != -1) {
			handleTypeMessage(topic, payload);
		} else if(topic.indexOf("meta") != -1) { 			
			handleMetaMessage(topic, payload);
		}	else {
			handleControlMessage(topic, payload);
		}

		console.log("-----------/ RECEIVE-----------");
	}


	// split 2 = id
	// split 4 = metatype {room, name}
	function handleMetaMessage(topic, payload) {
		var uniqueDeviceId = topic.split("/")[2];

		if(topic.indexOf("meta/room") != -1) { // /devices/$deviceid/meta/room 
			ensureRoomExists(payload);
			moveDeviceToRoom(uniqueDeviceId, payload);

		} else {
			console.log("Got name for device:" + uniqueId);
		}
	}

	function handleTypeMessage(topic, payload) {
			bareTopic = topic.replace('/type', ''); 

			if (!elementsExistForTopic(bareTopic))
				createControl(bareTopic, payload);
	}

	function handleControlMessage(topic, payload) {
		console.log("control message")
		if (elementsExistForTopic(topic)) {

			$('[data-topic=\'' + topic +'\']').each(function(index) {
				if ($(this).attr("data-type") == "switch") {
					setSwitch($(this), payload);
				} else if ($(this).attr("data-type") == "range") {
					setRange($(this), payload);
				} else {
				  console.log("handleDataMessage for type ("+ $(this).attr("type") +") is not implemented");
				}
			}); 

		} else {
			console.log("Data message queued");
			queuedMessages[topic] = payload;
		}
	}


	/*
	 *
	 * ROOM MANAGEMENT
	 *
	 */

	function ensureRoomExists(roomName) {
		var room;

		if (!(room = roomExists(roomName))) {
			room = $("<div id='room-"+roomName+ "' class='room'><div class='view-header'><h1><a href='' class='back'>Rooms / </a> "+roomName+"</h1></div></div>").appendTo("#room-details");
			var menuItem = $("<a href='' class='forward' data-room='room-"+roomName+"'>"+roomName+"</a>").appendTo("#room-list");







			rooms[roomName.replace("room-","")];
			console.log("Rooms: " + rooms);


		} else {
			console.log("Room does exist");
		}
		return room;
	}

	function showRoom(roomName) {
		console.log("showing room with id " +roomName);
		$(".room").css("display", "none");
		$("#"+roomName).css("display", "block");

		localStorage.selectedRoom = roomName;
	}



	function roomExists(roomName) {
		var room = $("#room-"+roomName)
		if (room.size() != 0) {
			return room;
		} else {
			return false;
		}
	}


	/*
	 *
	 * DEVICE MANAGEMENT
	 *
	 */

	function ensureDevieExists(topic) {
		var uniqueDeviceId = topic.split("/")[2];
		var device;
		if(!(device = deviceExists(uniqueDeviceId))) {
			device = $("<div class='device' id='device-"+ uniqueDeviceId + "'>  <div class='header forward'>"+uniqueDeviceId+"</div>  <div class='controls'><div class='settings'>Settings</div></div></div>").appendTo('#room-details #unassigned');
		}
		return device;
	}


	function showDeviceSettings(device) {
		var uniqueDeviceId = device.attr("id").replace('device-', '');
		var deviceName = device.children('.header').text();
		console.log("showing device settings of device " + uniqueDeviceId + " name: " + deviceName);

		var mdl = $("<div id='device-settings-overlay'></div><div id='device-settings'><h2>Device Settings</h2><a id='modalclose' href='#'>close</a><a id='modalsave' href='#'>save</a></div>");
				$('body').append(mdl);

		$('#modalsave').click(function () {
			console.log("closing with save");

			$("#device-settings-overlay").remove();
			$("#device-settings").remove();

		});	
				$('#modalclose').click(function () {
						console.log("closing with save");
						$("#device-settings-overlay").remove();
						$("#device-settings").remove();

		});




	}



	function deviceExists(uniqueDeviceId) {
		var device = $("#device-"+uniqueDeviceId)
		if (device.size() != 0) {
			return device;
		} else {
			return null;
		}
	}

	function nameDevice(uniqueDeviceId, name) {
		$("#device-"+uniqueDeviceId + " .name").text(name);
	}



	function moveDeviceToRoom(uniqueDeviceId, newRoomName) {
		console.log("Moving device " + uniqueDeviceId + " to room " + newRoomName);
		var device = $("#device-"+uniqueDeviceId);
		var room = $("#room-"+newRoomName);
		device.appendTo(room);
	}	





	/*
	 *
	 * CONTROLLS
	 *
	 */

	function createControl(bareTopic, type) {
		var deviceId = bareTopic.split("/")[2]
		var device = $("#device-" + deviceId + "");
		var attribute = bareTopic.split("/")[3]
		var controlAnchor = device.children(".controls")[0]
		
		// Create controll according to type and append it to device
		if (type == "range") {
			console.log("Creating new range control");
			var control =  $("<input type='range' value='" + queuedMessages[bareTopic]+ "' data-topic='"+bareTopic+"' data-type='range'/>").appendTo(controlAnchor);
			positionRangeBackground(control);			
		} else if (type == "switch") {
			console.log("Creating new switch control");
			var control = $("<div data-topic='"+ bareTopic +"' data-type='switch' data-value='" + queuedMessages[bareTopic] + "' ></div>").prependTo(controlAnchor);
			if (attribute == "power") {
				control.addClass("power");
				control.trigger('powerChanged',[queuedMessages[bareTopic]]);
			}
		}
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

	function setRange(domElement, payload) {
		domElement.val(payload);
		positionRangeBackground(domElement);
	}

	function setSwitch(domElement, payload) {
		console.log("asdf");
		domElement.attr('data-value', payload)

		if(domElement.is('.power')) {
			domElement.trigger('powerChanged',[payload]);
		}
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
   * UI CANDY
   *
   */


   function slideTo(domElement, hashtag) {
		console.log("sliding to: ");
		console.log(domElement);
		var offset = $("#slider").offset().left - domElement.offset().left ;
		console.log("Offset: " + offset);

   	$("#slider").css("transform","translateX("+ offset + "px)");
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



	function positionRangeBackground(domElement) {
		$(domElement).css('background-position-x', (-(400-$(domElement).val()*($(domElement).outerWidth()/100)))+"px");
	}

	function savePreferences() {
		console.log("saving preferences");
		localStorage.server = $('#settings-server').val();
	}


	if (roomName = localStorage.selectedRoom) {
		showRoom(roomName);			
	}


 $( "#foobar" ).autocomplete({
            source: Object.keys(rooms)
        });

}); 
