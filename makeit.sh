#!/bin/bash
javac -source 1.7 -target 1.7 -d ./ -cp ./lib/commons-net-3.3.jar:./lib/sqlitejdbc-v056.jar src/iridiumftp/*.java 
jar cfm IridiumFTP.jar manifest.txt iridiumftp/*.class 
if [ -d "dist" ]; then
    rm -r dist
fi
mkdir ./dist
rm -r ./iridiumftp
mv -t ./dist ./IridiumFTP.jar
cp -r ./lib ./dist


