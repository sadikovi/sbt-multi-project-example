FROM ubuntu:14.04

MAINTAINER Ivan Sadikov <sadikovi@docker.com>


################################################################
# Installing some dependencies
################################################################
RUN sudo apt-get update
RUN sudo apt-get install -y software-properties-common
RUN sudo apt-get install -y wget


################################################################
# Install OpenJDK and Scala
################################################################

ENV SCALA_VERSION=2.10.4

RUN wget http://www.scala-lang.org/files/archive/scala-$SCALA_VERSION.deb

RUN sudo dpkg -i --ignore-depends=openjdk-6-jre,libjansi-java ./scala-$SCALA_VERSION.deb && \
    sudo rm scala-$SCALA_VERSION.deb

RUN sudo apt-get -fy install


################################################################
# Install Oracle JDK 7
################################################################

ENV INSTALL_DIR=/usr/share

RUN wget --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/7u80-b15/jdk-7u80-linux-x64.tar.gz
RUN sudo tar -xf ./jdk-7u80-linux-x64.tar.gz --directory $INSTALL_DIR && \
    sudo rm jdk-7u80-linux-x64.tar.gz

RUN sudo update-alternatives --install "/usr/bin/java" "java" "$INSTALL_DIR/jdk1.7.0_80/bin/java" 9999 && \
    sudo update-alternatives --install "/usr/bin/javac" "javac" "$INSTALL_DIR/jdk1.7.0_80/bin/javac" 9999

RUN sudo chmod a+x /usr/bin/java && \
    sudo chmod a+x /usr/bin/javac && \
    sudo chown -R root:root $INSTALL_DIR/jdk1.7.0_80


################################################################
# Install Python 2.7.6
################################################################
RUN sudo apt-get install -y python-software-properties


################################################################
# Install Apache Spark 1.4.0 with Hadoop 2.6
################################################################
RUN wget http://d3kbcqa49mib13.cloudfront.net/spark-1.4.0-bin-hadoop2.6.tgz

RUN tar -xf spark-1.4.0-bin-hadoop2.6.tgz -C /usr/local/ && \
    sudo rm spark-1.4.0-bin-hadoop2.6.tgz

RUN ln -s /usr/local/spark-1.4.0-bin-hadoop2.6 /usr/local/spark

ENV SPARK_HOME=/usr/local/spark
ENV PATH=$PATH:$SPARK_HOME/bin


################################################################
# Install utilities (git, sbt, and etc.)
################################################################

# git:latest
RUN sudo apt-get install -y git

# sbt 0.13.8
ENV SBT_VERSION=0.13.8
RUN wget https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb
RUN sudo dpkg -i sbt-$SBT_VERSION.deb && \
    sudo rm sbt-$SBT_VERSION.deb


################################################################
# Install openssh server and spin up Spark cluster mode
################################################################

# all the stuff with SSH to open localhost for Spark without password prompt
RUN sudo apt-get install -y openssh-server
RUN sudo mkdir /var/run/sshd
RUN ssh-keygen -f ~/.ssh/id_rsa -t rsa -N ''
RUN cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys

# copy all necessary files for running Spark cluster
COPY ./start-cluster.sh $SPARK_HOME/
RUN chmod u+x $SPARK_HOME/start-cluster.sh

# 8080 - Spark main UI, 8081 - Spark worker node, 4040 - Spark job UI
EXPOSE 22 8080 8081 4040

CMD bash -C "$SPARK_HOME/start-cluster.sh"; "/bin/bash"
