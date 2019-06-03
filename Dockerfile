FROM java:openjdk-8-jre
# TODO (mauricio) update to use newer version of debian
# TODO (mauricio) this is based on the old spotify image, I copied over the same script and supervisor, but I'm not using any of the ENV vars they provide
# TODO (mauricio) ultimately this uses a hardcoded SASL_PLAINTEXT that forces `advertised.listeners` to `SASL_PLAINTEXT://localhost:9092` a lot of this can be simplify
# ~~TODO (mauricio) add fat jar into the image so it can be used.~~ <- DONE
ENV DEBIAN_FRONTEND noninteractive
ENV SCALA_VERSION 2.11
ENV KAFKA_VERSION 2.0.1
ENV KAFKA_HOME /opt/kafka_"$SCALA_VERSION"-"$KAFKA_VERSION"

# Install Kafka, Zookeeper and other needed things
RUN echo "deb [check-valid-until=no] http://cdn-fastly.deb.debian.org/debian jessie main" > /etc/apt/sources.list.d/jessie.list && \
    echo "deb [check-valid-until=no] http://archive.debian.org/debian jessie-backports main" > /etc/apt/sources.list.d/jessie-backports.list && \
    sed -i '/deb http:\/\/deb.debian.org\/debian jessie-updates main/d' /etc/apt/sources.list && \
    echo "Acquire::Check-Valid-Until \"false\";" > /etc/apt/apt.conf.d/100disablechecks && \
    apt-get update && \
    apt-get install -y zookeeper wget supervisor dnsutils vim less && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get clean && \
    wget -q https://archive.apache.org/dist/kafka/"$KAFKA_VERSION"/kafka_"$SCALA_VERSION"-"$KAFKA_VERSION".tgz -O /tmp/kafka_"$SCALA_VERSION"-"$KAFKA_VERSION".tgz && \
    tar xfz /tmp/kafka_"$SCALA_VERSION"-"$KAFKA_VERSION".tgz -C /opt && \
    rm /tmp/kafka_"$SCALA_VERSION"-"$KAFKA_VERSION".tgz

ADD docker-files/scripts /usr/bin
ADD docker-files/scripts/server-plain.properties /opt/kafka_"$SCALA_VERSION"-"$KAFKA_VERSION"/config
#ADD docker-files/scripts/addSaslPlainText.sh /usr/bin
#RUN /usr/bin/addSaslPlainText.sh && \
#    chmod 755 /usr/bin/start-kafka.sh
#ADD docker-files/scripts/kafka_server_plain_jaas.conf /opt/kafka_"$SCALA_VERSION"-"$KAFKA_VERSION"
ADD src/test/resources/kafka_server_vault_jaas.conf /opt/kafka_"$SCALA_VERSION"-"$KAFKA_VERSION"
ADD target/kafka-vault-jca-1.0-SNAPSHOT.jar /opt/kafka_"$SCALA_VERSION"-"$KAFKA_VERSION"/libs/

# Supervisor config
ADD docker-files/supervisor/kafka.conf docker-files/supervisor/zookeeper.conf /etc/supervisor/conf.d/

# 2181 is zookeeper, 9092 is kafka
EXPOSE 2181 9092

CMD ["supervisord", "-n"]