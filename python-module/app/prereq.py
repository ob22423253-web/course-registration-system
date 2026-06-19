"""
Recursive prerequisite-chain resolver.

`required_chain(course, catalog)` returns the *transitive* set of courses
needed before `course` — useful for a "what would I need to take to reach
CS401?" advisor view.

A memo dict accelerates repeat lookups: deeply shared prereqs (everything
hangs off CS101) would otherwise blow up exponentially.
"""
from __future__ import annotations

from typing import Dict, Iterable

from .parser import (
    And,
    CourseRef,
    Node,
    Or,
    parse_expression,
    referenced_courses,
)


def required_chain(course_id: str,
                   catalog: Dict[str, str],
                   _memo: Dict[str, frozenset] | None = None) -> frozenset:
    """
    catalog: course_id -> raw expression rule (or empty string for no prereq).
    Returns frozenset of all transitively required course_ids.

    Recursion is on the expression's leaves: for each referenced course, descend.
    """
    memo = _memo if _memo is not None else {}
    if course_id in memo:
        return memo[course_id]
    memo[course_id] = frozenset()  # placeholder to break cycles

    raw = catalog.get(course_id, "")
    if not raw:
        memo[course_id] = frozenset()
        return memo[course_id]

    ast = parse_expression(raw)
    direct = referenced_courses(ast)

    deep: set = set(direct)
    for ref in direct:
        # Recurse — this is the chain step.
        deep |= required_chain(ref, catalog, memo)

    result = frozenset(deep)
    memo[course_id] = result
    return result


def depth(node: Node) -> int:
    """Depth of the expression tree. Recursive — used for complexity metrics."""
    match node:
        case CourseRef():
            return 1
        case And(left=l, right=r) | Or(left=l, right=r):
            return 1 + max(depth(l), depth(r))
        case _:
            return 0


def satisfies_chain(course_id: str,
                    completed: frozenset,
                    catalog: Dict[str, str]) -> bool:
    """True iff every transitive prerequisite is already in completed."""
    return required_chain(course_id, catalog).issubset(completed)
