package ca.bc.gov.open.crdp.transmit.models;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateIncomingReqFileResponse implements Serializable {
    private String responseCd;
    private String responseMessageTxt;

    private Integer partOneCount;
    private Integer partTwoCount;
    private String fileName;
    private String dataExchangeFileSeqNo;
    private List<PartOneData> partOneData;
    private List<RegModData> regModData;
    private List<PartTwoData> partTwoData;
}
