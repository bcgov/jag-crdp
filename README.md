# jag-crdp

[![Lifecycle:Experimental](https://img.shields.io/badge/Lifecycle-Experimental-339999)](https://github.com/bcgov/jag-crdp)
[![Maintainability](https://api.codeclimate.com/v1/badges/a492f352f279a2d1621e/maintainability)](https://codeclimate.com/github/bcgov/jag-crdp/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/a492f352f279a2d1621e/test_coverage)](https://codeclimate.com/github/bcgov/jag-crdp/test_coverage)

### Recommended Tools
* Intellij
* Docker
* Maven
* Java 11
* Lombok
* RabbitMQ

### Application Endpoints

Local Host: http://127.0.0.1:8080

Actuator Endpoint Local: http://localhost:8080/actuator/health

Code Climate: https://codeclimate.com/github/bcgov/jag-crdp

WSDL Endpoint Local: N/A

### Required Environmental Variables

BASIC_AUTH_PASS: The password for the basic authentication. This can be any value for local

BASIC_AUTH_USER: The username for the basic authentication. This can be any value for local

### Additional Env Variables
* transmit receiver:
1) ORDS_HOST: The url for ords rest package
2) CRON_JOB_OUTGOING_FILE: CRON expression (6-field CRON) for running transmit receiver
3) RECEIVER_QUEUE_NAME: RabbitMQ queue name for receiver messages, up to 255 bytes of UTF-8 characters
4) RECEIVER_ROUTING_KEY: RabbitMQ routing key linking to RECEIVER_QUEUE_NAME
5) RABBIT_EXCHANGE_NAME: RabbitMQ direct exchange name, which links a pair of routing key and queue name
6) RABBIT_MQ_HOST: RabbitMQ host, 'localhost' by default if installing a RabbitMQ on a local computer
7) RABBIT_MQ_USERNAME: RabbitMQ host username
8) RABBIT_MQ_PASSWORD: RabbitMQ host password

* transmit sender:
1) ORDS_HOST: The url for ords rest package
2) OUTGOING_FILE_DIR: The path to where the outgoing file is sent on SFEG server
3) RECEIVER_QUEUE_NAME: RabbitMQ queue name for receiver messages, up to 255 bytes of UTF-8 characters
4) RABBIT_MQ_HOST: RabbitMQ host, 'localhost' by default if installing a RabbitMQ on a local computer
5) RABBIT_MQ_USERNAME: RabbitMQ host username
6) RABBIT_MQ_PASSWORD: RabbitMQ host password
7) SFEG_USERNAME: The username to access SFEG server
8) SFEG_HOST: The url to SFEG server
9) SFTP_KNOWN_HOSTS: The location of known_hosts file
10) SFTP_PRIVATE_KEY: The location of private security key for accessing SFEG server

* process scanner:
1) SCANNER_QUEUE_NAME: RabbitMQ queue name for scanner messages, up to 255 bytes of UTF-8 characters
2) SCANNER_ROUTING_KEY: RabbitMQ routing key linking to SCANNER_QUEUE_NAME
3) RABBIT_EXCHANGE_NAME: RabbitMQ direct exchange name, which links a pair of routing key and queue name
4) RABBIT_MQ_HOST: RabbitMQ host, 'localhost' by default if installing a RabbitMQ on a local computer
5) RABBIT_MQ_USERNAME: RabbitMQ host username
6) RABBIT_MQ_PASSWORD: RabbitMQ host password
7) INCOMING_FILE_DIR: The path at where the incoming file is stored on SFEG server
8) PROCESSING_DIR: The path at where the in-progress file is stored on SFEG server
9) CRON_JOB_INCOMING_FILE: CRON expression (6-field CRON) for running process scanner
10) RECORD_TTL_HOUR: TTL in hours for clearing ERRORS and COMPLETED directory
11) SFEG_USERNAME: The username to access SFEG server
12) SFEG_HOST: The url to SFEG server
13) SFTP_KNOWN_HOSTS: The location of known_hosts file
14) SFTP_PRIVATE_KEY: The location of private security key for accessing SFEG server
15) SFTP_ENABLED: true/false on whether SFTP is used (if false - only in local testing, local file system will be used)

* process transformer:
1) ORDS_HOST: The url for ords rest package
2) SCANNER_QUEUE_NAME: RabbitMQ queue name for scanner messages, up to 255 bytes of UTF-8 characters
3) RABBIT_MQ_USERNAME: RabbitMQ host username
4) RABBIT_MQ_PASSWORD: RabbitMQ host password
5) RABBIT_MQ_HOST: RabbitMQ host, 'localhost' by default if installing a RabbitMQ on a local computer
6) PROCESSING_DIR: The path at where the in-progress file is stored on SFEG server
7) COMPLETED_DIR: The path at where the completed file is stored on SFEG server
8) ERRORS_DIR: The path at where the erred file is stored on SFEG server
9) SFEG_USERNAME: The username to access SFEG server
10) SFEG_HOST: The url to SFEG server
11) SFTP_KNOWN_HOSTS: The location of known_hosts file
12) SFTP_PRIVATE_KEY: The private security key for accessing SFEG server
13) SFTP_ENABLED: true/false on whether SFTP is used (if false - only in local testing, local file system will be used)

### Optional Environmental Variables
SPLUNK_HTTP_URL: The url for the splunk hec.

SPLUNK_TOKEN: The bearer token to authenticate the application.

SPLUNK_INDEX: The index that the application will push logs to. The index must be created in splunk
before they can be pushed to.

### Building the Application
1) Make sure using java 11 for the project modals and sdk
2) Run ```mvn compile```
3) Make sure ```jag-crdp-common-models```, ```jag-crdp-transmit-models``` and ```jag-crdp-process-models``` are marked as generated sources roots (xjc)

### Pre-running the application
Run ```docker run -p 5672:5672 -p 15672:15672 rabbitmq:management```

### Running the application
Option A) Intellij
1) Set env variables.
2) Run the application

Option B) Jar, e.g., to run 'jag-crdp-transmit-receiver' application
1) Run ```mvn package```
2) Run ```cd jag-crdp-transmit-receiver```
3) Run ```java -jar ./target/jag-crdp-transmit-receiver.jar $ENV_VAR$```  (Note that $ENV_VAR$ are environment variables)

Option C) Docker, e.g., to run 'jag-crdp-transmit-receiver' application
1) Run ```mvn package```
2) Run ```cd jag-crdp-transmit-receiver```
3) Run ```docker build -t jag-crdp-transmit-receiver .``` from root folder
4) Run ```docker run -p 8080:8080 jag-crdp-transmit-receiver $ENV_VAR$```  (Note that $ENV_VAR$ are environment variables)

### Running RabbitMQ
* http://localhost:15672/
* Username: 'guest' by default
* Password: 'guest' by default

### Pre Commit
1) Do not commit \CRLF use unix line enders
2) Run the linter ```mvn spotless:apply```

### JaCoCo Coverage Report
1) Run ```mvn clean verify```
2) Open ```crdp-code-coverage/target/site/jacoco/index.html``` in a browser
