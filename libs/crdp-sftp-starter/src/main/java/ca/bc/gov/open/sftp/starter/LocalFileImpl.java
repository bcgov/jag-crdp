package ca.bc.gov.open.sftp.starter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalFileImpl implements FileService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public LocalFileImpl() {}

    /**
     * Get file content in byte array
     *
     * @param fileName
     */
    @Override
    public ByteArrayInputStream getContent(String fileName) {
        ByteArrayInputStream targetStream = null;
        File file = new File(fileName);
        try {
            targetStream = new ByteArrayInputStream(FileUtils.readFileToByteArray(file));
        } catch (Exception e) {
            logger.error("Failed to get " + fileName + ": " + e.getMessage());
        }
        return targetStream;
    }

    /**
     * Put the file to a destination
     *
     * @param inputFileName
     * @param remoteFileName
     */
    @Override
    public void put(String inputFileName, String remoteFileName) {
        try {
            moveFileSvc(inputFileName, remoteFileName);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Put the file with input stream to a destination
     *
     * @param inputStream
     * @param remoteFileName
     */
    @Override
    public void put(InputStream inputStream, String remoteFileName) {
        File target = new File(remoteFileName);
        try {
            Files.copy(inputStream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Move the file to a destination
     *
     * @param sourceFileName
     * @param destinationFilename
     */
    @Override
    public void moveFile(String sourceFileName, String destinationFilename) {
        try {
            File source = new File(sourceFileName);
            if (source.isDirectory()) {
                moveFolderSvc(sourceFileName, destinationFilename);
            } else {
                moveFileSvc(sourceFileName, destinationFilename);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * List all files and folders under the directory
     *
     * @param directory
     * @return list of files and folders names
     */
    @Override
    public List<String> listFiles(String directory) {
        File target = new File(directory);
        List<String> result = new ArrayList<>();
        for (File f : target.listFiles()) {
            result.add(f.getPath());
        }
        return result;
    }

    /**
     * Remove the directory under the folder path
     *
     * @param folderPath
     */
    @Override
    public void removeFolder(String folderPath) {
        File target = new File(folderPath);
        try {
            if (target.exists()) {
                FileUtils.deleteDirectory(target);
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Create a folder
     *
     * @param folderPath
     */
    @Override
    public void makeFolder(String folderPath) {
        File target = new File(folderPath);
        try {
            FileUtils.forceMkdir(target);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Create a folder with permission setup
     *
     * @param folderPath
     * @param permission
     */
    @Override
    public void makeFolder(String folderPath, Integer permission) {
        makeFolder(folderPath);
        File target = new File(folderPath);
        try {
            // Windows permissions are set to 777 by default
            // Note that this API will need upgrade
            target.setReadable(true);
            target.setWritable(true);
            target.setExecutable(true);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Check if a file exists
     *
     * @param filePath
     * @return true/false of file existence
     */
    @Override
    public boolean exists(String filePath) {
        File target = new File(filePath);
        return target.exists();
    }

    /**
     * Check if a file is a directory/folder
     *
     * @param filePath
     * @return true/false of if file is a directory
     */
    @Override
    public boolean isDirectory(String filePath) {
        File target = new File(filePath);
        return target.isDirectory();
    }

    /**
     * Get the last datetime timestamp of a file
     *
     * @param filePath
     * @return long timestamp
     */
    @Override
    public long lastModify(String filePath) {
        File target = new File(filePath);
        return target.lastModified();
    }

    /** The primary method for the Java service to move a single file */
    public static final void moveFileSvc(String filePath, String targetFolder) throws IOException {
        File source = new File(filePath);
        File destFolder = new File(targetFolder);
        FileUtils.moveFile(source, destFolder);
    }

    /** The primary method for the Java service to move a single folder */
    public static final void moveFolderSvc(String folderPath, String targetFolder)
            throws IOException {
        File source = new File(folderPath);
        File dest = new File(targetFolder);
        FileUtils.copyDirectory(source, dest);
        FileUtils.deleteDirectory(source);
    }
}
