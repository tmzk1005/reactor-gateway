FROM debian:12.1-openjdk17

ARG rgw_version

LABEL application=rgw version=${rgw_version} \
    author="zoukang89105@163.com" \
    maintainer="zoukang89105@163.com" \
    descrption="Reactor-Gateway: a reacitive http api gateway image, based on debian 12.1 and openjdk17."

RUN mkdir -p /var/log/rgw

ADD reactor-gateway-${rgw_version}.tgz /opt
RUN ln -s /opt/reactor-gateway-${rgw_version} /opt/reactor-gateway

COPY ./docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["help"]

STOPSIGNAL SIGQUIT
