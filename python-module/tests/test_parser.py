"""
Parser tests cover tokens, precedence, parentheses, and bad input. The
precedence test is the most important: AND must bind tighter than OR.
"""
import pytest

from app.parser import (
    And,
    CourseRef,
    Or,
    ParseError,
    evaluate,
    missing_for,
    parse_expression,
    parse_rule,
    tokenize,
)


def test_tokenize_basic():
    toks = tokenize("RULE CS301 REQUIRES CS101 AND MATH110")
    kinds = [k for k, _ in toks]
    assert kinds == ["KW", "COURSE", "KW", "COURSE", "KW", "COURSE"]


def test_parse_rule_simple():
    rule = parse_rule("RULE CS201 REQUIRES CS101")
    assert rule.course == "CS201"
    assert isinstance(rule.expr, CourseRef) and rule.expr.code == "CS101"


def test_and_binds_tighter_than_or():
    # CS101 OR CS102 AND CS103   ==   CS101 OR (CS102 AND CS103)
    expr = parse_expression("CS101 OR CS102 AND CS103")
    assert isinstance(expr, Or)
    assert isinstance(expr.left, CourseRef) and expr.left.code == "CS101"
    assert isinstance(expr.right, And)


def test_parentheses_override_precedence():
    expr = parse_expression("(CS101 OR CS102) AND CS103")
    assert isinstance(expr, And)
    assert isinstance(expr.left, Or)
    assert isinstance(expr.right, CourseRef) and expr.right.code == "CS103"


def test_evaluation_truth_table():
    expr = parse_expression("CS101 AND (CS102 OR CS103)")
    assert evaluate(expr, frozenset({"CS101", "CS102"})) is True
    assert evaluate(expr, frozenset({"CS101", "CS103"})) is True
    assert evaluate(expr, frozenset({"CS101"})) is False
    assert evaluate(expr, frozenset({"CS102", "CS103"})) is False


def test_missing_for_reports_only_referenced_missing():
    expr = parse_expression("CS101 AND CS102")
    assert missing_for(expr, frozenset({"CS101"})) == ["CS102"]


def test_rejects_garbage():
    with pytest.raises(ParseError):
        parse_expression("CS101 OR")
    with pytest.raises(ParseError):
        parse_expression("CS101 AND (CS102")
    with pytest.raises(ParseError):
        parse_expression("notacourse")
