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

function usage() {
    echo "This a reactor-gateway docker image, you can run this image as dashboard or gateway or access-log-consumer."
    echo "Usage: docker run -it reactor-gateway:<version> [help | dashboard | gateway | access-log-consumer] [params ... ]"
    echo "    help                   : print this simple help message"
    echo "    dashboard              : run this image as dashboard"
    echo "    gateway                : run this image as gateway"
    echo "    access-log-consumer    : run this image as access-log-consumer"
}

if [ $# -lt 1 ]; then
    usage
fi

component="$1"
shift

cd /opt/reactor-gateway/ || exit 1

case "${component}" in
    "help")
        usage
        exit 0
        ;;
    "dashboard")
        exec ./bin/rgw-dashboard.sh "$@"
        ;;
    "gateway")
        exec ./bin/rgw-gateway.sh "$@"
        ;;
    "access-log-consumer")
        exec ./bin/rgw-access-log-consumer.sh "$@"
        ;;
    *)
        usage
        exit 1
        ;;
esac
