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

set -eo pipefail

kafka_data_path=/data/kraft-combined-logs

if [ ! -d "${kafka_data_path}" ]; then
    mkdir -p "${kafka_data_path}"
fi

cd /opt/kafka || exit 1

if [ ! -f config/kraft/server.properties.default ]; then
    cp config/kraft/server.properties config/kraft/server.properties.default
    sed -i 's#^log.dirs=.*$#log.dirs=/data/kraft-combined-logs#g' config/kraft/server.properties
fi

if [ ! -f "${kafka_data_path}/meta.properties" ]; then
    bin/kafka-storage.sh format -t "$(bin/kafka-storage.sh random-uuid)" -c config/kraft/server.properties
fi

exec bin/kafka-server-start.sh config/kraft/server.properties "$@"
