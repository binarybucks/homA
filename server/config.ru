# config.ru
require './homer.rb'
Faye::WebSocket.load_adapter('thin')
run App
