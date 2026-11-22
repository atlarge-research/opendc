"""The bridge to the Kotlin SDK: the JVM, the conversion of values, and the wrappers of Kotlin classes and objects."""

from __future__ import annotations

import functools
import inspect
import numbers
import os
import re
import threading
from collections.abc import Callable, Iterable, Mapping
from pathlib import Path
from typing import Any, TypeVar

import jpype
import numpy as np

__all__ = [
    "UNSET",
    "Builder",
    "KotlinClass",
    "KotlinObject",
    "detachable",
    "given",
    "sdk",
    "sdk_member",
    "sdk_names",
    "start_jvm",
    "to_java",
    "to_python",
]

JAR_DIRECTORY = Path(__file__).parent / "jars"
"""The directory of the jars of the SDK and its dependencies, which Gradle stages."""

JVM_START_LOCK = threading.Lock()
"""Lets concurrent first uses of the SDK start a single JVM."""

JAVA_VERSION = 21
"""The Java version the SDK is compiled for, the oldest that runs it."""


def start_jvm(*options: str) -> None:
    """Starts the JVM with options such as "-Xmx8g"; without this call, the first use of the SDK starts it."""
    with JVM_START_LOCK:
        if jpype.isJVMStarted():
            if options:
                raise RuntimeError("The JVM already runs; call start_jvm() before anything else uses the SDK")
            return

        jars = sorted(JAR_DIRECTORY.glob("*.jar"))
        if not jars:
            raise RuntimeError(
                f"No SDK jars in {JAR_DIRECTORY}; run ./gradlew :opendc-sdk:opendc-sdk-python:stageJvmLibraries"
            )

        # In a script JPype would let Ctrl-C end the process, before Server.simulate could cancel its run
        jpype.startJVM(*options, classpath=[str(jar) for jar in jars], interrupt=False)

        # Checked here rather than by JPype, whose check rejects the version strings of early-access builds
        version = jpype.JClass("java.lang.Runtime").version().feature()
        if version < JAVA_VERSION:
            raise RuntimeError(
                f"The SDK needs Java {JAVA_VERSION} or newer, but the JVM it found (JAVA_HOME) is Java {version}"
            )


class JvmClasses:
    """The JVM classes this module uses, looked up once the JVM runs."""

    def __init__(self) -> None:
        self.interop = jpype.JClass("org.opendc.sdk.python.PythonInterop")
        self.binding_failure = jpype.JClass("org.opendc.sdk.python.ArgumentBindingException")
        self.value = jpype.JClass("org.opendc.sdk.python.Attribute$Value")
        self.member_function = jpype.JClass("org.opendc.sdk.python.Attribute$MemberFunction")
        self.top_level_function = jpype.JClass("org.opendc.sdk.python.Attribute$TopLevelFunction")

        self.boolean = jpype.JClass("java.lang.Boolean")
        self.number = jpype.JClass("java.lang.Number")
        self.real = (jpype.JClass("java.lang.Double"), jpype.JClass("java.lang.Float"))
        self.character = jpype.JClass("java.lang.Character")
        self.paths = (jpype.JClass("java.nio.file.Path"), jpype.JClass("java.io.File"))
        self.collection = jpype.JClass("java.util.Collection")
        self.map = jpype.JClass("java.util.Map")
        self.array_list = jpype.JClass("java.util.ArrayList")
        self.linked_hash_set = jpype.JClass("java.util.LinkedHashSet")
        self.linked_hash_map = jpype.JClass("java.util.LinkedHashMap")


@functools.cache
def jvm_classes() -> JvmClasses:
    """The JVM classes this module uses, starting the JVM if it does not run yet."""
    start_jvm()
    return JvmClasses()


class Unset:
    """The type of `UNSET`."""

    def __repr__(self) -> str:
        return "UNSET"


UNSET: Any = Unset()
"""Marks a DSL argument or property as not given, so that Kotlin's default applies to it."""


def given(**arguments: Any) -> dict[str, Any]:
    """The `arguments` that are not `UNSET`."""
    return {name: value for name, value in arguments.items() if value is not UNSET}


class Builder:
    """A DSL builder, which stands for the spec it builds wherever one is expected."""

    __slots__ = ("_detach",)

    def build(self) -> KotlinObject:
        """Builds the spec this builder collects."""
        raise NotImplementedError

    def __enter__(self) -> Any:
        return self

    def __exit__(self, exception_type: type[BaseException] | None, *exception: object) -> None:
        # A block that raises takes its builder back out of its parent, as in Kotlin; only `detachable` gives one
        if exception_type is not None and hasattr(self, "_detach"):
            self._detach()


ChildBuilder = TypeVar("ChildBuilder", bound=Builder)


def detachable(child: ChildBuilder, detach: Callable[[], None]) -> ChildBuilder:
    """Returns `child`, set to call `detach` to leave its parent if its `with` block raises."""
    child._detach = detach
    return child


def to_java(value: Any) -> Any:
    """Converts a Python value to the value passed to Kotlin, which coerces it further to the parameter's type."""
    if isinstance(value, KotlinObject):
        return value._jvm
    if isinstance(value, Builder):
        return value.build()._jvm

    if value is None or isinstance(value, (bool, str)):
        return value
    if isinstance(value, (np.generic, np.ndarray)):
        return to_java(value.tolist())
    if isinstance(value, numbers.Integral):
        return int(value)
    if isinstance(value, numbers.Real):
        return float(value)
    if isinstance(value, os.PathLike):
        return os.fspath(value)

    jvm = jvm_classes()
    if isinstance(value, (list, tuple)):
        return add_converted(jvm.array_list(), value)
    if isinstance(value, (set, frozenset)):
        return add_converted(jvm.linked_hash_set(), value)
    if isinstance(value, dict):
        return put_converted(jvm.linked_hash_map(), value)

    if isinstance(value, jpype.JObject):
        return value
    raise TypeError(f"A {type(value).__module__}.{type(value).__qualname__} cannot be passed to the SDK")


def add_converted(collection: Any, values: Iterable[Any]) -> Any:
    """Adds the `values`, converted, to the Java `collection` and returns it."""
    for value in values:
        collection.add(to_java(value))
    return collection


def put_converted(java_map: Any, entries: Mapping[Any, Any]) -> Any:
    """Puts the `entries`, converted, into the Java `java_map` and returns it."""
    for key, value in entries.items():
        java_map.put(to_java(key), to_java(value))
    return java_map


def to_python(value: Any) -> Any:
    """Converts a Kotlin value: paths to `Path`, collections to lists, maps to dicts, the rest to `KotlinObject`."""
    jvm = jvm_classes()
    if value is None:
        return None
    if isinstance(value, jvm.boolean):
        return bool(value)
    if isinstance(value, jvm.number):
        return float(value) if isinstance(value, jvm.real) else int(value)
    if isinstance(value, (jpype.JString, jvm.character)):
        return str(value)
    if isinstance(value, jvm.paths):
        return Path(str(value))

    if isinstance(value, (jpype.JArray, jvm.collection)):
        return [to_python(element) for element in value]
    if isinstance(value, jvm.map):
        return {to_python(key): to_python(element) for key, element in value.items()}

    if isinstance(value, (bool, int, float, str)):
        return value
    return KotlinObject(value)


def exception_message(exception: Any) -> str:
    """The message of a Java exception, without the class name that `str()` adds."""
    return str(exception.getMessage())


def call_kotlin(call: Callable[[Any], Any]) -> Any:
    """Runs `call` on `PythonInterop` and converts its result; arguments that fit no declaration raise a `TypeError`."""
    jvm = jvm_classes()
    try:
        return to_python(call(jvm.interop))
    except jvm.binding_failure as failure:
        raise TypeError(exception_message(failure)) from None


def resolve_attribute(lookup: Callable[[Any], Any]) -> Any:
    """Runs `lookup` on `PythonInterop` and wraps the attribute it finds; an unknown name raises an `AttributeError`."""
    jvm = jvm_classes()
    try:
        attribute = lookup(jvm.interop)
    except jvm.binding_failure as failure:
        raise AttributeError(exception_message(failure)) from None

    if isinstance(attribute, jvm.value):
        return to_python(attribute.getValue())
    if isinstance(attribute, jvm.member_function):
        return member_function(attribute.getReceiver(), str(attribute.getName()))
    if isinstance(attribute, jvm.top_level_function):
        return top_level_function(str(attribute.getName()))
    return kotlin_class(attribute.getType())


def java_arguments(args: tuple[Any, ...], kwargs: dict[str, Any]) -> tuple[Any, Any]:
    """The positional and named arguments of a call, as the Java list and map that `PythonInterop` takes."""
    return to_java(list(args)), to_java(kwargs)


def member_function(receiver: Any, name: str) -> Callable[..., Any]:
    """A Python function that calls the Kotlin function `name` of `receiver`."""

    def function(*args: Any, **kwargs: Any) -> Any:
        return call_kotlin(lambda interop: interop.call(receiver, name, *java_arguments(args, kwargs)))

    function.__name__ = name
    return function


def top_level_function(name: str) -> Callable[..., Any]:
    """A Python function that calls the top-level Kotlin function `name` of the SDK."""

    def function(*args: Any, **kwargs: Any) -> Any:
        return call_kotlin(lambda interop: interop.callTopLevel(name, *java_arguments(args, kwargs)))

    function.__name__ = name
    return function


@functools.cache
def kotlin_members(type_name: str) -> Any:
    """The members of the JVM class named `type_name` that Python lists and documents."""
    return jvm_classes().interop.describe(jpype.JClass(type_name).class_)


def snake_case(name: str) -> str:
    """The Kotlin `name` in snake_case, such as `core_speed` for `coreSpeed`."""
    return re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name).lower()


class KotlinObject:
    """An object of the SDK, whose Kotlin properties and functions are attributes, by Kotlin or snake_case name."""

    __slots__ = ("_jvm",)

    def __init__(self, jvm: Any) -> None:
        object.__setattr__(self, "_jvm", jvm)

    def __getattr__(self, name: str) -> Any:
        # Underscored names are Python's own, such as the `__array__` that numpy looks for, never Kotlin's
        if name.startswith("_"):
            raise AttributeError(name)
        return resolve_attribute(lambda interop: interop.attribute(self._jvm, name))

    def __setattr__(self, name: str, value: Any) -> None:
        raise AttributeError(f"SDK objects do not change; use copy({name}=...) for a changed one")

    def __dir__(self) -> list[str]:
        members = kotlin_members(str(self._jvm.getClass().getName()))
        return [str(name) for name in members.getInstanceAttributes()]

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, KotlinObject):
            return NotImplemented
        return bool(self._jvm.equals(other._jvm))

    def __hash__(self) -> int:
        return int(self._jvm.hashCode())

    def __repr__(self) -> str:
        return str(self._jvm.toString())

    def __copy__(self) -> KotlinObject:
        return self

    def __deepcopy__(self, memo: dict[int, Any]) -> KotlinObject:
        return self

    def _operator(self, name: str, *operands: Any) -> Any:
        """Calls the Kotlin operator function `name`, or returns `NotImplemented` when none fits the operands."""
        try:
            return call_kotlin(lambda interop: interop.call(self._jvm, name, *java_arguments(operands, {})))
        except (TypeError, AttributeError):
            return NotImplemented

    def __add__(self, other: Any) -> Any:
        return self._operator("plus", other)

    def __sub__(self, other: Any) -> Any:
        return self._operator("minus", other)

    def __mul__(self, other: Any) -> Any:
        return self._operator("times", other)

    def __rmul__(self, other: Any) -> Any:
        return self._operator("times", other)

    def __truediv__(self, other: Any) -> Any:
        return self._operator("div", other)

    def __neg__(self) -> Any:
        negated = self._operator("unaryMinus")
        if negated is NotImplemented:
            raise TypeError(f"bad operand type for unary -: '{self._jvm.getClass().getSimpleName()}'")
        return negated

    def _compare(self, other: Any) -> Any:
        return self._operator("compareTo", other)

    def __lt__(self, other: Any) -> Any:
        order = self._compare(other)
        return order if order is NotImplemented else order < 0

    def __le__(self, other: Any) -> Any:
        order = self._compare(other)
        return order if order is NotImplemented else order <= 0

    def __gt__(self, other: Any) -> Any:
        order = self._compare(other)
        return order if order is NotImplemented else order > 0

    def __ge__(self, other: Any) -> Any:
        order = self._compare(other)
        return order if order is NotImplemented else order >= 0


class KotlinClass:
    """A class of the SDK, called to construct an instance; its enum constants and companion members are attributes."""

    __slots__ = ("_type",)

    def __init__(self, type: Any) -> None:
        self._type = type

    def __call__(self, *args: Any, **kwargs: Any) -> Any:
        return call_kotlin(lambda interop: interop.construct(self._type, *java_arguments(args, kwargs)))

    def __getattr__(self, name: str) -> Any:
        if name.startswith("_"):
            raise AttributeError(name)
        return resolve_attribute(lambda interop: interop.classAttribute(self._type, name))

    def __instancecheck__(self, instance: Any) -> bool:
        return isinstance(instance, KotlinObject) and bool(self._type.isInstance(instance._jvm))

    def __dir__(self) -> list[str]:
        return [str(name) for name in self._describe().getClassAttributes()]

    def __repr__(self) -> str:
        return f"<Kotlin class {self._type.getName()}>"

    @property
    def __name__(self) -> str:
        return str(self._type.getSimpleName())

    @property
    def __signature__(self) -> inspect.Signature:
        """The constructor's parameters in snake_case, keyword-only from the first defaulted one on."""
        parameters = []
        keyword_only = False

        for parameter in self._describe().getConstructorParameters():
            # Kotlin lets a required parameter follow a defaulted one, which Python allows only for keyword-only ones
            keyword_only = keyword_only or bool(parameter.isOptional())
            parameters.append(
                inspect.Parameter(
                    snake_case(str(parameter.getName())),
                    inspect.Parameter.KEYWORD_ONLY if keyword_only else inspect.Parameter.POSITIONAL_OR_KEYWORD,
                    default=... if parameter.isOptional() else inspect.Parameter.empty,
                    annotation=str(parameter.getType()),
                )
            )

        return inspect.Signature(parameters)

    def _describe(self) -> Any:
        return kotlin_members(str(self._type.getName()))


KOTLIN_CLASSES: dict[str, KotlinClass] = {}
"""The wrapper of each JVM class that has one, by class name."""


def kotlin_class(java_class: Any) -> KotlinClass:
    """The one wrapper of the JVM class `java_class`."""
    return KOTLIN_CLASSES.setdefault(str(java_class.getName()), KotlinClass(java_class))


class SdkNamespace:
    """The classes, objects and top-level declarations of the SDK, as its attributes."""

    def __getattr__(self, name: str) -> Any:
        if name.startswith("_"):
            raise AttributeError(name)
        return sdk_member(name)

    def __dir__(self) -> list[str]:
        return sdk_names()


sdk = SdkNamespace()
"""The SDK's declarations by name, such as `sdk.HostSpec`."""


def sdk_member(name: str) -> Any:
    """The class, object, top-level value or top-level function of the SDK named `name`."""
    return resolve_attribute(lambda interop: interop.sdkMember(name))


def sdk_names() -> list[str]:
    """The names `sdk_member` resolves, or none while the JVM does not run."""
    # Completion lists the names, which must not start the JVM and so make a later `start_jvm(options)` raise
    if not jpype.isJVMStarted():
        return []
    return [str(name) for name in jvm_classes().interop.sdkNames()]
