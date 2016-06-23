# Agave External Build Tool Scripts

Eclipse will run all external build tools by wrapping them in a `sh -c` command. The implications of this are that your .bashrc file will not be called and, thus, your environment is not guaranteed to be present. The simplest way to work around this is to add any environment variables, aliases, etc into your system's default user environment at `/etc/profile` file.

## OSX

On OSX, you should run the following commands to ensure the external build tools in this folder can find the Docker Daemon and Docker Machine services.

```
sudo chmod o+w /etc/profile
sudo echo 'export PATH=$PATH:/usr/local/bin' >> /etc/profile
sudo echo "eval $(docker-machine env $DOCKER_MACHINE_NAME)" >> /etc/profile
sudo chmod o-w /etc/profile
```

## Linux

On Linux, you probably are not using Docker Machine, so it is sufficient to set the `DOCKER_HOST` variable pointing to your local install.

```
sudo chmod o+w /etc/profile
sudo echo 'export PATH=$PATH:/usr/local/bin' >> /etc/profile
sudo echo 'export DOCKER_HOST=unix:///var/run/docker.sock' >> /etc/profile
sudo chmod o-w /etc/profile
```
