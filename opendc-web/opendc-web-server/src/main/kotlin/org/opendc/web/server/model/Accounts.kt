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
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.intellij.lang.annotations.Language
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID

enum class PlanTier {
    FREE,
    EDUCATION,
    ENTERPRISE,
}

/**
 * Whether the account has picked the name other people see. A first sign-in gets a placeholder that
 * nothing can be shared under until the person chooses.
 */
enum class HandleKind {
    PROVISIONAL,
    CHOSEN,
}

enum class AccountState {
    ACTIVE,

    /** Signed out for good: its tokens are revoked and it left the projects others work in. */
    DEACTIVATED,
}

@Entity
@Table(name = "users")
class UserAccount : PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    lateinit var subject: String

    /** The account's name in identifiers other people read; prefixes every trace it uploads. */
    lateinit var handle: String

    @Enumerated(EnumType.STRING)
    var handleKind: HandleKind = HandleKind.PROVISIONAL

    lateinit var displayName: String

    @Enumerated(EnumType.STRING)
    var planTier: PlanTier = PlanTier.FREE

    var isAdmin: Boolean = false

    @Enumerated(EnumType.STRING)
    var state: AccountState = AccountState.ACTIVE

    lateinit var createdAt: Instant

    /** When [state] became [AccountState.DEACTIVATED], which a check constraint ties it to. */
    var deactivatedAt: Instant? = null

    fun deactivate(at: Instant) {
        state = AccountState.DEACTIVATED
        deactivatedAt = at
    }

    companion object : PanacheCompanion<UserAccount> {
        fun findBySubject(subject: String): UserAccount? = find("subject = ?1", subject).firstResult()

        fun findByHandle(handle: String): UserAccount? = find("handle = ?1", handle).firstResult()

        /** An account other people can address: one that has chosen its handle and is still active. */
        fun findActiveByHandle(handle: String): UserAccount? =
            find(
                "handle = ?1 AND handleKind = ?2 AND state = ?3",
                handle,
                HandleKind.CHOSEN,
                AccountState.ACTIVE,
            ).firstResult()
    }
}

enum class TokenUse {
    UNUSED,
    USED,
}

/** When an access token was last presented, to the hour. */
sealed interface TokenUsage {
    data object Unused : TokenUsage

    data class Used(val at: Instant) : TokenUsage
}

/**
 * A personal access token, which is how the CLI and scripts act as their owner. Only its hash is
 * kept; the secret is shown once, when it is minted.
 */
@Entity
@Table(name = "access_tokens")
class AccessToken : PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    var publicId: UUID = UUID.randomUUID()

    @ManyToOne(fetch = FetchType.LAZY)
    lateinit var user: UserAccount

    lateinit var name: String

    lateinit var tokenHash: String

    /** The start of the secret, so its owner can tell their tokens apart. */
    lateinit var tokenPrefix: String

    lateinit var createdAt: Instant

    @Enumerated(EnumType.STRING)
    var useKind: TokenUse = TokenUse.UNUSED

    var lastUsedAt: Instant? = null

    val lastUse: TokenUsage
        get() =
            when (useKind) {
                TokenUse.UNUSED -> TokenUsage.Unused
                TokenUse.USED -> TokenUsage.Used(checkNotNull(lastUsedAt) { "a used token records when" })
            }

    /** Notes a use, at most once an hour, so presenting a token is not a write every time. */
    fun recordUse(now: Instant) {
        val last = lastUse
        if (last is TokenUsage.Used && last.at.isAfter(now.minus(USE_RESOLUTION))) {
            return
        }
        useKind = TokenUse.USED
        lastUsedAt = now
    }

    companion object : PanacheCompanion<AccessToken> {
        @Language("JPAQL")
        private const val BY_HASH = """
            SELECT t FROM AccessToken t
            JOIN FETCH t.user
            WHERE t.tokenHash = ?1
        """

        /** A new token for [user], with its secret, which is never readable again. */
        fun mint(
            user: UserAccount,
            name: String,
            now: Instant,
        ): MintedSecret {
            val secret = newSecret(PAT_PREFIX, PAT_BYTES)
            val token = AccessToken()
            token.user = user
            token.name = name
            token.tokenHash = sha256Hex(secret)
            token.tokenPrefix = secret.take(DISPLAYED_PREFIX)
            token.createdAt = now
            token.persist()
            return MintedSecret(token, secret)
        }

        fun findBySecret(secret: String): AccessToken? = find(BY_HASH, sha256Hex(secret)).firstResult()

        fun findOwnedBy(userId: Long): List<AccessToken> = list("user.id = ?1", userId).sortedByDescending { it.createdAt }
    }
}

/** A token as it is minted, the only time its secret exists outside the caller's hands. */
data class MintedSecret(
    val token: AccessToken,
    val secret: String,
)

const val PAT_PREFIX = "odc_pat_"

const val EXECUTION_TOKEN_PREFIX = "odc_exec_"

private const val PAT_BYTES = 32

private const val DISPLAYED_PREFIX = 12

private val USE_RESOLUTION = Duration.ofHours(1)

private val RANDOM = SecureRandom()

/** A secret of [bytes] random bytes, written after [prefix] so a leaked one is recognisable. */
fun newSecret(
    prefix: String,
    bytes: Int,
): String = prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(bytes).also(RANDOM::nextBytes))

/** What is kept of a secret: enough to recognise it, nothing to recover it from. */
fun sha256Hex(text: String): String = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
