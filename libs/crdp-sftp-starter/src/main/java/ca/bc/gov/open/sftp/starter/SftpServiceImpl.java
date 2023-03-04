package ca.bc.gov.open.sftp.starter;

import com.jcraft.jsch.*;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SftpServiceImpl implements FileService {

    private static final char UNIX_SEPARATOR = '/';

    interface SftpFunction {
        void exec(ChannelSftp channelSftp) throws SftpException;
    }

    public static final int BUFFER_SIZE = 8000;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final JschSessionProvider jschSessionProvider;

    private final SftpProperties sftpProperties;

    public SftpServiceImpl(JschSessionProvider jschSessionProvider, SftpProperties sftpProperties) {
        this.jschSessionProvider = jschSessionProvider;
        this.sftpProperties = sftpProperties;
    }

    /**
     * Get file content in byte array
     *
     * @param filename
     */
    @Override
    public ByteArrayInputStream getContent(String filename) {
        String sftpRemoteFilename = getFilePath(filename);

        ByteArrayInputStream result = null;
        byte[] buff = new byte[BUFFER_SIZE];

        try (ByteArrayOutputStream bao = new ByteArrayOutputStream()) {

            executeSftpFunction(
                    channelSftp -> {
                        try {
                            int bytesRead;
                            logger.debug("Attempting to get remote file [{}]", sftpRemoteFilename);
                            InputStream inputStream = channelSftp.get(sftpRemoteFilename);
                            logger.debug("Successfully get remote file [{}]", sftpRemoteFilename);
                            while ((bytesRead = inputStream.read(buff)) != -1) {
                                bao.write(buff, 0, bytesRead);
                            }
                        } catch (IOException e) {
                            throw new StarterSftpException(e.getMessage(), e.getCause());
                        }
                    });

            byte[] data = bao.toByteArray();

            try (ByteArrayInputStream resultBao = new ByteArrayInputStream(data)) {
                result = resultBao;
            }

        } catch (IOException e) {
            throw new StarterSftpException(e.getMessage(), e.getCause());
        }

        return result;
    }

    /**
     * Put the file to a destination
     *
     * @param inputFileName
     * @param remoteFileName
     */
    @Override
    public void put(String inputFileName, String remoteFileName) {
        String sftpRemoteFilename;
        if (remoteFileName.contains("objstr_zd/")) {
            sftpRemoteFilename =
                    sftpProperties.getRemoteLocation()
                            + remoteFileName.substring(
                                    remoteFileName.indexOf("objstr_zd/") + "objstr_zd/".length());
        } else {
            sftpRemoteFilename = getFilePath(remoteFileName);
        }

        executeSftpFunction(
                channelSftp -> {
                    try {
                        channelSftp.put(inputFileName, sftpRemoteFilename);
                        logger.debug("Successfully uploaded file [{}]", remoteFileName);
                    } catch (SftpException e) {
                        logger.error(
                                "Failed to put "
                                        + sftpRemoteFilename
                                        + ": "
                                        + e.getMessage()
                                        + e.id);
                        throw e;
                    }
                });
    }

    /**
     * Put the file with input stream to a destination
     *
     * @param inputStream
     * @param remoteFileName
     */
    @Override
    public void put(InputStream inputStream, String remoteFileName) {
        String sftpRemoteFilename = getFilePath(remoteFileName);

        executeSftpFunction(
                channelSftp -> {
                    try {
                        channelSftp.put(inputStream, sftpRemoteFilename);
                        logger.debug("Successfully uploaded file [{}]", remoteFileName);
                    } catch (SftpException e) {
                        logger.error(
                                "Failed to put "
                                        + sftpRemoteFilename
                                        + ": "
                                        + e.getMessage()
                                        + e.id);
                        throw e;
                    }
                });
    }

    /**
     * Move the file to a destination
     *
     * @param sourceFileName
     * @param destinationFilename
     */
    @Override
    public void moveFile(String sourceFileName, String destinationFilename) {
        String sftpRemoteFilename = getFilePath(sourceFileName);
        String sftpDestinationFilename = getFilePath(destinationFilename);

        executeSftpFunction(
                channelSftp -> {
                    try {
                        if (isDirectory(sourceFileName)) {
                            logger.info("moveFolder..." + sourceFileName);
                            if (!exists(destinationFilename)) {
                                // if parent directory of the destination does not exist
                                recursiveMakeFolderSvc(destinationFilename);
                            }
                            Vector files = channelSftp.ls(sourceFileName);
                            logger.info("number of files " + files.size());
                            for (int i = 0; i < files.size(); i++) {
                                ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) files.get(i);
                                logger.info(
                                        sourceFileName + UNIX_SEPARATOR + lsEntry.getFilename());
                                if (!lsEntry.getFilename().startsWith(".")) {
                                    logger.info(
                                            "moving "
                                                    + sourceFileName
                                                    + UNIX_SEPARATOR
                                                    + lsEntry.getFilename()
                                                    + " to "
                                                    + sftpDestinationFilename
                                                    + UNIX_SEPARATOR
                                                    + lsEntry.getFilename());
                                    channelSftp.rename(
                                            sourceFileName + UNIX_SEPARATOR + lsEntry.getFilename(),
                                            sftpDestinationFilename
                                                    + UNIX_SEPARATOR
                                                    + lsEntry.getFilename());
                                }
                            }
                        } else {
                            channelSftp.rename(sftpRemoteFilename, sftpDestinationFilename);
                        }
                        logger.debug(
                                "Successfully renamed files on the sftp server from {} to {}",
                                sftpRemoteFilename,
                                sftpDestinationFilename);
                    } catch (SftpException e) {
                        logger.error(
                                "Failed to move "
                                        + sftpRemoteFilename
                                        + "->"
                                        + sftpDestinationFilename
                                        + ": "
                                        + e.getMessage()
                                        + e.id);
                        throw e;
                    }
                });
    }

    private void recursiveMakeFolderSvc(String destinationFilename) {
        String parentDir =
                destinationFilename.substring(0, destinationFilename.lastIndexOf(UNIX_SEPARATOR));
        if (!exists(parentDir)) {
            recursiveMakeFolderSvc(parentDir);
        }
        makeFolder(destinationFilename);
    }

    /**
     * List all files and folders under the directory
     *
     * @param directory
     * @return list of files and folders names
     */
    @Override
    public List<String> listFiles(String directory) {
        String sftpRemoteDirectory = getFilePath(directory);
        List<String> result = new ArrayList<>();

        executeSftpFunction(
                channelSftp -> {
                    try {
                        Vector fileList = channelSftp.ls(sftpRemoteDirectory);

                        for (int i = 0; i < fileList.size(); i++) {
                            logger.debug("Attempting to list files in [{}]", sftpRemoteDirectory);
                            ChannelSftp.LsEntry lsEntry = (ChannelSftp.LsEntry) fileList.get(i);
                            logger.debug("Successfully to list files in [{}]", sftpRemoteDirectory);
                            result.add(sftpRemoteDirectory + "/" + lsEntry.getFilename());
                        }
                    } catch (SftpException e) {
                        logger.error(
                                "Failed to list files under "
                                        + sftpRemoteDirectory
                                        + ": "
                                        + e.getMessage()
                                        + e.id);
                        throw e;
                    }
                });

        return result;
    }

    /**
     * Remove the directory under the folder path Note that SFTP rmdir does NOT support deleting
     * non-empty directory, therefore a recursive deletion is implemented.
     *
     * @param folderPath
     */
    @Override
    public void removeFolder(String folderPath) {
        String remoteFilePath = getFilePath(folderPath);
        executeSftpFunction(
                channelSftp -> {
                    try {
                        Collection<ChannelSftp.LsEntry> fileAndFolderList =
                                channelSftp.ls(folderPath);
                        // Iterate objects in the list to get file/folder names
                        for (ChannelSftp.LsEntry item : fileAndFolderList) {
                            if (!item.getAttrs().isDir()) {
                                // Remove file
                                channelSftp.rm(folderPath + "/" + item.getFilename());
                            } else if (!(".".equals(item.getFilename())
                                    || "..".equals(item.getFilename()))) {
                                try {
                                    // Remove sub-directory
                                    channelSftp.rmdir(folderPath + "/" + item.getFilename());
                                } catch (Exception e) {
                                    // Do recursive deletion on sub-directory
                                    removeFolder(folderPath + "/" + item.getFilename());
                                }
                            }
                        }
                        // Remove current directory
                        channelSftp.rmdir(folderPath);
                        logger.debug("Successfully removed folder [{}]", remoteFilePath);
                    } catch (SftpException e) {
                        logger.error(
                                "Failed to remove "
                                        + remoteFilePath
                                        + ": "
                                        + e.getMessage()
                                        + e.id);
                        throw e;
                    }
                });
    }

    /**
     * Create a folder
     *
     * @param folderPath
     */
    @Override
    public void makeFolder(String folderPath) {
        String remoteFilePath = getFilePath(folderPath);
        executeSftpFunction(
                channelSftp -> {
                    try {
                        channelSftp.mkdir(remoteFilePath);
                        logger.debug("Successfully created folder [{}]", remoteFilePath);
                    } catch (SftpException e) {
                        logger.error(
                                "Failed to make " + remoteFilePath + ": " + e.getMessage() + e.id);
                        throw e;
                    }
                });
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
        String remoteFilePath = getFilePath(folderPath);
        executeSftpFunction(
                channelSftp -> {
                    try {
                        channelSftp.chmod(permission, remoteFilePath);
                        logger.debug("Successfully chmod of folder [{}]", remoteFilePath);
                    } catch (SftpException e) {
                        logger.error(
                                "Failed to chmod "
                                        + permission
                                        + " to "
                                        + remoteFilePath
                                        + ": "
                                        + e.getMessage()
                                        + e.id);
                        throw e;
                    }
                });
    }

    /**
     * Check if a file exists
     *
     * @param filePath
     * @return true/false of file existence
     */
    @Override
    public boolean exists(String filePath) {
        AtomicBoolean result = new AtomicBoolean(false);
        executeSftpFunction(
                channelSftp -> {
                    try {
                        channelSftp.lstat(getFilePath(filePath));
                        result.set(true);
                    } catch (SftpException e) {
                        if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                            result.set(false);
                        } else {
                            logger.error(
                                    "Failed to check existence of "
                                            + getFilePath(filePath)
                                            + ": "
                                            + e.getMessage()
                                            + e.id);
                            throw e;
                        }
                    }
                    logger.debug(getFilePath(filePath) + " is found");
                });
        return result.get();
    }

    /**
     * Check if a file is a directory/folder
     *
     * @param filePath
     * @return true/false of if file is a directory
     */
    @Override
    public boolean isDirectory(String filePath) {
        AtomicBoolean result = new AtomicBoolean(false);
        String remoteFilePath = getFilePath(filePath);
        executeSftpFunction(
                channelSftp -> {
                    try {
                        result.set(channelSftp.lstat(remoteFilePath).isDir());
                        logger.debug(
                                remoteFilePath
                                        + " is a directory is "
                                        + channelSftp.lstat(remoteFilePath).isDir());
                    } catch (SftpException e) {
                        logger.error(
                                "Failed to check isDirectory for "
                                        + remoteFilePath
                                        + ": "
                                        + e.getMessage()
                                        + e.id);
                        throw e;
                    }
                });
        return result.get();
    }

    /**
     * Get the last datetime timestamp of a file
     *
     * @param filePath
     * @return long milliseconds timestamp
     */
    @Override
    public long lastModify(String filePath) {
        AtomicLong result = new AtomicLong();
        String remoteFilePath = getFilePath(filePath);
        executeSftpFunction(
                channelSftp -> {
                    try {
                        // Note that getMTime returns timestamp in seconds
                        result.set(channelSftp.lstat(remoteFilePath).getMTime() * 1000);
                        logger.debug(
                                "Last modified of "
                                        + remoteFilePath
                                        + " is "
                                        + channelSftp.lstat(remoteFilePath).getMtimeString());
                    } catch (SftpException e) {
                        logger.error(
                                "Failed to get last modified for "
                                        + remoteFilePath
                                        + ": "
                                        + e.getMessage()
                                        + e.id);
                        throw e;
                    }
                });
        return result.get();
    }

    /** The primary method for executing sftp apis from ChannelSftp lib */
    private void executeSftpFunction(SftpFunction sftpFunction) {
        ChannelSftp channelSftp = null;
        Session session = null;

        try {
            session = jschSessionProvider.getSession();

            logger.debug("Attempting to open sftp channel");
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            logger.debug("Successfully connected to sftp server");

            sftpFunction.exec(channelSftp);

        } catch (JSchException | SftpException e) {
            throw new StarterSftpException(e.getMessage(), e.getCause());
        } finally {
            if (channelSftp != null && channelSftp.isConnected()) channelSftp.disconnect();

            jschSessionProvider.closeSession(session);
        }
    }

    /** The primary method for getting remote file's full path */
    private String getFilePath(String remotePath) {
        return FilenameUtils.separatorsToUnix(
                StringUtils.isNotBlank(sftpProperties.getRemoteLocation())
                        ? Paths.get(sftpProperties.getRemoteLocation(), remotePath).toString()
                        : Paths.get(remotePath).toString());
    }
}
