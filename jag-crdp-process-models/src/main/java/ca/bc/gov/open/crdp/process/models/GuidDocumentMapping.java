package ca.bc.gov.open.crdp.process.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuidDocumentMapping {
    private String file_name;
    private String object_guid;
}
