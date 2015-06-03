/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package iridiumftp;

import java.io.*;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTP;
import java.net.SocketException;

public class UploadIt 
{
    FTPClient ftp;
    File localFile2;
    String remoteFile2;
    javax.swing.JTextArea j2;
    boolean dirExists;
    IridiumFTP iFTP1;
    SocketFactoryForFTPClient ssf;
   
    public UploadIt()
    {
        ftp = new FTPClient();
        ssf = new SocketFactoryForFTPClient();
        ftp.setSocketFactory(ssf);
        ftp.setConnectTimeout(10000);
        
       

    }

public boolean connectToSite(String server) throws SocketException,IOException
{

    int reply;
    boolean success=false;  
    ftp.connect(server);  
    reply=ftp.getReplyCode();  
    if(reply >= 200 && reply <=300) 
    {      
        success=true;     
    }//end if

    return success;
}// end connectosite


    public boolean connectToSite(String server, int attempts) {

        int reply;
        boolean success = false;
        for (int i = 0; i < attempts; i++) {
            try {
                Thread.sleep(1000);
                ftp.connect(server);
                Thread.sleep(1000);
                reply = ftp.getReplyCode();
                if (reply >= 200 && reply <= 300) {
                    success = true;
                    return success;
                }//end if
            } catch (Exception e) {
                System.out.println("Unable to connect, attempting to connect  " + (attempts - i) + " more times");

                //e.printStackTrace();
            }//end catch

        }//end for
        return success;
    }// end connectosite

public boolean binary() throws IOException
{
    return ftp.setFileType(FTP.BINARY_FILE_TYPE);   
}// end binary


public boolean login(String userName, String password) throws IOException
{    
    return ftp.login(userName, password);
}// end login


public boolean closeConnection() throws IOException
{
    ftp.disconnect();    
    return ftp.isConnected();
}//end close connection


 public boolean isConnected()
 {
     return ftp.isConnected();
 }// end isconnected


 void setIridiumFTP(IridiumFTP iFTP){
    iFTP1=iFTP;

 }
 
 void enterLocalPassiveMode()
 {
     ftp.enterLocalPassiveMode();
 }//end method
 

	
 public int size(String x) throws IOException
 {
     int numOfBytes =-1,
             replyCode;     
     String reply;     
     ftp.sendCommand("SIZE " + x);
     reply = ftp.getReplyString();
     replyCode = ftp.getReplyCode();
     if(replyCode > 200 && replyCode < 300)
     {
         numOfBytes = (new Integer((reply.split(" "))[1].trim()).intValue());
     }//end if
     return numOfBytes;            
 }//ebd size()

    public boolean uploadFile (File file, String serverFile) throws IOException 
    {		

        boolean success;
        String response;
        FileInputStream in = new FileInputStream(file); 
             
        if(iFTP1.getDirExists())       
        {            
            success = ftp.storeFile(serverFile, in);
            response=ftp.getReplyString();
            
            if(response.toLowerCase().contains("no such file or directory"))
            {
                iFTP1.setDirExists(false);
                success= ftp.storeFile("/default/"+file.getName(),in);                
            }//end if
            
        }//end if

        else

        {           
            success= ftp.storeFile("/default/"+file.getName(),in);                                        
        }//end else             

        
        in.close();
        return success;	
    }// end upload file

  
	
    public boolean appendFile (File file, String serverSideFile,javax.swing.JTextArea j) throws IOException 
    {
        String serverFile = serverSideFile;
        int startByte, 
                fileSize,
                existingFileSize; 
        if(!iFTP1.getDirExists())
        {
            startByte = size("/default/"+file.getName()); 
        }//end if
        else
        {
            startByte = size(serverFile);  
        }//end else
             
        fileSize = (int)file.length();
        existingFileSize = startByte;
        boolean success =  false; 

        if (startByte < fileSize && startByte >= 0) 
        {    
            j.append(file.getName()+" found with " + startByte +" bytes\n");
            j.append("Resuming interrupted download\n");
            RandomAccessFile raf = new RandomAccessFile(file,"r");
            byte[] bytesIn = new byte[fileSize - startByte];
            ByteArrayInputStream in;                             
            raf.seek(startByte);
            raf.read(bytesIn);
            in = new ByteArrayInputStream(bytesIn);
            j.append("Attempting to send remaining " + (fileSize - startByte) + " bytes\n");
            
            if(iFTP1.getDirExists())       
            {            
                success = ftp.appendFile(serverFile, in);
            }//end if
            else
            {           
                success= ftp.appendFile("/default/"+file.getName(),in);                                       
            }//end else 
            raf.close();	  
        }// end if
        
        if(fileSize==existingFileSize)
        {    
            j.append(file.getName()+ " is already on the server\n");
   
        }
        if(startByte == fileSize)
        {
            success = true;
        }
        if(startByte > fileSize)
        {   
            j.append("oddly this file is smaller\nthan the one on the server\nreplacing "+file.getName()+" with smaller one \n");
            success = uploadFile(file,serverFile);
        }
        if(startByte == -1 )
        {    
            j.append("Partial file is not on the server\n");
            success = uploadFile(file,serverFile);   
        }// end if
        return success; 
    }// end append file 



    
    public boolean appendFile (File file, String serverSideFile,IridiumFTP j) throws IOException 
    {
        String serverFile = serverSideFile;
        int startByte, 
                fileSize,
                existingFileSize; 
        if(!iFTP1.getDirExists())
        {
            startByte = size("/default/"+file.getName()); 
        }//end if
        else
        {
            startByte = size(serverFile);  
        }//end else
             
        fileSize = (int)file.length();
        existingFileSize = startByte;
        boolean success =  false; 

        if (startByte < fileSize && startByte >= 0) 
        {    
            j.updateStatusTextArea(file.getName()+" found with " + startByte +" bytes\n");
            j.updateStatusTextArea("Resuming interrupted download\n");
            RandomAccessFile raf = new RandomAccessFile(file,"r");
            byte[] bytesIn = new byte[fileSize - startByte];
            ByteArrayInputStream in;                             
            raf.seek(startByte);
            raf.read(bytesIn);
            in = new ByteArrayInputStream(bytesIn);
            j.updateStatusTextArea("Attempting to send remaining " + (fileSize - startByte) + " bytes\n");
            
            if(iFTP1.getDirExists())       
            {            
                success = ftp.appendFile(serverFile, in);
            }//end if
            else
            {           
                success= ftp.appendFile("/default/"+file.getName(),in);                                       
            }//end else 
            raf.close();	  
        }// end if
        
        if(fileSize==existingFileSize)
        {    
            j.updateStatusTextArea(file.getName()+ " is already on the server\n");
   
        }
        if(startByte == fileSize)
        {
            success = true;
        }
        if(startByte > fileSize)
        {   
            j.updateStatusTextArea("oddly this file is smaller\nthan the one on the server\nreplacing "+file.getName()+" with smaller one \n");
            success = uploadFile(file,serverFile);
        }
        if(startByte == -1 )
        {    
            j.updateStatusTextArea("Partial file is not on the server\n");
            success = uploadFile(file,serverFile);   
        }// end if
        return success; 
    }// end append file 
    
    public void closeAllSockets()
    {
        ssf.closeAllConnections();
    
    }
    
}// end class
