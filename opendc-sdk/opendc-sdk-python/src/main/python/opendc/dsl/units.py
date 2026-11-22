"""The unit helpers of the DSL, mirroring `Units.kt`: `ghz(3)` is Kotlin's `3.ghz`."""

from opendc.bridge import KotlinObject, sdk

# bytes stays out of __all__, so that a star import keeps the builtin
__all__ = [
    "gbps",
    "ghz",
    "gib",
    "hours",
    "kibps",
    "kwatts",
    "kwh",
    "mhz",
    "mib",
    "minutes",
    "ms",
    "seconds",
    "watts",
]


def ghz(amount: float) -> KotlinObject:
    """`amount` gigahertz, a `Frequency`."""
    return sdk.Frequency.ofGHz(amount)


def mhz(amount: float) -> KotlinObject:
    """`amount` megahertz, a `Frequency`."""
    return sdk.Frequency.ofMHz(amount)


def gib(amount: float) -> KotlinObject:
    """`amount` gibibytes, a `DataSize`."""
    return sdk.DataSize.ofGiB(amount)


def mib(amount: float) -> KotlinObject:
    """`amount` mebibytes, a `DataSize`."""
    return sdk.DataSize.ofMiB(amount)


def bytes(amount: float) -> KotlinObject:
    """`amount` bytes, a `DataSize`."""
    return sdk.DataSize.ofBytes(amount)


def watts(amount: float) -> KotlinObject:
    """`amount` watts, a `Power`."""
    return sdk.Power.ofWatts(amount)


def kwatts(amount: float) -> KotlinObject:
    """`amount` kilowatts, a `Power`."""
    return sdk.Power.ofKWatts(amount)


def kwh(amount: float) -> KotlinObject:
    """`amount` kilowatt-hours, an `Energy`."""
    return sdk.Energy.ofKWh(amount)


def gbps(amount: float) -> KotlinObject:
    """`amount` gigabytes per second, a `DataRate`."""
    return sdk.DataRate.ofGBps(amount)


def kibps(amount: float) -> KotlinObject:
    """`amount` kibibits per second, a `DataRate`."""
    return sdk.DataRate.ofKibps(amount)


def ms(amount: float) -> KotlinObject:
    """`amount` milliseconds, a `TimeDelta`."""
    return sdk.TimeDelta.ofMs(amount)


def seconds(amount: float) -> KotlinObject:
    """`amount` seconds, a `TimeDelta`."""
    return sdk.TimeDelta.ofSec(amount)


def minutes(amount: float) -> KotlinObject:
    """`amount` minutes, a `TimeDelta`."""
    return sdk.TimeDelta.ofMin(amount)


def hours(amount: float) -> KotlinObject:
    """`amount` hours, a `TimeDelta`."""
    return sdk.TimeDelta.ofHours(amount)
