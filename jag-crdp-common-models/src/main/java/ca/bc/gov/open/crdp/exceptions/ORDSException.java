package ca.bc.gov.open.crdp.exceptions;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

@SoapFault(
        faultCode = FaultCode.CLIENT,
        faultStringOrReason =
                "An error response was received from ORDS please check that your request is of valid form")
public class ORDSException extends RuntimeException {
    public ORDSException() {
        super();
    }

    public ORDSException(String message) {
        super(message);
    }
}
