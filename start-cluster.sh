#! /bin/bash

# open ssl
sudo /usr/sbin/sshd -D &
# start Spark cluster
$SPARK_HOME/sbin/start-all.sh
