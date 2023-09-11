FROM debian:12.1

LABEL author="zoukang89105@163.com" \
    maintainer="zoukang89105@163.com" \
    descrption="Base openjdk17 image based on debian 12.1"

COPY setup-apt-source.sh /tmp/setup-apt-source.sh
RUN /bin/bash /tmp/setup-apt-source.sh

RUN apt-get update
RUN apt-get install -y openjdk-17-jre
