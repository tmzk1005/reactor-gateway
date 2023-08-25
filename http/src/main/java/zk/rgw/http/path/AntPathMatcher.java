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

package zk.rgw.http.path;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;

import zk.rgw.common.util.StringUtil;

public class AntPathMatcher implements PathMatcher {

    private static final AntPathMatcher DEFAULT_INSTANCE = new AntPathMatcher();

    public static final String DEFAULT_PATH_SEPARATOR = "/";

    private static final int CACHE_REFRESH_THRESHOLD = 65536;

    private static final char[] WILDCARD_CHARS = { '*', '?', '{' };

    private final String pathSeparator;

    private final Map<String, String[]> tokenizedPatternCache = new ConcurrentHashMap<>(256);

    final Map<String, AntPathTokenMatcher> antPathTokenMatcherCache = new ConcurrentHashMap<>(256);

    public AntPathMatcher() {
        this(DEFAULT_PATH_SEPARATOR);
    }

    public AntPathMatcher(String pathSeparator) {
        Objects.requireNonNull(pathSeparator);
        this.pathSeparator = pathSeparator;
    }

    public static AntPathMatcher getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    @Override
    public boolean isPattern(String path) {
        if (Objects.isNull(path)) {
            return false;
        }
        boolean uriVar = false;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '*' || c == '?') {
                return true;
            }
            if (c == '{') {
                uriVar = true;
                continue;
            }
            if (c == '}' && uriVar) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean match(String pattern, String path) {
        if (!isPattern(pattern)) {
            return pattern.equals(path);
        }
        return new DoMatchContext(pattern, path).doMatch();
    }

    @Override
    public Map<String, String> extractUriTemplateVariables(String pattern, String path) {
        final DoMatchContext doMatchContext = new DoMatchContext(pattern, path);
        boolean matched = doMatchContext.doMatch();
        if (!matched) {
            throw new IllegalStateException("Pattern \"" + pattern + "\" is not a match for \"" + path + "\"");
        }
        return doMatchContext.getUriTemplateVariables();
    }

    private static boolean isWildcardChar(char c) {
        for (char candidate : WILDCARD_CHARS) {
            if (c == candidate) {
                return true;
            }
        }
        return false;
    }

    private class DoMatchContext {

        private final String pattern;

        private final String path;

        private String[] patternTokens;

        private String[] pathTokens;

        private int patternIndexStart = 0;

        private int patternIndexEnd = 0;

        private int pathIndexStart = 0;

        private int pathIndexEnd = 0;

        @Getter
        private final Map<String, String> uriTemplateVariables = new LinkedHashMap<>(2);

        private DoMatchContext(String pattern, String path) {
            this.pattern = pattern;
            this.path = path;
        }

        private boolean doMatch() {
            if (Objects.isNull(path) || path.startsWith(pathSeparator) != pattern.startsWith(pathSeparator)) {
                return false;
            }
            patternTokens = tokenizePattern(pattern);
            if (!isPotentialMatch(path, patternTokens)) {
                return false;
            }
            pathTokens = tokenizePath(path);
            patternIndexEnd = patternTokens.length - 1;
            pathIndexEnd = pathTokens.length - 1;

            if (!partBeforeFirstDoubleStarMatch()) {
                return false;
            }

            if (pathIndexStart > pathIndexEnd) {
                // Path部分已经找到了末尾，只有Pattern剩下的部分是 * 或者 ** 才可能匹配上
                return ifRestOfPatternIsStar();
            }

            if (patternIndexStart > patternIndexEnd) {
                // Pattern先于Path找到了末尾，那么不可能匹配成功
                return false;
            }

            if (!partAfterLastDoubleStarMatch()) {
                return false;
            }

            if (pathIndexStart > pathIndexEnd) {
                return isLeftPatternAllDoubleStar();
            }

            if (!partBetweenFirstDoubleStartAndLastDoubleStarMatch()) {
                return false;
            }

            return isLeftPatternAllDoubleStar();
        }

        private boolean partBeforeFirstDoubleStarMatch() {
            // 匹配第一个 ** 之前的部分
            while (patternIndexStart <= patternIndexEnd && pathIndexStart <= pathIndexEnd) {
                String patternToken = patternTokens[patternIndexStart];
                if ("**".equals(patternToken)) {
                    break;
                }
                if (notMatchOneToken(patternToken, pathTokens[pathIndexStart])) {
                    return false;
                }
                patternIndexStart++;
                pathIndexStart++;
            }
            return true;
        }

        private boolean isLeftPatternAllDoubleStar() {
            for (int i = patternIndexStart; i <= patternIndexEnd; i++) {
                if (!patternTokens[i].equals("**")) {
                    return false;
                }
            }
            return true;
        }

        private boolean partAfterLastDoubleStarMatch() {
            while (patternIndexStart <= patternIndexEnd && pathIndexStart <= pathIndexEnd) {
                String patternLastToken = patternTokens[patternIndexEnd];
                if (patternLastToken.equals("**")) {
                    break;
                }
                if (notMatchOneToken(patternLastToken, pathTokens[pathIndexEnd])) {
                    return false;
                }
                patternIndexEnd--;
                pathIndexEnd--;
            }
            return true;
        }

        private boolean partBetweenFirstDoubleStartAndLastDoubleStarMatch() {
            while (patternIndexStart != patternIndexEnd && pathIndexStart <= pathIndexEnd) {
                int nextDoubleStarIndex = findNextDoubleStarIndex();
                if (nextDoubleStarIndex == patternIndexStart + 1) {
                    // 遇到 **/** 的情形，可以直接略过一个
                    patternIndexStart++;
                    continue;
                }
                int foundMatchTokenIndex = findNextMatchTokenIndex(nextDoubleStarIndex);
                if (foundMatchTokenIndex < 0) {
                    return false;
                }
                patternIndexStart = nextDoubleStarIndex;
                pathIndexStart = foundMatchTokenIndex + (nextDoubleStarIndex - patternIndexStart - 1);
            }
            return true;
        }

        private int findNextDoubleStarIndex() {
            int nextDoubleStarIndex = -1;
            for (int i = patternIndexStart + 1; i <= patternIndexEnd; i++) {
                if (patternTokens[i].equals("**")) {
                    nextDoubleStarIndex = i;
                    break;
                }
            }
            return nextDoubleStarIndex;
        }

        private int findNextMatchTokenIndex(int nextDoubleStarIndex) {
            int patternTokenCount = nextDoubleStarIndex - patternIndexStart - 1;
            int pathTokenCount = pathIndexEnd - pathIndexStart + 1;
            for (int i = 0; i <= pathTokenCount - patternTokenCount; i++) {
                if (segmentsAllMatch(i, patternTokenCount)) {
                    return pathIndexStart + i;
                }
            }
            return -1;
        }

        private boolean segmentsAllMatch(
                int offsetToCurrentPathTokenIndexStart,
                int maxOffsetToCurrentPatternTokenIndexStart
        ) {
            for (int j = 0; j < maxOffsetToCurrentPatternTokenIndexStart; j++) {
                if (
                    notMatchOneToken(
                            patternTokens[patternIndexStart + j + 1],
                            pathTokens[pathIndexStart + offsetToCurrentPathTokenIndexStart + j]
                    )
                ) {
                    return false;
                }
            }
            return true;
        }

        private boolean ifRestOfPatternIsStar() {
            if (patternIndexStart > patternIndexEnd) {
                return pattern.endsWith(pathSeparator) == path.endsWith(pathSeparator);
            }
            if (
                patternIndexStart == patternIndexEnd && patternTokens[patternIndexStart].equals("*") && path.endsWith(
                        pathSeparator
                )
            ) {
                return true;
            }
            for (int i = patternIndexStart; i <= patternIndexEnd; i++) {
                if (!patternTokens[i].equals("**")) {
                    return false;
                }
            }
            return true;
        }

        private boolean isPotentialMatch(String path, String[] patternDirs) {
            int pos = 0;
            for (String dir : patternDirs) {
                int skipped = skipSeparator(path, pos, pathSeparator);
                pos += skipped;
                skipped = skipSegment(path, pos, dir);
                if (skipped < dir.length()) {
                    // Java中&&的优先级高于||
                    return skipped > 0 || !dir.isEmpty() && isWildcardChar(dir.charAt(0));
                }
                pos += skipped;
            }
            return true;
        }

        private String[] tokenizePattern(String pattern) {
            String[] tokenized = tokenizedPatternCache.get(pattern);
            if (Objects.nonNull(tokenized)) {
                return tokenized;
            }
            tokenized = tokenizePath(pattern);
            if (tokenizedPatternCache.size() > CACHE_REFRESH_THRESHOLD) {
                tokenizedPatternCache.clear();
            }
            tokenizedPatternCache.put(pattern, tokenized);
            return tokenized;
        }

        private String[] tokenizePath(String path) {
            return StringUtil.tokenizeToStringArray(path, pathSeparator, false, true);
        }

        private boolean notMatchOneToken(String pattern, String token) {
            return !getAntPathTokenMatcher(pattern).matchToken(token, this.uriTemplateVariables);
        }

        private AntPathTokenMatcher getAntPathTokenMatcher(String pattern) {
            AntPathTokenMatcher antPathTokenMatcher = antPathTokenMatcherCache.get(pattern);
            if (Objects.nonNull(antPathTokenMatcher)) {
                return antPathTokenMatcher;
            }
            antPathTokenMatcher = new AntPathTokenMatcher(pattern);
            if (antPathTokenMatcherCache.size() > CACHE_REFRESH_THRESHOLD) {
                antPathTokenMatcherCache.clear();
            }
            antPathTokenMatcherCache.put(pattern, antPathTokenMatcher);
            return antPathTokenMatcher;
        }

        private static int skipSeparator(String path, int pos, String separator) {
            int skipped = 0;
            while (path.startsWith(separator, pos + skipped)) {
                skipped += separator.length();
            }
            return skipped;
        }

        private static int skipSegment(String path, int pos, String prefix) {
            int skipped = 0;
            for (int i = 0; i < prefix.length(); i++) {
                char c = prefix.charAt(i);
                if (isWildcardChar(c)) {
                    return skipped;
                }
                int currPos = pos + skipped;
                if (currPos >= path.length()) {
                    return 0;
                }
                if (c == path.charAt(currPos)) {
                    skipped++;
                }
            }
            return skipped;
        }

    }

    private static class AntPathTokenMatcher {

        private static final Pattern GLOB_PATTERN = Pattern.compile("\\?|\\*|\\{((?:\\{[^/]+?}|[^/{}]|\\\\[{}])+?)}");

        private static final String DEFAULT_VARIABLE_PATTERN = "((?s).*)";

        private final String rawPattern;

        private final boolean exactMatch;

        private final Pattern pattern;

        private final List<String> variableNames = new ArrayList<>();

        public AntPathTokenMatcher(String pattern) {
            this.rawPattern = pattern;
            StringBuilder patternBuilder = new StringBuilder();
            Matcher matcher = GLOB_PATTERN.matcher(pattern);
            int end = 0;
            while (matcher.find()) {
                patternBuilder.append(quote(pattern, end, matcher.start()));
                String match = matcher.group();
                if ("?".equals(match)) {
                    patternBuilder.append('.');
                } else if ("*".equals(match)) {
                    patternBuilder.append(".*");
                } else if (match.startsWith("{") && match.endsWith("}")) {
                    int colonIdx = match.indexOf(':');
                    if (colonIdx == -1) {
                        patternBuilder.append(DEFAULT_VARIABLE_PATTERN);
                        this.variableNames.add(matcher.group(1));
                    } else {
                        String variablePattern = match.substring(colonIdx + 1, match.length() - 1);
                        patternBuilder.append('(');
                        patternBuilder.append(variablePattern);
                        patternBuilder.append(')');
                        String variableName = match.substring(1, colonIdx);
                        this.variableNames.add(variableName);
                    }
                }
                end = matcher.end();
            }
            // No glob pattern was found, this is an exact String match
            if (end == 0) {
                this.exactMatch = true;
                this.pattern = null;
            } else {
                this.exactMatch = false;
                patternBuilder.append(quote(pattern, end, pattern.length()));
                this.pattern = Pattern.compile(patternBuilder.toString());
            }
        }

        public boolean matchToken(String token, Map<String, String> uriTemplateVariables) {
            if (this.exactMatch) {
                return this.rawPattern.equals(token);
            }
            if (Objects.nonNull(pattern)) {
                Matcher matcher = this.pattern.matcher(token);
                if (matcher.matches()) {
                    tryFillUriTemplateVariables(matcher, uriTemplateVariables);
                    return true;
                }
            }
            return false;
        }

        private void tryFillUriTemplateVariables(Matcher matcher, Map<String, String> uriTemplateVariables) {
            if (Objects.isNull(uriTemplateVariables)) {
                return;
            }
            if (this.variableNames.size() != matcher.groupCount()) {
                throw new IllegalArgumentException(
                        "The number of capturing groups in the pattern segment " +
                                this.pattern + " does not match the number of URI template variables it defines, " +
                                "which can occur if capturing groups are used in a URI template regex. " +
                                "Use non-capturing groups instead."
                );
            }
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String name = this.variableNames.get(i - 1);
                if (name.startsWith("*")) {
                    throw new IllegalArgumentException(
                            "Capturing patterns (" + name + ") are not " +
                                    "supported by the AntPathMatcher. Use the PathPatternParser instead."
                    );
                }
                String value = matcher.group(i);
                uriTemplateVariables.put(name, value);
            }
        }

        private static String quote(String s, int start, int end) {
            if (start == end) {
                return "";
            }
            return Pattern.quote(s.substring(start, end));
        }

    }

}
