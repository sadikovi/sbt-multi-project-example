#! /bin/bash

# start ssh server in background
sudo /usr/sbin/sshd -D &

# start Spark cluster
$SPARK_HOME/sbin/start-all.sh

# start HDFS
$HADOOP_INSTALL/sbin/start-dfs.sh

# display message with all the things

cat << EOF
   _   ___ _____ _     ___          _
  /_\ / __|_   _/_\   |   \ ___  __| |_____ _ _
 / _ \ (__  | |/ _ \  | |) / _ \/ _| / / -_) '_|
/_/ \_\___| |_/_/ \_\ |___/\___/\__|_\_\___|_|

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
