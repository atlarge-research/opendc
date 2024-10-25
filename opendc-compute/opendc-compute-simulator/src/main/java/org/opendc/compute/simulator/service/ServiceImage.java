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

package org.opendc.compute.simulator.service;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.opendc.compute.api.Image;

/**
 * Implementation of {@link Image} provided by {@link ComputeService}.
 */
public final class ServiceImage implements Image {
    private final ComputeService service;
    private final UUID uid;
    private final String name;
    private final Map<String, String> labels;
    private final Map<String, ?> meta;

    ServiceImage(ComputeService service, UUID uid, String name, Map<String, String> labels, Map<String, ?> meta) {
        this.service = service;
        this.uid = uid;
        this.name = name;
        this.labels = labels;
        this.meta = meta;
    }

    @NotNull
    @Override
    public UUID getUid() {
        return uid;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public Map<String, Object> getMeta() {
        return Collections.unmodifiableMap(meta);
    }

    @Override
    public void reload() {
        // No-op: this object is the source-of-truth
    }

    @Override
    public void delete() {
        service.delete(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceImage image = (ServiceImage) o;
        return service.equals(image.service) && uid.equals(image.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, uid);
    }

    @Override
    public String toString() {
        return "Image[uid=" + uid + ",name=" + name + "]";
    }
}
