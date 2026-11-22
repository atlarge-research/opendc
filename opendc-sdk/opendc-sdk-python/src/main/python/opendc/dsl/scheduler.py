"""The scheduler DSL, mirroring `SchedulerDsl.kt`."""

from __future__ import annotations

from typing import Any

from opendc.bridge import UNSET, Builder, KotlinObject, detachable, given, sdk

__all__ = ["FilterSchedulerBuilder", "TimeshiftBuilder", "filter_scheduler", "prefab_scheduler"]


def prefab_scheduler(name: KotlinObject | str = UNSET) -> KotlinObject:
    """Selects a prefabricated scheduler, a `SchedulerPrefabSpec` or its name."""
    return sdk.PrefabAllocationPolicySpec(**given(prefab_name=name))


def filter_scheduler() -> FilterSchedulerBuilder:
    """Builds a filter-then-weigh scheduler."""
    return FilterSchedulerBuilder()


class FilterSchedulerBuilder(Builder):
    """Collects the configuration of a `FilterAllocationPolicySpec`."""

    __slots__ = ("_filters", "_weighers", "_timeshift")

    def __init__(self) -> None:
        self._filters: list[KotlinObject] = []
        self._weighers: list[KotlinObject] = []
        self._timeshift: Any = UNSET

    def filter(self, filter: KotlinObject) -> None:
        """Adds a host filter."""
        self._filters.append(filter)

    def weigher(self, weigher: KotlinObject) -> None:
        """Adds a host weigher."""
        self._weighers.append(weigher)

    def timeshift(self) -> TimeshiftBuilder:
        """Delays deferrable tasks while the carbon intensity is high."""
        previous = self._timeshift
        timeshift = TimeshiftBuilder()
        self._timeshift = timeshift

        def restore() -> None:
            self._timeshift = previous

        return detachable(timeshift, restore)

    def build(self) -> KotlinObject:
        # Without filters, the spec takes the model's default, the compute filter, as the Kotlin DSL does
        return sdk.FilterAllocationPolicySpec(
            weighers=self._weighers,
            **given(filters=self._filters or UNSET, timeshift=self._timeshift),
        )


class TimeshiftBuilder(Builder):
    """Collects the configuration of a `TimeshiftSpec`."""

    __slots__ = (
        "window_size",
        "forecast",
        "short_forecast_threshold",
        "long_forecast_threshold",
        "forecast_size",
        "task_stopper",
    )

    window_size: int
    """The number of past intensities in the moving average."""

    forecast: bool
    """Whether to compare against the forecast instead of the moving average."""

    short_forecast_threshold: float
    """The forecast quantile below which the intensity is low for short tasks."""

    long_forecast_threshold: float
    """The forecast quantile below which the intensity is low for long tasks."""

    forecast_size: int
    """The number of forecasted intensities to compare against."""

    task_stopper: KotlinObject | None
    """The optional policy that pauses running tasks while the carbon intensity is high."""

    def __init__(self) -> None:
        self.window_size = UNSET
        self.forecast = UNSET
        self.short_forecast_threshold = UNSET
        self.long_forecast_threshold = UNSET
        self.forecast_size = UNSET
        self.task_stopper = UNSET

    def build(self) -> KotlinObject:
        return sdk.TimeshiftSpec(
            **given(
                window_size=self.window_size,
                forecast=self.forecast,
                short_forecast_threshold=self.short_forecast_threshold,
                long_forecast_threshold=self.long_forecast_threshold,
                forecast_size=self.forecast_size,
                task_stopper=self.task_stopper,
            )
        )
