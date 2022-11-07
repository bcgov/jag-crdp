FROM eclipse-temurin:11-jre-alpine

RUN apk upgrade expat

COPY ./target/jag-crdp-process-transformer.jar jag-crdp-process-transformer.jar

ENTRYPOINT ["java", "-jar","/jag-crdp-process-transformer.jar"]
