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

package zk.rgw.http.route.locator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import reactor.core.publisher.Flux;

import zk.rgw.http.path.AntPathMatcher;
import zk.rgw.http.path.PathMatcher;
import zk.rgw.http.path.PathUtil;
import zk.rgw.http.route.Route;

public class ManageableRouteLocator implements RouteLocator {

    private final Map<String, Route> allRoutes = new ConcurrentHashMap<>(8);

    /**
     * Route配置的path都是常量，没有pattern，key就是normalize后的path
     */
    private final Map<String, RouteGroup> constantPathRoutes = new ConcurrentHashMap<>(8);

    /**
     * Route配置的path都不是常量，含有pattern，key是最长的常量前缀
     * 比如一个Route的path是/foo/bar/{name}/info,那么它应该在key为/foo/bar的group中
     */
    private final Map<String, RouteGroup> patternPathRoutes = new ConcurrentHashMap<>(8);

    private final PathMatcher pathMatcher = AntPathMatcher.getDefaultInstance();

    @Override
    public Flux<Route> getRoutes(String path) {
        String normalizePath = PathUtil.normalize(path);
        RouteGroup routeGroup = constantPathRoutes.get(normalizePath);
        List<RouteGroup> groups = findInPatternPathRoutes(normalizePath);
        if (Objects.nonNull(routeGroup)) {
            groups.add(0, routeGroup);
        }
        if (groups.isEmpty()) {
            return Flux.empty();
        } else if (groups.size() == 1) {
            return groups.get(0).getRoutes();
        }
        List<Flux<Route>> list = new ArrayList<>(groups.size());
        for (RouteGroup rg : groups) {
            list.add(rg.getRoutes());
        }
        return Flux.concat(list);
    }

    private List<RouteGroup> findInPatternPathRoutes(String normalizePath) {
        String prefix = PathUtil.removeLast(normalizePath);
        List<RouteGroup> groups = new LinkedList<>();
        while (!"".equals(prefix)) {
            RouteGroup routeGroup = patternPathRoutes.get(prefix);
            if (Objects.nonNull(routeGroup)) {
                groups.add(routeGroup);
            }
            prefix = PathUtil.removeLast(prefix);
        }
        return groups;
    }

    protected synchronized void addRoute(Route route) {
        if (allRoutes.containsKey(route.getId())) {
            // 是一个已经存在的api的更新动作，其path可能已经改变，要先根据id删除之
            removeRouteById(route.getId());
        }
        this.allRoutes.put(route.getId(), route);
        String normalizePath = PathUtil.normalize(route.getPath());

        RouteGroup targetGroup;

        if (pathMatcher.isPattern(normalizePath)) {
            String prefix = PathUtil.constantPrefix(normalizePath);
            targetGroup = patternPathRoutes.get(prefix);
            if (Objects.isNull(targetGroup)) {
                targetGroup = new RouteGroup();
                patternPathRoutes.put(prefix, targetGroup);
            }
        } else {
            targetGroup = constantPathRoutes.get(normalizePath);
            if (Objects.isNull(targetGroup)) {
                targetGroup = new RouteGroup();
                constantPathRoutes.put(normalizePath, targetGroup);
            }
        }
        targetGroup.addRoute(route);
    }

    protected synchronized Route removeRouteById(String routeId) {
        if (!allRoutes.containsKey(routeId)) {
            return null;
        }
        Route removedRoute = allRoutes.remove(routeId);
        String normalizePath = PathUtil.normalize(removedRoute.getPath());

        RouteGroup targetGroup;

        if (pathMatcher.isPattern(normalizePath)) {
            String prefix = PathUtil.constantPrefix(normalizePath);
            targetGroup = patternPathRoutes.get(prefix);
            if (Objects.nonNull(targetGroup)) {
                targetGroup.removeRouteById(routeId);
                if (targetGroup.isEmpty()) {
                    patternPathRoutes.remove(prefix);
                }
            }
        } else {
            targetGroup = constantPathRoutes.get(normalizePath);
            if (Objects.nonNull(targetGroup)) {
                targetGroup.removeRouteById(routeId);
                if (targetGroup.isEmpty()) {
                    constantPathRoutes.remove(normalizePath);
                }
            }
        }
        return removedRoute;
    }

    private static class RouteGroup {

        volatile boolean changed = false;

        final Map<String, Route> routes = new HashMap<>(4);

        Flux<Route> flux = Flux.empty();

        Flux<Route> getRoutes() {
            if (changed) {
                this.flux = Flux.fromIterable(routes.values());
                changed = false;
            }
            return flux;
        }

        void addRoute(Route route) {
            routes.put(route.getId(), route);
            changed = true;
        }

        void removeRouteById(String id) {
            changed = Objects.nonNull(this.routes.remove(id));
        }

        boolean isEmpty() {
            return this.routes.isEmpty();
        }

    }

}
