FROM eclipse-temurin:17-jre-alpine

RUN apk update && apk add --upgrade --no-cache expat # fix CVE-2024-8176

COPY ./target/jag-crdp-process-transformer.jar jag-crdp-process-transformer.jar

# Add xsdSchemas to docker image to support xml validation
COPY ./src/main/resources/xsdSchemas xsdSchemas

RUN mkdir .ssh
RUN touch .ssh/known_hosts
RUN touch .ssh/id_rsa
RUN chgrp -R 0 .ssh && \
        chmod -R g=u .ssh

ENTRYPOINT ["java", "-jar","/jag-crdp-process-transformer.jar"]
