/*
 * Copyright (c) 2022 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.web.ui.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

import java.util.List;

/**
 * Build item containing the routes for the OpenDC web UI.
 */
public final class OpenDCUiRoutingBuildItem extends SimpleBuildItem {

    private final boolean custom404;
    private final List<Page> pages;
    private final List<Redirect> redirects;

    /**
     * Construct a {@link OpenDCUiRoutingBuildItem} instance.
     *
     * @param routes The routes defined by Next.js.
     * @param redirects The redirects that have been defined by Next.js.
     * @param custom404 A flag to indicate that custom 404 pages are enabled.
     */
    public OpenDCUiRoutingBuildItem(List<Page> routes, List<Redirect> redirects, boolean custom404) {
        this.custom404 = custom404;
        this.pages = routes;
        this.redirects = redirects;
    }

    public List<Page> getPages() {
        return pages;
    }

    public List<Redirect> getRedirects() {
        return redirects;
    }

    public boolean hasCustom404() {
        return  this.custom404;
    }

    /**
     * A redirect defined by the Next.js routes manifest.
     */
    public static final class Redirect {

        private final String path;
        private final String destination;
        private final int statusCode;

        /**
         * Construct a {@link Redirect} route.
         *
         * @param path The path that should result in a redirect.
         * @param destination The destination of the redirect.
         * @param statusCode The status code of the redirect.
         */
        public Redirect(String path, String destination, int statusCode) {
            this.statusCode = statusCode;
            this.path = path;
            this.destination = destination;
        }

        public String getPath() {
            return path;
        }

        public String getDestination() {
            return destination;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

    /**
     * A page defined by the Next.js routes manifest.
     */
    public static final class Page {

        private final String path;
        private final String name;

        public Page(String path, String page) {
            this.path = path;
            this.name = page;
        }

        public String getPath() {
            return path;
        }

        public String getName() {
            return name;
        }
    }
}
