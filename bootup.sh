#!/bin/bash
set -ex

SQLTAP_JARFILE="/usr/lib/sqltap.jar"

if [[ "$SCHEMA_URL" != "" ]]; then
  curl -sL "$SCHEMA_URL" -o "${SQLTAP_SCHEMA}"
fi

require_arg() {
  local name="$1"
  local val="${!name}"

  echo "${name}: ${val}"

  if [[ "${val}" == "" ]]; then
    echo "Error. Required argument ${name} missing." 1>&2
    exit 1
  fi
}

require_args() {
  while [[ $# -ne 0 ]]; do
    require_arg $1
    shift
  done
}

require_args SQLTAP_SCHEMA \
             MYSQL_PORT \
             SQLTAP_HTTP_PORT \
             SQLTAP_THREADS \
             SCHEMA_URL \
             MYSQL_HOST \
             MYSQL_PORT \
             MYSQL_USER \
             MYSQL_DATABASE \
             MYSQL_NUMCONNS \
             MYSQL_QUEUELEN \
             JMX_PORT \
             RMI_BIND \
             JAVA_XMX \
             CACHE_BACKEND

opts=""
opts="$opts --config ${SQLTAP_SCHEMA}"
opts="$opts --http ${SQLTAP_HTTP_PORT}"
opts="$opts --disable-keepalive"
opts="$opts -t ${SQLTAP_THREADS}"
opts="$opts --mysql-host ${MYSQL_HOST}"
opts="$opts --mysql-port ${MYSQL_PORT}"
opts="$opts --mysql-user ${MYSQL_USER}"
opts="$opts --mysql-database ${MYSQL_DATABASE}"
opts="$opts --mysql-numconns ${MYSQL_NUMCONNS}"
opts="$opts --mysql-queuelen ${MYSQL_QUEUELEN}"
opts="$opts --cache-backend ${CACHE_BACKEND}"

if [[ "${CACHE_BACKEND}" == "memcache" ]]; then
  require_args MEMCACHE_HOST \
               MEMCACHE_PORT \
               MEMCACHE_QUEUELEN \
               MEMCACHE_NUMCONNS

  opts="$opts --memcache-host ${MEMCACHE_HOST}"
  opts="$opts --memcache-port ${MEMCACHE_PORT}"
  opts="$opts --memcache-queuelen ${MEMCACHE_QUEUELEN}"
  opts="$opts --memcache-numconns ${MEMCACHE_NUMCONNS}"
fi

opts="$opts ${SQLTAP_OPTS}"

exec java \
    -Djava.rmi.server.hostname="${RMI_BIND}" \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port="${JMX_PORT}" \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Xmx"${JAVA_XMX}" -XX:GCTimeRatio=99 -XX:+UseConcMarkSweepGC \
    -jar "${SQLTAP_JARFILE}" \
    $opts
