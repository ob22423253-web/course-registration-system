"""
Functional helpers used by the analytics core: compose, pipe, plus a few
generator-based higher-order utilities. Kept dependency-free so they remain
easy to reason about and unit test.
"""
from functools import reduce
from typing import Callable, Iterable, Iterator, TypeVar

A = TypeVar("A")
B = TypeVar("B")
C = TypeVar("C")


def compose(*fns: Callable) -> Callable:
    """Right-to-left composition: compose(f, g, h)(x) == f(g(h(x)))."""
    if not fns:
        # Identity is a sensible neutral element so chain code can stay symmetric.
        return lambda x: x
    return reduce(lambda f, g: lambda x: f(g(x)), fns)


def pipe(value, *fns: Callable):
    """Left-to-right application — reads like a Unix pipe."""
    return reduce(lambda acc, fn: fn(acc), fns, value)


def flatten(it: Iterable[Iterable[A]]) -> Iterator[A]:
    """Generator flatten so callers can consume lazily on large catalogs."""
    for sub in it:
        for x in sub:
            yield x


# Higher-order utility: apply a predicate-mapper pair across a sequence in a
# single pass. Keeps call sites declarative.
def filter_map(items: Iterable[A], keep: Callable[[A], bool], fn: Callable[[A], B]) -> Iterator[B]:
    for x in items:
        if keep(x):
            yield fn(x)


# Curried-style adapter so we can partially apply when building pipelines.
def by(attr: str) -> Callable[[object], object]:
    return lambda obj: getattr(obj, attr) if hasattr(obj, attr) else obj[attr]
