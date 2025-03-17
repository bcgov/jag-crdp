FROM eclipse-temurin:17-jre-alpine

RUN apk update && apk add --upgrade --no-cache expat # fix CVE-2024-8176

COPY ./target/jag-crdp-transmit-receiver.jar jag-crdp-transmit-receiver.jar

ENTRYPOINT ["java", "-jar","/jag-crdp-transmit-receiver.jar"]
