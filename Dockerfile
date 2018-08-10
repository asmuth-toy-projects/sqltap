FROM java:8
MAINTAINER DaWanda <cupcakes@dawanda.com>

ARG SBT_VERSION="0.13.8"

ENV SQLTAP_HTTP_PORT="3000" \
    SQLTAP_THREADS="16" \
    SQLTAP_SCHEMA="/etc/sqltap-schema.xml" \
    SCHEMA_URL="" \
    SQLTAP_OPTS="" \
    MYSQL_HOST="127.0.0.1" \
    MYSQL_PORT="3306" \
    MYSQL_USER="fetch" \
    MYSQL_DATABASE="test" \
    MYSQL_NUMCONNS="6" \
    MYSQL_QUEUELEN="2500" \
    JMX_PORT="9191" \
    RMI_BIND="127.0.0.1" \
    JAVA_XMX="16384M" \
    CACHE_BACKEND="memcache" \
    MEMCACHE_HOST="" \
    MEMCACHE_PORT="11211" \
    MEMCACHE_QUEUELEN="8192" \
    MEMCACHE_NUMCONNS="20"

ADD https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb /tmp/sbt.deb
RUN dpkg -i /tmp/sbt.deb && rm -f /tmp/sbt.deb
RUN apt-get update && apt-get install sbt

ADD project /usr/src/project/
ADD src /usr/src/src/
ADD build.sbt /usr/src/

RUN cd /usr/src && \
    sbt assembly && \
    cp -vpi /usr/src/target/scala-*/sqltap.jar /usr/lib/sqltap.jar && \
    rm -rf /usr/src/*

EXPOSE $SQLTAP_HTTP_PORT

ADD bootup.sh /bootup.sh
CMD ["/bootup.sh"]
