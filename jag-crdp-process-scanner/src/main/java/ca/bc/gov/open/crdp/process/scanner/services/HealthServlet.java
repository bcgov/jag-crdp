package ca.bc.gov.open.crdp.process.scanner.services;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@WebServlet("/")
public class HealthServlet extends HttpServlet {

    private static String PING_MSG = "Successful Ping to Process Scanner";

    @GetMapping(value = "ping")
    public String ping() {
        log.info(PING_MSG);
        return PING_MSG;
    }
}
