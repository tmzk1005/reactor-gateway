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

TMP_DIR=$(dirname "$0")
TMP_DIR=$(cd -P "$TMP_DIR" || exit 1;pwd)

clean_dir() {
    dir_name=$1
    if [ -d "${dir_name}" ]; then
        echo "clean directory: ${dir_name}"
        rm -rf "./${dir_name:?}"
        mkdir -p "./${dir_name:?}"
    fi
}


clean_kafka() {
    clean_dir .tmp/kafka/data-kafka
    clean_dir .tmp/kafka/data-kafka-kraft
    clean_dir .tmp/kafka/data-zookeeper
    clean_dir .tmp/kafka/logs
}

clean_mongodb() {
    clean_dir .tmp/mongodb/data
}

if [ "$1" = 'm' ]; then
    clean_mongodb
elif [ "$1" = 'k' ]; then
    clean_kafka
else
    echo "Usage: $0 <m|k>"
fi
