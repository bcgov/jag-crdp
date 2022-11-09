package ca.bc.gov.open.crdp.process.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveErrorResponse implements Serializable {
    private String resultCd;
    private String responseMessageTxt;
}
