package ca.bc.gov.open.crdp.transmit.models;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveDataExchangeFileRequest implements Serializable {
    private String fileName;
    private byte[] xmlString;
    private String dataExchangeFileSeqNo;
}
