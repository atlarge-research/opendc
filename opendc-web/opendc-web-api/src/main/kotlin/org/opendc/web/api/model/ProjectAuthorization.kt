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

package org.opendc.web.api.model

import org.opendc.web.proto.user.ProjectRole
import javax.persistence.*

/**
 * An authorization for some user to participate in a project.
 */
@Entity
@Table(name = "project_authorizations")
class ProjectAuthorization(
    /**
     * The user identifier of the authorization.
     */
    @EmbeddedId
    val key: ProjectAuthorizationKey,

    /**
     * The project that the user is authorized to participate in.
     */
    @ManyToOne(optional = false)
    @MapsId("projectId")
    @JoinColumn(name = "project_id", updatable = false, insertable = false, nullable = false)
    val project: Project,

    /**
     * The role of the user in the project.
     */
    @Column(nullable = false)
    val role: ProjectRole
) {
    /**
     * Return a string representation of this project authorization.
     */
    override fun toString(): String = "ProjectAuthorization[project=${key.projectId},user=${key.userId},role=$role]"
}
