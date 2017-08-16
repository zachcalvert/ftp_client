package FileActions;
import java.io.*;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import sun.rmi.runtime.Log;

import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;



public class FileActions extends Application{
    private static Scanner scan;
    private static FPTClient fptClient;
    @FXML
    public TextField addressText, usernameText;

    @FXML
    public PasswordField passText;

    public static void main(String[] args) {
        scan = new Scanner(System.in);
        fptClient = new FPTClient();
        launch(args);

        /*
        System.out.println("Enter user name: ");
        String username = scan.nextLine();
        System.out.println("Enter password: ");
        String password = scan.nextLine();

        boolean result = fptClient.Login(username, password);
        if(result){
            System.out.println("Connection exits succesfully.");
        } else{
            System.out.println("Connection fails.");
        }
        */
    }


    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/fxml/ftpclient.fxml"));
        Parent root = (Parent) loader.load();

        Controller controller = loader.<Controller>getController();
        controller.setFtpClient(fptClient);

        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 600, 450));
        primaryStage.show();
    }
    //System.exit(1);


}

class FPTClient{
    private FTPClient ftpClient;
    int replyCode = 0;
    FileInputStream localFileInputStream = null;
    String dirExistsReplyCode = null;
    List<File> fileFound = new ArrayList<File>();
    String strRemoteDirPath = null;

    public FPTClient(){
        ftpClient = new FTPClient();
    }

    public boolean Login(String username, String password) {
        try {
            ftpClient.connect("localhost",21); //agile //imanagiledude //
            boolean login = ftpClient.login(username, password);
            if (login) {
                System.out.println("Connection established...");
                System.out.println("Status: " + ftpClient.getStatus());

                return true;
            } else {
                return false;
            }

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean Logout() {
        try {
            boolean logout = ftpClient.logout();
            if (logout) {
                System.out.println("Succesfully logged out.");
                return true;
            } else {
                System.out.println("Logout was unsuccessful.");
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                ftpClient.disconnect(); // disconnect on logout
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean Rename(String oldName, String newName){
        try {
            boolean rename = ftpClient.rename(oldName,newName);
            if(rename) {
                System.out.println(oldName + " has been renamed to " + newName);
                return true;
            } else {
                System.out.println("Failed to rename: " + oldName);
                return false;
            }
        } catch (SocketException ex){
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public boolean DeleteFile(String fileName) {
        try {
            boolean deleted = ftpClient.deleteFile(fileName);
            if (deleted) {
                System.out.println("The file was deleted successfully.");
                return true;
            } else {
                System.out.println("Could not delete the file.");
                return false;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public boolean ChangePermissions(String fileName, String permissions) {
        /*
            Changes the permissions of the specified file. Expects a string
            of permission levels for the owner, the group and the world, e.g. '755'
            or another set of digits that work with the chmod command.

            https://www.linux.org/threads/file-permissions-chmod.4124/

            Requires the user to be in the directory where the file is located, or to provide relative path 
            to the file as filename.
         */
        try {
            boolean permissions_changed = ftpClient.sendSiteCommand("chmod " + permissions + " " + fileName);
            if (permissions_changed) {
                System.out.println("The file permissions were changed successfully to: " + permissions);
                return true;
            } else {
                System.out.println("Could not change permissions on the file.");
                return false;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * Created by haritha on 7/16/17.
     * Uploads single/multiple file(s) from local to server.
     * @param  listLocalFiles, strRemoteFile, countOfFiles
     * @return replyCode - the return code from server ( 226 - Closing data connection. Requested file action successful)
     */
    public int UploadFilesToFTPServer(String strRemoteFile, List<File> listLocalFiles) {
        try{
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.setSoTimeout(100000);
            ftpClient.setControlKeepAliveTimeout(100000);
            //localFileInputStream.close();

            if(strRemoteFile.endsWith("/"))
            {
                strRemoteFile = strRemoteFile;
            }else
                strRemoteFile = strRemoteFile + "/";
            if (ftpClient.changeWorkingDirectory(strRemoteFile)) {
                for (File listLocalFile : listLocalFiles) {
                    String remoteFileExistsReplyCode = ftpClient.getStatus(strRemoteFile + listLocalFile.getName());

                    if (!remoteFileExistsReplyCode.contains(listLocalFile.getName()) || remoteFileExistsReplyCode.contains("No such file or directory")) {
                        localFileInputStream = new FileInputStream(listLocalFile);

                        OutputStream outputStream = ftpClient.storeFileStream(strRemoteFile + listLocalFile.getName());
                        byte[] bytesIn = new byte[4096];
                        int read = 0;

                        while ((read = localFileInputStream.read(bytesIn)) != -1) {
                            outputStream.write(bytesIn, 0, read);
                        }
                        localFileInputStream.close();
                        outputStream.close();

                        boolean storeFileCompleted = ftpClient.completePendingCommand();
                        if (storeFileCompleted) {
                            System.out.println("Successfully uploaded the file " + strRemoteFile + listLocalFile.getName() + " to server");
                        }

                    } else
                        System.out.println("File " + strRemoteFile + listLocalFile.getName() + " already exists.");
                }
            }else
                System.out.println("Directory " + strRemoteFile + "doesn't exist");
        }catch (IOException ex) {
            ex.printStackTrace();
        }
        return ftpClient.getReplyCode();
    }


    /**
     * Created by haritha on 8/4/17.
     * Copies the directories and sub directories recursively from local to server.
     * @param localDirPath, strRemoteDirPath
     * @return replyCode -  the return code from server ( 226 - Closing data connection. Requested file action successful)
     */

    public int CopyDirectoryToFTPServer(File localDirPath, String strRemoteDirPath) {
        try{
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.setSoTimeout(50000);

            if(localDirPath.exists()) {
                File[] localDirFiles = localDirPath.listFiles();
                if(localDirFiles.length > 0) {
                    dirExistsReplyCode = ftpClient.getStatus(strRemoteDirPath);
                    if (ftpClient.changeWorkingDirectory(strRemoteDirPath)) {
                        String strLocalDirName = localDirPath.getName() + "/";

                        if (strRemoteDirPath.endsWith("/")) {
                            strRemoteDirPath = strRemoteDirPath + strLocalDirName;
                        } else
                            strRemoteDirPath = strRemoteDirPath + "/" + strLocalDirName;


                        if (!ftpClient.changeWorkingDirectory(strRemoteDirPath)) {
                            ftpClient.makeDirectory(strRemoteDirPath);

                            for (File localFile : localDirFiles) {
                                if (localFile.isDirectory()) {
                                    listSubDirAndDirectories(ftpClient, strRemoteDirPath, localFile);
                                } else {

                                    localFileInputStream = new FileInputStream(localFile);
                                    OutputStream outputStream = ftpClient.storeFileStream(strRemoteDirPath + localFile.getName());
                                    byte[] bytesIn = new byte[4096];
                                    int read = 0;

                                    while ((read = localFileInputStream.read(bytesIn)) != -1) {
                                        outputStream.write(bytesIn, 0, read);
                                    }
                                    localFileInputStream.close();
                                    outputStream.close();

                                    boolean storeFileCompleted = ftpClient.completePendingCommand();
                                    if (storeFileCompleted) {
                                        System.out.println("Successfully uploaded the file " + strRemoteDirPath + localFile.getName() + " to server");
                                    }else
                                        System.out.println(ftpClient.getReplyCode());
                                }
                            }
                        } else
                            System.out.println("The directory " + localDirPath.getName() + " at server already exists.");
                    } else
                        System.out.println("The directory " + strRemoteDirPath + " at server doesn't exist");
                }else
                    System.out.println("There are no files in the directory");
            }else
                System.out.println("The local directory " + localDirPath + " doesn't exist");
        }catch (IOException ex) {
            ex.printStackTrace();
        }
        return ftpClient.getReplyCode();
    }

    public void listSubDirAndDirectories(FTPClient ftpClient, String remoteDirPath, File localFile) throws IOException{
        remoteDirPath = remoteDirPath + localFile.getName() + "/";
        ftpClient.makeDirectory(remoteDirPath);
        File[] localFileSubDir = localFile.listFiles();
        for (File file:localFileSubDir) {
            if(file.isDirectory())
                listSubDirAndDirectories(ftpClient, remoteDirPath, file);
            else {
                localFileInputStream = new FileInputStream(file);
                OutputStream outputStream = ftpClient.storeFileStream(remoteDirPath + file.getName());
                byte[] bytesIn = new byte[4096];
                int read = 0;

                while ((read = localFileInputStream.read(bytesIn)) != -1) {
                    outputStream.write(bytesIn, 0, read);
                }
                localFileInputStream.close();
                outputStream.close();

                boolean storeFileCompleted = ftpClient.completePendingCommand();
                if (storeFileCompleted) {
                    System.out.println("Successfully uploaded the file " + remoteDirPath + file.getName() + " to server");
                }
            }
        }
    }


    /**
     * Created by haritha on 8/9/17.
     * Copies the directories and sub directories recursively from local to server.
     * @param localDirPath, fileNameToSearch
     * @return replyCode -  the return code from server ( 226 - Closing data connection. Requested file action successful)
     */
    public void searchLocalFile(File localDirPath, String fileNameToSearch) {
        if (localDirPath.isDirectory()) {
            List<File> filesFound = searchLocalDirectory(localDirPath, fileNameToSearch);

            System.out.println("Found: ");
            for(File file: filesFound){
                System.out.println(file.getName() + " at " + file.getParentFile().getAbsolutePath());
            }
        } else
            System.out.println(localDirPath.getAbsoluteFile() + " is not a directory!");
    }

    public List<File> searchLocalDirectory(File localDirPath, String fileNameToSearch){
        if(localDirPath.isDirectory()){
            if(localDirPath.canRead()){
                for (File localFile: localDirPath.listFiles()) {
                    if(localFile.isDirectory()) {
                        if(localFile.getName().toLowerCase().contains(fileNameToSearch))
                            fileFound.add(localFile);
                        searchLocalDirectory(localFile, fileNameToSearch);
                    }
                    else{
                        if(localFile.getName().toLowerCase().contains(fileNameToSearch))
                            fileFound.add(localFile);
                    }
                }
            }else
                System.out.println("Cannot read this directory: " + localDirPath.getAbsolutePath());
        }else
            System.out.println("Directory " + localDirPath.getAbsolutePath() + " doesn't exist");

        return fileFound;
    }

    /**
     * Created by haritha on 8/9/17
     * Copies the directories and sub directories recursively from local to server.
     * @param remoteDirPath, fileNameToSearch
     * @return replyCode -  the return code from server ( 226 - Closing data connection. Requested file action successful)
     */
    public void searchRemoteFile(File remoteDirPath, String fileNameToSearch) throws IOException{
        if (ftpClient.changeWorkingDirectory(remoteDirPath.getAbsolutePath())) {
            strRemoteDirPath = remoteDirPath.getAbsolutePath();
            FTPFile[] listRemoteFiles = ftpClient.listFiles(strRemoteDirPath);
            for (FTPFile localFile: listRemoteFiles) {
                if(localFile.isDirectory()) {
                    if(localFile.getName().toLowerCase().contains(fileNameToSearch))
                        System.out.println("Found: " + localFile.getName() + " at " + strRemoteDirPath);
                    searchRemoteDirectory(localFile, fileNameToSearch);
                } else{
                    if(localFile.getName().toLowerCase().contains(fileNameToSearch))
                        System.out.println("Found: " + localFile.getName() + " at " + strRemoteDirPath);
                }
            }
        } else {
            System.out.println(remoteDirPath.getAbsoluteFile() + " is not a directory!");
        }
    }

    /**
     * Created by haritha on 8/9/17
     * Searches for a file in the directories and sub directories recursively at server.
     * @param dirPath, fileNameToSearch
     * @return replyCode -  the return code from server ( 226 - Closing data connection. Requested file action successful)
     */
    public void searchRemoteDirectory(FTPFile dirPath, String fileNameToSearch)  throws IOException{
        if(dirPath.isDirectory()){
            String strRemoteSubDirPath = strRemoteDirPath + "/" + dirPath.getName();
            FTPFile[] listRemoteFiles = ftpClient.listFiles(strRemoteSubDirPath);
                for (FTPFile localFile: listRemoteFiles) {
                    if(localFile.isDirectory()){
                        if(localFile.getName().toLowerCase().contains(fileNameToSearch))
                            System.out.println("Found: " + localFile.getName() + " at " + strRemoteSubDirPath);
                        strRemoteDirPath = strRemoteSubDirPath;
                        searchRemoteDirectory(localFile, fileNameToSearch);
                    } else{
                        if(localFile.getName().toLowerCase().contains(fileNameToSearch))
                            System.out.println("Found: " + localFile.getName() + " at " + strRemoteSubDirPath);
                    }
                }
        }else
            System.out.println("Directory " + dirPath.getName() + " doesn't exist");
    }

}


