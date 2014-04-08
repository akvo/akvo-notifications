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
to run. On OS X it's trivial to install with brew install rabbitmq. On
Ubuntu you will need to add Rabbits APT repository, then it's a matter
of sudo apt-get install rabbitmq-server. On Archlinux erlang also needs
to be installed so: pacman -S erlang rabbitmq should do the trick
(untested). More info is available at the
[RabbitMQ site](https://www.rabbitmq.com/download.html)

Once installed Rabbit can be stared with rabbitmq-server. To verify take
a look at the [web admin ui](http://server-name:15672/) (username:
guest, password: guest)

## API
A "map" of the REST:ish API is available at the [root resouce](http://localhost:3000). The API supports JSON & EDN. Resources don't use trailing slashes.

## Legal
Copyright (C) 2014 Stichting Akvo (Akvo Foundation)

Distributed under the GNU Affero General Public License (AGPL) 
