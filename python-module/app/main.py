"""
FastAPI entrypoint. Exposes the endpoints the Java service calls during
registration:

    POST /api/v1/eligibility   -> EligibilityResponse
    POST /api/v1/gpa           -> GpaResponse
    POST /api/v1/rank          -> RankResponse
    POST /api/v1/prereq/chain  -> transitive prereq chain for a course
    GET  /api/v1/health        -> liveness

All business logic lives in the smaller modules (parser, eligibility, gpa,
prereq); this file just wires HTTP to them.
"""
from __future__ import annotations

import logging
from typing import Dict, List

from fastapi import Body, FastAPI

from .eligibility import decide
from .gpa import compute_gpa, rank_students
from .logging_setup import configure_logging
from .models import (
    EligibilityRequest,
    EligibilityResponse,
    GpaRequest,
    GpaResponse,
    RankResponse,
    StudentRecord,
)
from .prereq import required_chain

configure_logging()
log = logging.getLogger("analytics")

app = FastAPI(title="course-registration-analytics", version="1.0.0")


@app.get("/api/v1/health")
def health() -> dict:
    return {"status": "UP"}


@app.post("/api/v1/eligibility", response_model=EligibilityResponse)
def eligibility(req: EligibilityRequest) -> EligibilityResponse:
    log.info("eligibility student=%s course=%s", req.studentId, req.courseId)
    decision = decide(req.status, req.gpa, req.completedCourses, req.prerequisiteRule)
    return EligibilityResponse(
        eligible=decision.eligible,
        reason=decision.reason,
        gpa=req.gpa,
        missingPrerequisites=list(decision.missing),
    )


@app.post("/api/v1/gpa", response_model=GpaResponse)
def gpa(req: GpaRequest) -> GpaResponse:
    g = compute_gpa(req.grades, req.completedCourses)
    log.info("gpa student=%s -> %s", req.studentId, g)
    return GpaResponse(studentId=req.studentId, gpa=g)


@app.post("/api/v1/rank", response_model=RankResponse)
def rank(students: List[StudentRecord]) -> RankResponse:
    ranked = rank_students([s.model_dump() for s in students])
    return RankResponse(ranked=[StudentRecord(**r) for r in ranked])


@app.post("/api/v1/prereq/chain")
def chain(
    courseId: str = Body(...),
    catalog: Dict[str, str] = Body(...),
) -> dict:
    """Return the transitive prereq list for `courseId` given a catalog mapping."""
    result = required_chain(courseId, catalog)
    return {"courseId": courseId, "required": sorted(result)}
