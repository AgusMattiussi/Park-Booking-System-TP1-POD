#!/bin/bash

target_dir="server/target"
client_dir="client/target"
temp_dir="tmp"

mvn clean install

mkdir -p "$temp_dir"

cp "$target_dir/tpe1-g6-server-2023.2Q-bin.tar.gz" "$temp_dir/"
cp "$client_dir/tpe1-g6-client-2023.2Q-bin.tar.gz" "$temp_dir/"
cd "$temp_dir"

# Server
tar -xzf "tpe1-g6-server-2023.2Q-bin.tar.gz"
chmod +x tpe1-g6-server-2023.2Q/*.sh
sed -i -e 's/\r$//' tpe1-g6-server-2023.2Q/*.sh
rm "tpe1-g6-server-2023.2Q-bin.tar.gz"

# Client
tar -xzf "tpe1-g6-client-2023.2Q-bin.tar.gz"
chmod +x tpe1-g6-client-2023.2Q/*.sh
sed -i -e 's/\r$//' tpe1-g6-client-2023.2Q/*.sh
rm "tpe1-g6-client-2023.2Q-bin.tar.gz"

