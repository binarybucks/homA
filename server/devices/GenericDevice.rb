class GenericDevice
	attr_reader :identifier

	@@identifiers = [] # Array
	@attributes = {}	# Hash

  def assignIdentifier(identifier)
  	@@identifiers.each do |i|
			if (i.to_s == identifier.to_s)
				raise "Interactable identifier (#{identifier}) is not unique. Exiting!"
				exit
			end
		end 

		@identifier = identifier
		@@identifiers.push(identifier)
  end

	def initialize(identifier, attributes = {'on' => 1})
		assignIdentifier(identifier)
		@attributes = attributes
		$devices[identifier] = self

	end

	def attributes
		return @attributes
	end 

	# Gets called with attributes hash on user interaction
	def attributes=(attributes)
		attributes.each { |(attributeId, value)| changeAttribute(attributeId, value) if (@attributes.has_key?(attributeId)) }
	end

	# Sets the attribute value and calls a subclass and attribute specific changedAttribute method. 
	# In there you can provice code for interaction with that specific device 
	# E.g if the device has an attribute called temperature, the method changedAttribute_Temperature(value) is called with the new value as argument. 
	# Note, the first letter of the attribute is capitalized 
	def changeAttribute(attributeId, value)
		return if (@attributes[attributeId].to_i == value.to_i) # Do nothing if value won't be changed
	
		begin
  		send("changeAttribute_#{attributeId.capitalize}", value)
		rescue NoMethodError
  		puts "Device #{identifier} does not implement changeAttribute_#{attributeId.capitalize} for specific changes of attribute #{attributeId}"
		end
	end

	# Overwrite in subclass to provide device specific action for changes of the on attribute
	def changeAttribute_On(value)
			@attributes['on'] = value.to_i
			puts "Default changedAttribute_On method called for device #{@identifier}. Now set to #{@attributes['on']}"
	end
end