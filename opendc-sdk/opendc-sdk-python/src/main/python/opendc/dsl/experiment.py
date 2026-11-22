"""The experiment DSL, mirroring `ExperimentDsl.kt`."""

from __future__ import annotations

from typing import Any, overload

from opendc.bridge import UNSET, Builder, KotlinObject, detachable, given, sdk
from opendc.dsl.topology import TopologyBuilder
from opendc.dsl.workload import InlineWorkloadBuilder

__all__ = ["ExperimentBuilder", "ScenarioBuilder", "experiment", "scenario"]


def experiment() -> ExperimentBuilder:
    """Builds an `ExperimentSpec`, whose axes expand into the scenarios to run."""
    return ExperimentBuilder()


def scenario() -> ScenarioBuilder:
    """Builds a single, fully-resolved `ScenarioSpec`."""
    return ScenarioBuilder()


class ExperimentBuilder(Builder):
    """Collects the candidate values of each experiment axis, in the order they are added."""

    __slots__ = (
        "_topologies",
        "_workloads",
        "_allocation_policies",
        "_failure_models",
        "_export_models",
        "_checkpoint_models",
        "_max_num_failures",
        "runs",
        "initial_seed",
        "name",
    )

    runs: int
    """The number of independent repetitions per scenario."""

    initial_seed: int
    """The seed used to derive per-run random seeds."""

    name: str
    """A human-readable name for the experiment."""

    def __init__(self) -> None:
        self._topologies: list[Any] = []
        self._workloads: list[Any] = []
        self._allocation_policies: list[KotlinObject] = []
        self._failure_models: list[KotlinObject] = []
        self._export_models: list[KotlinObject] = []
        self._checkpoint_models: list[KotlinObject | None] = []
        self._max_num_failures: list[int] = []
        self.runs = UNSET
        self.initial_seed = UNSET
        self.name = UNSET

    @overload
    def topology(self) -> TopologyBuilder: ...

    @overload
    def topology(self, topology: KotlinObject | TopologyBuilder) -> None: ...

    def topology(self, topology: Any = UNSET) -> TopologyBuilder | None:
        """Adds a topology: the one given, or the one the returned builder builds."""
        if topology is not UNSET:
            self._topologies.append(topology)
            return None

        builder = TopologyBuilder()
        self._topologies.append(builder)
        return detachable(builder, lambda: self._topologies.remove(builder))

    @overload
    def workload(self) -> InlineWorkloadBuilder: ...

    @overload
    def workload(self, workload: KotlinObject | Builder) -> None: ...

    def workload(self, workload: Any = UNSET) -> InlineWorkloadBuilder | None:
        """Adds a workload: the one given, or the inline workload the returned builder builds."""
        if workload is not UNSET:
            self._workloads.append(workload)
            return None

        builder = InlineWorkloadBuilder()
        self._workloads.append(builder)
        return detachable(builder, lambda: self._workloads.remove(builder))

    def allocation_policy(self, policy: KotlinObject) -> None:
        """Adds an allocation policy."""
        self._allocation_policies.append(policy)

    def failure_model(self, model: KotlinObject) -> None:
        """Adds a failure model."""
        self._failure_models.append(model)

    def export_model(self, model: KotlinObject) -> None:
        """Adds an export model."""
        self._export_models.append(model)

    def checkpoint_model(self, model: KotlinObject | None) -> None:
        """Adds a checkpoint model, or `None` for none."""
        self._checkpoint_models.append(model)

    def max_num_failures(self, value: int) -> None:
        """Adds a maximum number of failures."""
        self._max_num_failures.append(value)

    def build(self) -> KotlinObject:
        # An axis left empty takes the model's default, as the Kotlin DSL does
        return sdk.ExperimentSpec(
            topologies=self._topologies,
            workloads=self._workloads,
            **given(
                allocation_policies=self._allocation_policies or UNSET,
                failure_models=self._failure_models or UNSET,
                export_models=self._export_models or UNSET,
                checkpoint_models=self._checkpoint_models or UNSET,
                max_num_failures=self._max_num_failures or UNSET,
                runs=self.runs,
                initial_seed=self.initial_seed,
                name=self.name,
            ),
        )


class ScenarioBuilder(Builder):
    """Collects the components of a single `ScenarioSpec`."""

    __slots__ = (
        "_topology",
        "_workload",
        "_allocation_policy",
        "export_model",
        "failure_model",
        "checkpoint_model",
        "max_num_failures",
        "seed",
        "id",
        "experiment_name",
    )

    export_model: KotlinObject
    """Controls which results are written and how often."""

    failure_model: KotlinObject
    """The failure-injection model applied during the run."""

    checkpoint_model: KotlinObject | None
    """Optional checkpointing configuration for tasks."""

    max_num_failures: int
    """The maximum number of failures a task may survive before it is terminated."""

    seed: int
    """The random seed of the run."""

    id: int
    """A stable numeric identifier for the scenario."""

    experiment_name: str
    """The name of the output directory of a scenario simulated on its own; `ExperimentSpec.expand` leaves it empty."""

    def __init__(self) -> None:
        self._topology: Any = UNSET
        self._workload: Any = UNSET
        self._allocation_policy: Any = UNSET
        self.export_model = UNSET
        self.failure_model = UNSET
        self.checkpoint_model = UNSET
        self.max_num_failures = UNSET
        self.seed = UNSET
        self.id = UNSET
        self.experiment_name = UNSET

    @overload
    def topology(self) -> TopologyBuilder: ...

    @overload
    def topology(self, topology: KotlinObject | TopologyBuilder) -> None: ...

    def topology(self, topology: Any = UNSET) -> TopologyBuilder | None:
        """Sets the topology: the one given, or the one the returned builder builds."""
        if topology is not UNSET:
            self._topology = topology
            return None

        previous = self._topology
        builder = TopologyBuilder()
        self._topology = builder

        def restore() -> None:
            self._topology = previous

        return detachable(builder, restore)

    @overload
    def workload(self) -> InlineWorkloadBuilder: ...

    @overload
    def workload(self, workload: KotlinObject | Builder) -> None: ...

    def workload(self, workload: Any = UNSET) -> InlineWorkloadBuilder | None:
        """Sets the workload: the one given, or the inline workload the returned builder builds."""
        if workload is not UNSET:
            self._workload = workload
            return None

        previous = self._workload
        builder = InlineWorkloadBuilder()
        self._workload = builder

        def restore() -> None:
            self._workload = previous

        return detachable(builder, restore)

    def allocation_policy(self, policy: KotlinObject) -> None:
        """Sets the allocation policy, a `prefab_scheduler()` unless set."""
        self._allocation_policy = policy

    def build(self) -> KotlinObject:
        if self._topology is UNSET:
            raise ValueError("scenario requires a topology")
        if self._workload is UNSET:
            raise ValueError("scenario requires a workload")

        # A ScenarioSpec requires an allocation policy, which the Kotlin DSL defaults
        allocation_policy = (
            sdk.PrefabAllocationPolicySpec() if self._allocation_policy is UNSET else self._allocation_policy
        )
        return sdk.ScenarioSpec(
            topology=self._topology,
            workload=self._workload,
            allocation_policy=allocation_policy,
            **given(
                export_model=self.export_model,
                failure_model=self.failure_model,
                checkpoint_model=self.checkpoint_model,
                max_num_failures=self.max_num_failures,
                seed=self.seed,
                id=self.id,
                experiment_name=self.experiment_name,
            ),
        )
