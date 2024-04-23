package ca.bc.gov.open.crdp.process.transformer.services;

import ca.bc.gov.open.crdp.exceptions.ORDSException;
import ca.bc.gov.open.crdp.models.OrdsErrorLog;
import ca.bc.gov.open.crdp.models.RequestSuccessLog;
import ca.bc.gov.open.crdp.process.models.*;
import ca.bc.gov.open.sftp.starter.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import jakarta.xml.bind.JAXB;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
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
public class TransformerService {

    @Value("${crdp.host}")
    private String host = "https://127.0.0.1/";

    @Value("${crdp.processing-dir}")
    private String processingDir = "/";

    @Value("${crdp.completed-dir}")
    private String completedDir = "/";

    @Value("${crdp.errors-dir}")
    private String errorsDir = "/";

    @Value("${crdp.sftp-enabled}")
    private String sftpEnabled = "true";

    private Integer PERMISSIONS_DECIMAL = 493;

    private String timestamp = null;

    @Autowired JschSessionProvider jschSessionProvider;
    private FileService fileService;
    private final SftpProperties sftpProperties;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static TreeMap<String, String> completedFilesToMove =
            new TreeMap<String, String>(); // completed files.
    private static TreeMap<String, String> erredFilesToMove =
            new TreeMap<String, String>(); // erred files.

    private static TreeMap<String, String> completedFoldersToMove =
            new TreeMap<String, String>(); // completed folders.
    private static TreeMap<String, String> erredFoldersToMove =
            new TreeMap<String, String>(); // erred folders.

    private static String auditSchemaPath = "xsdSchemas/outgoingAudit.xsd";
    private static String ccSchemaPath = "xsdSchemas/outgoingCCs.xsd";
    private static String lettersSchemaPath = "xsdSchemas/outgoingLetters.xsd";
    private static String statusSchemaPath = "xsdSchemas/outgoingStatus.xsd";

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    public TransformerService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            SftpProperties sftpProperties,
            FileService fileService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.sftpProperties = sftpProperties;
        this.fileService = fileService;
    }

    public void processFileService(ScannerPub pub) {
        fileService =
                Boolean.valueOf(sftpEnabled)
                        ? new SftpServiceImpl(jschSessionProvider, sftpProperties)
                        : new LocalFileImpl();

        // re-initialize arrays. Failing to do this can result in unpredictable results.
        completedFilesToMove = new TreeMap<String, String>(); // completed files.
        erredFilesToMove = new TreeMap<String, String>(); // erred files.
        completedFoldersToMove = new TreeMap<String, String>(); // completed folders.
        erredFoldersToMove = new TreeMap<String, String>(); // erred folders.

        this.timestamp = pub.getDateTime();

        if (fileService.exists(processingDir) && fileService.isDirectory(processingDir)) {
            // create Completed folder
            if (!fileService.exists(completedDir)) {
                fileService.makeFolder(completedDir, PERMISSIONS_DECIMAL);
            }

            // create Errors folder
            if (!fileService.exists(errorsDir)) {
                fileService.makeFolder(errorsDir, PERMISSIONS_DECIMAL);
            }

            if (!fileService.isDirectory(pub.getFilePath())) {
                // process files
                processFile(pub.getFilePath());
            } else {
                // process folders
                processFolder(pub.getFilePath());
            }

            try {
                // create completed folder with last scanning timestamp
                if (!completedFilesToMove.isEmpty()
                        || !completedFoldersToMove.isEmpty()
                        && !fileService.exists(completedDir + "/" + timestamp)) {
                    fileService.makeFolder(completedDir + "/" + timestamp, PERMISSIONS_DECIMAL);
                }
                for (Map.Entry<String, String> m : completedFilesToMove.entrySet()) {
                    fileService.moveFile(m.getKey(), m.getValue());
                }
                for (Map.Entry<String, String> m : completedFoldersToMove.entrySet()) {
                    fileService.moveFile(m.getKey(), m.getValue());
                }

                // create errors folder with last scanning timestamp
                if (!erredFilesToMove.isEmpty()
                        || !erredFoldersToMove.isEmpty()
                        && !fileService.exists(errorsDir + "/" + timestamp)) {
                    log.info("making " + errorsDir + "/" + timestamp);
                    fileService.makeFolder(errorsDir + "/" + timestamp, PERMISSIONS_DECIMAL);
                }
                for (Map.Entry<String, String> m : erredFilesToMove.entrySet()) {
                    fileService.moveFile(m.getKey(), m.getValue());
                }
                for (Map.Entry<String, String> m : erredFoldersToMove.entrySet()) {
                    fileService.moveFile(m.getKey(), m.getValue());
                }
                cleanUp(processingDir);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } else {
            log.error("Processing directory \"" + processingDir + "\" does not exist");
        }
    }

    private void cleanUp(String processingDir) {
        for (String folder : fileService.listFiles(processingDir)) {
            if (getFileName(folder).startsWith(".")) {
                continue;
            }
            for (String f : fileService.listFiles(folder)) {
                if (!getFileName(f).startsWith(".")
                        && fileService.isDirectory(f)
                        && fileService.listFiles(f).size() <= 2) {
                    fileService.removeFolder(f);
                }
            }
            if (fileService.listFiles(folder).size() <= 2) {
                fileService.removeFolder(folder);
            }
        }
    }

    private void processFile(String filePath) {
        String auditRegex = "(?i)^[A-Za-z]{4}O_Audit.\\d{6}.XML"; // ^[A-Z]{4}O_Audit.\d{6}.XML
        String statusRegex = "(?i)^[A-Za-z]{4}O_STATUS.\\d{6}.XML"; // ^[A-Z]{4}O_Status.\d{6}.XML
        try {
            if (Pattern.matches(auditRegex, getFileName(filePath))) {
                processAuditSvc(filePath);
            } else if (Pattern.matches(statusRegex, getFileName(filePath))) {
                processStatusSvc(filePath);
            } else {
                throw new IOException("Unexpected file: " + getFileName(filePath));
            }

            // Move file to 'completed' folder on success (status or audit only)
            completedFilesToMove.put(
                    filePath, completedDir + "/" + timestamp + "/" + getFileName(filePath));

        } catch (Exception e) {
            erredFilesToMove.put(
                    filePath, errorsDir + "/" + timestamp + "/" + getFileName(filePath));
        }
    }

    public void processAuditSvc(String fileName) throws IOException {
        String shortFileName = FilenameUtils.getName(fileName); // Extract file name from full path
        File schema = new File(auditSchemaPath);

        if (!validateXml(auditSchemaPath, fileName)) {
            File file = new File(fileName);
            if(file.exists() && !file.isDirectory()) {
                ByteArrayInputStream xmlFile = fileService.getContent(fileName);
                byte[] xmlBytes = new byte[xmlFile.available()];
                xmlFile.read(xmlBytes);
                saveError("XML file schema validation failed. fileName:" + fileName,
                        dateFormat.format(Calendar.getInstance().getTime()),
                        fileName,
                        xmlBytes);
            }
            throw new IOException("XML file schema validation failed. fileName: " + fileName);
        }

        log.info("validation completed");
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(host + "process-audit");

        InputStream xmlFile = fileService.getContent(fileName);
        byte[] file = IOUtils.toByteArray(xmlFile);
        xmlFile.close();

        ProcessAuditRequest req = new ProcessAuditRequest(shortFileName, file);
        // Send ORDS request
        try {

            HttpEntity<ProcessAuditRequest> payload = new HttpEntity<>(req, new HttpHeaders());

            HttpEntity<ProcessAuditResponse> resp =
                    restTemplate.exchange(
                            builder.toUriString(),
                            HttpMethod.POST,
                            payload,
                            ProcessAuditResponse.class);
            if (!resp.getBody().getResponseCd().equals("0")) {
                throw new ORDSException(resp.getBody().getResponseMessageTxt());
            }
            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog("Request Success", "processAuditSvc")));
        } catch (Exception e) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Error received from ORDS",
                                    "processAuditSvc",
                                    e.getMessage(),
                                    fileName)));
            saveError(
                    e.getMessage(),
                    dateFormat.format(Calendar.getInstance().getTime()),
                    fileName,
                    file);

            throw new ORDSException();
        }
    }

    public void processStatusSvc(String fileName) throws IOException {
        String shortFileName = FilenameUtils.getName(fileName); // Extract file name from full path
        if (!validateXml(statusSchemaPath, fileName)) {
            File file = new File(fileName);
            if(file.exists() && !file.isDirectory()) {
                ByteArrayInputStream xmlFile = fileService.getContent(fileName);
                byte[] xmlBytes = new byte[xmlFile.available()];
                xmlFile.read(xmlBytes);
                saveError("XML file schema validation failed. fileName:" + fileName,
                        dateFormat.format(Calendar.getInstance().getTime()),
                        fileName,
                        xmlBytes);
            }
            throw new IOException("XML file schema validation failed. fileName: " + fileName);
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(host + "process-status");

        InputStream xmlFile = fileService.getContent(fileName);
        byte[] file = IOUtils.toByteArray(xmlFile);
        xmlFile.close();

        ProcessStatusRequest req = new ProcessStatusRequest(shortFileName, file);
        HttpEntity<ProcessStatusRequest> payload = new HttpEntity<>(req, new HttpHeaders());
        // Send ORDS request
        try {
            HttpEntity<ProcessStatusResponse> resp =
                    restTemplate.exchange(
                            builder.toUriString(),
                            HttpMethod.POST,
                            payload,
                            ProcessStatusResponse.class);

            if (!resp.getBody().getResponseCd().equals("0")) {
                log.warn("ResponseCd from DB is " + resp.getBody().getResponseCd());
                throw new ORDSException(resp.getBody().getResponseMessageTxt());
            }
            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog("Request Success", "processStatusSvc")));
        } catch (Exception e) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Error received from ORDS",
                                    "processStatusSvc",
                                    e.getMessage(),
                                    fileName)));

            saveError(
                    e.getMessage(),
                    dateFormat.format(Calendar.getInstance().getTime()),
                    fileName,
                    file);

            throw new ORDSException();
        }
    }

    public void saveError(String errMsg, String date, String fileName, byte[] fileContentXml)
            throws JsonProcessingException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(host + "err/save");
        SaveErrorRequest req = new SaveErrorRequest(errMsg, date, fileName, fileContentXml);
        HttpEntity<SaveErrorRequest> payload = new HttpEntity<>(req, new HttpHeaders());
        try {
            HttpEntity<SaveErrorResponse> response =
                    restTemplate.exchange(
                            builder.toUriString(),
                            HttpMethod.POST,
                            payload,
                            SaveErrorResponse.class);
            if (!response.getBody().getResponseCd().equals("0")) {
                log.error(
                        objectMapper.writeValueAsString(
                                new OrdsErrorLog(
                                        "Error received from ORDS",
                                        "SaveError",
                                        response.getBody().getResponseMessageTxt(),
                                        req)));
            } else {
                log.info(
                        objectMapper.writeValueAsString(
                                new RequestSuccessLog("Request Success", "SaveError")));
            }
        } catch (Exception e) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Error received from ORDS", "SaveError", e.getMessage(), req)));
            throw new ORDSException();
        }
    }

    private void processFolder(String folderPath) {
        // Extract date from Processed folderName to pass service as 'processedDate'.
        Pattern p = Pattern.compile("\\bProcessed_\\w+[-][0-9][0-9][-][0-9][0-9]");
        Matcher m = p.matcher(folderPath);
        String processedDate = null;
        if (m.find()) {
            processedDate = m.group().substring("Processed_".length());
        }

        try {
            switch (getFileName(folderPath)) {
                case "Letters":
                case "CCs":
                    processDocumentsSvc(folderPath, getFileName(folderPath), processedDate);
                    break;
                case "JUS178s":
                case "R-Lists":
                    // folderShortName is not used in processReports
                    processReportsSvc(folderPath, processedDate);
                    break;
                default:
            }

            // Add the processed folder and its target location to the processedFolders map
            // dealt with at the end of processing.
            completedFoldersToMove.put(
                    folderPath, completedDir + "/" + timestamp + "/" + getFileName(folderPath));

        } catch (Exception e) {
            // Add the erred folder path and its target location to the erred folders map
            // dealt with at the end of processing.
            erredFoldersToMove.put(
                    folderPath, errorsDir + "/" + timestamp + "/" + getFileName(folderPath));
        }
    }

    public void processDocumentsSvc(String folderName, String folderShortName, String processedDate)
            throws IOException {
        // Populates the array with names of files and directories
        List<String> fileList = fileService.listFiles(folderName);
        String fileName = "";
        boolean isValid = false;
        InputStream xmlFile = null;
        if (folderShortName.equals("CCs")) {
            fileName = extractXMLFileName(fileList, "^[A-Z]{4}O_CCs.XML");
            xmlFile = fileService.getContent(fileName);
            isValid = validateXml(ccSchemaPath, fileName);
        } else if (folderShortName.equals("Letters")) {
            fileName = extractXMLFileName(fileList, "^[A-Z]{4}O_Letters.XML");
            xmlFile = fileService.getContent(fileName);
            isValid = validateXml(lettersSchemaPath, fileName);
        } else {
            throw new IOException("Unexpected folder short name: " + folderShortName);
        }

        if (!isValid) {
            byte[] xmlBytes = new byte[xmlFile.available()];
            xmlFile.read(xmlBytes);
            saveError("XML file schema validation failed. fileName:" + fileName,
                    dateFormat.format(Calendar.getInstance().getTime()),
                    fileName,
                    xmlBytes);
            throw new IOException("XML file schema validation failed. fileName: " + fileName);
        }

        byte[] document = IOUtils.toByteArray(xmlFile);
        xmlFile.close();

        UriComponentsBuilder builder =
                UriComponentsBuilder.fromHttpUrl(host + "doc/status")
                        .queryParam("processedDate", processedDate)
                        .queryParam("fileName", fileName);

        HttpEntity<Map<String, String>> resp = null;
        try {
            resp =
                    restTemplate.exchange(
                            builder.toUriString(),
                            HttpMethod.GET,
                            new HttpEntity<>(new HttpHeaders()),
                            new ParameterizedTypeReference<>() {});
            log.info(
                    objectMapper.writeValueAsString(
                            new RequestSuccessLog(
                                    "Request Success",
                                    "processDocumentsSvc - GetDocumentProcessStatus ("
                                            + folderShortName
                                            + ")")));

        } catch (Exception e) {
            log.error(
                    objectMapper.writeValueAsString(
                            new OrdsErrorLog(
                                    "Error received from ORDS",
                                    "processDocumentsSvc - GetDocumentProcessStatusRequest",
                                    e.getMessage(),
                                    fileName + " " + processedDate)));
            throw new ORDSException();
        }

        GuidMapDocument guidMapDocument = new GuidMapDocument("1", new ArrayList<>());
        if (resp.getBody().get("status").equals("N")) {
            List<String> pdfs = extractPDFFileNames(folderName);
            UriComponentsBuilder builder2 = UriComponentsBuilder.fromHttpUrl(host + "doc/save");
            for (String pdf : pdfs) {
                InputStream pdfStream = fileService.getContent(pdf);
                SavePDFDocumentRequest req =
                        new SavePDFDocumentRequest(IOUtils.toByteArray(pdfStream));
                pdfStream.close();
                HttpEntity<SavePDFDocumentRequest> payload =
                        new HttpEntity<>(req, new HttpHeaders());
                try {
                    HttpEntity<SavePDFDocumentResponse> response =
                            restTemplate.exchange(
                                    builder2.toUriString(),
                                    HttpMethod.POST,
                                    payload,
                                    SavePDFDocumentResponse.class);
                    log.info(
                            objectMapper.writeValueAsString(
                                    new RequestSuccessLog(
                                            "Request Success",
                                            "processDocumentsSvc - SavePDFDocument ("
                                                    + folderShortName
                                                    + ")")));
                    if (response.getBody().getResponseCd().equals("0")) {
                        // map file name and guid
                        guidMapDocument
                                .getMappings()
                                .add(
                                        new GuidDocumentMapping(
                                                FilenameUtils.getName(pdf),
                                                response.getBody().getObjectGuid()));
                    } else {
                        throw new ORDSException(response.getBody().getResponseMessageTxt());
                    }
                } catch (Exception e) {
                    log.error(
                            objectMapper.writeValueAsString(
                                    new OrdsErrorLog(
                                            "Error received from ORDS",
                                            "processDocumentsSvc - SavePDFDocument (\" + folderShortName + \")\"",
                                            e.getMessage(),
                                            req)));
                    saveError(
                            e.getMessage(),
                            dateFormat.format(Calendar.getInstance().getTime()),
                            fileName,
                            document);

                    throw new ORDSException();
                }
            }

            StringWriter sw = new StringWriter();
            JAXB.marshal(guidMapDocument, sw);
            String xml = sw.toString();

            ProcessXMLRequest req =
                    new ProcessXMLRequest(
                            document,
                            Base64.getEncoder()
                                    .encodeToString(xml.getBytes(StandardCharsets.UTF_8)));
            if (folderShortName.equals("CCs")) {
                UriComponentsBuilder builder3 =
                        UriComponentsBuilder.fromHttpUrl(host + "doc/processCCs");
                HttpEntity<ProcessXMLRequest> payload = new HttpEntity<>(req, new HttpHeaders());
                try {
                    HttpEntity<ProcessCCsResponse> response =
                            restTemplate.exchange(
                                    builder3.toUriString(),
                                    HttpMethod.POST,
                                    payload,
                                    ProcessCCsResponse.class);
                    if (!response.getBody().getResponseCd().equals("0")) {
                        log.warn("ResponseCd from DB is " + response.getBody().getResponseCd());
                        throw new ORDSException(response.getBody().getResponseMessageTxt());
                    }
                    log.info(
                            objectMapper.writeValueAsString(
                                    new RequestSuccessLog(
                                            "Request Success",
                                            "processDocumentsSvc - ProcessCCsXML")));
                } catch (Exception e) {
                    log.error(
                            objectMapper.writeValueAsString(
                                    new OrdsErrorLog(
                                            "Error received from ORDS",
                                            "processDocumentsSvc - ProcessCCsXML",
                                            e.getMessage(),
                                            req)));

                    saveError(
                            e.getMessage(),
                            dateFormat.format(Calendar.getInstance().getTime()),
                            fileName,
                            document);
                    throw new ORDSException();
                }
            } else if (folderShortName.equals("Letters")) {
                UriComponentsBuilder builder3 =
                        UriComponentsBuilder.fromHttpUrl(host + "doc/processLetters");
                HttpEntity<ProcessXMLRequest> payload = new HttpEntity<>(req, new HttpHeaders());
                try {
                    HttpEntity<ProcessLettersResponse> response =
                            restTemplate.exchange(
                                    builder3.toUriString(),
                                    HttpMethod.POST,
                                    payload,
                                    ProcessLettersResponse.class);
                    if (!response.getBody().getResponseCd().equals("0")) {
                        log.warn("ResponseCd from DB is " + response.getBody().getResponseCd());
                        throw new ORDSException(response.getBody().getResponseMessageTxt());
                    }
                    log.info(
                            objectMapper.writeValueAsString(
                                    new RequestSuccessLog(
                                            "Request Success",
                                            "processDocumentsSvc - ProcessLettersXML")));
                } catch (Exception e) {
                    log.error(
                            objectMapper.writeValueAsString(
                                    new OrdsErrorLog(
                                            "Error received from ORDS",
                                            "processDocumentsSvc - ProcessLettersXML",
                                            e.getMessage(),
                                            req)));
                    saveError(
                            e.getMessage(),
                            dateFormat.format(Calendar.getInstance().getTime()),
                            fileName,
                            document);
                    throw new ORDSException();
                }
            } else {
                saveError(
                        "Unexpected folder short name: " + folderShortName,
                        dateFormat.format(Calendar.getInstance().getTime()),
                        fileName,
                        document);
                throw new ORDSException();
            }

        } else {
            // do nothing
            log.info(
                    "Document already processed. Response from GetDocumentProcessStatus: "
                            + resp.getBody());
            return;
        }
    }

    public void processReportsSvc(String folderName, String processedDate) throws IOException {
        List<String> pdfs = extractPDFFileNames(folderName);
        for (String pdf : pdfs) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(host + "rpt");

            InputStream reqPDF = fileService.getContent(pdf);
            byte[] file = IOUtils.toByteArray(reqPDF);
            reqPDF.close();

            ProcessReportRequest req =
                    new ProcessReportRequest(getFileName(pdf), processedDate, file);
            HttpEntity<ProcessReportRequest> payload = new HttpEntity<>(req, new HttpHeaders());
            try {
                HttpEntity<ProcessReportResponse> response =
                        restTemplate.exchange(
                                builder.toUriString(),
                                HttpMethod.POST,
                                payload,
                                ProcessReportResponse.class);
                if (!response.getBody().getResponseCd().equals("0")) {
                    log.warn("ResponseCd from DB is " + response.getBody().getResponseCd());
                    throw new ORDSException(response.getBody().getResponseMessageTxt());
                }
                log.info(
                        objectMapper.writeValueAsString(
                                new RequestSuccessLog("Request Success", "processReportSvc")));
            } catch (Exception e) {
                log.error(
                        objectMapper.writeValueAsString(
                                new OrdsErrorLog(
                                        "Error received from ORDS",
                                        "processReportSvc",
                                        e.getMessage(),
                                        req)));

                saveError(
                        e.getMessage(),
                        dateFormat.format(Calendar.getInstance().getTime()),
                        getFileName(pdf),
                        file);

                throw new ORDSException();
            }
        }
    }

    public boolean validateXml(String xsdPath, String xmlFile) {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaFile = new StreamSource(xsdPath);
        try {
            ByteArrayInputStream xmlFileForValidation = fileService.getContent(xmlFile);
            Schema schema = factory.newSchema(schemaFile);
            Source streamSource = new StreamSource(xmlFileForValidation);
            schema.newValidator().validate(streamSource);
            return true;
        } catch (Exception e) {
            log.error("validateXml error: " + e.getMessage());
            return false;
        }
    }

    public List<String> extractPDFFileNames(String folderName) throws IOException {
        /** Purpose of this service is to extract a list of file names from a given folder */
        List<String> pdfs = new ArrayList<>();

        try {
            List<String> fileNames = fileService.listFiles(folderName);
            for (String f : fileNames) {
                if (f.substring(f.lastIndexOf('.')).equalsIgnoreCase(".pdf")) {
                    pdfs.add(f);
                }
            }
            return pdfs;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    public String extractXMLFileName(List<String> fileList, String regex) throws IOException {
        /**
         * Purpose of this service is to extract a file from a list of file names given a specific
         * regex.
         */
        String result = null;
        if (fileList == null || fileList.size() == 0 || regex == null) {
            throw new IOException(
                    "Unsatisfied parameter requirement(s) at CRDP.Source.ProcessIncomingFile.Java:extractXMLFileName");
        }
        try {
            for (int i = 0; i < fileList.size(); i++) {
                if (Pattern.matches(regex, getFileName(fileList.get(i)))) {
                    if (result != null)
                        throw new IOException(
                                "Multiple files found satisfying regex at CRDP.Source.ProcessIncomingFile.Java:extractXMLFileName. Should only be one.");
                    result = fileList.get(i);
                }
            }
            return result;
        } catch (IOException ex) {
            throw new IOException(ex.getMessage());
        }
    }

    private static String getFileName(String filePath) {
        if (filePath.contains("\\") && !filePath.contains("/")) {
            // Windows path
            return filePath.substring(filePath.lastIndexOf("\\") + 1);
        } else if (filePath.contains("/") && !filePath.contains("\\")) {
            // Linux path
            return filePath.substring(filePath.lastIndexOf("/") + 1);
        } else {
            log.warn("Invalid file path: " + filePath);
            return filePath;
        }
    }
}
