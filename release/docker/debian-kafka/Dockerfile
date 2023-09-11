FROM debian:12.1-openjdk17

LABEL author="zoukang89105@163.com" maintainer="zoukang89105@163.com"

ADD ./kafka_2.13-3.5.1.tgz /opt/
RUN ln -s /opt/kafka_2.13-3.5.1 /opt/kafka

EXPOSE 9092 9093

COPY ./docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh
ENTRYPOINT ["/docker-entrypoint.sh"]

STOPSIGNAL SIGQUIT
