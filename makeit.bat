echo off

javac -source 1.7 -target 1.7 -d .\ -cp .\lib\commons-net-3.3.jar;.\lib\sqlitejdbc-v056.jar .\src\iridiumftp\*.java   
jar cfm IridiumFTP.jar manifest.txt iridiumftp\*.class

IF EXIST .\dist goto deletedist

:deletedist
del /q /s .\dist  > nul
rmdir /q /s .\dist  > nul
:exit

mkdir .\dist
mkdir .\dist\lib
move /y IridiumFTP.jar .\dist > nul
copy /y .\lib .\dist\lib > nul
del /s /q .\iridiumftp  > nul
rmdir /s /q .\iridiumftp  > nul


