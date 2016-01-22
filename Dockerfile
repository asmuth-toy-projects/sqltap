FROM java:8

MAINTAINER DaWanda <cupcakes@dawanda.com>

ENV SCALA_VERSION="2.11.7" \
    SBT_VERSION="0.13.8" \
    SQLTAP_VERSION="0.8.1"

ADD http://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz /tmp/scala.tgz
RUN tar -C /opt -xzf /tmp/scala.tgz && rm -f /tmp/scala.tgz

ENV PATH="/optscala-$SCALA_VERSION/bin:$PATH"

ADD https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb /tmp/sbt.deb
RUN dpkg -i /tmp/sbt.deb && rm -f /tmp/sbt.deb
RUN apt-get update && apt-get install sbt

RUN sbt

ADD . /opt/sqltap
WORKDIR /opt/sqltap
RUN sbt assembly

ENV SQLTAP_HTTP_PORT="3000" \
    SQLTAP_THREADS="16" \
    SQLTAP_JARFILE="/opt/sqltap/target/scala-2.11/sqltap-${SQLTAP_VERSION}.jar" \
    SQLTAP_SCHEMA="/var/schema.xml" \
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
    HOSTNAME="sqltap1" \
    MEMCACHE_HOST="127.0.0.1" \
    MEMCACHE_PORT="11211" \
    MEMCACHE_QUEUELEN="8192" \
    MEMCACHE_NUMCONNS="20"

CMD ["/opt/sqltap/sqltap.sh"]
