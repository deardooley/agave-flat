# Agave Core Science API Database Migrations

This project contains the database migrations needed to keep the database in line with the code base. As of 2.1.5, the APIs no longer update the database automatically on startup. This provides better control and planning of database changes as well as an independent way to examine, document, and easily view the impact a change to the database will have on your infrastructure.

All database migrations are handled by Maven via the [Flyway](https://flywaydb.org/) plugin. For detailed documentation and tutorials on Flyway, see the Flyway [website](https://flywaydb.org/documentation/). The essential tasks are bundled as docker-compose commands and listed below.

## Using the migrations

To initialize a new database

```
docker-compose -f config/testbed/migrations.yml migrate
```

To clear the existing database and drop all tables, data, everything

```
docker-compose -f config/testbed/migrations.yml clean
```

To check the current database migration version

```
docker-compose -f config/testbed/migrations.yml info
```

To verify the migration went according to plan

```
docker-compose -f config/testbed/migrations.yml validate
```

Baseline migrations at the current version of the database and ignore any predefined migrations up to and including the baseline version.

```
docker-compose -f config/testbed/migrations.yml migrate
```



## Initializing a new database

