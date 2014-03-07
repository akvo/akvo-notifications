# Akvo notifications

The job of the notification service is to react on events from services
and make those events into notifications based on users. One could argue
that the notification service twists the perspective from services to
users. Making it possible to ask what happened in the system, from a
user perspective.

Akvo-notifications is a Clojure application that listen to a Rabbit
Message Queue and make data available for services with a REST API built
with Liberator.

## Development

### RabbitMQ
At this point Akvo-notifications require a running RabbitMQ to be able
to run. On OS X it's trivial to install with brew install rabbitmq. On
Ubuntu you will need to add Rabbits APT repository, then it's a matter
of sudo apt-get install rabbitmq-server. On Archlinux erlang also needs
to be installed so: pacman -S erlang rabbitmq should do the trick
(untested). More info is available at the
[RabbitMQ site](https://www.rabbitmq.com/download.html)

Once installed Rabbit can be stared with rabbitmq-server. To verify take
a look at the [web admin ui](http://server-name:15672/) (username:
guest, password: guest)

### Leiningen
To use leiningen for development make sure leiningen is installed.

To run Akvo-notifications issue
```bash
$ lein ring server
´´´
That should open a browser window with http://localhost:3000

## REST API
Available routes, you will need trailing slashes.

/services/
/services/1/ ; akvo-rsr
/services/2/ ; akvo-rsr

## Legal
Copyright (C) 2014 Stichting Akvo (Akvo Foundation)
