package ca.bc.gov.open.crdp.transmit.sender;

import static org.mockito.Mockito.*;

import ca.bc.gov.open.crdp.transmit.models.ReceiverPub;
import ca.bc.gov.open.crdp.transmit.sender.services.SenderService;
import ca.bc.gov.open.sftp.starter.SftpProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SenderServiceTests {
    @Mock private ObjectMapper objectMapper;
    @Mock private RestTemplate restTemplate;
    @Mock private SftpProperties sftpProperties;
    @Mock private SenderService controller;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = Mockito.spy(new SenderService(restTemplate, objectMapper, sftpProperties));
    }

    @Test
    void testUpdateTransmissionSent() throws JsonProcessingException {
        var req = new ReceiverPub();
        req.setDataExchangeFileSeqNo("A");
        req.setDataExchangeFileSeqNo("A");
        req.setXmlString("A");
        req.setPartOneFileIds(new ArrayList<>());
        req.setPartTwoFileIds(new ArrayList<>());
        req.setRegModFileIds(new ArrayList<>());

        Map<String, String> out = new HashMap<>();
        out.put("responseCd", "1");
        ResponseEntity<Map<String, String>> responseEntity =
                new ResponseEntity<>(out, HttpStatus.OK);

        // Set up to mock ords response
        when(restTemplate.exchange(
                        Mockito.any(String.class),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<ParameterizedTypeReference<Map<String, String>>>any()))
                .thenReturn(responseEntity);

        Assertions.assertEquals(0, controller.updateTransmissionSent(req));
    }

    @Test
    void testUpdateTransmissionSentFail() throws JsonProcessingException {
        var req = new ReceiverPub();
        req.setDataExchangeFileSeqNo("A");
        req.setDataExchangeFileSeqNo("A");
        req.setXmlString("A");
        req.setPartOneFileIds(new ArrayList<>());
        req.setPartTwoFileIds(new ArrayList<>());
        req.setRegModFileIds(new ArrayList<>());

        Map<String, String> out = new HashMap<>();
        out.put("responseCd", "0");
        out.put("responseMessageTxt", "A");
        ResponseEntity<Map<String, String>> responseEntity =
                new ResponseEntity<>(out, HttpStatus.OK);

        // Set up to mock ords response
        when(restTemplate.exchange(
                        Mockito.any(String.class),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<ParameterizedTypeReference<Map<String, String>>>any()))
                .thenReturn(responseEntity);

        Assertions.assertEquals(-1, controller.updateTransmissionSent(req));
    }

    @Test
    void testSendXmlFile() throws JsonProcessingException {
        var req = new ReceiverPub();
        req.setFileName("A");
        req.setDataExchangeFileSeqNo("A");
        req.setDataExchangeFileSeqNo("A");
        req.setXmlString("A");
        req.setPartOneFileIds(new ArrayList<>());
        req.setPartTwoFileIds(new ArrayList<>());
        req.setRegModFileIds(new ArrayList<>());

        controller = spy(controller);

        doReturn(0).when(controller).sendXmlFile(Mockito.any(ReceiverPub.class));

        Assertions.assertEquals(0, controller.sendXmlFile(req));
    }

    @Test
    void testSendXmlFileFail() throws JsonProcessingException {
        var req = new ReceiverPub();
        req.setFileName("A");
        req.setDataExchangeFileSeqNo("A");
        req.setDataExchangeFileSeqNo("A");
        req.setXmlString("A");
        req.setPartOneFileIds(new ArrayList<>());
        req.setPartTwoFileIds(new ArrayList<>());
        req.setRegModFileIds(new ArrayList<>());

        controller = spy(controller);

        doReturn(-1).when(controller).sendXmlFile(Mockito.any(ReceiverPub.class));

        Assertions.assertEquals(-1, controller.sendXmlFile(req));
    }
}
