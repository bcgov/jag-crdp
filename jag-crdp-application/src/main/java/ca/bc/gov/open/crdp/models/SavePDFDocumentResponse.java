package ca.bc.gov.open.crdp.models;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavePDFDocumentResponse implements Serializable {
    private String objectGuid;
    private String resultCd;
    private String resultMsg;
}