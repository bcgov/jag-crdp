package ca.bc.gov.open.crdp.transmit.sender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
/*
   TransmitSend is to send XML file
*/
public class TransmitSenderApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransmitSenderApplication.class, args);
    }
}
