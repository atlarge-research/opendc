"""The DSL builds the specs the constructors build, one test per case of the Kotlin `DslTest`, and mirrors that DSL."""

import inspect
import zipfile
from collections.abc import Callable
from pathlib import Path
from typing import Any

import jpype
import pytest

import opendc
from fixtures import sample_topology, sample_workload
from opendc import dsl, ghz, gib, kwatts, kwh, minutes, watts
from opendc.bridge import Builder, snake_case


def expected_filter_policy() -> opendc.KotlinObject:
    return opendc.FilterAllocationPolicySpec(
        filters=[opendc.ComputeHostFilterSpec, opendc.RamFilterSpec(1.5)],
        weighers=[opendc.CoreRamWeigherSpec(2.0), opendc.RamWeigherSpec()],
    )


def test_topology_dsl_equals_constructor_built_topology() -> None:
    with opendc.topology() as topology:
        with topology.datacenter(name="DC-a") as dc:
            with dc.cluster(name="cluster-a", count=2) as cluster:
                with cluster.host(count=4, name="compute-host") as host:
                    host.cpu(
                        core_count=8, core_speed=ghz(3), count=2, vendor="AMD", model_name="EPYC", architecture="Zen4"
                    )
                    host.memory(size=gib(32), speed=ghz(3), vendor="Samsung")
                    host.cpu_distribution = opendc.FixedSharePolicySpec(0.5)
                    host.gpu_distribution = opendc.EqualSharePolicySpec
                    host.cpu_power_model = opendc.SquarePowerModelSpec(max_power=watts(500), idle_power=watts(100))
            dc.power_source(name="grid", max_power=kwatts(50), carbon=opendc.NamedReference("carbon-trace"))
            dc.battery(
                capacity=kwh(100),
                charging_speed=watts(1000),
                policy=opendc.DoubleBatteryPolicySpec(lower_threshold=100.0, upper_threshold=300.0),
                name="cell",
                initial_charge=kwh(20),
                embodied_carbon=50.0,
                expected_lifetime=10.0,
            )

    assert topology.build() == sample_topology


def test_inline_workload_dsl_equals_constructor_built_workload() -> None:
    with opendc.inline_workload() as workload:
        workload.scaling_policy = opendc.ScalingPolicySpec.Perfect
        with workload.task(
            id=0,
            submission_time=minutes(0),
            duration=minutes(10),
            cpu_core_count=4,
            cpu_capacity=ghz(2),
            memory=gib(8),
        ) as task:
            task.fragment(duration=minutes(5), cpu_usage=ghz(2))
            task.fragment(duration=minutes(5), cpu_usage=ghz(1), gpu_usage=ghz(1), gpu_memory=gib(2))
        with workload.task(
            id=1,
            submission_time=minutes(2),
            duration=minutes(20),
            cpu_core_count=8,
            cpu_capacity=ghz(3),
            memory=gib(16),
            deferrable=True,
            deadline=minutes(60),
            parents=[0],
        ) as task:
            task.fragment(duration=minutes(20), cpu_usage=ghz(3))

    assert workload.build() == sample_workload


def test_filter_scheduler_dsl_equals_constructor_built_policy() -> None:
    with opendc.filter_scheduler() as scheduler:
        scheduler.filter(opendc.ComputeHostFilterSpec)
        scheduler.filter(opendc.RamFilterSpec(1.5))
        scheduler.weigher(opendc.CoreRamWeigherSpec(2.0))
        scheduler.weigher(opendc.RamWeigherSpec())

    assert scheduler.build() == expected_filter_policy()


def test_filter_scheduler_dsl_without_filters_defaults_to_the_compute_filter() -> None:
    with opendc.filter_scheduler() as scheduler:
        scheduler.weigher(opendc.RamWeigherSpec())

    assert scheduler.build() == opendc.FilterAllocationPolicySpec(
        [opendc.ComputeHostFilterSpec], [opendc.RamWeigherSpec()]
    )


def test_timeshift_dsl_equals_constructor_built_policy() -> None:
    with opendc.filter_scheduler() as scheduler:
        scheduler.filter(opendc.VCpuFilterSpec(2.0))
        scheduler.weigher(opendc.VCpuWeigherSpec())
        with scheduler.timeshift() as timeshift:
            timeshift.window_size = 100
            timeshift.forecast = False
            timeshift.short_forecast_threshold = 0.1
            timeshift.long_forecast_threshold = 0.5
            timeshift.forecast_size = 12
            timeshift.task_stopper = opendc.TaskStopperSpec(window_size=50)

    expected = opendc.FilterAllocationPolicySpec(
        filters=[opendc.VCpuFilterSpec(2.0)],
        weighers=[opendc.VCpuWeigherSpec()],
        timeshift=opendc.TimeshiftSpec(
            window_size=100,
            forecast=False,
            short_forecast_threshold=0.1,
            long_forecast_threshold=0.5,
            forecast_size=12,
            task_stopper=opendc.TaskStopperSpec(window_size=50),
        ),
    )
    assert scheduler.build() == expected


def test_timeshift_dsl_without_settings_uses_the_defaults() -> None:
    with opendc.filter_scheduler() as scheduler:
        scheduler.timeshift()

    assert scheduler.build() == opendc.FilterAllocationPolicySpec(timeshift=opendc.TimeshiftSpec())


def test_prefab_scheduler_dsl_equals_constructor_built_policy() -> None:
    core_mem = opendc.SchedulerPrefabSpec.CoreMem

    assert opendc.prefab_scheduler(core_mem) == opendc.PrefabAllocationPolicySpec(core_mem)
    assert opendc.prefab_scheduler() == opendc.PrefabAllocationPolicySpec(opendc.SchedulerPrefabSpec.Mem)


def test_scenario_dsl_equals_constructor_built_scenario() -> None:
    with opendc.scenario() as scenario:
        scenario.id = 7
        scenario.experiment_name = "baseline"
        scenario.topology(sample_topology)
        scenario.workload(sample_workload)
        scenario.allocation_policy(expected_filter_policy())
        scenario.export_model = opendc.ExportSpec(export_interval=minutes(15), print_frequency=12)
        scenario.failure_model = opendc.TraceBasedFailureSpec(opendc.NamedReference("failure-trace"))
        scenario.max_num_failures = 5
        scenario.seed = 42

    expected = opendc.ScenarioSpec(
        topology=sample_topology,
        workload=sample_workload,
        allocation_policy=expected_filter_policy(),
        export_model=opendc.ExportSpec(export_interval=minutes(15), print_frequency=12),
        failure_model=opendc.TraceBasedFailureSpec(opendc.NamedReference("failure-trace")),
        max_num_failures=5,
        seed=42,
        id=7,
        experiment_name="baseline",
    )
    assert scenario.build() == expected


def test_experiment_dsl_equals_constructor_built_experiment() -> None:
    with opendc.experiment() as experiment:
        experiment.name = "sweep"
        experiment.runs = 5
        experiment.initial_seed = 1
        experiment.topology(sample_topology)
        experiment.workload(sample_workload)
        experiment.allocation_policy(opendc.prefab_scheduler(opendc.SchedulerPrefabSpec.Mem))
        experiment.allocation_policy(opendc.prefab_scheduler(opendc.SchedulerPrefabSpec.CoreMem))
        experiment.failure_model(opendc.NoFailureSpec)
        experiment.export_model(opendc.ExportSpec())
        experiment.max_num_failures(5)
        experiment.max_num_failures(10)

    expected = opendc.ExperimentSpec(
        topologies=[sample_topology],
        workloads=[sample_workload],
        allocation_policies=[
            opendc.PrefabAllocationPolicySpec(opendc.SchedulerPrefabSpec.Mem),
            opendc.PrefabAllocationPolicySpec(opendc.SchedulerPrefabSpec.CoreMem),
        ],
        failure_models=[opendc.NoFailureSpec],
        max_num_failures=[5, 10],
        checkpoint_models=[None],
        export_models=[opendc.ExportSpec()],
        runs=5,
        initial_seed=1,
        name="sweep",
    )
    assert experiment.build() == expected


def test_unit_helpers_wrap_the_expected_values() -> None:
    assert opendc.Frequency.ofGHz(2) == ghz(2)
    assert ghz(2).toMHz() == 2000.0
    assert ghz(2) == opendc.mhz(2000)

    assert opendc.DataSize.ofGiB(4) == gib(4)
    assert gib(4).toMiB() == 4096.0
    assert gib(1) == opendc.mib(1024)

    assert opendc.Power.ofWatts(250) == watts(250)
    assert watts(250).toWatts() == 250.0
    assert kwatts(1) == watts(1000)

    assert opendc.TimeDelta.ofMin(5) == minutes(5)
    assert minutes(5).toMs() == 300000.0
    assert opendc.hours(1) == minutes(60)


def test_a_star_import_keeps_the_builtin_bytes() -> None:
    namespace: dict[str, Any] = {}

    exec("from opendc import *\nfrom opendc.dsl import *\nfrom opendc.dsl.units import *", namespace)

    assert "bytes" not in namespace
    assert opendc.bytes(1048576) == dsl.bytes(1048576) == opendc.mib(1)


def test_datacenter_and_cluster_default_their_names() -> None:
    with opendc.topology() as topology:
        with topology.datacenter() as dc:
            with dc.cluster() as cluster:
                with cluster.host() as host:
                    host.cpu(1, ghz(1))
                    host.memory(gib(1))

    datacenter = topology.build().datacenters[0]
    assert datacenter.name == "DataCenter"
    assert datacenter.clusters[0].name == "Cluster"
    assert datacenter.clusters[0].hosts[0].name == "Host"


def test_host_without_cpu_or_memory_fails_to_build() -> None:
    with opendc.topology() as topology:
        with topology.datacenter() as dc:
            with dc.cluster() as cluster:
                with cluster.host(name="h1") as host:
                    host.memory(gib(1))

    with pytest.raises(ValueError, match="host 'h1' requires a cpu"):
        topology.build()


def test_scenario_without_topology_fails_to_build() -> None:
    with opendc.scenario() as scenario:
        scenario.workload(sample_workload)

    with pytest.raises(ValueError, match="scenario requires a topology"):
        scenario.build()


def test_scenario_without_allocation_policy_takes_the_prefab_one() -> None:
    with opendc.scenario() as scenario:
        scenario.topology(sample_topology)
        scenario.workload(sample_workload)

    expected = opendc.ScenarioSpec(sample_topology, sample_workload, opendc.PrefabAllocationPolicySpec())
    assert scenario.build() == expected


def test_experiment_without_topologies_fails_validation() -> None:
    with opendc.experiment() as experiment:
        experiment.workload(sample_workload)

    issues = experiment.build().validate()
    assert [(issue.path, issue.message) for issue in issues] == [("topologies", "must not be empty")]


def test_experiment_axes_keep_their_declaration_order() -> None:
    prefabs = [
        opendc.SchedulerPrefabSpec.CoreMem,
        opendc.SchedulerPrefabSpec.Mem,
        opendc.SchedulerPrefabSpec.ActiveServers,
    ]
    with opendc.experiment() as experiment:
        experiment.topology(sample_topology)
        experiment.workload(sample_workload)
        for prefab in prefabs:
            experiment.allocation_policy(opendc.prefab_scheduler(prefab))

    scenarios = experiment.build().expand()
    assert [scenario.allocation_policy.prefab_name for scenario in scenarios] == prefabs
    assert [scenario.id for scenario in scenarios] == [0, 1, 2]


def test_misspelled_dsl_argument_or_attribute_fails_on_its_line() -> None:
    with opendc.topology() as topology, topology.datacenter() as dc, dc.cluster() as cluster, cluster.host() as host:
        with pytest.raises(TypeError, match="core_cont"):
            host.cpu(core_cont=8, core_speed=ghz(3))  # type: ignore[call-arg]
        with pytest.raises(AttributeError):
            host.cpu_distributon = opendc.EqualSharePolicySpec  # type: ignore[attr-defined]


def test_a_block_that_raises_adds_nothing() -> None:
    with opendc.topology() as topology, topology.datacenter() as dc:
        with dc.cluster(name="kept") as cluster, cluster.host() as host:
            host.cpu(1, ghz(1))
            host.memory(gib(1))
        with pytest.raises(RuntimeError), dc.cluster(name="failed"):
            raise RuntimeError("the block fails")

    assert [cluster.name for cluster in topology.build().datacenters[0].clusters] == ["kept"]


def test_a_replacing_block_that_raises_keeps_the_previous_value() -> None:
    with opendc.scenario() as scenario:
        scenario.topology(sample_topology)
        scenario.workload(sample_workload)
        with pytest.raises(RuntimeError), scenario.topology():
            raise RuntimeError("the block fails")

    assert scenario.build().topology == sample_topology


def test_a_property_reads_unset_until_assigned() -> None:
    with opendc.inline_workload() as workload:
        assert workload.scaling_policy is dsl.UNSET
        workload.scaling_policy = opendc.ScalingPolicySpec.Perfect
        assert workload.build().scaling_policy == opendc.ScalingPolicySpec.Perfect
        workload.scaling_policy = dsl.UNSET

    assert workload.build() == opendc.InlineWorkloadSpec(tasks=[])


def kotlin_dsl_classes() -> list[Any]:
    """The top-level classes of the Kotlin DSL package, read from the jar of the model."""
    location = jpype.JClass("org.opendc.sdk.model.dsl.ExperimentBuilder").class_.getProtectionDomain().getCodeSource()
    jar = Path(str(jpype.JClass("java.nio.file.Paths").get(location.getLocation().toURI())))
    with zipfile.ZipFile(jar) as archive:
        names = archive.namelist()

    prefix = "org/opendc/sdk/model/dsl/"
    return [
        jpype.JClass(name[: -len(".class")].replace("/", ".")).class_
        for name in names
        if name.startswith(prefix) and name.endswith(".class") and "$" not in name
    ]


def kotlin_parameters(callable_: Any) -> list[tuple[str, bool]]:
    """The value parameters of a Kotlin callable that Python mirrors, with whether each is optional."""
    value = jpype.JClass("kotlin.reflect.KParameter$Kind").VALUE

    # The builder lambdas are left out, as Python's `with` blocks stand for them
    return [
        (snake_case(str(parameter.getName())), bool(parameter.isOptional()))
        for parameter in callable_.getParameters()
        if parameter.getKind() == value and "->" not in str(parameter.getType())
    ]


def python_parameters(function: Callable[..., Any]) -> list[tuple[str, bool]]:
    """The parameters of a Python function or method, with whether each is optional."""
    signature = inspect.signature(function)
    return [
        (name, parameter.default is not inspect.Parameter.empty)
        for name, parameter in signature.parameters.items()
        if name != "self"
    ]


def assert_mirrors_top_level_declarations(file_class: Any) -> None:
    """Asserts that `opendc.dsl` has each public top-level function and unit of a Kotlin file, with its parameters."""
    public = jpype.JClass("kotlin.reflect.KVisibility").PUBLIC
    extension_receiver = jpype.JClass("kotlin.reflect.KParameter$Kind").EXTENSION_RECEIVER
    declarations = jpype.JClass("kotlin.jvm.internal.Reflection").getOrCreateKotlinPackage(file_class).getMembers()

    for declaration in declarations:
        if declaration.getVisibility() != public:
            continue
        name = str(declaration.getName())
        function = getattr(dsl, snake_case(name), None)
        assert function is not None, f"opendc.dsl lacks {name} of {file_class.getSimpleName()}"

        if declaration.getParameters()[0].getKind() == extension_receiver:
            assert [parameter for parameter, _ in python_parameters(function)] == ["amount"]
        else:
            assert python_parameters(function) == kotlin_parameters(declaration), f"{name} differs from Kotlin"


def assert_mirrors_builder(builder_class: Any) -> None:
    """Asserts that `opendc.dsl` has the Kotlin builder, with its public functions and mutable properties."""
    name = str(builder_class.getSimpleName())
    builder = getattr(dsl, name, None)
    assert builder is not None, f"opendc.dsl lacks {name}"

    kotlin_class = jpype.JClass("kotlin.jvm.JvmClassMappingKt").getKotlinClass(builder_class)
    assert_mirrors_functions(builder, kotlin_class)
    assert_mirrors_properties(builder, kotlin_class)


def assert_mirrors_functions(builder: type[Builder], kotlin_class: Any) -> None:
    """Asserts that `builder` has each public function of `kotlin_class`, with the parameters of all its overloads."""
    public = jpype.JClass("kotlin.reflect.KVisibility").PUBLIC
    overloads: dict[str, list[list[tuple[str, bool]]]] = {}
    for function in jpype.JClass("kotlin.reflect.full.KClasses").getMemberFunctions(kotlin_class):
        if function.getVisibility() == public and str(function.getName()) not in ("equals", "hashCode", "toString"):
            overloads.setdefault(snake_case(str(function.getName())), []).append(kotlin_parameters(function))

    for name, signatures in overloads.items():
        method = getattr(builder, name, None)
        assert method is not None, f"{builder.__name__} lacks {name}"

        mirrored = python_parameters(method)
        if len(signatures) == 1:
            assert mirrored == signatures[0], f"{builder.__name__}.{name} differs from Kotlin"
        else:
            kotlin_names = {parameter for signature in signatures for parameter, _ in signature}
            python_names = {parameter for parameter, _ in mirrored}
            assert kotlin_names <= python_names, f"{builder.__name__}.{name} lacks parameters of its Kotlin overloads"


def assert_mirrors_properties(builder: type[Builder], kotlin_class: Any) -> None:
    """Asserts that the public slots of `builder` are the public mutable properties of `kotlin_class`."""
    public = jpype.JClass("kotlin.reflect.KVisibility").PUBLIC
    mutable_property = jpype.JClass("kotlin.reflect.KMutableProperty")
    kotlin_properties = {
        snake_case(str(property_.getName()))
        for property_ in jpype.JClass("kotlin.reflect.full.KClasses").getMemberProperties(kotlin_class)
        if property_.getVisibility() == public and isinstance(property_, mutable_property)
    }

    python_properties = {slot for slot in builder.__slots__ if not slot.startswith("_")}
    assert python_properties == kotlin_properties, f"{builder.__name__} differs from Kotlin in its properties"


def test_dsl_mirrors_every_kotlin_dsl_function_parameter_and_property() -> None:
    for java_class in kotlin_dsl_classes():
        if str(java_class.getSimpleName()).endswith("Kt"):
            assert_mirrors_top_level_declarations(java_class)
        elif not java_class.isAnnotation():
            assert_mirrors_builder(java_class)
