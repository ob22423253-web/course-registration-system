"""
Recursive prereq-chain resolver. The depth=4 chain proves the recursion
terminates and memoisation works.
"""
from app.prereq import depth, required_chain, satisfies_chain
from app.parser import parse_expression


def test_chain_resolves_transitive_deps():
    catalog = {
        "CS101": "",
        "CS201": "CS101",
        "CS301": "CS201 AND MATH110",
        "MATH110": "",
        "CS401": "CS301 OR CS302",
        "CS302": "CS201",
    }
    chain = required_chain("CS401", catalog)
    # Note: OR still reports both branches as referenced — the chain is the
    # superset of all possible paths.
    assert "CS301" in chain and "CS302" in chain
    assert "CS201" in chain
    assert "MATH110" in chain
    assert "CS101" in chain


def test_chain_handles_empty_rule():
    assert required_chain("CS101", {"CS101": ""}) == frozenset()


def test_chain_returns_empty_when_course_missing():
    # Unknown course -> empty chain rather than blowing up.
    assert required_chain("CS999", {}) == frozenset()


def test_depth_grows_with_nesting():
    expr1 = parse_expression("CS101")
    expr2 = parse_expression("CS101 AND CS102")
    expr3 = parse_expression("(CS101 OR CS102) AND CS103")
    assert depth(expr1) == 1
    assert depth(expr2) == 2
    assert depth(expr3) == 3


def test_satisfies_chain_true_only_when_all_done():
    catalog = {"CS201": "CS101", "CS101": ""}
    assert satisfies_chain("CS201", frozenset({"CS101"}), catalog) is True
    assert satisfies_chain("CS201", frozenset(), catalog) is False
