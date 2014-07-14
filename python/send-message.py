#!/usr/bin/env python
# This script require pika, so create a virtualenv and install pika!

import pika
import logging
logging.basicConfig(format='%(levelname)s:%(message)s', level=logging.CRITICAL)

# Step #3
def on_open(connection):
    connection.channel(on_channel_open)


# Step #4
def on_channel_open(channel):
    channel.basic_publish('',
                          'akvo.service-events',
                          'Haj from Pyhton!',
                          pika.BasicProperties(content_type='text/plain',
                                               type='python.test',
                                               delivery_mode=2))
    print "Message sent!"
    connection.close()

# Step #1: Connect to RabbitMQ
parameters = pika.URLParameters('amqp://guest:guest@localhost:5672/%2F')
connection = pika.SelectConnection(parameters=parameters,
                                   on_open_callback=on_open)

try:
    # Step #2 - Block on the IOLoop
    connection.ioloop.start()

# Catch a Keyboard Interrupt to make sure that the connection is closed cleanly
except KeyboardInterrupt:
    # Gracefully close the connection
    connection.close()
    # Start the IOLoop again so Pika can communicate,
    # it will stop on its own when the connection is closed
    connection.ioloop.start()
