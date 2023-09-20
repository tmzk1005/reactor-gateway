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

@SuppressWarnings({ "PMD.UselessParentheses", "PMD.AvoidBranchingStatementAsLastInLoop" })
public class PathUtil {

    public static final String SLASH = "/";

    private PathUtil() {
    }

    public static String normalize(String path) {
        String newPath = normalize2(path);
        if (newPath.isEmpty()) {
            return SLASH;
        }
        if (!newPath.startsWith(SLASH)) {
            newPath = SLASH + newPath;
        }
        return newPath;
    }

    public static String constantPrefix(String normalizePath) {
        int slashIndex = 0;
        boolean foundWildcardChar = false;
        for (int i = 0; i < normalizePath.length(); ++i) {
            char c = normalizePath.charAt(i);
            if (c == '*' || c == '?' || c == '{') {
                foundWildcardChar = true;
                break;
            } else if (c == '/') {
                slashIndex = i;
            }
        }
        if (!foundWildcardChar) {
            return normalizePath;
        }
        if (slashIndex == 0) {
            return SLASH;
        }
        return normalizePath.substring(0, slashIndex);
    }

    public static String removeLast(String normalizePath) {
        if ("".equals(normalizePath)) {
            return "";
        }
        int index = normalizePath.length() - 1;
        while (index > 0 && normalizePath.charAt(index) != '/') {
            --index;
        }
        return normalizePath.substring(0, index);
    }

    public static String getFirstPart(String normalizePath) {
        final int secondSlashIndex = normalizePath.indexOf(SLASH, 1);
        if (secondSlashIndex == -1) {
            return normalizePath;
        } else {
            return normalizePath.substring(0, secondSlashIndex);
        }
    }

    public static String normalize2(String ps) {
        // Does this path need normalization?
        int ns = needsNormalization(ps); // Number of segments
        if (ns < 0)
            // Nope -- just return it
            return ps;

        char[] path = ps.toCharArray(); // Path in char-array form

        // Split path into segments
        int[] segs = new int[ns]; // Segment-index array
        split(path, segs);

        // Remove dots
        removeDots(path, segs);

        // Prevent scheme-name confusion
        maybeAddLeadingDot(path, segs);

        // Join the remaining segments and return the result
        String s = new String(path, 0, join(path, segs));
        if (s.equals(ps)) {
            // string was already normalized
            return ps;
        }
        return s;
    }

    private static int needsNormalization(String path) {
        boolean normal = true;
        int ns = 0; // Number of segments
        int end = path.length() - 1; // Index of last char in path
        int p = 0; // Index of next char in path

        // Skip initial slashes
        while (p <= end) {
            if (path.charAt(p) != '/')
                break;
            p++;
        }
        if (p > 1)
            normal = false;

        // Scan segments
        while (p <= end) {

            // Looking at "." or ".." ?
            if (
                (path.charAt(p) == '.') && ((p == end) || ((path.charAt(p + 1) == '/')
                        || ((path.charAt(p + 1) == '.') && ((p + 1 == end) || (path.charAt(p + 2) == '/')))))
            ) {
                normal = false;
            }
            ns++;

            // Find beginning of next segment
            while (p <= end) {
                if (path.charAt(p++) != '/')
                    continue;

                // Skip redundant slashes
                while (p <= end) {
                    if (path.charAt(p) != '/')
                        break;
                    normal = false;
                    p++;
                }

                break;
            }
        }

        return normal ? -1 : ns;
    }

    private static void split(char[] path, int[] segs) {
        int end = path.length - 1; // Index of last char in path
        int p = 0; // Index of next char in path
        int i = 0; // Index of current segment

        // Skip initial slashes
        while (p <= end) {
            if (path[p] != '/')
                break;
            path[p] = '\0';
            p++;
        }

        while (p <= end) {

            // Note start of segment
            segs[i++] = p++;

            // Find beginning of next segment
            while (p <= end) {
                if (path[p++] != '/')
                    continue;
                path[p - 1] = '\0';

                // Skip redundant slashes
                while (p <= end) {
                    if (path[p] != '/')
                        break;
                    path[p++] = '\0';
                }
                break;
            }
        }

        if (i != segs.length)
            throw new InternalError(); // ASSERT
    }

    private static void removeDots(char[] path, int[] segs) {
        int ns = segs.length;
        int end = path.length - 1;

        for (int i = 0; i < ns; i++) {
            int dots = 0; // Number of dots found (0, 1, or 2)

            // Find next occurrence of "." or ".."
            do {
                int p = segs[i];
                if (path[p] == '.') {
                    if (p == end) {
                        dots = 1;
                        break;
                    } else if (path[p + 1] == '\0') {
                        dots = 1;
                        break;
                    } else if (
                        (path[p + 1] == '.')
                                && ((p + 1 == end)
                                        || (path[p + 2] == '\0'))
                    ) {
                        dots = 2;
                        break;
                    }
                }
                i++;
            } while (i < ns);
            if ((i > ns) || (dots == 0))
                break;

            if (dots == 1) {
                // Remove this occurrence of "."
                segs[i] = -1;
            } else {
                // If there is a preceding non-".." segment, remove both that
                // segment and this occurrence of ".."; otherwise, leave this
                // ".." segment as-is.
                int j;
                for (j = i - 1; j >= 0; j--) {
                    if (segs[j] != -1)
                        break;
                }
                if (j >= 0) {
                    int q = segs[j];
                    if (
                        !((path[q] == '.')
                                && (path[q + 1] == '.')
                                && (path[q + 2] == '\0'))
                    ) {
                        segs[i] = -1;
                        segs[j] = -1;
                    }
                }
            }
        }
    }

    private static void maybeAddLeadingDot(char[] path, int[] segs) {

        if (path[0] == '\0')
            // The path is absolute
            return;

        int ns = segs.length;
        int f = 0; // Index of first segment
        while (f < ns) {
            if (segs[f] >= 0)
                break;
            f++;
        }
        if ((f >= ns) || (f == 0))
            // The path is empty, or else the original first segment survived,
            // in which case we already know that no leading "." is needed
            return;

        int p = segs[f];
        while ((p < path.length) && (path[p] != ':') && (path[p] != '\0'))
            p++;
        if (p >= path.length || path[p] == '\0')
            // No colon in first segment, so no "." needed
            return;

        // At this point we know that the first segment is unused,
        // hence we can insert a "." segment at that position
        path[0] = '.';
        path[1] = '\0';
        segs[0] = 0;
    }

    private static int join(char[] path, int[] segs) {
        int end = path.length - 1; // Index of last char in path
        int p = 0; // Index of next path char to write

        if (path[p] == '\0') {
            // Restore initial slash for absolute paths
            path[p++] = '/';
        }

        for (int seg : segs) {
            int q = seg; // Current segment
            if (q == -1)
                // Ignore this segment
                continue;

            if (p == q) {
                // We're already at this segment, so just skip to its end
                while ((p <= end) && (path[p] != '\0'))
                    p++;
                if (p <= end) {
                    // Preserve trailing slash
                    path[p++] = '/';
                }
            } else if (p < q) {
                // Copy q down to p
                while ((q <= end) && (path[q] != '\0'))
                    path[p++] = path[q++];
                if (q <= end) {
                    // Preserve trailing slash
                    path[p++] = '/';
                }
            } else
                throw new InternalError(); // ASSERT false
        }

        return p;
    }

}
