FROM debian:11.6

LABEL author="zoukang89105@163.com" maintainer="zoukang89105@163.com"

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

COPY setup-apt-source.sh /tmp/setup-apt-source.sh
RUN /bin/bash /tmp/setup-apt-source.sh

RUN apt-get install -y libcurl4

ADD ./mongodb-linux-x86_64-debian11-6.0.9.tgz /opt/
RUN ln -s /opt/mongodb-linux-x86_64-debian11-6.0.9 /opt/mongodb

RUN mkdir -p /data/db

EXPOSE 27017

COPY ./docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh
ENTRYPOINT ["/docker-entrypoint.sh"]

STOPSIGNAL SIGQUIT