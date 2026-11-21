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

package org.opendc.web.server.model

import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.LockModeType
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.intellij.lang.annotations.Language
import java.time.Instant
import java.util.UUID

enum class ProjectRole {
    OWNER,
    EDITOR,
    VIEWER,
}

/** [id] never leaves the server; the API carries [publicId], so identifiers cannot be walked from outside. */
@Entity
@Table(name = "projects")
class Project : PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    var publicId: UUID = UUID.randomUUID()

    lateinit var name: String

    lateinit var createdAt: Instant

    lateinit var updatedAt: Instant

    companion object : PanacheCompanion<Project> {
        fun findByPublicId(publicId: UUID): Project? = find("publicId = ?1", publicId).firstResult()

        /** The project, locked for the rest of the transaction, so changes to who is in it take turns. */
        fun lockByPublicId(publicId: UUID): Project? =
            find("publicId = ?1", publicId).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult()
    }
}

@Entity
@Table(name = "project_members")
class ProjectMember : PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    lateinit var project: Project

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    lateinit var user: UserAccount

    @Enumerated(EnumType.STRING)
    lateinit var role: ProjectRole

    companion object : PanacheCompanion<ProjectMember> {
        // The project is fetched eagerly because every caller reads it; lazy, it costs a query per row.
        @Language("JPAQL")
        private const val BY_USER = """
            SELECT m FROM ProjectMember m
            JOIN FETCH m.project
            WHERE m.user.id = ?1
        """

        @Language("JPAQL")
        private const val MEMBERS = """
            SELECT m FROM ProjectMember m
            JOIN FETCH m.user
            WHERE m.project.id = ?1
        """

        fun findByUser(userId: Long): List<ProjectMember> = list(BY_USER, userId)

        /** Everyone in a project, with their accounts. */
        fun findMembers(projectId: Long): List<ProjectMember> = list(MEMBERS, projectId)

        fun countOwners(projectId: Long): Long = count("project.id = ?1 AND role = ?2", projectId, ProjectRole.OWNER)

        fun findMembership(
            projectId: Long,
            userId: Long,
        ): ProjectMember? = find("project.id = ?1 and user.id = ?2", projectId, userId).firstResult()
    }
}
