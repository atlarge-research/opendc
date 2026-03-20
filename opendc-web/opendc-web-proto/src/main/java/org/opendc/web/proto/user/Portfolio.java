/*
 * Copyright (c) 2023 AtLarge Research
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

package org.opendc.web.proto.user;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.opendc.web.proto.Targets;

/**
 * A portfolio is the composition of multiple scenarios.
 *
 * @param id The unique identifier of the portfolio.
 * @param number The number of the portfolio with respect to the project.
 * @param project The project to which the portfolio belongs.
 * @param name The name of the portfolio.
 * @param targets The targets of the portfolio.
 * @param scenarios The scenarios in the portfolio.
 */
public record Portfolio(
        long id, int number, Project project, String name, Targets targets, List<Scenario.Summary> scenarios) {
    /**
     * A request to create a new portfolio.
     */
    @Schema(name = "Portfolio.Update")
    public record Create(@NotBlank(message = "Name must not be empty") String name, Targets targets) {}

    /**
     * A summary view of a [Portfolio] provided for nested relations.
     *
     * @param id The unique identifier of the portfolio.
     * @param number The number of the portfolio for the project.
     * @param name The name of the portfolio.
     * @param targets The targets of the portfolio.
     */
    @Schema(name = "Portfolio.Summary")
    public record Summary(long id, int number, String name, Targets targets) {}
}
