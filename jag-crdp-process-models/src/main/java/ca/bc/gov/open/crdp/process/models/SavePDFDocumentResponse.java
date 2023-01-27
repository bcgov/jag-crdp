package ca.bc.gov.open.crdp.process.models;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavePDFDocumentResponse implements Serializable {
    private String objectGuid;
    private String responseCd;
    private String responseMessageTxt;
}
