#!/bin/bash

if [[ "$SCHEMA_URL" != "" ]]; then
  curl -sL "$SCHEMA_URL" -o /var/schema.xml
fi

exec java \
    -Djava.rmi.server.hostname="${RMI_BIND}" \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port="${JMX_PORT}" \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Xmx"${JAVA_XMX}" -XX:GCTimeRatio=99 \
    -jar "${SQLTAP_JARFILE}" \
    --config "${SQLTAP_SCHEMA}" \
    --mysql-host "${MYSQL_HOST}" \
    --mysql-port "${MYSQL_PORT}" \
    --mysql-user "${MYSQL_USER}" \
    --mysql-database "${MYSQL_DATABASE}" \
    --mysql-numconns "${MYSQL_NUMCONNS}" \
    --mysql-queuelen "${MYSQL_QUEUELEN}" \
    --memcache-host "${MEMCACHE_HOST}" \
    --memcache-port "${MEMCACHE_PORT}" \
    --memcache-queuelen "${MEMCACHE_QUEUELEN}" \
    --memcache-numconns "${MEMCACHE_NUMCONNS}" \
    --http "${SQLTAP_HTTP_PORT}" \
    --disable-keepalive \
    -t "${SQLTAP_THREADS}" \
    "${SQLTAP_OPTS}"
