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

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import java.util.Optional;

/**
 * Auth configuration for the OpenDC UI extension.
 */
@ConfigGroup
public class AuthConfiguration {
    /**
     * The authentication domain.
     */
    @ConfigItem
    Optional<String> domain;

    /**
     * The client identifier used by the OpenDC web ui.
     */
    @ConfigItem
    Optional<String> clientId;

    /**
     * The audience of the OpenDC API.
     */
    @ConfigItem
    Optional<String> audience;
}
