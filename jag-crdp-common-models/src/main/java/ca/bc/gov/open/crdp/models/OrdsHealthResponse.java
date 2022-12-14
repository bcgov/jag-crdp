package ca.bc.gov.open.crdp.models;

import lombok.Getter;

@Getter
public class OrdsHealthResponse {
    public String appid;
    public String method;
    public String status;
    public String host;
    public String instance;
    public String version;
    public String compatibility;
}
