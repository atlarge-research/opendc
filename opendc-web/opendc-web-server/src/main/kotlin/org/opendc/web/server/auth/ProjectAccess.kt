/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.web.server.auth

import org.opendc.web.server.model.Experiment
import org.opendc.web.server.model.Project
import org.opendc.web.server.model.ProjectMember
import org.opendc.web.server.model.ProjectRole
import org.opendc.web.server.model.TopologyTemplate
import org.opendc.web.server.model.UserAccount
import org.opendc.web.server.rest.forbidden
import org.opendc.web.server.rest.notFound
import org.opendc.web.server.rest.publicId
import java.util.UUID

/** What a caller needs to be allowed to do in a project. */
enum class ProjectPermission {
    /** See it and everything in it. Every member may. */
    READ,

    /** Change what it holds, and run it. Owners and editors may. */
    EDIT,

    /** Decide who is in it, and delete it. Only owners may. */
    MANAGE,
}

fun ProjectRole.allows(permission: ProjectPermission): Boolean =
    when (this) {
        ProjectRole.OWNER -> true
        ProjectRole.EDITOR -> permission != ProjectPermission.MANAGE
        ProjectRole.VIEWER -> permission == ProjectPermission.READ
    }

/**
 * The caller's membership of the project behind [id], if it allows [needs]. A non-member gets the 404
 * of a missing project, so membership cannot be probed; a member lacking the role gets a 403.
 */
fun projectFor(
    user: UserAccount,
    id: String,
    needs: ProjectPermission,
): ProjectMember = projectFor(user, publicId(id, "Project"), needs)

fun projectFor(
    user: UserAccount,
    id: UUID,
    needs: ProjectPermission,
): ProjectMember {
    val project = Project.findByPublicId(id) ?: throw notFound("Project")
    return membershipAllowing(project, user, needs, "Project")
}

fun topologyFor(
    user: UserAccount,
    id: String,
    needs: ProjectPermission,
): TopologyTemplate {
    val template = TopologyTemplate.findByPublicId(publicId(id, "Topology")) ?: throw notFound("Topology")
    membershipAllowing(template.project, user, needs, "Topology")
    return template
}

fun experimentFor(
    user: UserAccount,
    id: String,
    needs: ProjectPermission,
): Experiment {
    val experiment = Experiment.findByPublicId(publicId(id, "Experiment")) ?: throw notFound("Experiment")
    membershipAllowing(experiment.project, user, needs, "Experiment")
    return experiment
}

private fun membershipAllowing(
    project: Project,
    user: UserAccount,
    needs: ProjectPermission,
    what: String,
): ProjectMember {
    val member = ProjectMember.findMembership(project.id, user.id) ?: throw notFound(what)
    if (!member.role.allows(needs)) {
        throw forbidden(
            when (needs) {
                ProjectPermission.READ, ProjectPermission.EDIT -> "Viewers cannot change this project"
                ProjectPermission.MANAGE -> "Only an owner can do this"
            },
        )
    }
    return member
}
