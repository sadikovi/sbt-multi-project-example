FROM ubuntu:14.04

MAINTAINER Ivan Sadikov <sadikovi@docker.com>


################################################################
# Installing some dependencies
################################################################
RUN apt-get update
RUN apt-get install -y software-properties-common
RUN apt-get install -y wget


################################################################
# Install OpenJDK and Scala
################################################################

ENV SCALA_VERSION=2.10.4

RUN wget http://www.scala-lang.org/files/archive/scala-$SCALA_VERSION.deb

RUN dpkg -i --ignore-depends=openjdk-6-jre,libjansi-java ./scala-$SCALA_VERSION.deb && \
    rm scala-$SCALA_VERSION.deb

RUN apt-get -fy install


################################################################
# Install Oracle JDK 7
################################################################

ENV INSTALL_DIR=/usr/share

RUN wget --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/7u80-b15/jdk-7u80-linux-x64.tar.gz
RUN tar -xf ./jdk-7u80-linux-x64.tar.gz --directory $INSTALL_DIR && \
    rm jdk-7u80-linux-x64.tar.gz

ENV JAVA_HOME=$INSTALL_DIR/jdk1.7.0_80

RUN update-alternatives --install "/usr/bin/java" "java" "$JAVA_HOME/bin/java" 9999 && \
    update-alternatives --install "/usr/bin/javac" "javac" "$JAVA_HOME/bin/javac" 9999

RUN chmod a+x /usr/bin/java && \
    chmod a+x /usr/bin/javac && \
    chown -R root:root $JAVA_HOME


################################################################
# Install Python 2.7.6
################################################################
RUN apt-get install -y python-software-properties


################################################################
# Install utilities (git, sbt, and etc.)
################################################################

# git:latest
RUN sudo apt-get install -y git

# sbt 0.13.8
ENV SBT_VERSION=0.13.8
RUN wget https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb
RUN dpkg -i sbt-$SBT_VERSION.deb && \
    rm sbt-$SBT_VERSION.deb


################################################################
# Install openssh server
################################################################

# all the stuff with SSH to open localhost for Spark without password prompt
RUN apt-get install -y openssh-server
RUN mkdir /var/run/sshd
RUN ssh-keygen -f ~/.ssh/id_rsa -t rsa -N ''
RUN cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys


################################################################
# Install Apache Spark 1.4.0 with Hadoop 2.6
################################################################
RUN wget http://d3kbcqa49mib13.cloudfront.net/spark-1.4.0-bin-hadoop2.6.tgz

RUN tar -xf spark-1.4.0-bin-hadoop2.6.tgz -C /usr/local/ && \
    rm spark-1.4.0-bin-hadoop2.6.tgz

RUN ln -s /usr/local/spark-1.4.0-bin-hadoop2.6 /usr/local/spark

ENV SPARK_HOME=/usr/local/spark
ENV PATH=$PATH:$SPARK_HOME/bin

COPY ./docker/spark/spark-env.sh $SPARK_HOME/conf/


################################################################
# Install Hadoop 2.6
################################################################
ENV HADOOP_VERSION=2.6.0
ENV HADOOP_INSTALL=/usr/local/hadoop
ENV PATH=$PATH:$HADOOP_INSTALL/bin:$HADOOP_INSTALL/sbin
ENV HADOOP_MAPRED_HOME=$HADOOP_INSTALL
ENV HADOOP_COMMON_HOME=$HADOOP_INSTALL
ENV HADOOP_HDFS_HOME=$HADOOP_INSTALL
ENV YARN_HOME=$HADOOP_INSTALL
ENV HADOOP_COMMON_LIB_NATIVE_DIR=$HADOOP_INSTALL/lib/native
ENV HADOOP_OPTS="-Djava.library.path=$HADOOP_INSTALL/lib"

# add hadoop group and including root into hadoop
RUN sudo addgroup hadoop
RUN sudo usermod -G hadoop -a 'root'

# download hadoop distribution
RUN wget http://mirrors.sonic.net/apache/hadoop/common/hadoop-$HADOOP_VERSION/hadoop-$HADOOP_VERSION.tar.gz
RUN tar -xf hadoop-$HADOOP_VERSION.tar.gz -C /usr/local/ && \
    rm hadoop-$HADOOP_VERSION.tar.gz

RUN ln -s /usr/local/hadoop-$HADOOP_VERSION $HADOOP_INSTALL

# add temp directory
RUN mkdir /tmp/hadoop
RUN sudo chown root:hadoop /tmp/hadoop

# copy hadoop-env.sh, force JAVA_HOME
COPY ./docker/hadoop/hadoop-env.sh $HADOOP_INSTALL/etc/hadoop/
# copy core-site.xml with new version of temp directory
COPY ./docker/hadoop/core-site.xml $HADOOP_INSTALL/etc/hadoop/
# copy mapred-site.xml
COPY ./docker/hadoop/mapred-site.xml $HADOOP_INSTALL/etc/hadoop/

# create directories for datanode and namenode
ENV HADOOP_STORE=/usr/local/hadoop_store

RUN mkdir -p $HADOOP_STORE/hdfs/namenode
RUN mkdir -p $HADOOP_STORE/hdfs/datanode
RUN sudo chown -R root:hadoop $HADOOP_STORE

# update hdfs-site.xml
COPY ./docker/hadoop/hdfs-site.xml $HADOOP_INSTALL/etc/hadoop/

# formatting hadoop file system
RUN hadoop namenode -format



# Once everything is set up we copy files and start system
# copy all necessary files for running Spark cluster
COPY ./docker/start-system.sh /usr/local/
RUN chmod u+x /usr/local/start-system.sh
# copy updated ssh configuration
COPY ./docker/ssh/ssh_config /etc/ssh/ssh_config

# Spark ports: 8080 - Spark main UI, 8081, 4040 - Spark job UI
EXPOSE 4040 8080 8081
# Hadoop ports
EXPOSE 50070 50090 54310

CMD bash -C "/usr/local/start-system.sh"; "/bin/bash"
