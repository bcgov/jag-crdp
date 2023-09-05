FROM openjdk:11-jre-slim

COPY ./jag-crdp-application/target/jag-crdp-application.jar jag-crdp-application.jar

ENTRYPOINT ["java", "-jar","/jag-crdp-application.jar"]
