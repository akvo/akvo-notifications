# Akvo notifications

**Very early work in progress!**

The job of the notification service is to react on events from services
and make those events into notifications based on users. One could argue
that the notification service twists the perspective from services to
users. Making it possible to ask what happened in the system, from a
user perspective.

Akvo-notifications is a Clojure application that listen to a Rabbit
Message Queue and make data available for services with a REST API built
with Liberator.

## PM 
- [Issue board](https://huboard.com/akvo/akvo-notifications/)  
- [Issue list](https://github.com/akvo/akvo-notifications/issues?state=open)

## Usage
At the moment, to run akvo-notification needs to be able to connect to RabbitMQ. This should not be the case, but for now that is how it is :-(

When running the REST:ish API is available at [localhost:3000](http://localhost:3000)


### Uberjar
Default usage:

```bash
$ java -jar target/akvo-notifications-[...]-standalone.jar
```

If needed: 

```bash
$ java -jar target/akvo-notifications-[...]-standalone.jar -h
Akvo notifications

usage: java -jar <path to jar> [options...]
Options:
  -wp, --web-port PORT            3000                 Web service port
  -dh, --data-host HOST           localhost            Datastore host
  -dp, --data-port PORT           5002                 Datastore port
  -qh, --queue-host HOST          localhost            Queue host
  -qp, --queue-port PORT          5672                 Queue port
  -qu, --queue-user USERNAME      guest                Queue username
  -qc, --queue-password PASSWORD  guest                Queue password
  -qv, --queue-vhost VHOST        /                    Queue vhost
  -qn, --queue-name QUEUE         akvo.service-events  Queue name
  -h, --help

Copyright (C) 2014 Stichting Akvo (Akvo Foundation)
```

### Leiningen
To run the application issue:

```bash
$ lein run
```

### REPL
The project is setup to follow ["Reloading"](https://github.com/stuartsierra/component/blob/2e66fb8ad9054e490f4f3e26398a536a05fb3a66/README.md#reloading) workflow. So easiest is to fire up a REPL and then use the convenience functions defined in dev/user.clj.

Often user/go, user/stop & user/reset will be used. But if user/reset don't succed we will need to refresh with clojure.tools.namespace.repl/refresh. This since the convenience functions will not be available in the repl.

Using Emacs & Cider a typical workflow is:

```emacs
M x cider-jack-in
> (user/go)
make changes...
> (user/reset)
make changes...
> (user/reset)
> (user/stop)
```

### RabbitMQ
At this point Akvo-notifications require a running RabbitMQ to be able
to run:

* On OS X it's trivial to install with `brew install rabbitmq`
* On Debian/Ubuntu you will need to add the [APT](https://www.rabbitmq.com/install-debian.html#apt) repository

More info is available at the [RabbitMQ site](https://www.rabbitmq.com/download.html)

Once installed, enable the [Management Plugin](https://www.rabbitmq.com/management.html). To verify,
start the server with `rabbitmq-server` and take a look at the [web admin ui](http://server-name:15672/) (username:
guest, password: guest)

## Python

In python/ there is a set of files to test sending things via RabbitMQ to the notification service.

```bash
$ cd python
$ ./setup.sh
$ source ./virt_env/env/bin/activate
$ python ./send-message.py
Message sent!
$ deactivate
```
This should make the notification service se a message containing "Haj from Python!"

## API
A "map" of the REST:ish API is available at the [root resouce](http://localhost:3000). The API supports JSON & EDN. Resources don't use trailing slashes.

## Legal
Copyright (C) 2014 Stichting Akvo (Akvo Foundation)

Distributed under the GNU Affero General Public License (AGPL) 
