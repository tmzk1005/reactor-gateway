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

# 作用：启动一个开发阶段用于测试的mongodb

if [ ! -d .tmp ]; then
    mkdir .tmp
fi

if [ ! -d .tmp/mongodb ]; then
    # 只支持较新版本的ubuntu,其他的linux如果缺少同版本的so库的话可能运行失败
    if [ ! -f .tmp/mongodb-linux-x86_64-ubuntu2204-6.0.5.tgz ]; then
        echo "Going to download mongodb installer, please wait!"
        wget https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-ubuntu2204-6.0.5.tgz --output-document .tmp/mongodb-linux-x86_64-ubuntu2204-6.0.5.tgz
        if [ $? != 0 ]; then
            echo "Download mongodb failed, please try again later!"
            exit 1
        fi
    fi
    tar -zxvf .tmp/mongodb-linux-x86_64-ubuntu2204-6.0.5.tgz -C .tmp/
    mv .tmp/mongodb-linux-x86_64-ubuntu2204-6.0.5 .tmp/mongodb
fi

cd .tmp/mongodb || exit 1

if [ ! -d data ]; then
    mkdir data
fi

gnome-terminal --working-directory "$(pwd)" --title mongodb -- bin/mongod --dbpath ./data
