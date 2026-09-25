"""Shared test scaffolding for the rosa-search Lambdas.

Two things need handling before a handler module can be imported in a test:

1. The Lambda runtime bundles boto3 and botocore, but a dev machine may not have
   them. Only SigV4 signing and S3 reads use them, and neither is what these
   tests exercise, so both are stubbed.

2. Both handlers are named `app.py`. Importing them as `app` makes the second one
   silently resolve to the first when the suites run in one process, so each is
   loaded under its own module name instead.
"""

from __future__ import annotations

import importlib.util
import sys
import types
from pathlib import Path

LAMBDA_ROOT = Path(__file__).resolve().parent.parent


class FakeBody:
    """Minimal stand-in for a botocore StreamingBody."""

    def __init__(self, data: bytes) -> None:
        self._data = data
        self.closed = False

    def iter_lines(self, chunk_size=None):  # noqa: ARG002
        for line in self._data.split(b"\n"):
            if line or not self._data.endswith(b"\n"):
                yield line

    def read(self) -> bytes:
        return self._data

    def close(self) -> None:
        self.closed = True


class FakeS3:
    """In-memory S3 supporting the calls the admin Lambda makes."""

    def __init__(self) -> None:
        self.objects: dict[tuple[str, str], bytes] = {}

    def put(self, bucket: str, key: str, data: bytes) -> None:
        self.objects[(bucket, key)] = data

    def head_object(self, Bucket, Key):  # noqa: N803
        return {"ContentLength": len(self.objects[(Bucket, Key)])}

    def get_object(self, Bucket, Key, Range=None):  # noqa: N803
        data = self.objects[(Bucket, Key)]
        if Range:
            start = int(Range.split("=")[1].split("-")[0])
            data = data[start:]
        return {"Body": FakeBody(data)}


FAKE_S3 = FakeS3()


def install_aws_stubs() -> None:
    """Install boto3/botocore stubs if the real packages are absent."""
    if "boto3" in sys.modules:
        return

    boto3 = types.ModuleType("boto3")

    class Session:
        def get_credentials(self):
            return object()

        def client(self, name, *args, **kwargs):  # noqa: ARG002
            return FAKE_S3

    boto3.Session = Session
    sys.modules["boto3"] = boto3

    botocore = types.ModuleType("botocore")
    auth = types.ModuleType("botocore.auth")
    awsrequest = types.ModuleType("botocore.awsrequest")

    class SigV4Auth:
        def __init__(self, *args, **kwargs):
            pass

        def add_auth(self, request):
            request.headers["Authorization"] = "AWS4-HMAC-SHA256 test"

    class AWSRequest:
        def __init__(self, method=None, url=None, data=None, headers=None):
            self.method = method
            self.url = url
            self.data = data
            self.headers = dict(headers or {})

    auth.SigV4Auth = SigV4Auth
    awsrequest.AWSRequest = AWSRequest
    sys.modules["botocore"] = botocore
    sys.modules["botocore.auth"] = auth
    sys.modules["botocore.awsrequest"] = awsrequest


def load_lambda(package: str, module_name: str):
    """Load <package>/app.py under a unique module name."""
    install_aws_stubs()

    if module_name in sys.modules:
        return sys.modules[module_name]

    spec = importlib.util.spec_from_file_location(module_name, LAMBDA_ROOT / package / "app.py")
    module = importlib.util.module_from_spec(spec)
    sys.modules[module_name] = module
    spec.loader.exec_module(module)
    return module
