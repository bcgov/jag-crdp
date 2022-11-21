package ca.bc.gov.open.crdp.process.scanner.services;

import ca.bc.gov.open.crdp.models.MqErrorLog;
import ca.bc.gov.open.crdp.process.models.ScannerPub;
import ca.bc.gov.open.crdp.process.scanner.configuration.QueueConfig;
import ca.bc.gov.open.sftp.starter.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;
import org.springframework.ws.server.endpoint.annotation.Endpoint;

@Endpoint
@Slf4j
public class ScannerService {

    @Value("${crdp.in-file-dir}")
    private String inFileDir = "/";

    @Value("${crdp.progressing-dir}")
    private String processingDir = "/";

    @Value("${crdp.record-ttl-hour}")
    private int recordTTLHour = 24;

    @Value("${crdp.sftp-enabled}")
    private String sftpEnabled = "true";

    private Integer PERMISSIONS_DECIMAL = 493;

    @Autowired JschSessionProvider jschSessionProvider;
    private FileService fileService;
    private final SftpProperties sftpProperties;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    private final AmqpAdmin amqpAdmin;
    private final Queue scannerQueue;
    private final QueueConfig queueConfig;

    private static String
            processFolderName; // current "Processed_yyyy_nn" folder name (not full path).

    private static TreeMap<String, String> processingFilesToMove = new TreeMap<String, String>();
    private static TreeMap<String, String> processingFoldersToMove =
            new TreeMap<String, String>(); // completed files.

    LocalDateTime scanDateTime;
    DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");

    @Autowired
    public ScannerService(
            @Qualifier("scanner-queue") Queue scannerQueue,
            AmqpAdmin amqpAdmin,
            QueueConfig queueConfig,
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            RabbitTemplate rabbitTemplate,
            SftpProperties sftpProperties,
            FileService fileService) {
        this.scannerQueue = scannerQueue;
        this.amqpAdmin = amqpAdmin;
        this.queueConfig = queueConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.sftpProperties = sftpProperties;
        this.fileService = fileService;
    }

    /** The primary method for the Java service to scan CRDP directory */
    @Scheduled(cron = "${crdp.cron-job-incoming-file}")
    public void CRDPScanner() {
        fileService =
                Boolean.valueOf(sftpEnabled)
                        ? new SftpServiceImpl(jschSessionProvider, sftpProperties)
                        : new LocalFileImpl();

        // re-initialize arrays
        processingFilesToMove = new TreeMap<String, String>();
        processingFoldersToMove = new TreeMap<String, String>();

        scanDateTime = LocalDateTime.now();

        log.info(
                "inFileDir:"
                        + inFileDir
                        + " exists:"
                        + fileService.exists(inFileDir)
                        + " isDirectory:"
                        + fileService.isDirectory(inFileDir));
        if (fileService.exists(inFileDir) && fileService.isDirectory(inFileDir)) {
            // Create Processing folder
            if (!fileService.exists(processingDir)) {
                log.info("Making Processing Dir:" + processingDir);
                // 493 -> 111 101 101 -> 755
                fileService.makeFolder(processingDir, PERMISSIONS_DECIMAL);
            }
            for (String f : fileService.listFiles(inFileDir)) {
                log.info("listing inFileDir Files:" + f);
            }
            String[] arr = fileService.listFiles(inFileDir).toArray(new String[0]);

            // Calling recursive method
            try {
                recursiveScan(arr, 0, 0);
            } catch (Exception ex) {
                log.error(ex.getMessage());
            }

            if (processingFilesToMove.isEmpty() && processingFoldersToMove.isEmpty()) {
                log.info("No file/fold found, closing current scan session: " + scanDateTime);
                return;
            }

            // Create Processing/Datetime folder
            // 493 -> 111 101 101 -> 755
            fileService.makeFolder(
                    processingDir + "/" + customFormatter.format(scanDateTime),
                    PERMISSIONS_DECIMAL);

            try {
                // Move files into processing folder
                for (Entry<String, String> m : processingFilesToMove.entrySet()) {
                    log.info("Moving " + m.getKey() + " to " + m.getValue());
                    fileService.moveFile(m.getKey(), m.getValue());
                    enQueue(new ScannerPub(m.getValue(), customFormatter.format(scanDateTime)));
                }

                for (Entry<String, String> m : processingFoldersToMove.entrySet()) {
                    log.info("Moving " + m.getKey() + " to " + m.getValue());
                    fileService.moveFile(m.getKey(), m.getValue());
                    enQueue(new ScannerPub(m.getValue(), customFormatter.format(scanDateTime)));
                }
                log.info("Scan Complete");
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } else {
            log.error("Incoming file directory \"" + inFileDir + "\" does not exist");
        }

        // Clean up IN directory
        cleanUp(inFileDir);
    }

    private void cleanUp(String headFolderPath) {
        for (var folder : fileService.listFiles(headFolderPath)) {
            if (!fileService.isDirectory(folder)
                    || getFileName(folder).equals("Processing")
                    || (getFileName(folder).startsWith("."))) {
                continue;
            }

            // delete old Errors and Completed subfolders
            if (getFileName(folder).equals("Errors") || getFileName(folder).equals("Completed")) {
                for (var f : fileService.listFiles(folder)) {
                    if (getFileName(f).startsWith(".")) {
                        continue;
                    }
                    if (new Date().getTime() - fileService.lastModify(f)
                            > recordTTLHour * 60 * 60 * 1000) {
                        log.info("Old file detected: " + fileService.lastModify(f));
                        log.info("Deleting... " + f);
                        fileService.removeFolder(f);
                    }
                }
                continue;
            }

            // delete processed folders (delivered from Ottawa).
            if (fileService.listFiles(folder).size() <= 2) {
                log.info("Deleting... " + folder);
                fileService.removeFolder(folder);
            } else {
                for (String f : fileService.listFiles(folder)) {
                    if (!getFileName(f).startsWith(".")
                            && fileService.isDirectory(f)
                            && fileService.listFiles(f).size() <= 2) {
                        log.info("Deleting... " + f);
                        fileService.removeFolder(f);
                    }
                }
            }
        }
    }

    private void recursiveScan(String[] arr, int index, int level) {
        // terminate condition
        if (index == arr.length) return;
        try {
            // for root folder files (Audit and Status).
            if (!fileService.isDirectory(arr[index])) {
                processingFilesToMove.put(
                        arr[index],
                        processingDir
                                + "/"
                                + customFormatter.format(scanDateTime)
                                + "/"
                                + Paths.get(arr[index]).getFileName().toString());
            }

            // for sub-directories
            if (fileService.isDirectory(arr[index])) {
                // Retain the name of the current process folder short name
                // and add to list for deletion at the end of processing.
                if (isProcessedFolder(getFileName(arr[index]))) {
                    processFolderName = getFileName(arr[index]);
                }
                if (isProcessedFolder(getFileName(arr[index]))
                        || isProcessedSubFolder(getFileName(arr[index]))) {
                    if ("CCs".equals(getFileName(arr[index]))
                            || "Letters".equals(getFileName(arr[index]))
                            || "R-Lists".equals(getFileName(arr[index]))
                            || "JUS178s".equals(getFileName(arr[index]))) {
                        processingFoldersToMove.put(
                                arr[index],
                                processingDir
                                        + "/"
                                        + customFormatter.format(scanDateTime)
                                        + "/"
                                        + processFolderName
                                        + "/"
                                        + getFileName(arr[index]));
                    } else {
                        // recursion for sub-directories
                        recursiveScan(
                                fileService.listFiles(arr[index]).toArray(new String[0]),
                                0,
                                level + 1);
                    }
                }
            }

        } catch (Exception ex) {
            log.error(
                    "An error was captured from the CRDP Scanner. Message: "
                            + ex.getLocalizedMessage());
        }

        // recursion for main directory
        recursiveScan(arr, ++index, level);
    }

    private static boolean isProcessedFolder(String name) {
        String processedRegex =
                "\\bProcessed_\\w+[-][0-9][0-9][-][0-9][0-9]"; // \bProcessed_\w+[-][0-9][0-9][-][0-9][0-9]
        return Pattern.matches(processedRegex, name);
    }

    private static boolean isProcessedSubFolder(String name) {
        if ("CCs".equals(name)
                || "JUS178s".equals(name)
                || "Letters".equals(name)
                || "R-Lists".equals(name)) return true;
        else return false;
    }

    private void enQueue(ScannerPub pub) throws JsonProcessingException {
        try {
            this.rabbitTemplate.convertAndSend(
                    queueConfig.getTopicExchangeName(), queueConfig.getScannerRoutingkey(), pub);
        } catch (Exception ex) {
            log.error(
                    objectMapper.writeValueAsString(
                            new MqErrorLog(
                                    "Enqueue failed",
                                    "RecursiveScan",
                                    ex.getMessage(),
                                    pub.getFilePath())));
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
