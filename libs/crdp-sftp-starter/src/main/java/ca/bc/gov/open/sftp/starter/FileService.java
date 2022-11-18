package ca.bc.gov.open.sftp.starter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

public interface FileService {

    ByteArrayInputStream getContent(String filename);

    void put(String inputFileName, String remoteFileName);

    void put(InputStream inputStream, String remoteFileName);

    void moveFile(String sourceFileName, String destinationFilename);

    List<String> listFiles(String directory);

    void removeFolder(String folderPath);

    void makeFolder(String folderPath);

    void makeFolder(String folderPath, Integer permission);

    boolean exists(String filePath);

    boolean isDirectory(String filePath);

    long lastModify(String filePath);
}
