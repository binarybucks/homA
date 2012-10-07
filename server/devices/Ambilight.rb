require_relative 'AB440RemotePowerplug.rb'

class Ambilight < AB440RemotePowerplug
	def initialize(identifier, first, last, attributes = {'on' => 0, 'fading' => 0})
		super(identifier, first, last, attributes)
		setColorRgb(0, 255, 0) # Sets initial color
	end


	def fade
    Thread.new do # Spawn background thread for color fading
      while @attributes['fading']==1 do
        if @currentr == 255 && @currentg == 0  && @currentb < 255
          setColorRgb(@currentr, @currentg, @currentb+1)
        elsif @currentb == 255 && @currentg == 0 && @currentr > 0
          setColorRgb(@currentr-1, @currentg, @currentb)
        elsif @currentr ==0 && @currentb == 255  && @currentg < 255
          setColorRgb(@currentr, @currentg+1, @currentb)
        elsif @currentr == 0 && @currentg == 255 && @currentb > 0
          setColorRgb(@currentr, @currentg, @currentb-1)
        elsif @currentg == 255 && @currentb == 0 && @currentr < 255
          setColorRgb(@currentr+1, @currentg, @currentb)
        elsif @currentr == 255 && @currentb == 0 && @currentg > 0
          setColorRgb(@currentr, @currentg-1, @currentb)
        end
        sleep 0.5
     	end
   	end
	end

	def setColorRgb(r, g, b)
	  @currentr = r  
	  @currentg = g 
	  @currentb = b
		$serialProxy.write("'c':{'r':#{@currentr},'g':#{@currentg},'b':#{@currentb}}")
	end



  def changeAttribute_Fading(value)
    @attributes['fading'] = value.to_i

    if (@attributes['fading'] == 1)
      fade()
    end
  end

  # Changes of On attribute are handled by superclass AB440RemotePowerplug
  def changeAttribute_On(value)
    super
  end

end