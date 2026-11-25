"""The parquet tables of traces: the schema the SDK reads each one in, and the casts that fit a table to it."""

from __future__ import annotations

import contextlib
import functools
import tempfile
from collections.abc import Generator, Iterable
from pathlib import Path
from typing import Any

import jpype
import pandas as pd
import pyarrow as pa
import pyarrow.parquet as pq

from opendc.bridge import start_jvm

__all__ = ["Table", "contract_schema", "reads_in_place", "stage_table", "write_file", "write_parts"]

Table = pd.DataFrame | Path
"""A table of a trace: a DataFrame, or the parquet file, or directory of parquet files, that holds it."""


def kotlin_trace_table(table: str) -> Any:
    """The Kotlin `TraceTable` named `table`, which knows its schema and its required columns."""
    start_jvm()
    return jpype.JClass("org.opendc.sdk.python.TraceTable").valueOf(table)


@functools.cache
def requested_metadata(table: str) -> pq.FileMetaData:
    """The parquet metadata the SDK requests for the trace `table`, read back from an empty file the JVM writes."""
    with tempfile.TemporaryDirectory(prefix="opendc-schema-") as directory:
        path = Path(directory) / f"{table.lower()}.parquet"
        kotlin_trace_table(table).writeEmpty(jpype.JClass("java.nio.file.Path").of(str(path)))
        return pq.read_metadata(path)


@functools.cache
def requested_schema(table: str) -> pa.Schema:
    """The arrow schema the SDK requests for the trace `table`."""
    return requested_metadata(table).schema.to_arrow_schema().remove_metadata()


@functools.cache
def required_columns(table: str) -> tuple[str, ...]:
    """The columns of the trace `table` that its reader needs a value in for every row."""
    return tuple(str(column) for column in kotlin_trace_table(table).getRequired())


def contract_schema(table: str) -> pa.Schema:
    """The schema a table of the trace `table` has to fit, in which only the required columns are not nullable."""
    required = required_columns(table)
    return pa.schema([field.with_nullable(field.name not in required) for field in requested_schema(table)])


def column_paths(schema: pq.ParquetSchema) -> set[str]:
    """The paths of the parquet columns of `schema`, which tell how a list is nested."""
    return {schema.column(index).path for index in range(len(schema))}


def check_columns(columns: list[str], table: str, label: str) -> None:
    """Raises unless the DataFrame `columns` hold every required column of the trace `table` and no unknown one."""
    schema = requested_schema(table)
    required = list(required_columns(table))
    unknown = [column for column in columns if column not in schema.names]
    missing = [column for column in required if column not in columns]

    if unknown or missing:
        raise ValueError(
            f"{label} lacks the columns {missing} and has the unknown columns {unknown}; "
            f"its columns are {schema.names}, of which {required} are required"
        )


def read_columns(columns: list[str], table: str, label: str) -> list[str]:
    """The `columns` of a parquet file that the SDK reads as the trace `table`, which skips any it does not request."""
    read = [column for column in columns if column in requested_schema(table).names]
    required = list(required_columns(table))
    missing = [column for column in required if column not in read]

    if missing:
        raise ValueError(f"{label} lacks the columns {missing}; the required columns are {required}")
    return read


def table_label(table: str, path: Path) -> str:
    """How an error names the trace `table` held in the file or directory `path`."""
    return f"The {table.lower()} table in {path}"


def part_files(path: Path) -> list[Path]:
    """The parquet files that hold the table at `path`: the file itself, or the files of the directory in name order."""
    if path.is_dir():
        parts = sorted(part for part in path.iterdir() if part.is_file() and not part.name.startswith("."))
        if not parts:
            raise ValueError(f"The directory {path} holds no parquet files")
        return parts

    if not path.is_file():
        raise FileNotFoundError(f"There is no parquet file or directory at {path}")
    return [path]


def reads_in_place(path: Path, table: str) -> bool:
    """Whether the SDK reads the table at `path` where it is, which it does when it reads each file as it is."""
    return all(reads_file_as_is(part, table) for part in part_files(path))


def reads_file_as_is(part: Path, table: str) -> bool:
    """Whether the SDK reads the file `part` as it is: nested and typed as requested, no required column nullable."""
    with pq.ParquetFile(part) as file:
        fields = file.schema_arrow
        paths = column_paths(file.schema)

    read = read_columns(fields.names, table, table_label(table, part))

    # The column paths tell how a list is nested, which the types do not
    read_paths = {path for path in paths if path.split(".")[0] in read}
    nested_as_requested = read_paths <= column_paths(requested_metadata(table).schema)
    return nested_as_requested and all(field_reads_as_is(field, table) for field in fields if field.name in read)


def field_reads_as_is(field: pa.Field, table: str) -> bool:
    """Whether the SDK reads `field` of the trace `table` as it is: typed as requested, and not nullable if required."""
    requested = requested_schema(table).field(field.name).type
    return type_reads_as(field.type, requested) and not (field.nullable and field.name in required_columns(table))


def type_reads_as(actual: pa.DataType, requested: pa.DataType) -> bool:
    """Whether the SDK reads a column of type `actual` as one of type `requested`."""
    # It reads a time as milliseconds since the epoch, so an int64, or a time in milliseconds in any zone, reads as one
    if pa.types.is_timestamp(requested):
        return actual == pa.int64() or (pa.types.is_timestamp(actual) and actual.unit == requested.unit)
    return actual == requested


def stage_table(source: Table, table: str, target: Path) -> Path:
    """Returns where the SDK reads the table `source`: where it is, or `target`, where it is written cast."""
    if not isinstance(source, pd.DataFrame) and reads_in_place(Path(source), table):
        return Path(source).absolute()

    write_parts(source, table, target)
    return target


def write_parts(source: Table, table: str, target: Path) -> None:
    """Writes `source` as the trace `table`, cast, to `target`: a directory of the same parts if `source` is one."""
    if isinstance(source, pd.DataFrame):
        write_frame(source, table, target)
        return

    path = Path(source)
    parts = part_files(path)
    if path.is_dir():
        target.mkdir()
        for part in parts:
            write_part(part, table, target / part.name)
    else:
        write_part(path, table, target)


def write_file(source: Table, table: str, target: Path) -> None:
    """Writes the table `source` as the trace `table`, cast, to the one file `target`."""
    if isinstance(source, pd.DataFrame) or not Path(source).is_dir():
        write_parts(source, table, target)
        return

    directory = Path(source)
    parts = part_files(directory)
    columns = columns_of_parts(parts, table)

    # Closed on a failure too, or a part file would stay open, and locked on Windows, while the error is held
    with contextlib.closing(aligned_batches(parts, columns)) as batches:
        write_batches(batches, columns, table, table_label(table, directory), target)


def write_frame(frame: pd.DataFrame, table: str, target: Path) -> None:
    """Writes `frame` as the trace `table`, cast, to the file `target`."""
    label = f"The {table.lower()} table"
    check_columns([str(column) for column in frame.columns], table, label)

    rows = pa.Table.from_pandas(frame, preserve_index=False)
    write_batches(rows.to_batches(), rows.schema.names, table, label, target)


def write_part(part: Path, table: str, target: Path) -> None:
    """Writes the parquet file `part` as the trace `table`, cast a batch at a time, to the file `target`."""
    label = table_label(table, part)
    with pq.ParquetFile(part) as file:
        columns = read_columns(file.schema_arrow.names, table, label)
        write_batches(file.iter_batches(columns=columns), columns, table, label, target)


def columns_of_parts(parts: list[Path], table: str) -> list[str]:
    """The columns of the trace `table` that the files `parts` hold between them, in its order."""
    held: set[str] = set()
    for part in parts:
        held.update(read_columns(pq.read_schema(part).names, table, table_label(table, part)))

    return [name for name in requested_schema(table).names if name in held]


def aligned_batches(parts: list[Path], columns: list[str]) -> Generator[pa.RecordBatch, None, None]:
    """The batches of the files `parts`, one file after another, each with exactly `columns`."""
    for part in parts:
        with pq.ParquetFile(part) as file:
            yield from (with_columns(batch, columns) for batch in file.iter_batches())


def with_columns(batch: pa.RecordBatch, columns: list[str]) -> pa.RecordBatch:
    """`batch` with exactly `columns`, in that order, and nulls in those it lacks."""
    arrays = [batch.column(name) if name in batch.schema.names else pa.nulls(batch.num_rows) for name in columns]
    return pa.RecordBatch.from_arrays(arrays, names=columns)


def write_batches(batches: Iterable[pa.RecordBatch], columns: list[str], table: str, label: str, target: Path) -> None:
    """Writes `batches` to the file `target` as the trace `table`: their `columns` in its order, cast to its types."""
    fields = pa.schema([field for field in requested_schema(table) if field.name in columns])
    with pq.ParquetWriter(target, fields, use_compliant_nested_type=True) as writer:
        for batch in batches:
            writer.write_table(cast_rows(batch, fields, table, label))


def cast_rows(batch: pa.RecordBatch, fields: pa.Schema, table: str, label: str) -> pa.Table:
    """The rows of `batch` cast to `fields`; raises if a required column of the trace `table` holds a null."""
    columns = [cast_column(batch.column(field.name), field, label) for field in fields]
    rows = pa.Table.from_arrays(columns, schema=fields)

    gaps = [column for column in required_columns(table) if rows.column(column).null_count > 0]
    if gaps:
        raise ValueError(f"{label} has missing values in the required columns {gaps}")
    return rows


def cast_column(column: pa.Array, field: pa.Field, label: str) -> pa.Array:
    """Casts `column` to the type of `field`, through milliseconds for a duration or a time in an int64 column."""
    # The int64 times, such as durations, are milliseconds, where a plain cast keeps the nano- or microseconds of pandas
    try:
        if pa.types.is_duration(column.type):
            column = column.cast(pa.duration("ms")).cast(pa.int64())
        elif pa.types.is_timestamp(column.type) and pa.types.is_integer(field.type):
            column = column.cast(pa.timestamp("ms")).cast(pa.int64())
        return column.cast(field.type)
    except (pa.ArrowInvalid, pa.ArrowNotImplementedError) as failure:
        raise ValueError(f"{label}: the column '{field.name}' cannot be a {field.type}: {failure}") from None
