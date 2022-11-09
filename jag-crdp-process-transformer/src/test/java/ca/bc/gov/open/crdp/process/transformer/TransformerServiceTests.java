package ca.bc.gov.open.crdp.process.transformer;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

import ca.bc.gov.open.crdp.exceptions.ORDSException;
import ca.bc.gov.open.crdp.process.models.*;
import ca.bc.gov.open.crdp.process.transformer.services.TransformerService;
import ca.bc.gov.open.sftp.starter.SftpProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.mockito.AdditionalMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TransformerServiceTests {

    private String inFileDir;

    @Mock private ObjectMapper objectMapper;
    @Mock private RestTemplate restTemplate;
    @Mock private TransformerService controller;
    @Mock private SftpProperties sftpProperties;

    @BeforeAll
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        controller =
                Mockito.spy(new TransformerService(restTemplate, objectMapper, sftpProperties));

        String appPath = new File("").getCanonicalPath();
        inFileDir = appPath + "/src/test/resources/test/processingIncoming/";

        File backupFolder = new File(appPath + "/src/test/resources/backup/");
        File testFolder = new File(appPath + "/src/test/resources/test/");

        try {
            FileUtils.deleteDirectory(testFolder);
            FileUtils.copyDirectory(backupFolder, testFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void processAuditSvcTest() throws IOException {
        var fileName = inFileDir + "ABCDO_Audit.000001.XML";
        var processAuditResponse = new ProcessAuditResponse();
        processAuditResponse.setResultCd("0");

        ResponseEntity<ProcessAuditResponse> responseEntity =
                new ResponseEntity<>(processAuditResponse, HttpStatus.OK);

        //     Set up to mock ords response
        when(restTemplate.exchange(
                        Mockito.any(String.class),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<Class<ProcessAuditResponse>>any()))
                .thenReturn(responseEntity);

        when(controller.validateXml(Mockito.any(String.class), Mockito.any(File.class)))
                .thenReturn(true);
        controller.processAuditSvc(fileName);
    }

    @Test
    public void processAuditSvcTestFail() throws IOException {
        var fileName = inFileDir + "ABCDO_Audit.000001.XML";

        when(restTemplate.exchange(
                        Mockito.any(String.class),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<Class<ProcessAuditResponse>>any()))
                .thenThrow(ORDSException.class);

        // mock the file is a valid xml
        when(controller.validateXml(Mockito.any(String.class), Mockito.any(File.class)))
                .thenReturn(true);
        Assertions.assertThrows(ORDSException.class, () -> controller.processAuditSvc(fileName));
    }

    @Test
    public void processAuditSvcTestInvalidXml() {
        var fileName = inFileDir + "ABCDO_Audit.000001.XML";
        var processAuditResponse = new ProcessAuditResponse();
        processAuditResponse.setResultCd("0");
        ResponseEntity<ProcessAuditResponse> responseEntity =
                new ResponseEntity<>(processAuditResponse, HttpStatus.OK);

        //     Set up to mock ords response
        when(restTemplate.exchange(
                        Mockito.any(String.class),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<Class<ProcessAuditResponse>>any()))
                .thenReturn(responseEntity);

        when(controller.validateXml(Mockito.any(String.class), Mockito.any(File.class)))
                .thenReturn(false);
        Assertions.assertThrows(IOException.class, () -> controller.processAuditSvc(fileName));
    }

    @Test
    public void processStatusSvcTest() throws IOException {
        var fileName = inFileDir + "ABCDO_Status.000001.XML";
        var processStatusResponse = new ProcessStatusResponse();
        processStatusResponse.setResultCd("0");

        ResponseEntity<ProcessStatusResponse> responseEntity =
                new ResponseEntity<>(processStatusResponse, HttpStatus.OK);

        //     Set up to mock ords response
        when(restTemplate.exchange(
                        Mockito.any(String.class),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<Class<ProcessStatusResponse>>any()))
                .thenReturn(responseEntity);

        when(controller.validateXml(Mockito.any(String.class), Mockito.any(File.class)))
                .thenReturn(true);
        controller.processStatusSvc(fileName);
    }

    @Test
    public void processStatusSvcTestFail() {
        var fileName = inFileDir + "ABCDO_Status.000001.XML";

        when(restTemplate.exchange(
                        Mockito.any(String.class),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<Class<ProcessStatusResponse>>any()))
                .thenThrow(ORDSException.class);

        // mock the file is a valid xml
        when(controller.validateXml(Mockito.any(String.class), Mockito.any(File.class)))
                .thenReturn(true);
        Assertions.assertThrows(ORDSException.class, () -> controller.processStatusSvc(fileName));
    }

    @Test
    public void processStatusSvcTestInvalidXml() {
        var fileName = inFileDir + "ABCDO_Status.000001.XML";
        var processStatusResponse = new ProcessStatusResponse();
        processStatusResponse.setResultCd("0");
        ResponseEntity<ProcessStatusResponse> responseEntity =
                new ResponseEntity<>(processStatusResponse, HttpStatus.OK);

        //     Set up to mock ords response
        when(restTemplate.exchange(
                        Mockito.any(String.class),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<Class<ProcessStatusResponse>>any()))
                .thenReturn(responseEntity);

        when(controller.validateXml(Mockito.any(String.class), Mockito.any(File.class)))
                .thenReturn(false);
        Assertions.assertThrows(IOException.class, () -> controller.processStatusSvc(fileName));
    }

    @Test
    public void processDocumentsSvcTest() throws IOException {
        var folderName = inFileDir + "Processed_2020-03-24/CCs/";
        var folderShortName = "CCs";
        var processedDate = "2020-03-24";

        //     Set up to mock ords response
        Map<String, String> m = new HashMap<>();
        ResponseEntity<Map<String, String>> responseEntity = new ResponseEntity<>(m, HttpStatus.OK);
        m.put("status", "N");

        // Set up to mock ords response
        when(restTemplate.exchange(
                        AdditionalMatchers.not(contains("tokenvalue")),
                        Mockito.eq(HttpMethod.GET),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<ParameterizedTypeReference<Map<String, String>>>any()))
                .thenReturn(responseEntity);

        var savePDFDocumentResponse = new SavePDFDocumentResponse();
        ResponseEntity<SavePDFDocumentResponse> responseEntity1 =
                new ResponseEntity<>(savePDFDocumentResponse, HttpStatus.OK);
        responseEntity1.getBody().setResultCd("0");

        //     Set up to mock ords response
        when(restTemplate.exchange(
                        contains("doc/save"),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<Class<SavePDFDocumentResponse>>any()))
                .thenReturn(responseEntity1);

        var processCCsResponse = new ProcessCCsResponse();
        ResponseEntity<ProcessCCsResponse> responseEntity2 =
                new ResponseEntity<>(processCCsResponse, HttpStatus.OK);
        processCCsResponse.setResultCd("0");

        //     Set up to mock ords response
        when(restTemplate.exchange(
                        contains("doc/processCCs"),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<Class<ProcessCCsResponse>>any()))
                .thenReturn(responseEntity2);

        controller.processDocumentsSvc(folderName, folderShortName, processedDate);
    }

    @Test
    public void processReportsSvcTest() throws IOException {
        var folderName = inFileDir + "Processed_2020-03-24/R-Lists/";
        var processedDate = "2020-03-24";

        //     Set up to mock ords response
        var processReportResponse = new ProcessReportResponse();
        ResponseEntity<ProcessReportResponse> responseEntity =
                new ResponseEntity<>(processReportResponse, HttpStatus.OK);
        processReportResponse.setResultCd("0");

        // Set up to mock ords response
        when(restTemplate.exchange(
                        Mockito.any(String.class),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<Class<ProcessReportResponse>>any()))
                .thenReturn(responseEntity);

        controller.processReportsSvc(folderName, processedDate);
    }

    @Test
    public void processReportsSvcTestFail() throws IOException {
        var folderName = inFileDir + "Processed_2020-03-24/R-Lists/";
        var processedDate = "2020-03-24";

        // Set up to mock ords response
        when(restTemplate.exchange(
                        Mockito.any(String.class),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<Class<ProcessReportRequest>>any()))
                .thenThrow(ORDSException.class);

        Assertions.assertThrows(
                ORDSException.class, () -> controller.processReportsSvc(folderName, processedDate));
    }

    @Test
    public void saveErrorTest() throws IOException {
        String errMsg = "AA";
        String date = "AA";
        String fileName = "AA";
        String fileContentXml = "AA";

        SaveErrorResponse resp = new SaveErrorResponse();
        resp.setResultCd("0");
        resp.setResponseMessageTxt("AA");
        ResponseEntity<SaveErrorResponse> responseEntity = new ResponseEntity<>(resp, HttpStatus.OK);

        // Set up to mock ords response
        when(restTemplate.exchange(
                        AdditionalMatchers.not(contains("tokenvalue")),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<Class<SaveErrorResponse>>any()))
                .thenReturn(responseEntity);

        controller.saveError(
                errMsg, date, fileName, fileContentXml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void saveErrorTestFail() {
        String errMsg = "AA";
        String date = "AA";
        String fileName = "AA";
        String fileContentXml = "AA";

        // Set up to mock ords response
        when(restTemplate.exchange(
                        AdditionalMatchers.not(contains("tokenvalue")),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<ParameterizedTypeReference<Map<String, String>>>any()))
                .thenThrow(ORDSException.class);

        Assertions.assertThrows(
                ORDSException.class,
                () ->
                        controller.saveError(
                                errMsg,
                                date,
                                fileName,
                                fileContentXml.getBytes(StandardCharsets.UTF_8)));
    }
}
