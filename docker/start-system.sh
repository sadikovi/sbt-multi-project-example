#! /bin/bash

# start ssh server in background
sudo /usr/sbin/sshd -D &

# start HDFS
$HADOOP_INSTALL/sbin/start-dfs.sh

# create log directory for Spark
hdfs dfs -mkdir -p /logs/spark

# start Spark cluster
$SPARK_HOME/sbin/start-all.sh

# display message with all the things
cat << "EOF"
 ____                   _    ____  ____             _
/ ___| _ __   __ _ _ __| | _|___ \|  _ \  ___   ___| | _____ _ __
\___ \| '_ \ / _` | '__| |/ / __) | | | |/ _ \ / __| |/ / _ \ '__|
 ___) | |_) | (_| | |  |   < / __/| |_| | (_) | (__|   <  __/ |
|____/| .__/ \__,_|_|  |_|\_\_____|____/ \___/ \___|_|\_\___|_|
      |_|


In the box:
Essentials:
    - Oracle JDK 7
    - Scala $SCALA_VERSION
    - Python 2.7.6

Distribution:
    - Hadoop $HADOOP_VERSION
    - Apache Spark 1.4.0 with Hadoop 2.6

Util:
    - git
    - sbt $SBT_VERSION
    - vim
    - nano

# SPARK
Spark cluser spins up 2 nodes, master url: spark://sandbox:7077
For Spark UI type in browser: 0.0.0.0:38080
Sometimes redirect in UI does not work, so see below:
Spark worker nodes UI: 0.0.0.0:38081, 0.0.0.0:38082
Spark job UI: 0.0.0.0:34040

# HDFS
Hadoop runs single, one-node cluster, which is more than enough.
HDFS url: hdfs://sandbox:8020
HDFS UI available here: 0.0.0.0:50070

[!] Note
If you run boot2docker you need to replace "0.0.0.0" with boot2docker ip.
To get that ip run: $ boot2docker ip

{ Enjoy }

EOF
