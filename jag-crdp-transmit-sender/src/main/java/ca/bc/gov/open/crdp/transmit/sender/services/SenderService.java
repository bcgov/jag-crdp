package ca.bc.gov.open.crdp.transmit.sender.services;

import ca.bc.gov.open.crdp.ErrorHandler;
import ca.bc.gov.open.crdp.exceptions.ORDSException;
import ca.bc.gov.open.crdp.models.OrdsErrorLog;
import ca.bc.gov.open.crdp.models.RequestSuccessLog;
import ca.bc.gov.open.crdp.transmit.models.ReceiverPub;
import ca.bc.gov.open.crdp.transmit.models.UpdateTransmissionSentRequest;
import ca.bc.gov.open.sftp.starter.JschSessionProvider;
import ca.bc.gov.open.sftp.starter.SftpProperties;
import ca.bc.gov.open.sftp.starter.SftpServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
import org.springframework.mail.javamail.JavaMailSender;
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

    @Value("${crdp.notification-addresses}")
    public void setErrNotificationAddresses(String addresses) {
        SenderService.errNotificationAddresses = addresses;
    }

    private static String errNotificationAddresses = "";

    @Value("${crdp.smtp-from}")
    public void setDefaultSmtpFrom(String from) {
        SenderService.defaultSmtpFrom = from;
    }

    private static String defaultSmtpFrom = "";

    private final String tempFileDir = "temp-pdfs/";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final JavaMailSender emailSender;
    private final SftpProperties sftpProperties;

    @Autowired JschSessionProvider jschSessionProvider;

    @Autowired
    public SenderService(
            JavaMailSender emailSender,
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            SftpProperties sftpProperties) {
        this.emailSender = emailSender;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.sftpProperties = sftpProperties;
    }

    public void updateTransmissionSent(ReceiverPub xmlPub) throws JsonProcessingException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(host + "update-sent");

        UpdateTransmissionSentRequest req = new UpdateTransmissionSentRequest();
        req.setCurrentDate(LocalDate.now().toString());
        req.setDataExchangeFileSeqNo(xmlPub.getDataExchangeFileSeqNo());
        req.setPartOneIds(xmlPub.getPartOneFileIds());
        req.setRegModIds(xmlPub.getRegModFileIds());
        req.setPartTwoIds(xmlPub.getPartTwoFileIds());

        HttpEntity<UpdateTransmissionSentRequest> payload =
                new HttpEntity<>(req, new HttpHeaders());
        HttpEntity<Map<String, String>> resp = null;
        // Update Transmission Sent
        try {
            resp =
                    restTemplate.exchange(
                            builder.toUriString(),
                            HttpMethod.POST,
                            payload,
                            new ParameterizedTypeReference<>() {});

            if (resp.getBody().get("responseCd").equals("0")) {
                throw new ORDSException(resp.getBody().get("responseMessageTxt"));
            }

            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog("Request Success", "updateTransmissionSent")));

        } catch (Exception ex) {
            ErrorHandler.processError(); // TO BE COMPLETED
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Error received from ORDS",
                                    "updateTransmissionSent",
                                    ex.getMessage(),
                                    payload)));
            return;
        }
    }

    public void sendXmlFile(ReceiverPub xmlPub) throws JsonProcessingException {
        File f = null;
        try {
            f = new File(tempFileDir + "TmpPDF" + UUID.randomUUID() + ".xml");
            FileUtils.writeStringToFile(f, xmlPub.getXmlString(), StandardCharsets.UTF_8);
            // SCP the file to a server
            sftpTransfer(outFileDir + xmlPub.getFileName(), f);
            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog(
                                    "Request Success", "pdfTransformSCPByReference")));
        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Failure of SFTP to " + sftpProperties.getHost(),
                                    "sendXmlFile",
                                    ex.getMessage(),
                                    xmlPub)));
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
}