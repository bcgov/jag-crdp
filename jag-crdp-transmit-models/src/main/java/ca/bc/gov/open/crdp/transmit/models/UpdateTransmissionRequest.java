package ca.bc.gov.open.crdp.transmit.models;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTransmissionRequest implements Serializable {
    private List<String> partOneIds;
    private List<String> regModIds;
    private List<String> partTwoIds;
    private String dataExchangeFileSeqNo;
    private String currentDate;
    private String fileName;
    private byte[] xmlString;
}
