Agave Stats API
===================

The Agave Stats API provides usage and analytics information about all aspects of the Agave Platform. The API is built on Laravel 4.2 and PHP 5.6. It is distributed as a Docker image `agaveapi/agave-stats` in the Docker Public Registry.

## What is the Agave Platform?

The Agave Platform ([http://agaveapi.co](http://agaveapi.co)) is an open source, science-as-a-service API platform for powering your digital lab. Agave allows you to bring together your public, private, and shared high performance computing (HPC), high throughput computing (HTC), Cloud, and Big Data resources under a single, web-friendly REST API.

* Run scientific codes

  *your own or community provided codes*

* ...on HPC, HTC, or cloud resources

  *your own, shared, or commercial systems*

* ...and manage your data

  *reliable, multi-protocol, async data movement*

* ...from the web

  *webhooks, rest, json, cors, OAuth2*

* ...and remember how you did it

  *deep provenance, history, and reproducibility built in*

For more information, visit the [Agave Developer’s Portal](http://agaveapi.co) at [http://agaveapi.co](http://agaveapi.co).


## Using this image

The Agave Stats API leverages aggressive caching, complex database queries and reverse IP lookups. The easiest way to run this file in a development environment is with the fig.yml file. This will stand up a Redis cache, mysql server, and reverse ip lookup service for you as an orchestrated process. In production, the fig-prod.yml will do the same, but leverage the production database.

### Requirements

You will need to have Docker installed to do anything else in this README. Additionally, to automate, manage, and scale the API, you will need to have Fig installed.

* [Docker](https://docs.docker.com/installation/#installation): an open platform for developers and sysadmins to build, ship, and run distributed applications.
* [Docker Compose](https://docs.docker.com/compose/install/): Fast, isolated orchestrated environments using Docker.

### External dependencies

The Agave Developer APIs require three external services in order to function:

* [MySQL](http://dev.mysql.com/): the world's most popular open-source relational database.
* [Redis](http://www.redis.io/): Redis is an open source, BSD licensed, advanced key-value cache and store. It is often referred to as a data structure server since keys can contain strings, hashes, lists, sets, sorted sets, bitmaps and hyperloglogs.
* [FreeGeoIP](https://freegeoip.net/): freegeoip.net provides a public HTTP API for software developers to search the geolocation of IP addresses. It uses a database of IP addresses that are associated to cities along with other relevant information like time zone, latitude and longitude.

Each of these services is available as a Docker image. You can create containers for them manually and link them to the Agave container(s) or you can orchestrate the process with [Docker Compose](https://docs.docker.com/compose/install/) and the included file. We will use Docker Compose here.

### Starting the API containers

  > docker-compose up -d

Once the containers start (this may take a minute or two), the APIs will be available at:

  > https://docker.example.com/stats/v2/

### Stopping the API containers

To stop the containers, you would make the following command

  > docker-compose kill

    Docker Compose is a simple orchestration tool for running multiple linked containers on a single host. You can replicate the behavior by hand...but that's insane and error prone. Use Fig.

## Getting started

From here you can interact with API at

    https://docker.example.com/stats

Several options are available to you to explore the Agave Developer APIs:

* [Hypermedia](https://docker.example.com/stats): The Agave Stats API is a hypermedia api. You can discover the available routes by visiting the server root.

* [Agave CLI](https://bitbucket.org/taccaci/foundation-cli/src/master/docker/?at=master): a command line interface to the Agave Platform.

  > docker run -i -t --rm -v `$HOME`/.agave:/root/.agave --name agave-cli agaveapi/agave-cli bash

* [Agave Dashboard](https://bitbucket.org/agaveapi/agave-togo): a lightweight client-side, web application for interacting with Agave.

  > docker run -d -t -p 9000:9000 --name agave-togo agaveapi/agave-togo

* [Agave Live Docs](https://agaveapi.co/documentation/live-docs/): a interactive web application allowing you to exercise the APIs without writing any code.

* [Agave Developer Portal](https://agaveapi.co/documentation): the official developer portal for the Agave Platform.
