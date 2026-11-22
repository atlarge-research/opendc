description = "Python bindings for the OpenDC SDK model and runner"

plugins {
    `kotlin-conventions`
    `testing-conventions`
}

dependencies {
    implementation(projects.opendcSdk.opendcSdkRunner)
    implementation(projects.opendcTrace.opendcTraceApi)
    implementation(kotlin("reflect"))

    runtimeOnly(libs.log4j.core)
    runtimeOnly(libs.log4j.slf4j)

    testImplementation(kotlin("test"))
}

/**
 * Runs uv in this directory. JPype starts the JVM that JAVA_HOME points at, so it is set to the Java toolchain of the
 * build.
 */
abstract class UvTask : DefaultTask() {
    @get:Input
    abstract val command: ListProperty<String>

    @get:Input
    @get:Optional
    @get:Option(option = "args", description = "The arguments appended to the uv command, separated by spaces")
    abstract val extraArguments: Property<String>

    @get:Nested
    abstract val javaLauncher: Property<JavaLauncher>

    @get:Internal
    abstract val offline: Property<Boolean>

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Inject
    abstract val projectLayout: ProjectLayout

    @TaskAction
    fun runUv() {
        execOperations.exec {
            workingDir = projectLayout.projectDirectory.asFile
            executable = "uv"
            args(command.get() + extraArguments.getOrElse("").split(" ").filter { it.isNotBlank() })
            environment("JAVA_HOME", javaLauncher.get().metadata.installationPath.asFile.absolutePath)
            if (offline.get()) {
                environment("UV_OFFLINE", "1")
            }
        }
    }
}

tasks.withType<UvTask>().configureEach {
    group = "uv"
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
    offline.set(gradle.startParameter.isOffline)
}

// The package finds its JVM classpath here, and the wheel ships it; the directory is gitignored
val stagedJars = layout.projectDirectory.dir("src/main/python/opendc/jars")

val stageJvmLibraries by tasks.registering(Sync::class) {
    group = "build"
    description = "Copies the bridge jar and its runtime classpath into the Python package"
    from(tasks.jar, configurations.runtimeClasspath)
    into(stagedJars)
}

// The staged jars are left out, so that the Python checks do not depend on the JVM build
val pythonSources =
    files(
        fileTree("src/main/python") { exclude("opendc/jars/**", "**/__pycache__/**") },
        fileTree("src/test/python") { exclude("**/__pycache__/**") },
    )
val uvProject = files("pyproject.toml", "uv.lock", ".python-version")

// The small traces of the runner that the Python tests read
val testTraces =
    fileTree("../opendc-sdk-runner/src/test/resources") {
        include(
            "workloadTraces/single_task/**",
            "carbonTraces/2022-01-01_2022-12-31_NL.parquet",
            "failureTraces/single_failure.parquet",
        )
    }

// uv sync and uv run get --locked, so a uv.lock that pyproject.toml has outgrown fails the build with no lock check
tasks.register<UvTask>("uvSync") {
    description = "Creates the virtual environment from uv.lock"
    command.set(listOf("sync", "--locked"))
    dependsOn(stageJvmLibraries)
}

tasks.register<UvTask>("uvFormat") {
    description = "Formats the Python sources"
    command.set(listOf("run", "--locked", "ruff", "format"))
}

// The checks take a moment, so they run every time rather than track their inputs
val uvFormatCheck by tasks.registering(UvTask::class) {
    description = "Fails if the Python sources are not formatted"
    command.set(listOf("run", "--locked", "ruff", "format", "--check"))
}

val uvLint by tasks.registering(UvTask::class) {
    description = "Lints the Python sources"
    command.set(listOf("run", "--locked", "ruff", "check"))
}

val uvTypeCheck by tasks.registering(UvTask::class) {
    description = "Type checks the Python sources with ty"
    // uv check is a preview feature; opting in to it silences the warning that says so
    command.set(listOf("check", "--locked", "--preview-features", "check-command"))
}

val uvTest by tasks.registering(UvTask::class) {
    description = "Runs the Python test suite"
    command.set(listOf("run", "--locked", "pytest", "--junitxml=build/test-results/pytest/TEST-pytest.xml"))
    // The README is an input, as a test runs its example
    inputs.files(stageJvmLibraries, pythonSources, uvProject, testTraces, "README.md")
    outputs.file(layout.buildDirectory.file("test-results/pytest/TEST-pytest.xml"))

    // The test JVM of the runner may take 10 GB, so the two never run side by side
    mustRunAfter(":opendc-sdk:opendc-sdk-runner:test")
}

// A single py3-none-any wheel with the jars inside and no sdist, as the jars are built rather than sources. --clear
// leaves only this wheel in build/distributions, the directory that CI publishes to PyPI as a whole.
val uvBuild by tasks.registering(UvTask::class) {
    description = "Builds the wheel into build/distributions"
    command.set(listOf("build", "--wheel", "--clear", "--out-dir", "build/distributions"))
    inputs.files(stageJvmLibraries, pythonSources, uvProject, "README.md")
    outputs.dir(layout.buildDirectory.dir("distributions"))
}

tasks.assemble {
    dependsOn(uvBuild)
}

tasks.check {
    dependsOn(uvTest, uvLint, uvFormatCheck, uvTypeCheck)
}

tasks.clean {
    delete(stagedJars)
}
