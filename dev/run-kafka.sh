#!/bin/bash

# Copyright 2023 zoukang, All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# 作用：启动一个开发阶段用于测试的kafka

if [ ! -d .tmp ]; then
    mkdir .tmp
fi

if [ ! -d .tmp/kafka ]; then
    if [ ! -f .tmp/kafka_2.13-3.5.2.tgz  ]; then
        echo "Going to download kafka, please wait!"
        if ! wget https://mirror.tuna.tsinghua.edu.cn/apache/kafka/3.5.1/kafka_2.13-3.5.2.tgz --output-document .tmp/kafka_2.13-3.5.2.tgz; then
              echo "Download kafka failed, please try again later!"
              exit 1
        fi
    fi
    tar -zxvf .tmp/kafka_2.13-3.5.2.tgz -C .tmp/
    mv .tmp/kafka_2.13-3.5.1 .tmp/kafka

    cd .tmp/kafka || exit 1

    cp config/server.properties config/server.properties.backup
    cp config/zookeeper.properties config/zookeeper.properties.backup

    sed -i 's/^log.dirs=.*$/log.dirs=data-kafka/g' config/server.properties
    sed -i 's/^log.dirs=.*$/log.dirs=data-kafka-kraft/g' config/kraft/server.properties
    sed -i 's/^dataDir=.*$/dataDir=data-zookeeper/g' config/zookeeper.properties

    mkdir data-kafka
    mkdir data-kafka-kraft
    mkdir data-zookeeper
    cd ../..
fi

kafka_mode=kraft
if [ $# -le 1 ]; then
    kafka_mode="$1"
    shift
fi

cd .tmp/kafka || exit 1

if [ "${kafka_mode}" = 'zk' ]; then
    gnome-terminal --working-directory "$(pwd)" --title zookeeper -- bin/zookeeper-server-start.sh config/zookeeper.properties
    sleep 2
    gnome-terminal --working-directory "$(pwd)" --title kafka -- bin/kafka-server-start.sh config/server.properties
else
    if [ ! -f "data-kafka-kraft/meta.properties" ]; then
        bin/kafka-storage.sh format -t "$(bin/kafka-storage.sh random-uuid)" -c config/kraft/server.properties
    fi
    gnome-terminal --working-directory "$(pwd)" --title kafka -- bin/kafka-server-start.sh config/kraft/server.properties
fi
