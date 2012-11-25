import mqtt, time, sqlite3, re, threading

TABLE_NAME = "messages"

def db_prepare():
  db_connection = sqlite3.connect('homa.db')
  return db_connection, db_connection.cursor()

def db_close(db_connection):
  db_connection.commit()
  db_connection.close()

def db_remove_topics(topic):
  global test
  print "db_remove_topic"
  db_connection, db_cursor = db_prepare()

  #get all topics from this device
  m = re.search('(?<=\/devices\/)\d+', topic)
  try:
    identifier = m.group(0)
  except:
    #doesn't seem to follow the /devices/$id/xyz naming scheme
    print "Topic doesn't follow the naming scheme"
    return

  try:
    for row in db_cursor.execute("SELECT * FROM %s WHERE id = %s" % (TABLE_NAME, identifier)):
      print "Removing ",str(row[0])
      test.publish(str(row[0]), "")
  except sqlite3.Error as e:
    print "An error occurred:", e
    return

  r = Remover(identifier)
  r.start()
  
  db_close(db_connection)
  return;

def db_write(topic, value):
  print "db_write"
  db_connection, db_cursor = db_prepare()

  m = re.search('(?<=\/devices\/)\d+', topic)
  try:
    identifier = m.group(0)
  except:
    identifier = 9999

  query = "INSERT INTO %s VALUES (\"%s\", \"%s\", %s)" % (TABLE_NAME, topic, value, identifier)
  print query
  try:
    db_cursor.execute(query)
  except sqlite3.Error as e:
    print "An error occurred:", e

  db_close(db_connection)
  return;

def message_callback(message):
  print "message callback!"
  if str(message.payload) == "remove":
    print
    db_remove_topics(message.topic)
  else:
    db_write(message.topic, message.payload)


class Remover(threading.Thread):
  def __init__(self, identifier):
    self._identifier = identifier
    super(Remover, self).__init__()

  def run(self):
    time.sleep(3)
    try:
      db_connection, db_cursor = db_prepare()
      query = "DELETE FROM %s WHERE id = %s" % (TABLE_NAME, self._identifier)
      print "Executing ",query
      db_cursor.execute(query)
    except sqlite3.Error as e:
      print "An error occurred:", e
    db_close(db_connection)


# make a new mqtt client. You can pass broker and port. Defaults to 127.0.0.1 (localhost) and 1883
test = mqtt.mqtt()
# add a callback that gets executed when a message arrives
  # you can also set callbacks for:
   # on_connect(return_code)
   # on_disconnect()
   # on_subscribe(message_id, qos_list)
   # on_unsubscribe(message_id)
   # on_publish(message_id)
   # on_message(message)
  # if you want but we only do that for on_message here
test.on_message = message_callback
# subscribe to something (topic, Quality of Service) - there is no connection to a broker yet so the action will be queued
test.subscribe("/#", 0)

# starts the client
test.startMQTT()

time.sleep(1)

# prevent the script from exiting
try:
  while(True):
    time.sleep(1)
    #test.publish("time", time.now())

# handle app closure
except (KeyboardInterrupt):
  # unsubscribes from all subscriptions and closes connection
  test.stopMQTT()
  db_connection.close()
  # just wait for the disconnect messages to appear (not really necessary)
  time.sleep(1)