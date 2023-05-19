#!/usr/bin/env bash

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

BIN_DIR=$(dirname "$0")
RGW_HOME=$(cd -P "$BIN_DIR"/.. || exit 1;pwd)
export RGW_HOME

cd "$RGW_HOME" || exit 1

JAVA_CMD=$(command -v java)

if [[ ! -x $JAVA_CMD ]] ; then
    echo "No executable java command found!" >&2
    exit 1
fi

JAVA_VERSION=$(${JAVA_CMD} -version 2>&1 | head -n 1 | awk '{print $3}' | sed -e 's/^"//' -e 's/"$//' | awk -F . '{print $1}')

if [[ $JAVA_VERSION -lt 17 ]]; then
    echo "Current java version is $JAVA_VERSION, please use 17 or greater."
    exit 1
fi

CLASSPATH_FILE=$1
MAIN_CLASS=$2
shift 2

if [[ ! $CLASSPATH_FILE ]]; then
    echo "No classpath file specified!"
    exit 1
fi

if [[ ! $MAIN_CLASS ]]; then
    echo "No main class name specified!"
    exit 1
fi

unset CLASSPATH
CLASSPATH=""

CLASSPATH_FILE="$RGW_HOME/conf/.$CLASSPATH_FILE"
readarray -t jars < "$CLASSPATH_FILE"

for jar in "${jars[@]}"
do
    CLASSPATH="${CLASSPATH}libs/${jar}:"
done

ALL_OPTS=()

# read jvm options from config file if exist
JVM_OPT_CONF_FILE="$RGW_HOME/conf/jvmoptions"
if [[ -f $JVM_OPT_CONF_FILE ]] ; then
    lines=$(sed -e "s/^\s*//" -e '/^#/d' -e 's/\s*$//' -e '/^$/d' "$JVM_OPT_CONF_FILE")
    while read -r line
    do
        ALL_OPTS+=("$line")
    done <<< "$lines"
fi

# read jvm options from system environment if exist
if [[ ${JVM_OPTS} ]]; then
    for item in ${JVM_OPTS}; do
        ALL_OPTS+=("${item}")
    done
fi

ALL_OPTS+=("-Dfile.encoding=UTF-8")
ALL_OPTS+=("-Duser.timezone=GMT+08")

exec "${JAVA_CMD}" -cp "${CLASSPATH}" "${ALL_OPTS[@]}" "${MAIN_CLASS}" "$@"
