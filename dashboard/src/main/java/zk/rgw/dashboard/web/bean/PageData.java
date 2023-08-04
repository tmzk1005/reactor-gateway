/*
 * Copyright 2023 zoukang, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zk.rgw.dashboard.web.bean;

import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PageData<T> extends Page {

    private List<T> data;

    private long total;

    public PageData(List<T> data, long total, int pageNum, int pageSize) {
        this.data = data;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    @JsonProperty
    public long getTotalPages() {
        long result = total / pageSize;
        long remainder = total % pageSize;
        if (remainder > 0) {
            ++result;
        }
        return result;
    }

    public <V> PageData<V> map(Function<T, V> function) {
        return new PageData<>(data.stream().map(function).toList(), total, pageNum, pageSize);
    }

    public static <E> PageData<E> empty(int pageSize) {
        PageData<E> pageData = new PageData<>();
        pageData.pageSize = pageSize;
        pageData.pageNum = 1;
        pageData.total = 0;
        pageData.data = List.of();
        return pageData;
    }

}