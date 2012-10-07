require_relative 'GenericDevice.rb'

class AB440RemotePowerplug < GenericDevice
	def initialize(identifier, first, last, attributes = {'on' => 0})
		super(identifier, attributes)

		# Position of first and last 5 dip switches according to http://code.google.com/p/rc-switch/wiki/HowTo_OperateLowCostOutlets
		@first = first # e.g 10010 
		@last = last # e.g 2

	end

	def changeAttribute_On(value)
			puts "Changing #{identifier}'s attribute On to #{value.to_s}"
			@attributes['on'] = value.to_i
			$serialProxy.write("{w:{'t':'dip10','f':#{@first},'l':#{@last},'s':#{@attributes['on']}}}")
	end

end