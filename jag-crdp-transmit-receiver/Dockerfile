FROM eclipse-temurin:11-jre-alpine

RUN apk upgrade expat

COPY ./target/jag-crdp-transmit-receiver.jar jag-crdp-transmit-receiver.jar

ENTRYPOINT ["java", "-jar","/jag-crdp-transmit-receiver.jar"]
