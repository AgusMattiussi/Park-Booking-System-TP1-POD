#!/bin/bash

mvn clean install

mkdir tmp

cp server/target/tpe1-g6-server-2023.2Q-bin.tar.gz ./tmp/tpe1-g6-server-2023.2Q-bin.tar.gz
cp client/target/tpe1-g6-client-2023.2Q-bin.tar.gz ./tmp/tpe1-g6-client-2023.2Q-bin.tar.gz

cd tmp

tar -xzf tpe1-g6-server-2023.2Q-bin.tar.gz
tar -xzf tpe1-g6-client-2023.2Q-bin.tar.gz

chmod +x tpe1-g6-server-2023.2Q/*.sh
sed -i -e 's/\r$//' tpe1-g6-server-2023.2Q/*.sh
rm tpe1-g6-server-2023.2Q-bin.tar.gz

chmod +x tpe1-g6-client-2023.2Q/*.sh
sed -i -e 's/\r$//' tpe1-g6-client-2023.2Q/*.sh
rm tpe1-g6-client-2023.2Q-bin.tar.gz

