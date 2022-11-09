FROM eclipse-temurin:11-jre-alpine

RUN apk upgrade expat

COPY ./target/jag-crdp-process-scanner.jar jag-crdp-process-scanner.jar
RUN mkdir .ssh
RUN touch .ssh/known_hosts
RUN touch .ssh/id_rsa
RUN chgrp -R 0 .ssh && \
        chmod -R g=u .ssh

ENTRYPOINT ["java", "-jar","/jag-crdp-process-scanner.jar"]
