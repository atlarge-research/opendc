"""The workload DSL, mirroring `WorkloadDsl.kt`."""

from __future__ import annotations

from collections.abc import Sequence

from opendc.bridge import UNSET, Builder, KotlinObject, detachable, given, sdk

__all__ = ["InlineWorkloadBuilder", "TaskBuilder", "inline_workload", "trace_workload"]


def trace_workload(
    source: KotlinObject,
    sample_fraction: float = UNSET,
    submission_time: str | None = UNSET,
    scaling_policy: KotlinObject = UNSET,
    defer_all: bool = UNSET,
) -> KotlinObject:
    """Builds a `TraceWorkloadSpec` backed by the trace that `source` references."""
    return sdk.TraceWorkloadSpec(
        source=source,
        **given(
            sample_fraction=sample_fraction,
            submission_time=submission_time,
            scaling_policy=scaling_policy,
            defer_all=defer_all,
        ),
    )


def inline_workload() -> InlineWorkloadBuilder:
    """Builds an `InlineWorkloadSpec` from tasks defined in place."""
    return InlineWorkloadBuilder()


class InlineWorkloadBuilder(Builder):
    """Collects the tasks of an `InlineWorkloadSpec`."""

    __slots__ = ("_tasks", "scaling_policy")

    scaling_policy: KotlinObject
    """How tasks react to resource contention."""

    def __init__(self) -> None:
        self._tasks: list[TaskBuilder] = []
        self.scaling_policy = UNSET

    def task(
        self,
        id: int,
        submission_time: KotlinObject,
        duration: KotlinObject,
        cpu_core_count: int,
        cpu_capacity: KotlinObject,
        memory: KotlinObject,
        gpu_core_count: int = UNSET,
        gpu_capacity: KotlinObject = UNSET,
        gpu_memory: KotlinObject = UNSET,
        deferrable: bool = UNSET,
        deadline: KotlinObject = UNSET,
        parents: Sequence[int] = UNSET,
        children: Sequence[int] = UNSET,
    ) -> TaskBuilder:
        """Adds a task, whose fragments the returned builder collects."""
        task = TaskBuilder(
            id=id,
            submission_time=submission_time,
            duration=duration,
            cpu_core_count=cpu_core_count,
            cpu_capacity=cpu_capacity,
            memory=memory,
            **given(
                gpu_core_count=gpu_core_count,
                gpu_capacity=gpu_capacity,
                gpu_memory=gpu_memory,
                deferrable=deferrable,
                deadline=deadline,
                parents=parents,
                children=children,
            ),
        )
        self._tasks.append(task)
        return detachable(task, lambda: self._tasks.remove(task))

    def build(self) -> KotlinObject:
        return sdk.InlineWorkloadSpec(tasks=self._tasks, **given(scaling_policy=self.scaling_policy))


class TaskBuilder(Builder):
    """Collects the execution fragments of a `TaskSpec`."""

    __slots__ = ("_task", "_fragments")

    def __init__(self, **task: object) -> None:
        self._task = task
        self._fragments: list[KotlinObject] = []

    def fragment(
        self,
        duration: KotlinObject,
        cpu_usage: KotlinObject,
        gpu_usage: KotlinObject = UNSET,
        gpu_memory: KotlinObject = UNSET,
    ) -> None:
        """Adds an execution fragment."""
        self._fragments.append(
            sdk.TaskFragmentSpec(
                duration=duration,
                cpu_usage=cpu_usage,
                **given(gpu_usage=gpu_usage, gpu_memory=gpu_memory),
            )
        )

    def build(self) -> KotlinObject:
        return sdk.TaskSpec(fragments=self._fragments, **self._task)
