#!/bin/bash

sed -i 's#https://docker.example.com/docs/v2/resources#http://'$HOSTNAME'/docs/resources#' /var/www/html/docs/resources/index
