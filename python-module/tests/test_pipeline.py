"""
Coverage for the functional helpers: compose, pipe, filter_map, plus
GPA ranking (a stream pipeline in production).
"""
from app.functional import compose, filter_map, pipe
from app.gpa import compute_gpa, rank_students


def test_compose_right_to_left():
    add1 = lambda x: x + 1
    times2 = lambda x: x * 2
    # times2(add1(3)) == 8
    assert compose(times2, add1)(3) == 8


def test_pipe_left_to_right():
    assert pipe(3, lambda x: x + 1, lambda x: x * 2) == 8


def test_filter_map_lazy_pipeline():
    result = list(filter_map(range(10), lambda x: x % 2 == 0, lambda x: x * x))
    assert result == [0, 4, 16, 36, 64]


def test_compute_gpa_with_grades():
    assert compute_gpa([4.0, 3.0, 2.0], []) == 3.0


def test_compute_gpa_synthesised_is_stable():
    # Same input must yield the same number — pure function.
    g1 = compute_gpa(None, ["CS101", "MATH110"])
    g2 = compute_gpa(None, ["CS101", "MATH110"])
    assert g1 == g2
    assert 0.0 <= g1 <= 4.0


def test_rank_students_orders_by_gpa_then_credits():
    students = [
        {"studentId": "a", "gpa": 3.0, "creditsEarned": 30},
        {"studentId": "b", "gpa": 3.5, "creditsEarned": 30},
        {"studentId": "c", "gpa": 3.5, "creditsEarned": 60},
    ]
    ranked = rank_students(students)
    assert [s["studentId"] for s in ranked] == ["c", "b", "a"]
