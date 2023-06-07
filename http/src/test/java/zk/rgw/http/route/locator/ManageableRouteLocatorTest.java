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

import java.util.Objects;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import zk.rgw.http.route.Route;

class ManageableRouteLocatorTest {

    @Test
    void test() {
        ManageableRouteLocator mrl = new ManageableRouteLocator();
        StepVerifier.create(mrl.getRoutes("/")).expectComplete().verify();

        String id1 = "/foo";
        RouteForTest route1 = new RouteForTest();
        route1.setId(id1);
        route1.setPath("/foo");

        mrl.addRoute(route1);

        StepVerifier.create(mrl.getRoutes("/foo")).expectNext(route1).verifyComplete();
        StepVerifier.create(mrl.getRoutes("/")).expectComplete().verify();
        StepVerifier.create(mrl.getRoutes("/bar")).expectComplete().verify();

        String id2 = "/bar";
        RouteForTest route2 = new RouteForTest();
        route2.setId(id2);
        route2.setPath("/bar/foo/{name}");

        mrl.addRoute(route2);

        StepVerifier.create(mrl.getRoutes("/bar/foo/alice")).expectNext(route2).verifyComplete();
        StepVerifier.create(mrl.getRoutes("/bar/foo/bob")).expectNext(route2).verifyComplete();
        StepVerifier.create(mrl.getRoutes("/foo/alice")).expectComplete().verify();

        String id3 = "/bar/{x}/{y}";
        RouteForTest route3 = new RouteForTest();
        route3.setId(id3);
        route3.setPath("/bar/{x}/{y}");

        mrl.addRoute(route3);

        StepVerifier.create(mrl.getRoutes("/bar/foo/alice")).expectNext(route2).expectNext(route3).verifyComplete();
        StepVerifier.create(mrl.getRoutes("/bar/bar/alice")).expectNext(route3).verifyComplete();

        String id4 = "/bar/foo/joey";
        RouteForTest route4 = new RouteForTest();
        route4.setId(id4);
        route4.setPath("/bar/foo/joey");

        mrl.addRoute(route4);

        StepVerifier.create(mrl.getRoutes("/bar/foo/joey")).expectNext(route4).expectNext(route2).expectNext(route3).verifyComplete();

        mrl.removeRouteById(id2);

        StepVerifier.create(mrl.getRoutes("/bar/foo/joey")).expectNext(route4).expectNext(route3).verifyComplete();
    }

    static class RouteForTest extends Route {

        @Override
        public int hashCode() {
            return Objects.hash(this.getId(), this.getPath());
        }

        @Override
        public boolean equals(Object obj) {
            if (Objects.isNull(obj) || !(obj instanceof RouteForTest another)) {
                return false;
            }
            return Objects.equals(this.getId(), another.getId()) && Objects.equals(this.getPath(), another.getPath());
        }

    }

}
