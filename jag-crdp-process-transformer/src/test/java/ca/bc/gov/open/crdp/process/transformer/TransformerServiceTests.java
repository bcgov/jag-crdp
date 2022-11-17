package ca.bc.gov.open.crdp.process.transformer;

import static org.mockito.Mockito.*;

import ca.bc.gov.open.crdp.exceptions.ORDSException;
import ca.bc.gov.open.crdp.process.models.*;
import ca.bc.gov.open.crdp.process.transformer.services.TransformerService;
import ca.bc.gov.open.sftp.starter.FileService;
import ca.bc.gov.open.sftp.starter.SftpProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
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

    @Mock private ObjectMapper objectMapper;
    @Mock private RestTemplate restTemplate;
    @Mock private FileService fileService;
    @Mock private TransformerService controller;
    @Mock private SftpProperties sftpProperties;

    @BeforeAll
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        controller =
                Mockito.spy(
                        new TransformerService(
                                restTemplate, objectMapper, sftpProperties, fileService));
    }

    @Test
    public void processAuditSvcTest() throws IOException {
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

        doReturn(true)
                .when(controller)
                .validateXml(Mockito.any(String.class), Mockito.any(String.class));
        InputStream stubInputStream = IOUtils.toInputStream("test data stream", "UTF-8");
        when(fileService.get(Mockito.any(String.class))).thenReturn(stubInputStream);
        controller.processAuditSvc("AAA");
    }

    @Test
    public void processAuditSvcTestFail() throws IOException {
        when(restTemplate.exchange(
                        Mockito.any(String.class),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<Class<ProcessAuditResponse>>any()))
                .thenThrow(ORDSException.class);

        // mock the file is a valid xml
        doReturn(true)
                .when(controller)
                .validateXml(Mockito.any(String.class), Mockito.any(String.class));
        InputStream stubInputStream = IOUtils.toInputStream("test data stream", "UTF-8");
        when(fileService.get(Mockito.any(String.class))).thenReturn(stubInputStream);
        Assertions.assertThrows(ORDSException.class, () -> controller.processAuditSvc("AAA"));
    }

    @Test
    public void processAuditSvcTestInvalidXml() {
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

        doReturn(false)
                .when(controller)
                .validateXml(Mockito.any(String.class), Mockito.any(String.class));
        Assertions.assertThrows(IOException.class, () -> controller.processAuditSvc("AAA"));
    }

    @Test
    public void processStatusSvcTest() throws IOException {
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

        doReturn(true)
                .when(controller)
                .validateXml(Mockito.any(String.class), Mockito.any(String.class));
        InputStream stubInputStream = IOUtils.toInputStream("test data stream", "UTF-8");
        when(fileService.get(Mockito.any(String.class))).thenReturn(stubInputStream);
        controller.processStatusSvc("AAA");
    }

    @Test
    public void processStatusSvcTestFail() throws IOException {
        when(restTemplate.exchange(
                        Mockito.any(String.class),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<Class<ProcessStatusResponse>>any()))
                .thenThrow(ORDSException.class);

        // mock the file is a valid xml
        doReturn(true)
                .when(controller)
                .validateXml(Mockito.any(String.class), Mockito.any(String.class));
        InputStream stubInputStream = IOUtils.toInputStream("test data stream", "UTF-8");
        when(fileService.get(Mockito.any(String.class))).thenReturn(stubInputStream);
        Assertions.assertThrows(ORDSException.class, () -> controller.processStatusSvc("AAA"));
    }

    @Test
    public void processStatusSvcTestInvalidXml() {
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

        doReturn(false)
                .when(controller)
                .validateXml(Mockito.any(String.class), Mockito.any(String.class));
        Assertions.assertThrows(IOException.class, () -> controller.processStatusSvc("AAA"));
    }

    @Test
    public void processDocumentsSvcTest() throws IOException {
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

        // mock the file is a valid xml
        doReturn(true)
                .when(controller)
                .validateXml(Mockito.any(String.class), Mockito.any(String.class));
        InputStream stubInputStream = IOUtils.toInputStream("test data stream", "UTF-8");
        when(fileService.get(Mockito.any(String.class))).thenReturn(stubInputStream);

        List<String> stringList = new ArrayList<>();
        stringList.add("A.PDF");
        stringList.add("B.XML");

        List<Object> objects = mock(List.class);
        when(objects.size()).thenReturn(5);

        when(fileService.listFiles(Mockito.any(String.class))).thenReturn(stringList);
        when(controller.extractPDFFileNames(Mockito.anyString())).thenReturn(stringList);

        controller.processDocumentsSvc("AAA", "CCs", "CCC");
    }

    @Test
    public void processReportsSvcTest() throws IOException {
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
        InputStream stubInputStream = IOUtils.toInputStream("test data stream", "UTF-8");
        when(fileService.get(Mockito.any(String.class))).thenReturn(stubInputStream);
        controller.processReportsSvc("AAA", "BBB");
    }

    @Test
    public void processReportsSvcTestFail() throws IOException {
        // Set up to mock ords response
        when(restTemplate.exchange(
                        Mockito.any(String.class),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<Class<ProcessReportRequest>>any()))
                .thenThrow(ORDSException.class);

        InputStream stubInputStream = IOUtils.toInputStream("test data stream", "UTF-8");
        when(fileService.get(Mockito.any(String.class))).thenReturn(stubInputStream);
        Assertions.assertThrows(
                ORDSException.class, () -> controller.processReportsSvc("AAA", "BBB"));
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
        ResponseEntity<SaveErrorResponse> responseEntity =
                new ResponseEntity<>(resp, HttpStatus.OK);

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
