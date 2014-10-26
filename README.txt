*************************************************************************
*                                                                       *
* IridiumFTP, an auto dialing FTP program for dialup connections.       *
* Copyright (C) 2014  Pedro Pena                                        *
*                                                                       *
* This program is free software: you can redistribute it and/or modify  *
* it under the terms of the GNU General Public License as published by  *
* the Free Software Foundation, either version 3 of the License, or     *
* any later version.                                                    *
*                                                                       * 
* This program is distributed in the hope that it will be useful,       *
* but WITHOUT ANY WARRANTY; without even the implied warranty of        *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
* GNU General Public License for more details.                          *
*                                                                       *
* You should have received a copy of the GNU General Public License     *
* along with this program.  If not, see <http://www.gnu.org/licenses/>. *
*                                                                       *                                         
*************************************************************************



This is an FTP program that can automatically upload files from a specified queue
to an FTP server. The program can be configured to check the queue every X minutes.
It was originally written to upload data from ships via an Iridium satellite
connection but can be used to upload data over an established network connection.
When using it under Microsoft Windows, it can dial out using the Windows rasdialer,
so long as there is a phone book entry.

This program was originally written to run under Windows using the built in rasdial.exe.
rasdial.exe is called externally.

Since it is written in java, it works fine under linux except for the dialing out part.
To work around this, a simple wrapper script was written around pppd to mimic rasdialer.exe
This script is good enough to fool the program into thinking it's using the windows rasdialer.

Under Windows, a phonebook entry must exist and be configured to establish a dialup
connection.

Under linux, pppd and chat must be installed and a chat script must exist
that can be used to establish a dialup connection.

-----------------------------------------
**COMPILING AND RUNNING THE FTP PROGRAM**
-----------------------------------------

To compile, make sure that a java sdk version of at least 1.7 is installed
and that the jar archive tool is installed.

You can check by typing "javac -version"
you should see something like "javac 1.7.0_65"

Type "jar" and your screen should scroll with many jar options.

When you list the contents of the directory, you should see the following.

lib             - directory containing libraries  
makeit.bat      - batch script to compile and archive the program 
makeit.sh       - bash  script to compile and archive the program 
manifest.txt    - info that will be added to the manifest in the resulting jar file
ras_wrapper     - directory containing wrapper for rasdialer.exe
README.txt      - this document
src             - directory with source files.
src/client.java -connects to the ftp program via a socket and prints messages sent

In Windows run makeit.bat in linux/unix run makeit.sh .
In linux/unix you will have to make makeit.sh executable.
To make it executable type

"chmod +x makeit.sh"

When done compiling, the ftp program will be placed in the dist directory.
To run the program enter the dist directory and type.

"java -jar IridiumFTP.jar "

If for some reason the script doesn't work you can compile and archive with the following commands.

For linux/unix
"javac -source 1.7 -target 1.7 -d ./ -cp ./lib/commons-net-3.3.jar:./lib/sqlitejdbc-v056.jar src/iridiumftp/*.java"
"jar cfm IridiumFTP.jar manifest.txt iridiumftp/*.class"


For Windows
"javac -source 1.7 -target 1.7 -d .\ -cp .\lib\commons-net-3.3.jar;.\lib\sqlitejdbc-v056.jar .\src\iridiumftp\*.java"   
"jar cfm IridiumFTP.jar manifest.txt iridiumftp\*.class"

Keep the IridiumFTP.jar file and the lib folder together.
You can create a directory like "dist" and copy IridiumFTP.jar and the lib folder into it.

-------------------
**INSTALLING PPPD**
-------------------
These instructions work under debian.

to install
"sudo apt-get install ppp"

in order to use pppd the user must be part of the dip group
too add to group dip

"sudo usermod -a -G dip USERNAME"

in some cases it is necessary for a user to run pppd with root privileges.
DO THIS ONLY IF NECESSARY, it is highly recommended against.
"chmod u+s /usr/sbin/pppd"


to compile client.java type
"javac client.java"

to run the resulting .class file
"java client 127.0.0.1 25000"

127.0.0.1 is the default ip address and 25000 is the default port

Make sure the ftp program is already up and running or client.class will timeout and close.

---------------------------------------------------------
**INSTALLING WRAPPER FOR RASDIAL (ONLY FOR NON WINDOWS)**
---------------------------------------------------------

The wrapper files for rasdial are under ras_wrapper/
here is a list of what's in there.

aspergarria - a test script I made to login to my server
cmd         - a script to mimic a single case of windows cmd.exe
install.sh  - the rasdial wrapper install script 
iridium     - a chat script for logging into the iridium dialup gateway
options     - option file for pppd
ppp-off     - bash script that closes a connection on ppp0
ppp-on      - bash script to start pppd(this should actually not be there,
                  it will be created when running install.sh) 
rasdial     - wrapper that mimics  certain aspects of rasdial.exe 
start_chat  - bash script that starts the chat program(this should actually not be there,
                  it will be created when running install.sh) 


For this wrapper to work it must be installed somewhere in the path.
This script must be run as sudo unless the -o and -l are supplied with paths
for which the user has rw privileges.

you can get the install options by typing 
"./install.sh -h"


To run without any options type
"sudo ./install.sh"

By default the bash scripts will be copied to 
"/usr/local/sbin"
The chat scripts will be copied to
"/usr/local/chat-scripts"
The pppd options file will be copied to 
"/etc/ppp"
The serial port will be set to
"/dev/ttyUSB0"
The baudrate will be set to.
"19200"

Example with options

"sudo ./install.sh -l /etxe/kepa -p /dev/ttyS7 -b 9600 -o /etxe/kepa/temp"


-----------------------------
**MAKING SURE RASDIAL WORKS**
-----------------------------

Modify the aspergarria chat script for your specific needs.
include a username and password if required.

"rasdial aspergarria"

you shoud see stuff on the screen and activity on your modem.

If you're using an iridium modem then the included script should work.
Modify it if you have to.

"rasdial iridium"



















