package ca.bc.gov.open.crdp.process.transformer.services;

import ca.bc.gov.open.crdp.exceptions.ORDSException;
import ca.bc.gov.open.crdp.models.OrdsErrorLog;
import ca.bc.gov.open.crdp.models.OrdsHealthResponse;
import ca.bc.gov.open.crdp.models.RequestSuccessLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RestController
@WebServlet("/")
public class HealthServlet extends HttpServlet {

    @Value("${crdp.host}")
    private String host = "https://127.0.0.1/";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static String PING_MSG = "Successful Ping to Process Transformer";

    @Autowired
    public HealthServlet(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "ping")
    public String ping() {
        log.info(PING_MSG);
        return PING_MSG;
    }

    @GetMapping(value = "ordsHealth")
    public OrdsHealthResponse ordsHealth() throws JsonProcessingException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(host + "health");

        try {
            HttpEntity<OrdsHealthResponse> resp =
                    restTemplate.exchange(
                            builder.toUriString(),
                            HttpMethod.GET,
                            new HttpEntity<>(new HttpHeaders()),
                            OrdsHealthResponse.class);
            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog("Request Success", "ordsHealth")));
            return resp.getBody();
        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Error received from ORDS",
                                    "ordsHealth",
                                    ex.getMessage(),
                                    null)));
            throw new ORDSException();
        }
    }
}
