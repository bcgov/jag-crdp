package ca.bc.gov.open.crdp.transmit.sender.services;

import ca.bc.gov.open.crdp.exceptions.ORDSException;
import ca.bc.gov.open.crdp.models.OrdsErrorLog;
import ca.bc.gov.open.crdp.models.RequestSuccessLog;
import ca.bc.gov.open.crdp.transmit.models.ReceiverPub;
import ca.bc.gov.open.crdp.transmit.models.UpdateTransmissionRequest;
import ca.bc.gov.open.sftp.starter.JschSessionProvider;
import ca.bc.gov.open.sftp.starter.SftpProperties;
import ca.bc.gov.open.sftp.starter.SftpServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.ws.server.endpoint.annotation.Endpoint;

@Endpoint
@Slf4j
public class SenderService {

    @Value("${crdp.host}")
    private String host = "https://127.0.0.1/";

    @Value("${crdp.out-file-dir}")
    private String outFileDir = "/";

    private final String tempFileDir = "temp-xmls/";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SftpProperties sftpProperties;

    @Autowired JschSessionProvider jschSessionProvider;

    @Autowired
    public SenderService(
            RestTemplate restTemplate, ObjectMapper objectMapper, SftpProperties sftpProperties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.sftpProperties = sftpProperties;
    }

    public int updateTransmissionSent(ReceiverPub xmlPub) throws JsonProcessingException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(host + "update-sent");

        UpdateTransmissionRequest req = new UpdateTransmissionRequest();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        req.setCurrentDate(LocalDateTime.now().format(format.withZone(ZoneId.of("Canada/Pacific"))));
        req.setDataExchangeFileSeqNo(xmlPub.getDataExchangeFileSeqNo());
        req.setPartOneIds(xmlPub.getPartOneFileIds());
        req.setRegModIds(xmlPub.getRegModFileIds());
        req.setPartTwoIds(xmlPub.getPartTwoFileIds());
        req.setFileName(xmlPub.getFileName());
        req.setXmlString(xmlPub.getXmlString().getBytes(StandardCharsets.UTF_8));

        HttpEntity<UpdateTransmissionRequest> payload = new HttpEntity<>(req, new HttpHeaders());
        HttpEntity<Map<String, String>> resp = null;
        // Update Transmission status and save data exchange file
        try {
            resp =
                    restTemplate.exchange(
                            builder.toUriString(),
                            HttpMethod.POST,
                            payload,
                            new ParameterizedTypeReference<>() {});
            if (resp.getBody().get("responseCd") != null
                    && resp.getBody().get("responseCd").equals("1")) {
                throw new ORDSException(resp.getBody().get("responseMessageTxt"));
            }
            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog("Request Success", "updateTransmissionSent")));
            return 0;
        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Error received from ORDS",
                                    "updateTransmissionSent",
                                    ex.getMessage(),
                                    payload)));
            return -1;
        }
    }

    public int sendXmlFile(ReceiverPub xmlPub) throws JsonProcessingException {
        File f = null;
        try {
            f = new File(tempFileDir + "TmpXML" + UUID.randomUUID() + ".xml");
            FileUtils.writeStringToFile(
                    f, xmlPub.getXmlString(), String.valueOf(StandardCharsets.UTF_8));
            // SCP the file to a server
            sftpTransfer(outFileDir + "/" + xmlPub.getFileName(), f);
            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog("Request Success", "sendXmlFile")));
            return 0;
        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Failure of SFTP to " + sftpProperties.getHost(),
                                    "sendXmlFile",
                                    ex.getMessage(),
                                    xmlPub)));
            // Rollback exchange file save and status update (SENT to RDY)
            return revertSavingAndStatusUpdate(xmlPub);
        } finally {
            if (f != null && f.exists()) {
                if (!f.delete()) {
                    log.warn("Failed to delete temp xml file.");
                }
            }
        }
    }

    public void sftpTransfer(String dest, File payload) {
        SftpServiceImpl sftpService = new SftpServiceImpl(jschSessionProvider, sftpProperties);
        sftpService.put(payload.getAbsoluteFile().getPath(), dest);
    }

    private int revertSavingAndStatusUpdate(ReceiverPub xmlPub) throws JsonProcessingException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(host + "revert");

        UpdateTransmissionRequest req = new UpdateTransmissionRequest();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        req.setCurrentDate(LocalDateTime.now().format(format.withZone(ZoneId.of("Canada/Pacific"))));
        req.setDataExchangeFileSeqNo(xmlPub.getDataExchangeFileSeqNo());
        req.setPartOneIds(xmlPub.getPartOneFileIds());
        req.setRegModIds(xmlPub.getRegModFileIds());
        req.setPartTwoIds(xmlPub.getPartTwoFileIds());

        HttpEntity<UpdateTransmissionRequest> payload = new HttpEntity<>(req, new HttpHeaders());
        HttpEntity<Map<String, String>> resp = null;
        // Revert saving XML and status update (status changes back to RDY from SENT)
        try {
            resp =
                    restTemplate.exchange(
                            builder.toUriString(),
                            HttpMethod.POST,
                            payload,
                            new ParameterizedTypeReference<>() {});
            if (resp.getBody().get("responseCd") != null
                    && resp.getBody().get("responseCd").equals("1")) {
                throw new ORDSException(resp.getBody().get("responseMessageTxt"));
            }
            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog(
                                    "Request Success", "revertSavingAndStatusUpdate")));
            return 1;
        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Error received from ORDS",
                                    "revertSavingAndStatusUpdate",
                                    ex.getMessage(),
                                    payload)));
            return -1;
        }
    }
}
