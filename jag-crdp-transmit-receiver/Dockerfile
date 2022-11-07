FROM eclipse-temurin:11-jre-alpine

RUN apk upgrade expat

COPY ./target/jag-crdp-transmit-receiver.jar jag-crdp-transmit-receiver.jar
RUN mkdir .ssh
RUN touch .ssh/known_hosts
RUN chgrp -R 0 .ssh && \
        chmod -R g=u .ssh

ENTRYPOINT ["java", "-jar","/jag-crdp-transmit-receiver.jar"]
