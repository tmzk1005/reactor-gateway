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

image_name="reactor-gateway"
image_tag="$1"
image="${image_name}:${image_tag}"

echo -e "Start building docker image ${image}\n"

if docker build --file 'Dockerfile' "--build-arg" "rgw_version=${image_tag}" -t "${image}" . ; then
    echo -e "\033[1;32;40mBuild docker image ${image} succeed.\033[0m"
else
    echo -e "\033[5;31;40mBuild docker image ${image} failed.\033[0m"
    exist 1
fi
