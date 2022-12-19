package ca.bc.gov.open.crdp.transmit.receiver;

import static org.mockito.Mockito.*;

import ca.bc.gov.open.crdp.exceptions.ORDSException;
import ca.bc.gov.open.crdp.transmit.models.*;
import ca.bc.gov.open.crdp.transmit.receiver.configuration.QueueConfig;
import ca.bc.gov.open.crdp.transmit.receiver.services.ReceiverService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.*;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReceiverServiceTests {
    @Mock private ObjectMapper objectMapper;
    @Mock private RestTemplate restTemplate;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private AmqpAdmin amqpAdmin;
    @Mock private QueueConfig queueConfig;

    @Qualifier("receiver-queue")
    private org.springframework.amqp.core.Queue receiverQueue;

    @Mock private ReceiverService controller;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        controller =
                Mockito.spy(
                        new ReceiverService(
                                receiverQueue,
                                amqpAdmin,
                                queueConfig,
                                restTemplate,
                                objectMapper,
                                rabbitTemplate));
    }

    @Test
    public void GenerateIncomingRequestFileTest() throws IOException {
        var generateIncomingReqFileResponse = new GenerateIncomingReqFileResponse();

        generateIncomingReqFileResponse.setFileName("unittest.xml");
        generateIncomingReqFileResponse.setResponseCd("A");
        generateIncomingReqFileResponse.setDataExchangeFileSeqNo("A");
        generateIncomingReqFileResponse.setPartOneCount(1);
        generateIncomingReqFileResponse.setPartTwoCount(1);

        PartOneData oneData = new PartOneData();
        oneData.setApplicantBirthDate("A");
        oneData.setApplicantGender("A");
        oneData.setApplicantGivenName("A");
        oneData.setApplicantSurname("A");
        oneData.setCourtNumber("A");
        oneData.setDivorceRegistryNumber("A");
        oneData.setFeeCode("A");
        oneData.setMarriageDate("A");
        oneData.setJointApplication("A");
        oneData.setFilingDate("A");
        oneData.setOriginalCourtNumber("A");
        oneData.setOriginalDivorceRegNumber("A");
        oneData.setPetitionSignedDate("A");
        oneData.setPhysicalFileId("A");
        oneData.setRecordType("A");
        oneData.setRespondentBirthDate("A");
        oneData.setRespondentGender("A");
        oneData.setRespondentGivenName("A");
        oneData.setRespondentSurname("A");
        List<PartOneData> partOneDataList = new ArrayList<>();
        partOneDataList.add(oneData);
        generateIncomingReqFileResponse.setPartOneData(partOneDataList);

        PartTwoData twoData = new PartTwoData();
        twoData.setCourtNumber("A");
        twoData.setPhysicalFileId("A");
        twoData.setRecordType("A");
        twoData.setDivorceRegistryNumber("A");
        twoData.setDispositionCode("A");
        twoData.setDispositionSignedDate("A");
        twoData.setSourceCaseNumber("A");
        twoData.setTransferredCourtNumber("A");
        twoData.setPhysicalFileId("A");
        List<PartTwoData> partTwoDataList = new ArrayList<>();
        partTwoDataList.add(twoData);
        generateIncomingReqFileResponse.setPartTwoData(partTwoDataList);

        RegModData modData = new RegModData();
        modData.setCourtNumber("A");
        modData.setPhysicalFileId("A");
        modData.setRecordType("A");
        modData.setDivorceRegistryNumber("A");
        modData.setOriginalDivorceRegNumber("A");
        modData.setSourceCaseNumber("A");
        List<RegModData> regModDataList = new ArrayList<>();
        regModDataList.add(modData);
        generateIncomingReqFileResponse.setRegModData(regModDataList);

        ResponseEntity<GenerateIncomingReqFileResponse> responseEntity =
                new ResponseEntity<>(generateIncomingReqFileResponse, HttpStatus.OK);

        //     Set up to mock ords response
        when(restTemplate.exchange(
                        Mockito.any(String.class),
                        Mockito.eq(HttpMethod.GET),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<Class<GenerateIncomingReqFileResponse>>any()))
                .thenReturn(responseEntity);

        Map<String, String> out = new HashMap<>();
        out.put("responseCd", "1");
        ResponseEntity<Map<String, String>> responseEntity2 =
                new ResponseEntity<>(out, HttpStatus.OK);
        when(restTemplate.exchange(
                        Mockito.any(String.class),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<ParameterizedTypeReference<Map<String, String>>>any()))
                .thenReturn(responseEntity2);

        doReturn(0).when(controller).GenerateIncomingRequestFile();
        Assertions.assertEquals(0, controller.GenerateIncomingRequestFile());
    }

    @Test
    public void generateIncomingRequestFileFail1() throws IOException {
        // Set up to mock ords response
        when(restTemplate.exchange(
                        Mockito.<String>any(),
                        Mockito.eq(HttpMethod.GET),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<Class<GenerateIncomingReqFileResponse>>any()))
                .thenThrow(ORDSException.class);

        Assertions.assertEquals(-1, controller.GenerateIncomingRequestFile());
    }

    @Test
    public void generateIncomingRequestFileFail2() throws IOException {
        var generateIncomingReqFileResponse = new GenerateIncomingReqFileResponse();
        List<PartOneData> partOneDataList = new ArrayList<>();
        PartOneData partOneData = new PartOneData();
        partOneDataList.add(partOneData);
        generateIncomingReqFileResponse.setPartOneData(partOneDataList);
        generateIncomingReqFileResponse.setResponseCd("A");
        generateIncomingReqFileResponse.setResponseMessageTxt("A");
        generateIncomingReqFileResponse.setPartOneCount(1);
        generateIncomingReqFileResponse.setPartTwoCount(2);

        ResponseEntity<GenerateIncomingReqFileResponse> responseEntity =
                new ResponseEntity<>(generateIncomingReqFileResponse, HttpStatus.OK);

        //     Set up to mock ords response
        when(restTemplate.exchange(
                        Mockito.any(String.class),
                        Mockito.eq(HttpMethod.GET),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<Class<GenerateIncomingReqFileResponse>>any()))
                .thenReturn(responseEntity);

        doReturn(null).when(controller).xmlBuilder(responseEntity.getBody());

        Assertions.assertEquals(-2, controller.GenerateIncomingRequestFile());
    }

    @Test
    public void generateIncomingRequestFileFail3() throws IOException {
        var generateIncomingReqFileResponse = new GenerateIncomingReqFileResponse();
        generateIncomingReqFileResponse.setPartOneCount(1);
        generateIncomingReqFileResponse.setPartTwoCount(2);

        generateIncomingReqFileResponse.setFileName("unittest.xml");
        generateIncomingReqFileResponse.setResponseCd("0");

        ResponseEntity<GenerateIncomingReqFileResponse> responseEntity =
                new ResponseEntity<>(generateIncomingReqFileResponse, HttpStatus.OK);

        //     Set up to mock ords response
        when(restTemplate.exchange(
                        Mockito.any(String.class),
                        Mockito.eq(HttpMethod.GET),
                        Mockito.<HttpEntity<String>>any(),
                        Mockito.<Class<GenerateIncomingReqFileResponse>>any()))
                .thenReturn(responseEntity);

        Assertions.assertEquals(-3, controller.GenerateIncomingRequestFile());
    }
}
