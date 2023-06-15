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

package zk.rgw.dashboard.framework.validate;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import zk.rgw.common.exception.RgwRuntimeException;
import zk.rgw.common.util.ObjectUtil;

public class ParameterValidator {

    private static final Map<String, java.util.regex.Pattern> CACHED_REGEXES = new ConcurrentHashMap<>();

    private final List<String> errorMessages = new ArrayList<>(1);

    public void validate(Parameter[] parameters, Object[] values) {
        errorMessages.clear();
        if (parameters.length == 0) {
            return;
        }
        for (int i = 0; i < parameters.length; ++i) {
            validateParameter(parameters[i], values[i]);
        }
    }

    public boolean hasError() {
        return !errorMessages.isEmpty();
    }

    public String getErrorMessage() {
        return String.join("; ", errorMessages);
    }

    private void validateParameter(Parameter parameter, Object value) {
        if (parameter.getAnnotations().length > 0) {
            if (parameter.getType().isArray()) {
                for (int i = 0; i < Array.getLength(value); ++i) {
                    validateAnnotatedElement(parameter, Array.get(value, i));
                }
            } else {
                validateAnnotatedElement(parameter, value);
            }
        }
    }

    private void validateAnnotatedElement(AnnotatedElement element, Object value) {
        if (value instanceof Number numValue) {
            doValidateNumValue(element, numValue);
        } else if (value instanceof String strValue) {
            doValidateStringValue(element, strValue);
        } else if (value instanceof Validatable validatable) {
            doValidateValidatable(validatable);
        }
    }

    private void doValidateNumValue(AnnotatedElement element, Number number) {
        checkPageNum(element.getAnnotation(PageNum.class), number);
        checkPageSize(element.getAnnotation(PageSize.class), number);
        checkRange(element.getAnnotation(Range.class), number);
    }

    private void doValidateStringValue(AnnotatedElement element, String strValue) {
        checkNotBlank(element.getAnnotation(NotBlank.class), strValue);
        checkSize(element.getAnnotation(Size.class), strValue);
        checkPattern(element.getAnnotation(Pattern.class), strValue);
    }

    private void doValidateValidatable(Validatable validatable) {
        errorMessages.addAll(validatable.validate());
        final Field[] declaredFields = validatable.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            if (field.getAnnotations().length > 0) {
                validateField(field, validatable);
            }
        }
    }

    private void validateField(Field field, Object object) {
        final Object fieldValue;
        try {
            field.setAccessible(true);
            fieldValue = field.get(object);
        } catch (ReflectiveOperationException exception) {
            throw new RgwRuntimeException(exception.getMessage());
        }
        validateAnnotatedElement(field, fieldValue);
    }

    private void checkPageNum(PageNum pageNum, Number number) {
        if (Objects.nonNull(pageNum)) {
            checkRange(PageNum.class.getAnnotation(Range.class), number);
        }
    }

    private void checkPageSize(PageSize pageSize, Number number) {
        if (Objects.nonNull(pageSize)) {
            checkRange(PageSize.class.getAnnotation(Range.class), number);
        }
    }

    private void checkNotBlank(NotBlank notBlank, Object object) {
        if (Objects.nonNull(notBlank) && ObjectUtil.isEmpty(object)) {
            errorMessages.add(notBlank.message());
        }
    }

    private void checkPattern(Pattern pattern, String strValue) {
        if (Objects.nonNull(pattern)) {
            final String regexp = pattern.regexp();
            CACHED_REGEXES.putIfAbsent(regexp, java.util.regex.Pattern.compile(regexp));
            final Matcher matcher = CACHED_REGEXES.get(regexp).matcher(strValue);
            if (!matcher.matches()) {
                errorMessages.add(pattern.message());
            }
        }
    }

    private void checkRange(Range range, Number number) {
        if (Objects.nonNull(range)) {
            final long longVale = number.longValue();
            if (longVale < range.min() || longVale > range.max()) {
                errorMessages.add(range.message());
            }
        }
    }

    private void checkSize(Size size, String strValue) {
        if (Objects.nonNull(size)) {
            final int length = strValue.getBytes(StandardCharsets.UTF_8).length;
            if (length < size.min() || length > size.max()) {
                errorMessages.add(size.message());
            }
        }
    }

}
