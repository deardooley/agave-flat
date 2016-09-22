#!/bin/bash

sed -i 's#https://docker.example.com/docs/v2/resources#http://'$(hostname)'/docs/resources#' /var/www/html/docs/resources/index
for i in `ls /var/www/html/docs/resources/`
do
  sed -i 's#"https://docker.example.com"#"https://public.agaveapi.co"#g' /var/www/html/docs/resources/$i
done
