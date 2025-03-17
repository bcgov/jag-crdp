FROM eclipse-temurin:17-jre-alpine

RUN apk update && apk add --upgrade --no-cache expat # fix CVE-2024-8176

COPY ./target/jag-crdp-transmit-sender.jar jag-crdp-transmit-sender.jar
RUN mkdir .ssh
RUN touch .ssh/known_hosts
RUN touch .ssh/id_rsa
RUN chgrp -R 0 .ssh && \
        chmod -R g=u .ssh

RUN mkdir temp-xmls
RUN chgrp -R 0 temp-xmls && \
        chmod -R g=u temp-xmls

ENTRYPOINT ["java", "-jar","/jag-crdp-transmit-sender.jar"]
