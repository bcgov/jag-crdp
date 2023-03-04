package ca.bc.gov.open.crdp.process.models;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveErrorRequest implements Serializable {
    private String errMsg;
    private String errorDt;
    private String fileName;
    private byte[] fileContentXml;
}
