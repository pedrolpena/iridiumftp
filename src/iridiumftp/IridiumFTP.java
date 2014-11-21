/**
*    IridiumFTP is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    IridiumFTP is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with IridiumFTP.  If not, see <http://www.gnu.org/licenses/>.
*
*/

/*
 * IridiumFTP.java
 *@author Pedro Pena
 * Created on Jan 20, 2011, 10:39:30 AM
 * May 5th, 2011 changed UleadThread from a Runnable implementation to inherit from Thread. this allows a reference to use he isAlive() method
 * to make sure the thread is dead before creating it to access rasdial.
 *
 * May 5th, 2011 changed default caret update policy for the status JTextArea to always update
 * may 11th, 2011 added append method in Uploadit and SunFtpWrapper that accepts a reference to the File object to be uploaded
 * instead passing a string that is later used to create a File object.
 * The reason is that IridiumFTP later creates a File object for the same file and this can cause conflicts.
 *
 * May 5ht, 2011 append methos now acceps a JTextArea Object that it updates with file upload status
 * Aug 2nd, 2011 the program zips any file that does not contain the .zip extension before transmitting.
 * Aug 2nd, 2011 if there is a no such directory error while uploading it will upload to the /default folder
 * Aug 5th added statements to close any open streams in the catch blocks of the append and upload methods of SunFtpWrapper
 * Aug 8th saves transmitted files names in an sqlite database and checks the database before dialing. if the filename is found
 * in the database then the file is deleted and a connection is never attempted. This was added because it was noticed that
 * the program attempted to upload a file that had already been uploaded continuously.
 * Dec 8th 2011, added login and server connect attempt tracking. If there are 5 consecutive failed login or server connect attempts
 * then the program will stop attempting to connect every time it checks the queue. instead it will try and connect once every 24 hours until it
 * successfully connects. Since every successfull connection is has a cost to it one can potentially receive a huge bill without ever transmitting any data.
 * Dec 9th 2011, restart
 * Dec 13 2011, added aa conection log to track connect times.
 * May 24 2012, added initilizatons to app preferences because new isntalls were crashing at startup.
 * Jul 9 2014, repalced sun ftp libray with apache commons ftp client library 3.3
 * Jul 13 2014 added messaging socket server to transmist messages
 * Jul 14 2014 add crc check o determine of zip file is good before sending
 * Jul 17 2014 added A CommandClient which is a socket thread that attempts to connect to the messaging server. The prgram will close if a connection is made.
 * This is to make sure there is  only ever one instance of the program up and running.
 * Aug 4 2014 modified transmitted file database to include transmission date. This is added using epoch time. milliseconds since jan 1st 1970 00:00:00
 * Aug 7 2014 modified wasTransmitted method to return false if an exception occurs and to check if the resultset is empty
 * Aug 7 2014 added method to log exceptions
 * Aug 8 2014 changed when it is considered a successful connection for the purposes of the 24 hour queue timer. the failed attempts timer is reset when successfully set to binary mode.
 * Aug 8 2014 replaced \n by system dependent newline character in the log file mehtods
 * 10.15.14 removed method this.revalidate() so that it could be compiled with java 1.6 
 * 10.15.14 updated getDate(). no longer deprecated
 */

package iridiumftp;
import java.io.*;
import java.util.zip.*;
import java.util.prefs.*;
import java.util.Date;
import javax.swing.Timer;
import java.awt.event.*;
import javax.swing.text.DefaultCaret;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.TimeZone;




/**
 *
 * @author Pedro.Pena
 */
public class IridiumFTP extends javax.swing.JFrame implements ActionListener{
MessagingServer messageMan;
String NL =  System.getProperty("line.separator");
UploadIt u;
Timer timer;
Timer prefsTimer;
Timer queueTimer;
Thread messageThread;
ActionListener prefsActionListener,queueTimerActionListener;
RasDialer rD;
PPPConnectThread pppConnect;
String host = "127.0.0.1";
int port = 25000;
CommandClient comClient;
boolean dirExists = true,isVisible=true;
Preferences prefs = Preferences.userNodeForPackage (getClass ());
boolean isTransmitting = false,tempbool;
 File pWD,logDIR;
int centerX=0,centerY=0;
//long connectTime=-1,loginTime=-1,uploadAttempt=-1,uploaded=-1,connectionClosed=-1;
int unsuccessfulLoginAttempts=0;    // this holds the number of failed login attempts
int unsuccessfulServerConnectAttempts=0; //  this holds the number of failed server connects 

long    dialTime,       //holds time when a dialout attempt is made
        internetConnect, //holds the time the actual internet connection is made
        serverConnect,  //holds the time when connection to the FTP server is made
        loginTime,      //holds the time when a successful login to the ftp server is made
        uploadStartTime,//holds the time when an upload/append is started
        uploadEndTime,  //holds the time when an upload/append is completed
        disconnectTime, //holds the time when the modem disconnects from the internet
        fileSize,       //holds the number of bytes
        averageTransferRate;//holds the average transfer rate

String fileName = "None",temp;
String connectionHeader="Time_Stamp,Server_Connect(ms),Login_Time(ms),Upload_duration(ms),File_Size(Bytes),Transfer_Rate(bps),Total_Time_Connected(ms),File_Name\n";


    /** Creates new form IridiumFTP */
    public IridiumFTP() {
         this.setLocationRelativeTo(null);
         
        initComponents();
       

        int h = this.getSize().height;
        int w = this.getSize().width;
         centerX = this.getLocation().x;
         centerY = this.getLocation().y;
        this.setLocation(centerX-w/2, centerY-h/2);
                                      
        init();

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileChooserFrame = new javax.swing.JFrame();
        someFileChooser = new javax.swing.JFileChooser();
        passWordPromptFrame = new javax.swing.JFrame();
        jPanel4 = new javax.swing.JPanel();
        passWordLabel = new javax.swing.JLabel();
        jPasswordField1 = new javax.swing.JPasswordField();
        passWordCancelButton = new javax.swing.JButton();
        passWordOKButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        BrowsejButton = new javax.swing.JButton();
        queueLocationTextField = new javax.swing.JTextField();
        QueuePanel = new javax.swing.JPanel();
        transmitScrollPane = new javax.swing.JScrollPane();
        transmitTextArea = new javax.swing.JTextArea();
        StatusScrollPane = new javax.swing.JScrollPane();
        statusTextArea = new javax.swing.JTextArea();
        FTPSeverPanel = new javax.swing.JPanel();
        serverTextField = new javax.swing.JTextField();
        pathTextField = new javax.swing.JTextField();
        userNameTextField = new javax.swing.JTextField();
        serverLabel = new javax.swing.JLabel();
        pathLabel = new javax.swing.JLabel();
        userNameLabel = new javax.swing.JLabel();
        passwordLabel = new javax.swing.JLabel();
        serverPasswordField = new javax.swing.JPasswordField();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        queueRefreshIntervalTextField = new javax.swing.JTextField();
        transmitCheckbox = new javax.swing.JCheckBox();
        phoneBookEntryCheckBox = new javax.swing.JCheckBox();
        phoneBookentryTextField = new javax.swing.JTextField();
        phoneBookEntryLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        savePreferencesjButton = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        ExitMenuItem = new javax.swing.JMenuItem();

        fileChooserFrame.setMinimumSize(new java.awt.Dimension(581, 431));

        someFileChooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        someFileChooser.setSelectedFiles(null);
        someFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                someFileChooserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout fileChooserFrameLayout = new javax.swing.GroupLayout(fileChooserFrame.getContentPane());
        fileChooserFrame.getContentPane().setLayout(fileChooserFrameLayout);
        fileChooserFrameLayout.setHorizontalGroup(
            fileChooserFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, fileChooserFrameLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(someFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, 561, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        fileChooserFrameLayout.setVerticalGroup(
            fileChooserFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fileChooserFrameLayout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(someFileChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        passWordPromptFrame.setAlwaysOnTop(true);
        passWordPromptFrame.setMinimumSize(new java.awt.Dimension(273, 185));
        passWordPromptFrame.setResizable(false);

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createTitledBorder("Password")));
        jPanel4.setMaximumSize(new java.awt.Dimension(231, 206));
        jPanel4.setMinimumSize(new java.awt.Dimension(231, 206));
        jPanel4.setRequestFocusEnabled(false);

        passWordLabel.setText("Password");

        jPasswordField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jPasswordField1ActionPerformed(evt);
            }
        });

        passWordCancelButton.setText("Accept");
        passWordCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                passWordCancelButtonActionPerformed(evt);
            }
        });

        passWordOKButton.setText("Cancel");
        passWordOKButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                passWordOKButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(passWordLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 99, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(passWordCancelButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 116, Short.MAX_VALUE)
                        .addComponent(passWordOKButton)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(passWordLabel))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(passWordCancelButton)
                    .addComponent(passWordOKButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout passWordPromptFrameLayout = new javax.swing.GroupLayout(passWordPromptFrame.getContentPane());
        passWordPromptFrame.getContentPane().setLayout(passWordPromptFrameLayout);
        passWordPromptFrameLayout.setHorizontalGroup(
            passWordPromptFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(passWordPromptFrameLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        passWordPromptFrameLayout.setVerticalGroup(
            passWordPromptFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(passWordPromptFrameLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setTitle("AOML Iridium FTPer");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Queue Location"));

        BrowsejButton.setText("Browse");
        BrowsejButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BrowsejButtonActionPerformed(evt);
            }
        });

        queueLocationTextField.setEditable(false);
        queueLocationTextField.setToolTipText("");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(BrowsejButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(queueLocationTextField)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(BrowsejButton)
                    .addComponent(queueLocationTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        QueuePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Queue"));

        transmitScrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder("To Transmit"));

        transmitTextArea.setEditable(false);
        transmitTextArea.setColumns(20);
        transmitTextArea.setRows(5);
        transmitScrollPane.setViewportView(transmitTextArea);

        StatusScrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder("Status"));

        statusTextArea.setEditable(false);
        statusTextArea.setColumns(20);
        statusTextArea.setRows(5);
        StatusScrollPane.setViewportView(statusTextArea);

        javax.swing.GroupLayout QueuePanelLayout = new javax.swing.GroupLayout(QueuePanel);
        QueuePanel.setLayout(QueuePanelLayout);
        QueuePanelLayout.setHorizontalGroup(
            QueuePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(QueuePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(transmitScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(StatusScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 364, Short.MAX_VALUE)
                .addContainerGap())
        );
        QueuePanelLayout.setVerticalGroup(
            QueuePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, QueuePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(QueuePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(StatusScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 242, Short.MAX_VALUE)
                    .addComponent(transmitScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 242, Short.MAX_VALUE))
                .addContainerGap())
        );

        FTPSeverPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("FTP Server info"));

        serverTextField.setMinimumSize(new java.awt.Dimension(20, 25));
        serverTextField.setPreferredSize(new java.awt.Dimension(150, 25));
        serverTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serverTextFieldActionPerformed(evt);
            }
        });

        pathTextField.setPreferredSize(new java.awt.Dimension(150, 25));
        pathTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pathTextFieldActionPerformed(evt);
            }
        });

        userNameTextField.setPreferredSize(new java.awt.Dimension(150, 25));

        serverLabel.setText("Server");

        pathLabel.setText("Upload Path");

        userNameLabel.setText("Username");

        passwordLabel.setText("Password");

        serverPasswordField.setPreferredSize(new java.awt.Dimension(150, 25));
        serverPasswordField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serverPasswordFieldActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout FTPSeverPanelLayout = new javax.swing.GroupLayout(FTPSeverPanel);
        FTPSeverPanel.setLayout(FTPSeverPanelLayout);
        FTPSeverPanelLayout.setHorizontalGroup(
            FTPSeverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(FTPSeverPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(FTPSeverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(serverPasswordField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(userNameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pathTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(serverTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(4, 4, 4)
                .addGroup(FTPSeverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(userNameLabel)
                    .addComponent(pathLabel)
                    .addComponent(passwordLabel)
                    .addComponent(serverLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        FTPSeverPanelLayout.setVerticalGroup(
            FTPSeverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(FTPSeverPanelLayout.createSequentialGroup()
                .addGroup(FTPSeverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(serverTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(serverLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(FTPSeverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pathTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pathLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(FTPSeverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(userNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(userNameLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(FTPSeverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(serverPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(passwordLabel))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Configure transmission"));

        jLabel1.setText("Check Queue every");

        jLabel2.setText("minutes");

        queueRefreshIntervalTextField.setHighlighter(null);
        queueRefreshIntervalTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                queueRefreshIntervalTextFieldActionPerformed(evt);
            }
        });
        queueRefreshIntervalTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                queueRefreshIntervalTextFieldKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                queueRefreshIntervalTextFieldKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                queueRefreshIntervalTextFieldKeyTyped(evt);
            }
        });

        transmitCheckbox.setSelected(true);
        transmitCheckbox.setText("Transmit on");
        transmitCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transmitCheckboxActionPerformed(evt);
            }
        });

        phoneBookEntryCheckBox.setSelected(true);
        phoneBookEntryCheckBox.setText("Use MS RAS dialer");
        phoneBookEntryCheckBox.setToolTipText("Will only work if your running windows.");
        phoneBookEntryCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                phoneBookEntryCheckBoxActionPerformed(evt);
            }
        });

        phoneBookentryTextField.setToolTipText("Name of the phonebook entry in windows\nthat windows RAS dialer will use.\nA phonebook entry must exist in \norder for this to work. ");
        phoneBookentryTextField.setPreferredSize(new java.awt.Dimension(96, 20));

        phoneBookEntryLabel.setText("Phonebook entry name");
        phoneBookEntryLabel.setToolTipText("Name of the phonebook entry in windows\nthat windows RAS dialer will use.\nA phonebook entry must exist in \norder for this to work. ");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(queueRefreshIntervalTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2))
                    .addComponent(transmitCheckbox)
                    .addComponent(phoneBookEntryCheckBox)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(phoneBookentryTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(phoneBookEntryLabel)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(queueRefreshIntervalTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(transmitCheckbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(phoneBookEntryCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(phoneBookentryTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(phoneBookEntryLabel))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        savePreferencesjButton.setText("Save");
        savePreferencesjButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                savePreferencesjButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(savePreferencesjButton, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(savePreferencesjButton)
        );

        jMenu1.setText("File");

        ExitMenuItem.setText("Exit");
        ExitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExitMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(ExitMenuItem);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(FTPSeverPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(QueuePanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(FTPSeverPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addComponent(QueuePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(26, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
/**
 * This method is called when the user clicks on the browse button.
 * it will open the a file chooser
 * @param evt
 */
    private void BrowsejButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BrowsejButtonActionPerformed
    fileChooserFrame.setLocationRelativeTo(null);
        this.fileChooserFrame.setVisible(true);


    }//GEN-LAST:event_BrowsejButtonActionPerformed
/**
 * This method handles events from the file chooser window
 * @param evt
 */
    private void someFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_someFileChooserActionPerformed
        // TODO add your handling code here:
        if(evt.getActionCommand().equals("ApproveSelection")){
            String queuePath = this.someFileChooser.getSelectedFile().getAbsolutePath();
            //Preferences prefs = Preferences.userNodeForPackage (getClass ());
            prefs.put("queuePath", queuePath);
            updateTransmitTextArea();;
            this.fileChooserFrame.setVisible(false);
           
           
        }// end if

        if(evt.getActionCommand().equals("CancelSelection"))
            this.fileChooserFrame.setVisible(false);
        //this.jTextField1.setText(evt.getActionCommand());

    }//GEN-LAST:event_someFileChooserActionPerformed
/**
 * this method is called when the user clicks on the save button.
 * any changes made on the form will be saved when this method is called.
 * @param evt 
 */
    private void savePreferencesjButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_savePreferencesjButtonActionPerformed
       Preferences prefs = Preferences.userNodeForPackage (getClass ());
       prefs.put("serverName", serverTextField.getText());
       prefs.put("uploadPath", pathTextField.getText());
       prefs.put("userName", userNameTextField.getText());
       prefs.put("password", serverPasswordField.getText());
       prefs.putBoolean("transmitCheckbox",transmitCheckbox.isSelected());
       prefs.putBoolean("phoneBookEntryCheckBox",phoneBookEntryCheckBox.isSelected());
       prefs.put("phoneBookentryTextField", phoneBookentryTextField.getText());
       if(!queueRefreshIntervalTextField.getText().equals(prefs.get("queueRefresh", "5")))
       {
                  prefs.put("queueRefresh", queueRefreshIntervalTextField.getText());
                  queueTimer.stop();
                  queueTimer.setDelay((new Integer(prefs.get("queueRefresh","5")).intValue())*60000);
                  //queueTimer.restart();
                  queueTimer.setInitialDelay(1000);
                  queueTimer.restart(); 
       }//end if


    }//GEN-LAST:event_savePreferencesjButtonActionPerformed

    private void serverTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_serverTextFieldActionPerformed

    private void queueRefreshIntervalTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_queueRefreshIntervalTextFieldActionPerformed
        // TODO add your handling code here
       
        //queueRefreshIntervalTextField.setText(queueRefreshIntervalTextField.getText().replaceAll("[^0-9]", ""));
    }//GEN-LAST:event_queueRefreshIntervalTextFieldActionPerformed

    private void queueRefreshIntervalTextFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_queueRefreshIntervalTextFieldKeyTyped
     //if(evt.getKeyChar() != KeyEvent.VK_LEFT || evt.getKeyChar() != KeyEvent.VK_RIGHT || evt.getKeyChar() != KeyEvent.VK_KP_LEFT || evt.getKeyChar() != KeyEvent.VK_KP_RIGHT)        
        //queueRefreshIntervalTextField.setText(queueRefreshIntervalTextField.getText().replaceAll("[^0-9]", ""));

    }//GEN-LAST:event_queueRefreshIntervalTextFieldKeyTyped

    private void queueRefreshIntervalTextFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_queueRefreshIntervalTextFieldKeyPressed
       // if(evt.getKeyChar() != KeyEvent.VK_LEFT || evt.getKeyChar() != KeyEvent.VK_RIGHT || evt.getKeyChar() != KeyEvent.VK_KP_LEFT || evt.getKeyChar() != KeyEvent.VK_KP_RIGHT)       
       // queueRefreshIntervalTextField.setText(queueRefreshIntervalTextField.getText().replaceAll("[^0-9]", ""));
               

    }//GEN-LAST:event_queueRefreshIntervalTextFieldKeyPressed

    private void queueRefreshIntervalTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_queueRefreshIntervalTextFieldKeyReleased
     int keyCode = evt.getKeyCode();
        if(keyCode != 226 && keyCode != 227 && keyCode != 37 && keyCode != 39 && keyCode != 96 && keyCode != 97 && keyCode != 98 && keyCode != 99 && keyCode != 100 && keyCode != 101 && keyCode != 102 && keyCode != 103 && keyCode != 104 && keyCode != 105 && keyCode != 127 && keyCode != 8)
        {
            //System.out.println(keyCode);
        
            queueRefreshIntervalTextField.setText(queueRefreshIntervalTextField.getText().replaceAll("[^0-9]", ""));
     }

    }//GEN-LAST:event_queueRefreshIntervalTextFieldKeyReleased

    private void transmitCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transmitCheckboxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_transmitCheckboxActionPerformed

    private void phoneBookEntryCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_phoneBookEntryCheckBoxActionPerformed
        // TODO add your handling code here:
        phoneBookentryTextField.setEditable(!phoneBookEntryCheckBox.isSelected());
    }//GEN-LAST:event_phoneBookEntryCheckBoxActionPerformed

    private void ExitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExitMenuItemActionPerformed
        
        //this.passWordPromptFrame.set
        ///int h = this.getSize().height;
        //int w = this.getSize().width;
        //int x = this.getLocation().x;
        //int y = this.getLocation().y;
       passWordPromptFrame.setLocation(centerX-136, centerY-93);
       //this.passWordPromptFrame.setSize(300, 300);
      //  passWordPromptFrame.setLocation(x-w/2, y-h/2);
        passWordPromptFrame.setVisible(true);
       // System.exit(0);        // TODO add your handling code here:

    }//GEN-LAST:event_ExitMenuItemActionPerformed
/**
 * This method is called when the user clicks on the cancel button fromthe password dialog
 * @param evt
 */
    private void passWordCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_passWordCancelButtonActionPerformed
        // TODO add your handling code here:
        if(this.jPasswordField1.getText().equals("AOML"))
            System.exit(0);
    }//GEN-LAST:event_passWordCancelButtonActionPerformed

    private void passWordOKButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_passWordOKButtonActionPerformed
this.passWordPromptFrame.setVisible(false);
        // TODO add your handling code here:
    }//GEN-LAST:event_passWordOKButtonActionPerformed


    private void serverPasswordFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverPasswordFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_serverPasswordFieldActionPerformed

    private void jPasswordField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jPasswordField1ActionPerformed
if(evt.getActionCommand().equals("AOML"))
    System.exit(0);

        // TODO add your handling code here:
    }//GEN-LAST:event_jPasswordField1ActionPerformed

    private void pathTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pathTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_pathTextFieldActionPerformed

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        final boolean visible;
       
        if(args.length > 0 && args[0].toLowerCase().trim().equals("false"))
        {
            visible = false;
            

        }
        else
        {
            visible=true;
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                IridiumFTP iFTP = new IridiumFTP();
                
                iFTP.setVisible(visible);
     
                iFTP.setGuiVisible(visible);
                iFTP.prefs.putBoolean("isVisible", visible);
                
            }
        });
    }

    
  
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BrowsejButton;
    private javax.swing.JMenuItem ExitMenuItem;
    private javax.swing.JPanel FTPSeverPanel;
    private javax.swing.JPanel QueuePanel;
    private javax.swing.JScrollPane StatusScrollPane;
    private javax.swing.JFrame fileChooserFrame;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JButton passWordCancelButton;
    private javax.swing.JLabel passWordLabel;
    private javax.swing.JButton passWordOKButton;
    private javax.swing.JFrame passWordPromptFrame;
    private javax.swing.JLabel passwordLabel;
    private javax.swing.JLabel pathLabel;
    private javax.swing.JTextField pathTextField;
    private javax.swing.JCheckBox phoneBookEntryCheckBox;
    private javax.swing.JLabel phoneBookEntryLabel;
    private javax.swing.JTextField phoneBookentryTextField;
    private javax.swing.JTextField queueLocationTextField;
    private javax.swing.JTextField queueRefreshIntervalTextField;
    private javax.swing.JButton savePreferencesjButton;
    private javax.swing.JLabel serverLabel;
    private javax.swing.JPasswordField serverPasswordField;
    private javax.swing.JTextField serverTextField;
    private javax.swing.JFileChooser someFileChooser;
    private javax.swing.JTextArea statusTextArea;
    private javax.swing.JCheckBox transmitCheckbox;
    private javax.swing.JScrollPane transmitScrollPane;
    private javax.swing.JTextArea transmitTextArea;
    private javax.swing.JLabel userNameLabel;
    private javax.swing.JTextField userNameTextField;
    // End of variables declaration//GEN-END:variables


   /**
    * this method is called from the constructor and this is where initialization stuff should be placed.
    */
    private void init(){
    String remoteFile="";
    //System.out.println("version 2.0 compiled 7.23.14");
    
    try{

        
           pWD = new File(System.getProperty("user.home") + File.separatorChar + "iridium_ftp_queue");
           logDIR = new File(System.getProperty("user.home") + File.separatorChar + "iridium_ftp_logs");
           
           if(!pWD.exists())
           {
               pWD.mkdir();
           }//end if
           
           if(!logDIR.exists())
           {
               logDIR.mkdir();
           }//end if


       if(prefs.get("queuePath", "").equals(""))
       {
           prefs.put("queuePath",pWD.getAbsolutePath());
           queueLocationTextField.setText(pWD.getAbsolutePath());
  
       }//end if

        
        


   
   
    //********initialize prefs****************//
    temp = prefs.get("password","@@@");
    if(temp.equals("@@@"))
    {
        prefs.put("password", "password");
    }//end if

        temp = prefs.get("queueRefresh","@@@");
    if(temp.equals("@@@"))
    {
        prefs.put("queueRefresh", "5");
    }//end if
        temp = prefs.get("serverName","@@@");
    if(temp.equals("@@@"))
    {
        prefs.put("serverName", "192.111.123.134");
    }//end iftransmitCheckbox
        temp = prefs.get("uploadPath","@@@");
    if(temp.equals("@@@"))
    {
        prefs.put("uploadPath", "/default/");
    }//end if
        temp = prefs.get("userName","@@@");
    if(temp.equals("@@@"))
    {
        prefs.put("userName", "username");
    }//end if
    
    
     temp = prefs.get("phoneBookentryTextField","@@@");
        if(temp.equals("@@@"))
    {
        prefs.put("phoneBookentryTextField", "Iridium");
    }//end if
    
    
        temp = prefs.get("close","@@@");
    if(temp.equals("@@@"))
    {
        prefs.put("close", "false");
    }//end if
        temp = prefs.get("isVisible","true");
    if(temp.equals("@@@"))
    {
        prefs.put("isVisible", "true");
    }//end if    
        temp = prefs.get("phoneBookEntryCheckBox","@@@");
    if(temp.equals("@@@"))
    {
        prefs.put("phoneBookEntryCheckBox", "false");
    }//end if
        temp = prefs.get("transmitCheckbox","@@@");
    if(temp.equals("@@@"))
    {
        prefs.put("transmitCheckbox", "true");
    }//end if   
    
       temp = prefs.get("logFilePath","@@@");
    if(temp.equals("@@@"))
    {
        prefs.put("logFilePath", logDIR.getAbsolutePath());
    }//end if 
    
    
        temp = prefs.get("host","@@@");
    if(temp.equals("@@@"))
    {
        prefs.put("host", "127.0.0.1");
    }//end if
        temp = prefs.get("port","@@@");
    if(temp.equals("@@@"))
    {
        prefs.put("port", "25000");
    }//end if  
        int num;
        num = prefs.getInt("fileSizeLimit",-2);
        
    if(num == -2)
    {
        prefs.putInt("fileSizeLimit", -1);
    }//end if 
    
    
   
    //**************************************//
    
    
    
      //database
    String dbPath=prefs.get("logFilePath","")+ File.separator;
  
   
//***************check for running instance**************
        comClient = new CommandClient(host,port);
    comClient.start();
    
    if(comClient.isConnected())
    {
        System.out.println("shutting down because an instance of this program is already running");
        System.exit(0);
        
    }//end is connected
    else
    {
        
        comClient.stopThread();
    
    }//end else
    
    //********************************************************
    initSQLLiteDB("jdbc:sqlite:"+dbPath+"transmissions.db");

    
   //System.out.println(remoteFile);
    prefs.putBoolean("isVisible", this.savePreferencesjButton.isVisible());
    serverTextField.setText(prefs.get("serverName","192.111.123.134"));
    pathTextField.setText(prefs.get("uploadPath", "/default/"));
    userNameTextField.setText(prefs.get("userName", "username"));
    serverPasswordField.setText(prefs.get("password", "password"));
    queueRefreshIntervalTextField.setText(prefs.get("queueRefresh","5"));
    transmitCheckbox.setSelected(prefs.getBoolean("transmitCheckbox", true));
    phoneBookEntryCheckBox.setSelected(prefs.getBoolean("phoneBookEntryCheckBox", true));
    phoneBookentryTextField.setEditable(!prefs.getBoolean("phoneBookEntryCheckBox", true));
    phoneBookentryTextField.setText(prefs.get("phoneBookentryTextField", "Iridium"));
    prefs.putBoolean("close",false);
    host = prefs.get("host", "127.0.0.1");
    port = prefs.getInt("port", 25000);
    isVisible=prefs.getBoolean("isVisible", true);
    
    DefaultCaret dc = (DefaultCaret)this.statusTextArea.getCaret();
    dc.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    unsuccessfulLoginAttempts=0;    // this holds the number of failed login attempts
    unsuccessfulServerConnectAttempts=0; //  this holds the number of failed server connects
    

  



    compressFiles();
    updateTransmitTextArea();
    remoteFile = prefs.get("uploadPath", "/default/");
    if(remoteFile.length() > 0 && !remoteFile.endsWith("/"))
    {
        remoteFile+="/";
        prefs.put("uploadPath", remoteFile);
    }// end if 
    resetCounters();
   
    //startTimer();
    startQueueTimer();
    startPreferenceLoaderTimer();

    comClient = new CommandClient(host,port);
    comClient.start();
    
    if(comClient.isConnected())
    {
        System.out.println("shutting down because an instance of this program is already running");
        System.exit(0);
        
    }//end is connected
    else
    {
        
        comClient.stopThread();
    
    }//end else
    
  //*****************************************************************
    
    
    
    messageMan = new MessagingServer(host,port);
    messageThread = new Thread(messageMan);
    messageThread.start();
    //comClient = new CommandClient(host,port);
    //comClient.start();
    pppConnect = new PPPConnectThread();
    rD =  new RasDialer();
    compressFiles();
   
    updateStatusTextArea("AOML Iridium FTPer version 2.1\n");
    //updateStatusTextArea("compiled 09.17.14\n");
    updateStatusTextArea("java vendor " + System.getProperty("java.vendor")+"\n");
    updateStatusTextArea("java version " + System.getProperty("java.version")+"\n");
    updateTransmitTextArea();
    //sendFiles();
  
   
    }// end try
    catch(Exception e){
        String error=e.toString();
                logExceptions(e);
        if(!error.contains("SocketTimeoutException"))
        {
           e.printStackTrace();
        }//end if
        else
        {
            System.out.println("listening for socket");
        
        }//end else
        
        pWD = new File(System.getProperty("user.home") + File.separatorChar + "iridium_ftp_queue");
        if(!pWD.exists())
        {
            pWD.mkdir();  
        }//end if
        queueLocationTextField.setText(pWD.getAbsolutePath());


       
        
    
    }// end catch 
}// end init
    
    
    void initSQLLiteDB(String dbName) {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn
                    = DriverManager.getConnection(dbName);
            Statement stat = conn.createStatement();
            stat.executeUpdate("create table IF NOT EXISTS transmitted (date,transmittedFile);");

            stat.close();
            conn.close();
        } catch (Exception e) {
            logExceptions(e);
            e.printStackTrace();

        }//
    }// end initSQLLiteDB
/**
 * this method lists the files in the queue.
 * before returning the list of files, it calls a compression method that
 * compresses and archives files in a zip file.
 * The method also checks to see if files currently in the queue have already been transmitted
 * by looking at a sqlite database file that keeps a list of transmitted file names.
 * If a file name matches a filename in the database, it will be ignored.
 * @param filePath
 * @return string containing all the file names
 */
    private String fileLister(String filePath){
    compressFiles();
    if (filePath == null)
    {
        return "";
    }//end if
    File folder = new File(filePath);
    File[] listOfFiles = folder.listFiles();
    String fileList="";
    String fileName="";
    
        if (listOfFiles == null)
    {
        return "";
    
    }//end if
    
    for (int i = 0; i < listOfFiles.length; i++) {
        if (listOfFiles[i].isFile()){
            fileName = listOfFiles[i].getName();
            if(!wasTransmitted(fileName)){
                fileList+=fileName+"\n";
            }// end if
            else{
                if(listOfFiles.length > 0){
                    this.updateStatusTextArea(fileName + " will not be transmitted because it was previously sent.\n");
                    try{
                
                        listOfFiles[i].delete();
                    
                    }//end try
                    catch(Exception e)
                    {
                       this.updateStatusTextArea(fileName + " could not be deleted, make sure the current user has\n"); 
                       this.updateStatusTextArea(" permission to delete this file\n"); 
                       this.logExceptions(e);
                    
                    }//end catch
                }//end if

            }// end else
        }// end if

     }// end for
return fileList;
}//end fileLister
    
    
    private void resetCounters(){
        
    dialTime=0;
    internetConnect = 0;
    serverConnect =0;
    loginTime = 0;
    uploadStartTime = 0;
    uploadEndTime = 0;
    disconnectTime = 0;
    fileName = "None";
    fileSize = 0;
    averageTransferRate = -1;
    
    }// end resetCounters()

/**
 * this method returns an array of files currently in the queue
 * before returning the list of files, it calls a compression method that
 * compresses and archives files in a zip file.
 * The method also checks to see if files currently in the queue have already been transmitted
 * by looking at a sqlite database file that keeps a list of transmitted file names.
 * If a file name matches a filename in the database, it will be ignored.
 *
 */
private File[] filesInQueue(String filePath){
    compressFiles();
    int j = 0;
    File folder = new File(filePath);
    File[] listOfFiles = folder.listFiles();
    File[] ff = new File[listOfFiles.length];

    String fileName="";
    long fileSizeLimit=-2;
    long fileLength=0;
    boolean transmitted = false;
    boolean noLimit = false;
    boolean tooLarge = false;
            
    for (int i = 0; i < listOfFiles.length; i++) {
        if (listOfFiles[i].isFile()) {
            fileName = listOfFiles[i].getName();
            fileLength=listOfFiles[i].length();
            fileSizeLimit = prefs.getInt("fileSizeLimit", -1);
            transmitted=wasTransmitted(fileName);
            noLimit = (fileSizeLimit == -1);
            tooLarge = (fileLength > fileSizeLimit);

            if (!transmitted && (!tooLarge || noLimit)) {
                ff[j++] = listOfFiles[i];

            }// end if
            else {
                
                if(transmitted){
                    this.updateStatusTextArea(fileName + " will not be transmitted because it was previously sent.\n");
                }
                
                if(tooLarge){
                   
                    this.updateStatusTextArea(fileName + " will not be transmitted because the file size exceeds " + fileSizeLimit + " bytes\n");
                }                
                
                try {

                    listOfFiles[i].delete();

                }//end try
                catch (Exception e) {
                    this.updateStatusTextArea(fileName + " could not be deleted, make sure the current user has\n");
                    this.updateStatusTextArea(" permission to delete this file\n");
                    this.logExceptions(e);

                }//end catch

            }// end else
            
        }// end if

    }// end for
    File[] f2 = new File[j];
    for (int i = 0 ; i < j ; i++){
        f2[i] = ff[i];
    }// end for
    return f2;
}//end filesInQueue

/**
 * This method checks the sqlite database to see if the file has already been
 * transmitted 
 * @param fileName
 * @return
 */
    boolean wasTransmitted(String fileName) {

        String dbPath = prefs.get("logFilePath", "") + File.separator;
        boolean wasTransmitted = false;
        Connection conn;
        ResultSet rs;
        Statement stat;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath + "transmissions.db");
            stat = conn.createStatement();
            rs = stat.executeQuery("SELECT * FROM transmitted WHERE transmittedFile='" + fileName + "';");

            if (rs.isBeforeFirst()) {
                wasTransmitted = rs.next();
            }// end if
            rs.close();
            conn.close();

        } catch (Exception e) {
            logExceptions(e);
            if (e.getMessage().contains("no such table")) {
                initSQLLiteDB("jdbc:sqlite:" + dbPath + "transmissions.db");
            }//end if
            e.printStackTrace();
            return false;
        }




return wasTransmitted;
}// end wasTransmitted

    /**
     * this method adds a filename to an sqlite database
     *
     * @param fileName
     */
    public void addFile2DB(String fileName) {

        String epochTime = "";
        epochTime = System.currentTimeMillis() + "";
        String dbPath = prefs.get("logFilePath", "") + File.separator;
        Connection conn;
        PreparedStatement ps;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath + "transmissions.db");
            //ps = conn.prepareStatement("INSERT INTO transmitted VALUES ('"+fileName+"');");
            ps = conn.prepareStatement("INSERT INTO transmitted ('transmittedFile','date') VALUES ('" + fileName + "','" + epochTime + "');");
            ps.execute();
            ps.close();
            conn.close();

        } catch (Exception e) {
            logExceptions(e);
            if (e.getMessage().contains("no such table")) {
                initSQLLiteDB("jdbc:sqlite:" + dbPath + "transmissions.db");
            }//end if
            e.printStackTrace();

        }

    }// end addFile2DB


/**
 * this method starts the timer thread that is used to check to see if 
 * there are files ready to transmit
 */
public void startTimer(){
    timer = new Timer((new Integer(prefs.get("queueRefresh","5")).intValue())*60000,this);
    timer.setInitialDelay(2000);
    timer.start();
    

}// end startTimer







public void startQueueTimer(){

    queueTimerActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
           //System.out.println("QueueTimer");
          
            updateTransmitTextArea();
            String fileList = fileLister(prefs.get("queuePath", ""));

            if(!fileList.equals("") && (transmitCheckbox.isSelected()&& !isTransmitting()) &&  !pppConnect.isAlive())
            {
                pppConnect = new PPPConnectThread();
                pppConnect.start();            
            }//end if    

        }//end action performed
  };// end action listener 

    
  
    

    queueTimer = new Timer((new Integer(prefs.get("queueRefresh","5")).intValue())*60000, queueTimerActionListener);
    //queueTimer.setInitialDelay(2000);
    queueTimer.start();
    

}// end startTimer
/**
 * this method starts the timer thread that refreshes the user preferences
 */
    public void startPreferenceLoaderTimer() {

        prefsActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            //System.out.println(evt.getActionCommand());

       //**********************************
                if (prefs.getBoolean("phoneBookEntryCheckBox", true)
                        && rD != null && !rD.isAlive()
                        && u != null && u.isConnected()) {
                    u.closeAllSockets();

                }//endif

      //**********************************     
                try {

                    prefs.flush();
                } catch (Exception e) {
                    logExceptions(e);
                    e.printStackTrace();

                }//end catch

                if (prefs.getBoolean("close", true)) {
                    prefs.putBoolean("close", false);
                    System.exit(0);

                }//end if

                setGuiVisible(prefs.getBoolean("isVisible", false));

                if (!prefs.getBoolean("isVisible", false)) {

                    serverTextField.setText(prefs.get("serverName", "192.111.123.134"));
                    pathTextField.setText(prefs.get("uploadPath", "/default/"));
                    userNameTextField.setText(prefs.get("userName", "username"));
                    serverPasswordField.setText(prefs.get("password", "password"));
                    if (!prefs.get("queueRefresh", "5").trim().equals(queueRefreshIntervalTextField.getText().trim())) {
                        queueRefreshIntervalTextField.setText(prefs.get("queueRefresh", "5"));
                        queueTimer.stop();
                        queueTimer.setDelay((new Integer(prefs.get("queueRefresh", "5")).intValue()) * 60000);
                        queueTimer.setInitialDelay(1000);
                        queueTimer.restart();

                    }//end if
                    transmitCheckbox.setSelected(prefs.getBoolean("transmitCheckbox", true));
                    phoneBookEntryCheckBox.setSelected(prefs.getBoolean("phoneBookEntryCheckBox", true));
                    phoneBookentryTextField.setEditable(!prefs.getBoolean("phoneBookEntryCheckBox", true));
                    phoneBookentryTextField.setText(prefs.get("phoneBookentryTextField", "Iridium"));

                }//end if

            }

        };

        if (rD != null && !rD.isAlive() && u != null) {
            u.ssf.closeAllConnections();

        }//endif

        prefsTimer = new Timer(1000, prefsActionListener);
        //prefsTimer.setInitialDelay(2000);
        prefsTimer.start();

    }// end startTimer



void setGuiVisible(boolean v)
{



    if(isVisible != v)
    {

        this.setVisible(v);
        //10.15.14 removed method so that it could be compiled with java 1.6 
        //this.revalidate();
        this.invalidate();
        this.validate();
        isVisible = v;
        
    }

}



/**
 * The bulk of the logic for transmitting files is probably done in this method
 * this method will attempt to connect to an ftp server
 * and login to the ftp server. if all three are successful then it will
 * attempt to upload a file or restart a previously interrupted upload.
 */

    public void sendFiles() {
        //if(!u.isConnected())
        u = new UploadIt();
        u.setIridiumFTP(this);
        compressFiles();
        File folder = new File(prefs.get("queuePath", ""));
        File uFile;
        //File[] listOfFiles = folder.listFiles();
        File[] listOfFiles = this.filesInQueue(prefs.get("queuePath", ""));
        int good = 0;

        boolean success = false;
        String server, user, password, localFile, remoteFile, badFile = "";
        server = prefs.get("serverName", "");
        user = prefs.get("userName", "");
        password = prefs.get("password", "");
        localFile = prefs.get("queuePath", "");
        remoteFile = prefs.get("uploadPath", "");

        try {
            for (int i = 0; i < listOfFiles.length; i++) {

                if (listOfFiles[i].isFile() && isValidZipFile(listOfFiles[i])) {
                    good++;

                }//end if
                else {
                    badFile = listOfFiles[i].getName();

                }//end else

            }//end for

            if (good > 0) {

                isTransmitting = true;

                if (u.connectToSite(server,5)) {//connect to the server

                    serverConnect = getTime() - internetConnect;
                    //unsuccessfulServerConnectAttempts = 0;
                    updateStatusTextArea("Connected to " + serverTextField.getText() + "\n");
                    if (u.login(user, password)) {//login to the ftp server
                        loginTime = getTime() - internetConnect;
                        u.enterLocalPassiveMode();
                        updateStatusTextArea("Entering passive mode\n");

                        updateStatusTextArea("Login successful\n");

                        if (u.binary()) {//switch to binary mode

                            if (unsuccessfulServerConnectAttempts > 0) { // restore check queue timer 

                                unsuccessfulServerConnectAttempts = 0;
                                queueTimer.stop();
                                queueTimer.setInitialDelay(2000);
                                queueTimer.restart();
                            }

                            updateStatusTextArea("Set to binary mode\n");

                            for (int i = 0; i < listOfFiles.length; i++) {
                                success = false;
                                if (listOfFiles[i].isFile()) {

                                    fileName = listOfFiles[i].getName();
                                    if (isValidZipFile(listOfFiles[i])) {
                                        uFile = new File(localFile + folder.separator + fileName);
                                        fileSize = uFile.length();
                                        updateStatusTextArea("Attempting to upload " + fileName + "\n");
                                        uploadStartTime = getTime();
                                        //success = u.appendFile(uFile,remoteFile+fileName,statusTextArea);
                                        success = u.appendFile(uFile, remoteFile + fileName, this);
                                        if (success) {
                                            uploadEndTime = getTime();
                                            averageTransferRate = 8000 * fileSize / (uploadEndTime - uploadStartTime);
                                            updateStatusTextArea(fileName + " successfully uploaded\n");
                                            addFile2DB(fileName);

                                            try {

                                                uFile.delete();

                                            }//end try
                                            catch (Exception e) {
                                                this.updateStatusTextArea(fileName + " could not be deleted, make sure the current user has\n");
                                                this.updateStatusTextArea(" permission to delete this file\n");
                                                this.logExceptions(e);

                                            }//end catch
                                            updateTransmitTextArea();
                                        }// end where file upload verification happens if   
                                        else {
                                            uploadEndTime = uploadStartTime;
                                            averageTransferRate = -1;
                                            updateStatusTextArea(folder.separator + fileName + " not sent\n");
                                        }// end else
                                        //uploadEndTime = getTime();

                                    }//end if where zip is checked.
                                    else {
                                        updateStatusTextArea(fileName + " failed crc check, skipping\n");

                                    }//end else

                                }// end list of files if

                                disconnectTime = getTime() - internetConnect;
                                logText(connectionHeader, "," + serverConnect + "," + loginTime + "," + (uploadEndTime - uploadStartTime) + "," + fileSize + "," + averageTransferRate + "," + disconnectTime + "," + fileName + "\n", "connectionLog.csv");
                                //System.out.println("DialTime = " +dialTime+ " Internet Connect Time = " +internetConnect+ " FTP server Connect Time = " +serverConnect+ " File Name = " + fileName + " Login Time = "+loginTime+" Upload Start Time = "+uploadStartTime+" Upload End Time = "+uploadEndTime+" Disconnect Time = "+disconnectTime);

                            }// end for

                        }// end binary mode if
                        else {
                            updateStatusTextArea("Could not set to binary mode\n");
                        }// end else

                    }//end login 
                    else {

                        unsuccessfulServerConnectAttempts++;
                        updateStatusTextArea("Could not log in\n");
                        if (unsuccessfulServerConnectAttempts >= 5) {
                            delaySendTimer();
                        }

                    }// end else

                }// end if where connect to site
                else {
                    unsuccessfulServerConnectAttempts++;
                    updateStatusTextArea("Could not connect to " + serverTextField.getText() + "\n");
                    if (unsuccessfulServerConnectAttempts >= 5) {
                        delaySendTimer();
                    }//end if

                }// end else

                u.closeConnection();

                isTransmitting = false;

            }//end if wherer is good 
            else {
                if (listOfFiles.length > 0) {
                    updateStatusTextArea(badFile + " is not a valid zip file\n");
                }//end if

            }//end else
        }// end try
        catch (Exception e) {
            logExceptions(e);
            e.printStackTrace();

            try {
                u.closeConnection();
            } catch (Exception e1) {
                logExceptions(e1);
                e1.printStackTrace();

            }//end catch

            isTransmitting = false;
            unsuccessfulServerConnectAttempts++;
            updateStatusTextArea("An exception occurred while trying\n");
            updateStatusTextArea("to establish a connection\n");            
            if (unsuccessfulServerConnectAttempts >= 5) {
                delaySendTimer();
            }//end if

        }// end catch
//logText(dialTime+","+internetConnect+","+serverConnect+","+loginTime+","+(uploadStartTime-uploadEndTime)+","+disconnectTime+","+fileName+"\n","connectionLog.csv");

    }// end sendFile
    
    
    void delaySendTimer() {
        queueTimer.stop();
        queueTimer.setInitialDelay(86400000);
        queueTimer.restart();
        updateStatusTextArea("There have been " + unsuccessfulServerConnectAttempts + " failed\n");
        updateStatusTextArea("attempts to connect to the server\nnext attempt will be in 24h\n");

    }//end delyaSendTimer    

/**
 * This is the event handler for the timer thread. every time the timer is up,
 * this method is called.
 * This method updates the files listed and starts the transmission process
 * @param e
 */

public void actionPerformed(ActionEvent e) {
       
 /*

    updateTransmitTextArea();
    String fileList = fileLister(prefs.get("queuePath", ""));

    if(!fileList.equals("") && (transmitCheckbox.isSelected()&& !isTransmitting()) &&  !pppConnect.isAlive()){
        pppConnect = new PPPConnectThread();
        pppConnect.start();




  
    }

*/

    }// end actionPerformed





/**
 * Updates the list of files to transmit
 */
public void updateTransmitTextArea(){

    String lofs =fileLister(queueLocationTextField.getText());
    

    transmitTextArea.setText("");
    transmitTextArea.append(lofs);
    transmitTextArea.setCaretPosition(0);
    sendMessageOnSocket("<QUEUE>");
    
    queueLocationTextField.setText(prefs.get("queuePath",""));
    if(lofs.trim().equals(""))
    {
        lofs=";EMPTY;";
        sendMessageOnSocket("<FILES>"+lofs.replace("\n","").trim()+"</FILES>");
    }
    else
    {
        String[] files = lofs.split("\n");
        for (int i = 0 ; i < files.length; i++)
        {
            sendMessageOnSocket("<FILE>"+files[i].trim()+"</FILE>");
        
        }// end for
    
    }//end else
    sendMessageOnSocket("</QUEUE>");
        
    
}// end

/**
 * Updates the status of the upload/attempt
 * 
 * @param status
 */
public void updateStatusTextArea(String status){
    logText(status,"log.txt");
    sendMessageOnSocket("<FTPSTATUS>"+status.replace("\n","").trim()+"</FTPSTATUS>");
    int lineCount = statusTextArea.getLineCount();

    //lineCount-=1;
    if (lineCount<0 )
        lineCount = 0;
    statusTextArea.append(status);
    statusTextArea.revalidate();
    
    



}// updateStatusTextArea

/**
 * returns weather or not there is a transmission currently happening
 * @return
 */

public boolean isTransmitting(){

    return isTransmitting;

}// end isTransmitting

/**
 * This method is called to attempt a ppp connection using the windows RASDialer
 */

public class PPPConnectThread extends Thread{
    @Override
    public void run(){
        boolean rasIsAlive = false;
        resetCounters(); // reset time counters
        dialTime=getTime(); // get dial time
        if(!phoneBookEntryCheckBox.isSelected())
            rasIsAlive= false;
        else
            rasIsAlive = rD.isAlive();
    
        try{

            if(!rasIsAlive && phoneBookEntryCheckBox.isSelected()&& !phoneBookentryTextField.getText().equals("")){

                updateStatusTextArea("Dialing phonebook entry " + phoneBookentryTextField.getText()+"\n\n");
                
                if(rD.openConnection(phoneBookentryTextField.getText())){
                    internetConnect = getTime();
                    updateStatusTextArea("PPP connection Successful\n\n");

                        sendFiles();
                                if(rD.isAlive()){
                                    rD.closeAllConnections();
                                    //rD.closeConnection(phoneBookentryTextField.getText());
                                    disconnectTime = getTime()-internetConnect;
                                    updateStatusTextArea("Connection to "+phoneBookentryTextField.getText()+" is now closed\n\n");
                                }//end if      .replace("\n", "")      
                                else{                                    
                                    disconnectTime = getTime()-internetConnect;
                                    updateStatusTextArea("Connection to "+phoneBookentryTextField.getText()+" was already closed\n\n");
                                }// end else
                }// end if


            }// end if

        }// rnd try
        catch(RasDialerException e){
            logExceptions(e);
            if(e.getMessage().contains("The port is already in use or is not configured for Remote Access dialout.")){
                queueTimer.stop();
                queueTimer.setInitialDelay(1000);
                queueTimer.restart();

            }// end if
            updateStatusTextArea(e.getMessage()+"\n");;
        }// end catch
        if(transmitCheckbox.isSelected() && !phoneBookEntryCheckBox.isSelected()){
            //if(connectToServer()){
            internetConnect = getTime();
            sendFiles();
            disconnectTime = getTime()-internetConnect;
            //}
        }// if

logText(connectionHeader,",-1,-1,-1,-1,-1,"+disconnectTime+",NONE\n","connectionLog.csv");


    }// end run

}// end PPPConnectThread


/**
 * When called strings passed to it are appended to a file
 * @param line
 * @param logFileName
 */
    public void logText(String line, String logFileName) {
        


        logFileName = prefs.get("logFilePath", "") + File.separator + logFileName;

        try {
            line = line.replaceAll("\n", "");
            line = getDate() + " : " + line + NL;
            FileWriter logFile = new FileWriter(logFileName, true);
            logFile.append(line);
            logFile.close();
        } catch (Exception e) {

        }// end catch
    }// end logText


/**
 * When called strings passed to it are appended to a file
 * @param line
 * @param logFileName
 */
public void logText(String header,String line, String logFileName){

    logFileName=prefs.get("logFilePath","")+ File.separator + logFileName;
    try{
        File f;
        header = header.replaceAll("\n", "");
        header = header + NL;
        
        line = line.replaceAll("\n", "");
        line = getDate() +" "+ line + NL;
        f = new File(logFileName);


        if (f.exists()){
            FileWriter logFile = new FileWriter(logFileName,true);
            logFile.append(line);
            logFile.close();
        }//end if
        else{
            FileWriter logFile = new FileWriter(logFileName,true);
            logFile.append(header);
            logFile.append(line);
            logFile.close();
        }// end else

       
    }
    catch(Exception e){
                
    }// end catch
}// end logText


    void logExceptions(Exception e) {
        logText("--------------------ERROR-----------------------", "exceptions.txt");
        logText(e.toString(), "exceptions.txt");

    }//end logExceptions

/**
 * returns the current GMT date
 * @return
 */
    public String getDate() {

        Date currentDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy hh:mm:ss z");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(currentDate);

    }/// end getDate
/**
 * returns the current time
 * @return
 */
public long getTime(){
    return new Date().getTime();
}// end getTime



/**
 * this method zips files that are in the queue that don't already have the zip extension.
 * after compressing, it deletes the original file
 * the method does not verify that a file with a zip extension is
 * really a zip file.
 */
    public void compressFiles() {

        IridiumZipper iz = new IridiumZipper();
        File folder = new File(prefs.get("queuePath", pWD.getAbsolutePath()));
        File[] listOfFiles = folder.listFiles();
        String fns[];
        if (listOfFiles != null && listOfFiles.length > 0) {
            for (int i = 0; i < listOfFiles.length; i++) {
                fns = listOfFiles[i].getName().split("\\.");
                if (fns.length > 1 && !fns[1].toLowerCase().equals("zip") && listOfFiles[i].isFile()) {
                    iz = new IridiumZipper();
                    if (iz.compress(listOfFiles[i])) {
                        try {

                            listOfFiles[i].delete();

                        }//end try
                        catch (Exception e) {
                            this.updateStatusTextArea(fileName + " could not be deleted, make sure the current user has\n");
                            this.updateStatusTextArea(" permission to delete this file\n");
                            this.logExceptions(e);

                        }//end catch
                    }
                }// end if

            }//end for

        }//end if

    }//compress the files


 static boolean isValidZipFile(final File file) {
    ZipFile zipfile = null;
    try {
        zipfile = new ZipFile(file);
        return true;
    } catch (ZipException e) {

        return false;
    } catch (IOException e) {
        return false;
    } finally {
        try {
            if (zipfile != null) {
                zipfile.close();
                zipfile = null;
            }
        } catch (IOException e) {
        }
    }
}
void setDirExists(boolean b){
    dirExists = b;
}

boolean getDirExists(){
    return dirExists;
}


void sendMessageOnSocket(String msg)
{
    
  if(messageMan != null)
  {
      messageMan.sendToAllClients(msg+"\r");
  }//end if
}
}// end class
