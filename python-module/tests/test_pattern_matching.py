"""
Pattern-matching coverage on the status gate inside eligibility.decide.
"""
from app.eligibility import decide


def test_active_with_no_rule_is_eligible():
    d = decide("ACTIVE", 3.5, [], "")
    assert d.eligible is True
    assert d.missing == ()


def test_suspended_always_blocked():
    d = decide("SUSPENDED", 4.0, ["CS101", "CS102"], "CS101")
    assert d.eligible is False
    assert "suspended" in d.reason


def test_graduated_always_blocked():
    d = decide("GRADUATED", 4.0, [], "")
    assert d.eligible is False


def test_probation_below_threshold_blocked():
    d = decide("PROBATION", 1.5, ["CS101"], "CS101")
    assert d.eligible is False


def test_probation_above_threshold_allowed_through_prereq_check():
    d = decide("PROBATION", 2.5, ["CS101"], "CS101")
    assert d.eligible is True


def test_unknown_status_blocked():
    d = decide("WEIRD", 4.0, [], "")
    assert d.eligible is False
    assert "unknown status" in d.reason


def test_missing_prereqs_reported():
    d = decide("ACTIVE", 3.0, ["CS101"], "CS101 AND MATH110")
    assert d.eligible is False
    assert "MATH110" in d.missing
