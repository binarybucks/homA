require 'rubygems'
require 'mqtt'
require 'eventmachine'


MQTT_SERVER = 'fermi'


$topics = {}


def receive(t, m)
	unless m.empty? then
		$topics[t] = m
	else 
		$topics.delete(t)
	end

		print

end

def print
	puts "\n\n" 
	$topics.each_with_index { |(t,m), i| p "#{i}: #{t}:#{m}" }
	puts "Enter a number to unpublish: "
end

def remove(index)
	return if index > $topics.size()

	k = $topics.keys[index]
	puts "Removing #{k}"

	$c.publish(k, '', true, 0)
end


def connect
		puts "HomA cleaner starting"	
		puts "Broker is at: #{MQTT_SERVER}"

	Thread.new {
		$c = MQTT::Client.connect(MQTT_SERVER)
		  $c.get('#') do |topic,message|
		  	receive(topic, message)
		  end
	}
end



def readInput
	Thread.new {
		while true do 
			remove(gets.to_i)
		end
	}
end

EventMachine.run {
 	connect()
	readInput()
 	Signal.trap("INT") {  EventMachine.stop }
}