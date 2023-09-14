#!/bin/zsh

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

# ----- 统计项目代码行数 -----

SCRIPT_DIR=$(dirname "$0")
REPO_HOME=$(cd -P "$SCRIPT_DIR"/.. || exit 1;pwd)

cd "${REPO_HOME}" || exit 1

exclude_exp=(\
'-not' '-path' './**/build/*' \
'-not' '-path' './.git/*' \
'-not' '-path' './.idea/*' \
'-not' '-path' './.gradle/*' \
'-not' '-path' './dashboard/src/main/resources/rgw-builtin-plugins.json' \
'-not' '-path' './release/static/*' \
'-not' '-path' './reactor-gateway-ui/.git/*' \
'-not' '-path' './reactor-gateway-ui/.vscode/*' \
'-not' '-path' './reactor-gateway-ui/.gradle/*' \
'-not' '-path' './reactor-gateway-ui/node_modules/*' \
'-not' '-path' './reactor-gateway-ui/build/*' \
'-not' '-path' './reactor-gateway-ui/dist/*' \
)

function count_code_line() {
    ext="$1"
    find . -type f -name "*.${ext}" "${exclude_exp[@]}" -print0 | xargs --null cat | wc -l
}

java_code_lines=$(count_code_line 'java')
shell_code_lines=$(count_code_line 'sh')
gradle_code_lines=$(count_code_line 'gradle')
json_code_lines=$(count_code_line 'json')
xml_code_lines=$(count_code_line 'xml')
js_code_lines=$(count_code_line 'js')
vue_code_lines=$(count_code_line 'vue')
css_code_lines=$(count_code_line 'css')

total_lines=$((java_code_lines + shell_code_lines + gradle_code_lines + json_code_lines + xml_code_lines + js_code_lines + vue_code_lines + css_code_lines))

echo -e "语言 行数\n" \
"-------- --------\n" \
"Java ${java_code_lines} \n" \
"Shell ${shell_code_lines} \n" \
"Gradle ${gradle_code_lines} \n" \
"Json ${json_code_lines} \n" \
"Xml ${xml_code_lines} \n" \
"Javascript ${js_code_lines} \n" \
"Vue ${vue_code_lines} \n" \
"Css ${css_code_lines} \n" \
"-------- --------\n" \
"\n总计 ${total_lines} \n" | column -t
