# Agave Core Science APIs

> *This repository contains the back-end APIs for the Agave Platform. These APIs are not meant to run in isolation and, in and of themselves, are not sufficient to deliver the intended Science-as-a-Service functionality. For a full deployment of the Agave Platform, please see the [Agave Platform](https://bitbucket.org/agaveapi/platform) repository.*

The Agave Science APIs are a set of REST APIs that provide the core Science-as-a-Service functionality to the Agave Platform. Each API runs independently as an separate process. The APIs themselves do not handle authentication. Rather, they look for a signed Java Web Token (JWT) in each request header from which they obtain the identity, scope, authorization information, and other contextual information about the caller. The JWT are generated by the Agave OAuth2 server, which is part of the [Agave Container Tenant Infrastructure Management](https://bitbucket.org/agaveapi/cotim) project. 

For ease of deployment, and easier scaling from demo to heavy utilization scenarios, we build and deploy the entire Agave Platform as a collection of [Docker](http://docker.com) images. This README will walk you through the process of running a development instance of Agave for yourself. For more information about the use cases satisfied by the APIs as well as functional documentation about their design, use, and performance, please see the [Agave Developer’s Website](http://agaveapi.co). To get started using the APIs right away, pull down the Agave CLI as [source](https://bitbucket.org/agaveapi/cli/) or a [Docker image](https://hub.docker.com/u/agaveapi/cli) and take it for a spin.


## Installation

The Agave Developer APIs are distributed as multiple Docker images. This repository serves as a aggregation point for all the different services and a common configuration point used to propagate various configuration options when compiling from source. One of the things we love about Docker is that it simplifies our deployment environments. Each API is built into its own image and that exact same image is used in development, staging, and production. Because of this, we make all our images available publicly in the [Docker Public Registry](http://hub.docker.com/u/agaveapi/). 

One of the other great things about Docker is the ecosystem of tooling around the container technology itself. The Agave core services are currently comprised of over a dozen different containers. Starting, stopping, monitoring, etc all those containers would be challenging on its own. Thankfully, [Docker Compose](https://docs.docker.com/compose/) is available. Compose is an orchestration tool for managing Docker containers and their dependencies. Included in this repository you will find a `docker-compose.yml.SAMPLE` file which you can use to stand up, configure, and scale the entire stack, dependencies and all. 

In the following sections we walk you through what it takes to stand up and interact with the stack. For more information, including lots of useful tutorials and examples on using Agave to power your own digital lab, pleas visit the [Agave Developer's Site](http://agaveapi.co).

## Requirements

You will need to have the Docker Engine installed to run the API containers and their dependencies. You will need to have Docker Compose installed to automate, manage, and scale the APIs. Both technologies have native installers for every platform available from the links below.

* [Docker](https://docs.docker.com/installation/#installation): an open platform for developers and sysadmins to build, ship, and run distributed applications.
* [Docker Compose](https://docs.docker.com/compose/): Compose is a tool for defining and running multi-container applications with Docker.

If you intend on building the APIs from scratch, you will also need the following: 

* [Java 7]
* [PHP 5.5+]
* [Maven 3.2+]

### Port ranges

When you start up the APIs, each API will be assigned a dynamic port. This is purely for development purposes. In order to access the APIs, you will always point your requests to the proxy container which serves as the web root of the APIs. The proxy image handles URL rewriting, load balancing, and port mapping so you do not have to worry about runtime changes if you restart or scale a running API.

The proxy container exposes ports `80`, and `443`. By default, SSL is disabled and all SSL traffic is redirected to port 80. This is **NOT** recommended for production or public use. We default to port 80 here so you do not need to deal with hostname validation, creating and reassigning SSL certs, and importing public keys to the Agave API Manager if you bring it up. It short, we use port 80 because it makes things easier for you to stand up and kick around quickly. 

In the event you will be interacting with an FTP or GridFTP server, you will also need to make sure that the Docker host on which the APIs are running has the standard FTP and GridFTP port ranges installed. Subsets of these port ranges are exposed by default in both the API and worker containers and are necessary to avoid performance penalties when interacting with remote servers over these protocols.

| **Protocol** | **Ports**                           |
|--------------|-------------------------------------|
| ftp          | 21, 30000, 31000                    |
| gridftp      | 21, 2811, 50000, 51000              |


### DNS

Finally, to make networking a little simpler, and to avoid diving into the rabbit hole that is overlay networking, please add `docker.example.com` to your `/etc/hosts` file. This hostname will be used throughout the documentation to avoid problems with service discovery, firewalls, NAT, and philosophical differences in the way different operating system differences handle networking.

### Third-party services

The Agave Developer APIs require three external services in order to function:

* [MariaDB](https://mariadb.org/): An enhanced, drop-in replacement for MySQL.
* [MongoDB](http://www.mongodb.org/): an open-source document database, and the leading NoSQL database.
* [Beanstalkd](http://kr.github.io/beanstalkd/): a simple, fast work queue.

Each of these services is available as a Docker image. We primarily include them here to make you aware of the dependencies. In practice, these services will be deployed in Docker containers along with the core services, so you don't need to worry about them.

> If you plan on testing out email notifications, you will need to edit the `docker-compose.yml` file to include your email configuration settings. By default, email notifications are serialized to JSON and writting to the service logs.  
 

## Building

> ***The Agave Core APIs are automatically built and tested as part of our CI/CD workflow on every commit. Docker images for successful builds are automatically creatd and pushed into the [Docker public registry](http://docker.com/u/agaveapi). Unless you are actively developing against the code base, you do not need to build the images yourself. The `docker-compose.yml` file will guarantee you have the latest version of the APIs running.***  
  
To create a development build of the APIs, you will need to check out this project and its submodules, then kick off the maven build. The following commands will build and package each API, build its Docker image, and tag it with the current version and project commit hash. 

```  
$ git clone http://bitbucket.org/agaveapi/science-apis.git agave-science-apis
$ cd agave-science-apis
$ git submodule init
$ git submodule update
$ mvn -s config/maven/settings.xml -P build,publish clean deploy
$ cp docker-compose.yml.SAMPLE docker-compose.yml
``` 

If you are not actively developing the APIs, skip the checkout and build and simply download the `docker-compose.yml` file. 

``` 
$ mkdir agave-science-apis
$ cd agave-science-apis
$ curl -sk -o docker-compose.yml https://bitbucket.org/agaveapi/agave/raw/master/docker-compose.yml.SAMPLE  
``` 

## Running

The `docker-compose.yml` file from the previous step will be used to stop, start, and scale the APIs and their dependencies.


### Starting

The APIs can be started using the following command. Once the containers start (this may take a minute or two), they will be available at: [http://<docker_host>/docs](http://<docker_host>/docs).

```
$ docker-compose agave up -d 
```

### Stopping

The APIs can be started using the following command. This will halt all containers while maintaining their data and runtime configuration.

```
$ docker-compose stop  
```

### Updating

The Agave Science APIs are under active development, so it's a good idea to update your images regularly. You can update all images and restart your container with the following commands.

```
$ docker-compose pull
$ docker-compose kill
$ docker-compose rm -f
$ docker-compose up -d
```

## Getting started

Once you have the APIs running, you should initialize them with some sample data. You can do this using the initialization scripts included in the repository:

``` 
$ curl -sk -o agave-init.sql https://bitbucket.org/agaveapi/agave/raw/master/agave-db-migrations/src/main/resources/db/migration/V2.1.3__Base_version.sql
$ docker run -it --rm \
			 -v $(pwd)/agave-init.sql:/data/agave-init.sql \
			 -link agavescienceapis_mysql_1:mysql
			 mysql:latest \
			 mysql -u agaveuser -Ppassword -p 3301 -h docker.example.com << /data/agave-init.sql
``` 

This will create a default tenant and add some sample data to get you started.  
 
### Interactive docs

The APIs ship with a working instance of the Agave Live Docs. The Live Docs are a preconfigured version of Swagger UI, customized for use with Agave. You can visit the Live Docs for the APIs you just started at [https://<docker_host>/docs](https://<docker_host>/docs).  
		
### Platform tooling 

Several options are available to you to explore your instance of the Agave Core Science APIs:

* [Agave CLI](https://bitbucket.org/agaveapi/cli/src/master/docker/): a command line interface to the Agave Platform.  

    ``` $ docker run -i -t --rm -v $HOME/.agave:/root/.agave --name agave-cli agaveapi/agave-cli bash  ```
  
* [Agave ToGo](https://bitbucket.org/agaveapi/agave-togo): a lightweight client-side, web application for interacting with Agave.  

    ``` $ docker run -d -t -p 9000:9000 --name agave-togo agaveapi/agave-togo  ```
  
### Documentation

Full documentation about the use cases satisfied by the APIs as well as functional documentation about their design, use, and performance, please see the [Agave Developer’s Website](http://agaveapi.co).  
 
* [Agave Developer Portal](https://agaveapi.co/documentation): the official developer portal for the Agave Platform.