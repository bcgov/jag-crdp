package ca.bc.gov.open.crdp.models;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessReportRequest implements Serializable {
    private String reportFileName;
    private String processedDate;
    private byte[] data;
}
