FROM eclipse-temurin:11-jre-alpine

RUN apk upgrade expat

COPY ./target/jag-crdp-process-scanner.jar jag-crdp-process-scanner.jar

ENTRYPOINT ["java", "-jar","/jag-crdp-process-scanner.jar"]
