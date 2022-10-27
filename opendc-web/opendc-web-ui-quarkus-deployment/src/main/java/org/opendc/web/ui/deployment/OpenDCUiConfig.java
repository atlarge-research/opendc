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

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import java.util.Optional;

/**
 * Build-time configuration for the OpenDC UI extension.
 */
@ConfigRoot(name = "opendc-ui")
public class OpenDCUiConfig {
    /**
     * A flag to include the OpenDC UI extension into the build.
     */
    @ConfigItem(defaultValue = "true")
    boolean include;

    /**
     * The base URL of the OpenDC API.
     */
    @ConfigItem(defaultValue = "/api")
    String apiBaseUrl;

    /**
     * Configuration properties for web UI authentication.
     */
    AuthConfiguration auth;

    /**
     * Sentry DSN.
     */
    @ConfigItem
    Optional<String> sentryDsn;
}
