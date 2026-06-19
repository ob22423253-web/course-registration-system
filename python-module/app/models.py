"""
Pydantic request/response models AND a few frozen-dataclass immutable types
used in the functional core. Pydantic handles I/O; the dataclasses keep the
inner pipeline pure.
"""
from dataclasses import dataclass
from typing import List, Optional

from pydantic import BaseModel, Field


# --- HTTP-facing models -----------------------------------------------------

class EligibilityRequest(BaseModel):
    studentId: str
    status: str
    gpa: float
    completedCourses: List[str] = Field(default_factory=list)
    courseId: str
    prerequisiteRule: str


class EligibilityResponse(BaseModel):
    eligible: bool
    reason: str
    gpa: float
    missingPrerequisites: List[str] = Field(default_factory=list)


class GpaRequest(BaseModel):
    studentId: str
    completedCourses: List[str] = Field(default_factory=list)
    # Grades let real GPA computation work when supplied; otherwise we synthesise.
    grades: Optional[List[float]] = None


class GpaResponse(BaseModel):
    studentId: str
    gpa: float


class StudentRecord(BaseModel):
    """Used for ranking endpoint payloads."""
    studentId: str
    gpa: float
    creditsEarned: int = 0


class RankResponse(BaseModel):
    ranked: List[StudentRecord]


# --- Internal immutable domain types ---------------------------------------

# frozen=True gives us value-equality + hashability — important because the
# functional core puts these into frozensets when memoising the prereq resolver.
@dataclass(frozen=True)
class Student:
    student_id: str
    status: str
    gpa: float
    completed: frozenset  # frozenset[str] — immutable membership set
