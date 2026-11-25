"""The topology DSL, mirroring `TopologyDsl.kt` and `HostBuilder.kt`."""

from __future__ import annotations

from typing import Any

from opendc.bridge import UNSET, Builder, KotlinObject, detachable, given, sdk

__all__ = ["ClusterBuilder", "DataCenterBuilder", "HostBuilder", "TopologyBuilder", "topology"]


def topology() -> TopologyBuilder:
    """Builds a `TopologySpec` from one or more datacenters."""
    return TopologyBuilder()


class TopologyBuilder(Builder):
    """Collects the datacenters of a `TopologySpec`."""

    __slots__ = ("_datacenters",)

    def __init__(self) -> None:
        self._datacenters: list[DataCenterBuilder] = []

    def datacenter(self, name: str = "DataCenter") -> DataCenterBuilder:
        """Adds a datacenter."""
        datacenter = DataCenterBuilder(name)
        self._datacenters.append(datacenter)
        return detachable(datacenter, lambda: self._datacenters.remove(datacenter))

    def build(self) -> KotlinObject:
        return sdk.TopologySpec(datacenters=self._datacenters)


class DataCenterBuilder(Builder):
    """Collects the clusters, power source and optional battery of a `DataCenterSpec`."""

    __slots__ = ("_name", "_clusters", "_power_source", "_battery")

    def __init__(self, name: str) -> None:
        self._name = name
        self._clusters: list[ClusterBuilder] = []
        self._power_source: Any = UNSET
        self._battery: Any = UNSET

    def cluster(self, count: int = UNSET, name: str = UNSET) -> ClusterBuilder:
        """Adds a cluster, deployed `count` times."""
        cluster = ClusterBuilder(name, count)
        self._clusters.append(cluster)
        return detachable(cluster, lambda: self._clusters.remove(cluster))

    def power_source(
        self,
        name: str = UNSET,
        max_power: KotlinObject = UNSET,
        carbon: KotlinObject | None = UNSET,
    ) -> None:
        """Replaces the power source of the datacenter; a trace reference as `carbon` is a trace-based carbon model."""
        if isinstance(carbon, sdk.ResourceReference):
            carbon = sdk.TraceBasedCarbonSpec(carbon)

        self._power_source = sdk.PowerSourceSpec(**given(name=name, max_power=max_power, carbon=carbon))

    def battery(
        self,
        capacity: KotlinObject,
        charging_speed: KotlinObject,
        policy: KotlinObject,
        name: str = UNSET,
        initial_charge: KotlinObject = UNSET,
        embodied_carbon: float = UNSET,
        expected_lifetime: float = UNSET,
    ) -> None:
        """Sets the battery of the datacenter."""
        self._battery = sdk.BatterySpec(
            capacity=capacity,
            charging_speed=charging_speed,
            policy=policy,
            **given(
                name=name,
                initial_charge=initial_charge,
                embodied_carbon=embodied_carbon,
                expected_lifetime=expected_lifetime,
            ),
        )

    def build(self) -> KotlinObject:
        return sdk.DataCenterSpec(
            clusters=self._clusters,
            name=self._name,
            **given(power_source=self._power_source, battery=self._battery),
        )


class ClusterBuilder(Builder):
    """Collects the hosts of a `ClusterSpec`."""

    __slots__ = ("_name", "_count", "_hosts")

    def __init__(self, name: str, count: int) -> None:
        self._name = name
        self._count = count
        self._hosts: list[HostBuilder] = []

    def host(self, count: int = UNSET, name: str = UNSET) -> HostBuilder:
        """Adds a host, deployed `count` times."""
        host = HostBuilder(name, count)
        self._hosts.append(host)
        return detachable(host, lambda: self._hosts.remove(host))

    def build(self) -> KotlinObject:
        return sdk.ClusterSpec(hosts=self._hosts, **given(name=self._name, count=self._count))


class HostBuilder(Builder):
    """Builds a `HostSpec`; `cpu` and `memory` must be set before it is built."""

    __slots__ = (
        "_name",
        "_count",
        "_cpu",
        "_memory",
        "_gpu",
        "cpu_power_model",
        "gpu_power_model",
        "cpu_distribution",
        "gpu_distribution",
    )

    cpu_power_model: KotlinObject
    """The power model of the CPU."""

    gpu_power_model: KotlinObject
    """The power model of the GPU."""

    cpu_distribution: KotlinObject
    """The policy distributing CPU capacity across tasks."""

    gpu_distribution: KotlinObject
    """The policy distributing GPU capacity across tasks."""

    def __init__(self, name: str, count: int) -> None:
        self._name = name
        self._count = count
        self._cpu: Any = UNSET
        self._memory: Any = UNSET
        self._gpu: Any = UNSET
        self.cpu_power_model = UNSET
        self.gpu_power_model = UNSET
        self.cpu_distribution = UNSET
        self.gpu_distribution = UNSET

    def cpu(
        self,
        core_count: int,
        core_speed: KotlinObject,
        count: int = UNSET,
        vendor: str = UNSET,
        model_name: str = UNSET,
        architecture: str = UNSET,
    ) -> None:
        """Sets the CPU of the host."""
        self._cpu = sdk.CpuSpec(
            core_count=core_count,
            core_speed=core_speed,
            **given(count=count, vendor=vendor, model_name=model_name, architecture=architecture),
        )

    def memory(
        self,
        size: KotlinObject,
        speed: KotlinObject = UNSET,
        vendor: str = UNSET,
        model_name: str = UNSET,
        architecture: str = UNSET,
    ) -> None:
        """Sets the memory of the host."""
        self._memory = sdk.MemorySpec(
            size=size,
            **given(speed=speed, vendor=vendor, model_name=model_name, architecture=architecture),
        )

    def gpu(
        self,
        core_count: int,
        core_speed: KotlinObject,
        count: int = UNSET,
        memory: KotlinObject = UNSET,
        memory_bandwidth: KotlinObject = UNSET,
        vendor: str = UNSET,
        model_name: str = UNSET,
        architecture: str = UNSET,
        virtualization_overhead: KotlinObject = UNSET,
    ) -> None:
        """Sets the GPU of the host."""
        self._gpu = sdk.GpuSpec(
            core_count=core_count,
            core_speed=core_speed,
            **given(
                count=count,
                memory=memory,
                memory_bandwidth=memory_bandwidth,
                vendor=vendor,
                model_name=model_name,
                architecture=architecture,
                virtualization_overhead=virtualization_overhead,
            ),
        )

    def build(self) -> KotlinObject:
        host = "host" if self._name is UNSET else f"host '{self._name}'"
        if self._cpu is UNSET:
            raise ValueError(f"{host} requires a cpu")
        if self._memory is UNSET:
            raise ValueError(f"{host} requires memory")

        return sdk.HostSpec(
            cpu=self._cpu,
            memory=self._memory,
            **given(
                name=self._name,
                count=self._count,
                gpu=self._gpu,
                cpu_power_model=self.cpu_power_model,
                gpu_power_model=self.gpu_power_model,
                cpu_distribution=self.cpu_distribution,
                gpu_distribution=self.gpu_distribution,
            ),
        )
