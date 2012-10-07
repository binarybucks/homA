class Sender
	include Singleton

	def initialize
		@sockets = []
	end 

	def addSocket (socket)
		@sockets.push(socket)
		sendStatesToSocket(socket, statesToJson())
	end

	def removeSocket (socket)
		@sockets.each do |s|
			if (s == socket)
				@sockets.delete(s)
			end
		end 
	end

	# returns e.g. {'device1':{'on':0, 'someOtherAttribute':1}, 'device2':{'on':1}}
	def statesToJson()
		states =	$devices.each_with_object({}) {|(deviceIdentifier, device), s| s[deviceIdentifier] = device.attributes }
		return states.to_json
	end 

	def broadcastStatesToAllSockets ()
		puts "Broadcasting: " + statesToJson()
  	EM.next_tick { @sockets.each{|socket| sendStatesToSocket(socket, statesToJson())} }
	end

	def sendStatesToSocket (socket, state)
		socket.send(state)
	end
end