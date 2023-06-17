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

# 作用：启动mongodb shell

command -v mongosh > /dev/null 2>&1
if [ $? -eq 0 ]; then
    exec mongosh "$@"
    exit 0
fi

echo "Command mongosh not found!"

if [ $(lsb_release -d  | grep Ubuntu | wc -l) -eq "0" ]; then
    echo "此命令暂时只支持在Ubuntu下运行!"
    exit 1
else
    echo "Try to install mongoshell ..."
    if [ ! -f .tmp/mongodb-mongosh_1.10.0_amd64.deb ]; then
        wget https://downloads.mongodb.com/compass/mongodb-mongosh_1.10.0_amd64.deb --output-file .tmp/mongodb-mongosh_1.10.0_amd64.deb
    fi
    sudo dpkg -i .tmp/mongodb-mongosh_1.10.0_amd64.deb
    exec mongosh "$@"
fi

