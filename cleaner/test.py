import mqtt, time, sqlite3, re, threading

TABLE_NAME = "messages"
DB_NAME = "homa.db"

database = None

class HomaDB:
  def __init__(self):
    self._db_connection = None
    self._db_cursor = None

  def db_prepare(self):
    self._db_connection = sqlite3.connect(DB_NAME)
    self._db_cursor = self._db_connection.cursor()

  def db_close(self, db_connection):
    self._db_connection.commit()
    self._db_connection.close()

  def db_remove_topics(self, topic):
    global test
    print "db_remove_topic"

    #get all topics from this device
    m = re.search('(?<=\/devices\/)\d+', topic)
    try:
      identifier = m.group(0)
    except:
      #doesn't seem to follow the /devices/$id/xyz naming scheme
      print "Topic doesn't follow the naming scheme"
      return

    try:
      for row in self._db_cursor.execute("SELECT * FROM %s WHERE id = %s" % (TABLE_NAME, identifier)):
        print "Removing ",str(row[0])
        test.publish(str(row[0]), "")
    except sqlite3.Error as e:
      print "An error occurred:", e
      return

    r = Remover(identifier)
    r.start()
    
    self._db_connection.commit()
    return;

  def db_write(self, topic, value):
    print "db_write"

    m = re.search('(?<=\/devices\/)\d+', topic)
    try:
      identifier = m.group(0)
    except:
      identifier = 9999

    query = "INSERT INTO %s VALUES (\"%s\", \"%s\", %s)" % (TABLE_NAME, topic, value, identifier)
    print query
    try:
      self._db_cursor.execute(query)
    except sqlite3.Error as e:
      print "An error occurred:", e

    self._db_connection.commit()
    return;

class Remover(threading.Thread):
  def __init__(self, identifier):
    self._identifier = identifier
    super(Remover, self).__init__()

  def run(self):
    time.sleep(3)
    try:
      db_connection = sqlite3.connect(DB_NAME)
      db_cursor = db_connection.cursor()

      query = "DELETE FROM %s WHERE id = %s" % (TABLE_NAME, self._identifier)
      print "Executing ",query
      db_cursor.execute(query)
    except sqlite3.Error as e:
      print "An error occurred:", e
    db_connection.commit()
    db_connection.close()


def message_callback(message):
  print "message callback!"
  database.db_prepare()

  if str(message.payload) == "remove":
    print
    database.db_remove_topics(message.topic)
  else:
    database.db_write(message.topic, message.payload)


database = HomaDB()

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