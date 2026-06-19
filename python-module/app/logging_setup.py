"""Centralised logging so every module shares the same handler & format."""
import logging
import sys


def configure_logging(level: int = logging.INFO) -> None:
    # Only configure once — uvicorn imports modules in unpredictable order
    # and re-running basicConfig clobbers handlers.
    root = logging.getLogger()
    if root.handlers:
        return
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(
        logging.Formatter("%(asctime)s [%(levelname)s] %(name)s: %(message)s")
    )
    root.addHandler(handler)
    root.setLevel(level)
