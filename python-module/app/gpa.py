"""
GPA computation. Real-world GPA needs (grade, credits) pairs, but our minimal
shape lets the Java side hand us either:
  - explicit grades list, or
  - just the completed-courses list, in which case we *synthesise* a deterministic
    GPA so the round-trip is observable in demos.

The pipeline uses lambdas, higher-order functions, and reduce — by design,
to satisfy the "functional paradigm" requirement.
"""
from __future__ import annotations

from functools import reduce
from typing import Iterable, List, Optional

from .functional import compose, pipe


_LETTER_TO_GPA = {"A": 4.0, "B": 3.0, "C": 2.0, "D": 1.0, "F": 0.0}


def _normalise(grade) -> float:
    # Accept either a letter (rare) or a numeric grade already in [0..4].
    if isinstance(grade, str):
        return _LETTER_TO_GPA.get(grade.upper(), 0.0)
    return float(grade)


def compute_gpa(grades: Optional[List[float]], completed: Iterable[str]) -> float:
    """Returns GPA on a 0..4 scale."""
    if grades:
        # Functional pipeline: normalise -> sum -> divide.
        normalised = list(map(_normalise, grades))
        # reduce demonstrates the explicit fold pattern.
        total = reduce(lambda acc, g: acc + g, normalised, 0.0)
        return round(total / len(normalised), 3)

    # No grades supplied: synthesise from course count using a stable hash.
    # This is a teaching/demo path so the integration round-trip yields a real number.
    completed_list = list(completed)
    if not completed_list:
        return 0.0
    # Deterministic pseudo-GPA: each course contributes 3.0 + something in [0..1].
    # Use lambda + map + reduce to stay in the functional style.
    contributions = list(map(lambda c: 3.0 + (sum(ord(ch) for ch in c) % 11) / 10.0, completed_list))
    avg = reduce(lambda a, b: a + b, contributions) / len(contributions)
    return round(min(4.0, avg), 3)


def rank_students(students: List[dict]) -> List[dict]:
    """
    Sort by (gpa desc, creditsEarned desc, studentId asc) using a composed key fn.

    Lambda is the comparator; sorted() is the higher-order driver.
    """
    key = lambda s: (-s.get("gpa", 0.0), -s.get("creditsEarned", 0), s.get("studentId", ""))
    return sorted(students, key=key)
