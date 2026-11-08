"""Run native mining checks in fresh, ignored, loopback-only server fixtures."""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java-home", type=Path, required=True)
    parser.add_argument("--accept-eula", action="store_true")
    parser.add_argument("--phase", choices=("default", "optin", "both"), default="both")
    args = parser.parse_args()
    if not args.accept_eula:
        parser.error("Explicit --accept-eula is required for these Minecraft fixtures")
    java_home = args.java_home.resolve()
    if not (java_home / "bin" / ("javac.exe" if os.name == "nt" else "javac")).is_file():
        parser.error("--java-home must identify a JDK 25 installation")
    environment = os.environ.copy()
    environment["JAVA_HOME"] = str(java_home)
    phases = ("default", "optin") if args.phase == "both" else (args.phase,)
    local = ROOT / "local"
    local.mkdir(exist_ok=True)
    wrapper = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    for phase in phases:
        directory = Path(tempfile.mkdtemp(prefix="native-" + phase + "-", dir=local))
        with socket.socket() as connection:
            connection.bind(("127.0.0.1", 0))
            port = connection.getsockname()[1]
        (directory / "eula.txt").write_text("eula=true\n", encoding="utf-8")
        properties = {
            "server-ip": "127.0.0.1", "server-port": str(port), "online-mode": "false",
            "level-name": "deepslate-fixture", "level-seed": "263",
            "level-type": "minecraft:flat", "generate-structures": "false",
            "generator-settings": json.dumps({"biome": "minecraft:plains", "layers": [
                {"block": "minecraft:bedrock", "height": 1},
                {"block": "minecraft:dirt", "height": 2},
                {"block": "minecraft:grass_block", "height": 1}]}),
            "view-distance": "2", "simulation-distance": "2", "difficulty": "peaceful",
            "enable-rcon": "false", "enable-query": "false", "enable-status": "false",
            "pause-when-empty-seconds": "0", "max-players": "1",
            "motd": "Disposable native mining fixture",
        }
        (directory / "server.properties").write_text(
            "".join(f"{key}={value}\n" for key, value in properties.items()), encoding="utf-8")
        if phase == "optin":
            shutil.copytree(ROOT / "tests/fixtures/optin", directory / "deepslate-fixture/datapacks/optin")
        print(f"Running {phase} native checks in a fresh local fixture", flush=True)
        with (directory / "gradle-console.log").open("w", encoding="utf-8") as output:
            result = subprocess.run([
                str(wrapper), "--no-daemon", "-PintegrationTests", "-PtestPhase=" + phase,
                "-PtestRunDir=" + directory.relative_to(ROOT).as_posix(), "build", "runIntegrationServer",
            ], cwd=ROOT, env=environment, stdout=output, stderr=subprocess.STDOUT)
        receipt_path = directory / "local" / f"deepslate-integration-{phase}.txt"
        receipt = receipt_path.read_text(encoding="utf-8") if receipt_path.exists() else ""
        passed = result.returncode == 0 and receipt.startswith(f"PASS phase={phase} ")
        artifact = ROOT / "build/libs/deepslate-instamine-1.1.0.jar"
        record = {"phase": phase, "passed": passed, "exit_code": result.returncode,
                  "completed_utc": datetime.now(timezone.utc).isoformat(), "receipt": receipt,
                  "artifact_sha256": hashlib.sha256(artifact.read_bytes()).hexdigest() if artifact.exists() else None}
        (directory / "result.json").write_text(json.dumps(record, indent=2) + "\n", encoding="utf-8")
        print(receipt.splitlines()[0] if receipt else "FAIL: no native receipt", flush=True)
        if not passed:
            print("Inspect " + str((directory / "gradle-console.log").relative_to(ROOT)))
            raise SystemExit(1)


if __name__ == "__main__":
    main()
