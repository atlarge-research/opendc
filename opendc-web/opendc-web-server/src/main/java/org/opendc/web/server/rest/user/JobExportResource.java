/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.web.server.rest.user;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.File;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.opendc.web.server.model.Job;

/**
 * Resource for downloading simulation export files (ZIP archives) produced by the runner.
 */
@Path("/jobs/{job}/exports")
@RolesAllowed("openid")
public final class JobExportResource {
    /**
     * The directory where the runner writes export ZIP files.
     */
    @ConfigProperty(name = "opendc.export-dir", defaultValue = "/tmp/opendc-exports")
    String exportDir;

    /**
     * Download the export ZIP archive for the specified job.
     */
    @GET
    @Produces("application/zip")
    public Response getExports(@PathParam("job") long jobId) {
        Job job = Job.findById(jobId);

        if (job == null) {
            throw new WebApplicationException("Job not found", 404);
        }

        if (!job.hasExports) {
            throw new WebApplicationException("No exports available for this job", 404);
        }

        File zip = new File(exportDir, jobId + ".zip");
        if (!zip.exists()) {
            throw new WebApplicationException("Export file not found", 404);
        }

        return Response.ok(zip)
                .header("Content-Disposition", "attachment; filename=\"job-" + jobId + "-exports.zip\"")
                .build();
    }
}
