FROM eclipse-temurin:11-jre-alpine

RUN apk upgrade expat

COPY ./target/jag-crdp-transmit-sender.jar jag-crdp-transmit-sender.jar

ENTRYPOINT ["java", "-jar","/jag-crdp-transmit-sender.jar"]
