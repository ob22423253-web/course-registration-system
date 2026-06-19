"""
Recursive-descent parser for the prerequisite-rule grammar:

    rule        = "RULE" course "REQUIRES" expression
    expression  = term { "OR" term }
    term        = factor { "AND" factor }
    factor      = course | "(" expression ")"
    course      = letter letter { letter } digit digit digit

AND binds tighter than OR. Parentheses override.

The parser is split into a tokenizer + a tiny RD parser. We produce an AST
of frozen dataclasses so evaluation can be cleanly pattern-matched and the
tree can be safely shared / hashed (e.g. cached per course).
"""
from __future__ import annotations

import re
from dataclasses import dataclass
from typing import List, Optional, Tuple


# --- AST --------------------------------------------------------------------

@dataclass(frozen=True)
class CourseRef:
    code: str


@dataclass(frozen=True)
class And:
    left: "Node"
    right: "Node"


@dataclass(frozen=True)
class Or:
    left: "Node"
    right: "Node"


@dataclass(frozen=True)
class Rule:
    course: str          # the course the rule applies to
    expr: "Node"         # the boolean tree of prerequisites


Node = object  # CourseRef | And | Or — Python lacks a sealed type, so we document.


# --- Tokenizer --------------------------------------------------------------

# Course codes: 2+ letters then exactly 3 digits, e.g. CS301 / MATH110.
_COURSE_RE = re.compile(r"[A-Z]{2,}[0-9]{3}")
_KEYWORDS = {"RULE", "REQUIRES", "AND", "OR"}


class ParseError(Exception):
    pass


def tokenize(src: str) -> List[Tuple[str, str]]:
    """Returns a list of (kind, lexeme). Kinds: KW, COURSE, LP, RP."""
    tokens: List[Tuple[str, str]] = []
    i = 0
    n = len(src)
    while i < n:
        c = src[i]
        if c.isspace():
            i += 1
            continue
        if c == "(":
            tokens.append(("LP", "("))
            i += 1
            continue
        if c == ")":
            tokens.append(("RP", ")"))
            i += 1
            continue
        if c.isalpha():
            # Greedy word match: keyword or course id.
            j = i
            while j < n and (src[j].isalnum()):
                j += 1
            word = src[i:j]
            if word in _KEYWORDS:
                tokens.append(("KW", word))
            elif _COURSE_RE.fullmatch(word):
                tokens.append(("COURSE", word))
            else:
                raise ParseError(f"unrecognized token '{word}' at {i}")
            i = j
            continue
        raise ParseError(f"unexpected char {c!r} at {i}")
    return tokens


# --- Recursive-descent parser ----------------------------------------------

class _Parser:
    def __init__(self, tokens: List[Tuple[str, str]]) -> None:
        self.tokens = tokens
        self.pos = 0

    def peek(self) -> Optional[Tuple[str, str]]:
        return self.tokens[self.pos] if self.pos < len(self.tokens) else None

    def eat(self, kind: str, lexeme: Optional[str] = None) -> Tuple[str, str]:
        t = self.peek()
        if t is None or t[0] != kind or (lexeme is not None and t[1] != lexeme):
            raise ParseError(f"expected {kind} {lexeme or ''} but got {t}")
        self.pos += 1
        return t

    # rule = "RULE" course "REQUIRES" expression
    def parse_rule(self) -> Rule:
        self.eat("KW", "RULE")
        course = self.eat("COURSE")[1]
        self.eat("KW", "REQUIRES")
        expr = self.parse_expression()
        if self.peek() is not None:
            raise ParseError(f"trailing tokens at {self.pos}: {self.tokens[self.pos:]}")
        return Rule(course=course, expr=expr)

    # expression = term { "OR" term }   (left-associative)
    def parse_expression(self) -> Node:
        left = self.parse_term()
        while True:
            t = self.peek()
            if t and t == ("KW", "OR"):
                self.pos += 1
                right = self.parse_term()
                left = Or(left, right)
            else:
                break
        return left

    # term = factor { "AND" factor }
    def parse_term(self) -> Node:
        left = self.parse_factor()
        while True:
            t = self.peek()
            if t and t == ("KW", "AND"):
                self.pos += 1
                right = self.parse_factor()
                left = And(left, right)
            else:
                break
        return left

    # factor = course | "(" expression ")"
    def parse_factor(self) -> Node:
        t = self.peek()
        if t is None:
            raise ParseError("unexpected end of input")
        if t[0] == "LP":
            self.eat("LP")
            inner = self.parse_expression()
            self.eat("RP")
            return inner
        if t[0] == "COURSE":
            self.eat("COURSE")
            return CourseRef(t[1])
        raise ParseError(f"expected course or '(' at pos {self.pos}, got {t}")


def parse_rule(src: str) -> Rule:
    """Parse a rule string and return the AST."""
    return _Parser(tokenize(src)).parse_rule()


def parse_expression(src: str) -> Node:
    """Parse just an expression — useful when prereq is supplied without RULE wrapping."""
    p = _Parser(tokenize(src))
    expr = p.parse_expression()
    if p.peek() is not None:
        raise ParseError(f"trailing tokens: {p.tokens[p.pos:]}")
    return expr


# --- Evaluation -------------------------------------------------------------

def evaluate(node: Node, completed: frozenset) -> bool:
    """Walk the AST against a student's completed-courses set."""
    # Pattern matching makes the tree walk obvious; each arm = one node kind.
    match node:
        case CourseRef(code=c):
            return c in completed
        case And(left=l, right=r):
            return evaluate(l, completed) and evaluate(r, completed)
        case Or(left=l, right=r):
            return evaluate(l, completed) or evaluate(r, completed)
        case _:
            raise ParseError(f"unknown node {node!r}")


def referenced_courses(node: Node) -> frozenset:
    """All course IDs mentioned anywhere in the AST. Used to report 'missing'."""
    match node:
        case CourseRef(code=c):
            return frozenset({c})
        case And(left=l, right=r) | Or(left=l, right=r):
            return referenced_courses(l) | referenced_courses(r)
        case _:
            return frozenset()


def missing_for(node: Node, completed: frozenset) -> list:
    """The subset of referenced courses the student hasn't done."""
    return sorted(referenced_courses(node) - completed)
