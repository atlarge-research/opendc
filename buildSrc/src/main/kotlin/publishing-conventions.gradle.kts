/*
 * Copyright (c) 2021 AtLarge Research
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

import org.gradle.api.credentials.PasswordCredentials

plugins {
    `maven-publish`
    signing
}

val isSnapshot = project.version.toString().contains("SNAPSHOT")

// Ensure project is built successfully before publishing it
tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn(tasks.build)
}
tasks.withType<PublishToMavenLocal>().configureEach {
    dependsOn(tasks.build)
}

signing {
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project

    if (!signingKey.isNullOrBlank()) {
        if (signingKeyId?.isNotBlank() == true) {
            useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        } else {
            useInMemoryPgpKeys(signingKey, signingPassword)
        }
    }

    sign(publishing.publications)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            pom {
                name.set(project.name)
                description.set("Open-source platform for datacenter simulation")
                url.set("https://opendc.org")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("AtLarge Research")
                        name.set("AtLarge Research Team")
                        organization.set("AtLarge Research")
                        organizationUrl.set("https://atlarge-research.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/atlarge-research/opendc.git")
                    developerConnection.set("scm:git:git://github.com/atlarge-research/opendc.git")
                    url.set("https://github.com/atlarge-research/opendc")
                }
            }
        }
    }

    repositories {
        maven {
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")

            name = "ossrh"
            url = if (isSnapshot) snapshotsRepoUrl else releasesRepoUrl
            credentials(PasswordCredentials::class)
        }
    }
}
