require 'oauth2'
require 'json'
require 'eventmachine'
require 'time'
require 'rufus/scheduler'
require 'mqtt'


MQTT_SERVER = '192.168.8.2'
MQTT_ID ='458293-GoogleCalendarBridge'
CALENDAR_POLL_INTERVALL_SECONDS = 1800

# Don't touch these unless you know what they're doing
SCOPE = 'https://www.googleapis.com/auth/calendar'
REDIRECT_URI ='http://localhost/oauth2callback'
MQTT_TOPIC_API_KEY = "/sys/#{MQTT_ID}/apiKey"
MQTT_TOPIC_API_SECRED = "/sys/#{MQTT_ID}/apiSecret"
MQTT_TOPIC_CALENDAR_ID = "/sys/#{MQTT_ID}/calendarID"
MQTT_TOPIC_REFRESH_TOKEN = "/sys/#{MQTT_ID}/refreshToken"


def connect
	unscheduleAllEvents()
	Thread.stop(@sysThread) unless (@sysThread.nil?) 

	@mqtt = nil
	@accessToken = nil
	@sysThread = nil
	@settings = {}
	@events = {}
	@scheduler = Rufus::Scheduler.start_new

	puts "Connecting to broker at: #{MQTT_SERVER}"
	@mqtt = MQTT::Client.connect(MQTT_SERVER)









		@sysThread = Thread.new {
			begin
		  	@mqtt.get("/sys/#{MQTT_ID}/\#") do |topic,payload|
					@settings[topic] = payload
		 	 	end
		 	rescue MQTT::ProtocolException => e
		 		Thread.stop
				# TODO: Reconnect on EM reactor thread
		 	end
		}
end

def unscheduleAllEvents
	if (!@scheduler.nil? && !@scheduler.running_jobs.nil?) then
			@scheduler.running_jobs.each{|j| j.unschedule}
	end
end

def bootstrap()
		connect()

		timer = EventMachine.add_periodic_timer(1) { 
			if bootstrapComplete? then
			 bootstrapComplete() 
			 timer.cancel()
			end
		}
end

def publish(topic, payload)
	begin
		puts "Publishing #{topic}: #{payload} "
		@mqtt.publish(topic, payload, true, 0)
	rescue MQTT::ProtocolException => pe
		puts "ERROR: Publish failed (#{pe}). Trying to reconnect to the broker"
		EventMachine.add_timer(15){bootstrap()}
	end
end

def bootstrapComplete?
		pass = true
		[MQTT_TOPIC_API_KEY, MQTT_TOPIC_API_SECRED, MQTT_TOPIC_CALENDAR_ID].each do |topic|
				if @settings[topic].to_s.empty? then 
					puts "#{topic} : Not yet received"
					pass = false
				end
		end
		pass

end

def bootstrapComplete
	puts "Bootstrap complete"

	if (@settings[MQTT_TOPIC_REFRESH_TOKEN].to_s.empty?) then 
		puts "No refreshToken received yet. Waiting 15 seconds until continuing"
		count = 15
	end 

	poll()
	EventMachine.add_timer(CALENDAR_POLL_INTERVALL_SECONDS)  do
		puts "Polling periodically every #{CALENDAR_POLL_INTERVALL_SECONDS} seconds"
		EventMachine::PeriodicTimer.new(interval) do
			poll()
		end
	end
end



def poll() 
	# Uncomment to get a list of calendars by id
	# getQuery("/calendar/v3/users/me/calendarList")

	# Format minimal start date of events according to te wishes of the Google Calendar API (2012-10-12T11:50:00+02:00) and URL escapes ':' and '+' characters
	startTime = "#{Time.now.strftime('%Y-%m-%dT%H%%3A%M%%3A%S%:z').sub( "+", "%2B" ).sub(":", "%3A")}"
	query = "calendar/v3/calendars/#{@settings[MQTT_TOPIC_CALENDAR_ID]}/events?singleEvents=true&fields=items(id%2Cdescription%2Cstart%2Cend%2Csummary)&orderBy=startTime&timeMin=#{startTime}"		
	#puts "Query: #{query}"

	result = getQuery(query)	
	#puts result 
	begin
		items = JSON.parse(result.body)['items'];
	rescue Exception=>e
		puts "Unable to get calendar data from the calendar server (#{e})"
		items = nil
	end

	#puts items

	@events = {}
	
	return if items.nil?

	items.each do |event|
		startTimeStr = event["start"]["dateTime"];
		endTimeStr = event["end"]["dateTime"];
		device = event['summary']
		description = event['description'];
		id = event['id']

		next if description.nil?
		
		descriptionJSON=JSON.parse(description);
			startTimediff = Time.parse(startTimeStr) - Time.now()
			endTimediff = Time.parse(endTimeStr) - Time.now()

			# Start events
			if (startTimediff > 0) then
				descriptionJSON["start"].each do |event| 
					@events[startTimeStr] ||= []
				@events[startTimeStr].push({event.keys[0] => event.values[0]})
				end
			end

			# End events
		if (endTimediff > 0) then
				descriptionJSON["end"].each do |event| 
					@events[endTimeStr] ||= []
					@events[endTimeStr].push({event.keys[0] => event.values[0]})
				end
			end


	end
	#puts "Run done: Events are: "
	#puts @events

	#Unschedule all events
	unscheduleAllEvents()

	# Reschedule all events
	@events.each do |timestring, events| 
		events.each do |event| 
			startTimediff = (Time.parse(timestring) - Time.now()).to_i
			puts "Publishing #{event.keys[0]}:#{event.values[0]} in #{startTimediff} seconds"
			@scheduler.in "#{startTimediff}s" do
				puts "Publising #{event.keys[0]}:#{event.values[0]}"
				publish(event.keys[0], event.values[0])
			end
		end
	end
end

def getQuery(query)
	begin
		oaut2AuthorizeIfRequired()
		api_client_obj = OAuth2::Client.new(@settings[MQTT_TOPIC_API_KEY], @settings[MQTT_TOPIC_API_SECRED], {:site => 'https://www.googleapis.com'})
		api_access_token_obj = OAuth2::AccessToken.new(api_client_obj, @accessToken.token)
		return api_access_token_obj.get(query)
	rescue Exception=>e
		puts "Exception during getQuery(): #{e}"
		return nil
	end
end

def oaut2AuthorizeIfRequired()
	if (@accessToken == nil || @accessToken.expired?) then
		if not (@settings[MQTT_TOPIC_REFRESH_TOKEN].to_s.empty?) then #Request new access token with refresh token if request token exists
			oaut2RefreshTokens()
		else # If no access token and no refresh token exists, we have to request authentication again
			puts "Client is not authorized yet. Autorizing for Google Calendar with OAuth2"
			auth_client_obj = OAuth2::Client.new(@settings[MQTT_TOPIC_API_KEY], @settings[MQTT_TOPIC_API_SECRED], {:site => 'https://accounts.google.com', :authorize_url => "/o/oauth2/auth", :token_url => "/o/oauth2/token"})
		
			puts "1) Paste this URL into your browser where you are logged in to the relevant Google account:"
			puts auth_client_obj.auth_code.authorize_url(:scope => SCOPE, :access_type => "offline", :redirect_uri => REDIRECT_URI, :approval_prompt => 'force')
		
			puts "2) Accept the authorization request from Google in your browser:"
		
			puts "3) Google will redirect you to localhost, but just copy the code parameter out of the URL they redirect you to, paste it here and hit enter:\n"
			code = gets.chomp.strip
			@accessToken = auth_client_obj.auth_code.get_token(code, { :redirect_uri => REDIRECT_URI, :token_method => :post })
			publish(MQTT_TOPIC_REFRESH_TOKEN, @accessToken.refresh_token);
		
			puts "4) The refresh token was published to #{MQTT_TOPIC_REFRESH_TOKEN}:#{@accessToken.refresh_token} for future starts of this application"
		end

		if (!@accessToken.refresh_token.nil?) then
			@settings[MQTT_TOPIC_REFRESH_TOKEN] = @accessToken.refresh_token
		end
	end
end

def oaut2RefreshTokens()
	puts "Refreshing Google Calendar OAuth2 token using refreshToken #{@settings[MQTT_TOPIC_REFRESH_TOKEN]}"
	accessTokenStr = ""
	if (@accessToken != nil && @accessToken.token != nil) then 
		accessTokenStr = @accessToken.token
	end

	refresh_client_obj = OAuth2::Client.new(@settings[MQTT_TOPIC_API_KEY], @settings[MQTT_TOPIC_API_SECRED], {:site => 'https://accounts.google.com', :authorize_url => '/o/oauth2/auth', :token_url => '/o/oauth2/token'})
	refresh_access_token_obj = OAuth2::AccessToken.from_hash(refresh_client_obj, {:access_token => accessTokenStr, :refresh_token => @settings[MQTT_TOPIC_REFRESH_TOKEN], :token_type => "Bearer"})
	
	newAccessToken = refresh_access_token_obj.refresh!
	@accessToken = newAccessToken

	# The new AccessToken object might not have a refresh token set when no refresh token was returned from the server
	# In this case we keep using the old one. Otherwise we update the used refresh token with the new one
	if (@accessToken.refresh_token != nil && @accessToken.refresh_token != "") then 
		@settings[MQTT_TOPIC_REFRESH_TOKEN] = @accessToken.refresh_token
	end
end

# Start EM
EventMachine.run {
	bootstrap()
	Signal.trap("INT") {  EventMachine.stop }
}


