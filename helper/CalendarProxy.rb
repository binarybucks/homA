require 'oauth'

require 'oauth2'

OAUTH_CREDENTIALS={
  :google => {
    :key => "127336077993.apps.googleusercontent.com",
    :secret => "lBBwOd7p_w27qvE9WeyzmFX_",
    :refreshToken => "1/NGwpD_Q14uSmSgrMcRNbP9aj8MxWPkVOcik-rpakRJw"
   }
}


class CalendarProxy

	def initialize(calendarID)
		@client_id = OAUTH_CREDENTIALS[:google][:key]
		@client_secret = OAUTH_CREDENTIALS[:google][:secret]		
		@refreshToken = OAUTH_CREDENTIALS[:google][:refreshToken]
		@scope =  "https://www.googleapis.com/auth/calendar"

		@accessToken = nil


		queryOAuth2("calendar/v3/users/me/calendarList")

	end




	def autorizeOAuth2()
		puts "Checking Google Calendar Oauth2 authentication"
		raise "Missing client_id variable" if @client_id.to_s.empty?
		raise "Missing client_secret variable" if @client_secret.to_s.empty?
		raise "Missing scope variable" if @scope.to_s.empty?

		if (@accessToken == nil || @accessToken.isExpired?) then

			#Request new access token with our refresh token
			unless (@refreshToken == "") then
				refreshTokens()


				# auth_client_obj = OAuth2::Client.new(@client_id, @client_secret, {:site => 'https://accounts.google.com', :authorize_url => "/o/oauth2/auth", :token_url => "/o/oauth2/token"})
				# @accessToken = OAuth2::AccessToken.from_hash(auth_client_obj, {:access_token => OAUTH_CREDENTIALS[:google][:accessTokenToken], :refresh_token => OAUTH_CREDENTIALS[:google][:accessTokenRefreshToken], :token_type => "Bearer"})
			else # If no access token and no refresh token is found, we have to request authentication again
				puts "Client is not authorized yet. Now Autorizing for Google Calendar with OAuth2"

				redirect_uri = 'http://localhost/oauth2callback'
				auth_client_obj = OAuth2::Client.new(@client_id, @client_secret, {:site => 'https://accounts.google.com', :authorize_url => "/o/oauth2/auth", :token_url => "/o/oauth2/token"})

				puts "1) Paste this URL into your browser where you are logged in to the relevant Google account:"
				puts auth_client_obj.auth_code.authorize_url(:scope => @scope, :access_type => "offline", :redirect_uri => redirect_uri, :approval_prompt => 'force')

				puts "2) Accept the authorization request from Google in your browser:"
				puts "3) Google will redirect you to localhost, but just copy the code parameter out of the URL they redirect you to, paste it here and hit enter:\n"

				code = gets.chomp.strip
				@accessToken = auth_client_obj.auth_code.get_token(code, { :redirect_uri => redirect_uri, :token_method => :post })
			end

			puts "Got OAuth2 access token: #{@accessToken.token} (SAVE THIS)"
			puts "Got OAuth2 access token refresh token: #{@accessToken.refresh_token} (SAVE THIS)"
			puts "Access token refresh token expired?: #{@accessToken.expired?} "

		else 
			puts "Client is already authorized"
		end


			refreshTokens()

		# if (@accessToken.expired? == true || @accessToken.expires_at == nil) then
		# 	refreshTokens()
		# 	refreshTokens()

		# end

	end

	def queryCalendar(method, query)
		autorizeOAuth2()


		#puts "api_access_token_obj.get('some_relative_path_here') OR in your browser: http://www.googleapis.com/some_relative_path_here?access_token=#{access_token_obj.token}"


		puts "Querying API at #{query}"
		api_client_obj = OAuth2::Client.new(@client_id, @client_secret, {:site => 'https://www.googleapis.com'})
		api_access_token_obj = OAuth2::AccessToken.new(api_client_obj, @accessToken.token)
		result = api_access_token_obj.get("/#{query}?access_token=#{@accessToken.token}")

		puts "Got API result: #{result.body	}"
	end


	def refreshTokens()
		puts "Refreshing Google Calendar OAuth2 token using refreshToken #{@refreshToken}"
		accessTokenStr = ""
		if (@accessToken != nil && @accessToken.token != nil) then 
			accessTokenStr = @accessToken.token
		end

		refresh_client_obj = OAuth2::Client.new(@client_id, @client_secret, {:site => 'https://accounts.google.com', :authorize_url => '/o/oauth2/auth', :token_url => '/o/oauth2/token'})
		refresh_access_token_obj = OAuth2::AccessToken.from_hash(refresh_client_obj, {:access_token => accessTokenStr, :refresh_token => @refreshToken, :token_type => "Bearer"})
		
		newAccessToken = refresh_access_token_obj.refresh!
		@accessToken = newAccessToken

		# The new AccessToken object might not have a refresh token set when no refresh token was returned from the server
		# In this case we keep using the old one. Otherwise we update the used refresh token with the new one
		if (@accessToken.refresh_token != nil && @accessToken.refresh_token != "") then 
			@refreshToken = @accessToken.refresh_token
		end

		# puts "Refreshed OAuth2 access token: #{@accessToken.token}"
		# puts "Refreshed OAuth2 access token refresh token: #{@accessToken.refresh_token} (SAVE THIS)"
		# puts "Refreshed Access token refresh token expired?: #{@accessToken.expired?}"
		# puts "Refreshed Access token refresh token expires in: #{@accessToken.expires_in}"

	end


end

