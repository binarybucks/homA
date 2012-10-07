class Receiver
	include Singleton

	def receive(data)
		Mutex.new.synchronize {
			JSON.parse(data).each  { |(deviceId, attributes)| $devices[deviceId].attributes = attributes if $devices.has_key?(deviceId)} #Sets status value of interactables from s. Ignores values for switchables that are not present in @switchables
			$sender.broadcastStatesToAllSockets()
		}
	end
end