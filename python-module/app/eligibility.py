"""
Eligibility decision = parse rule -> evaluate against completed -> apply
status-specific overrides (suspended students never pass, etc.).

The status step uses Python's match/case to satisfy the pattern-matching
requirement and keeps each path explicit.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import List

from .parser import evaluate, missing_for, parse_expression, ParseError


@dataclass(frozen=True)
class EligibilityDecision:
    eligible: bool
    reason: str
    missing: tuple   # tuple = immutable list — keeps the dataclass hashable


def _status_gate(status: str, gpa: float) -> EligibilityDecision | None:
    """Pre-filter on student status. None means 'continue evaluating'."""
    match status.upper():
        case "SUSPENDED":
            return EligibilityDecision(False, "student is suspended", ())
        case "GRADUATED":
            return EligibilityDecision(False, "student already graduated", ())
        case "PROBATION":
            # On probation, require GPA >= 2.0 before allowing registration.
            if gpa < 2.0:
                return EligibilityDecision(False, "probation: GPA below 2.0", ())
            return None
        case "ACTIVE":
            return None
        case _:
            return EligibilityDecision(False, f"unknown status {status}", ())


def decide(status: str,
           gpa: float,
           completed: List[str],
           prerequisite_rule: str) -> EligibilityDecision:
    pre = _status_gate(status, gpa)
    if pre is not None:
        return pre

    if not prerequisite_rule.strip():
        return EligibilityDecision(True, "no prerequisites", ())

    try:
        ast = parse_expression(prerequisite_rule)
    except ParseError as ex:
        # Bad rule => fail closed: better to halt registration than auto-approve.
        return EligibilityDecision(False, f"invalid prereq rule: {ex}", ())

    completed_set = frozenset(completed)
    if evaluate(ast, completed_set):
        return EligibilityDecision(True, "prerequisites satisfied", ())
    return EligibilityDecision(False, "prerequisites not met", tuple(missing_for(ast, completed_set)))
