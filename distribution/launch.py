"""Install or update RocketFlyIII on a local Docker host."""
import argparse
from getpass import getpass
import json
import os
from pathlib import Path
import socket
import tempfile
import subprocess
import sys
import time
from urllib.error import URLError
from urllib.parse import urlsplit
from urllib.request import urlopen

HOME_DIR = Path(__file__).resolve().parent
CONFIG = HOME_DIR / "connection.json"


def port_number(value):
    port = int(value)
    if not 1 <= port <= 65535:
        raise ValueError("Choose a port between 1 and 65535.")
    return str(port)


def save_settings(settings):
    with tempfile.NamedTemporaryFile(mode="w", dir=CONFIG.parent, delete=False) as stream:
        temporary = Path(stream.name)
        try:
            json.dump(settings, stream)
            stream.close()
            os.replace(temporary, CONFIG)
        finally:
            temporary.unlink(missing_ok=True)


def configure():
    print("Enter the Firefly III URL reachable from inside Docker.")
    print("For Firefly on this computer, use host.docker.internal and its published port.")
    url = input("Firefly III base URL: ").strip()
    address = urlsplit(url)
    if address.scheme not in {"http", "https"} or not address.hostname or address.username or address.password:
        raise ValueError("Enter an HTTP or HTTPS base URL without credentials.")
    if address.hostname in {"127.0.0.1", "::1", "localhost"}:
        raise ValueError("Use a Firefly address reachable from the container.")
    token = getpass("Firefly III personal access token (hidden): ").strip()
    if not token:
        raise ValueError("A Firefly III token is required.")
    port = port_number(input("Local RocketFlyIII port [8080]: ").strip() or "8080")
    settings = {"FIREFLY_BASE_URL": url, "FIREFLY_API_TOKEN": token, "ROCKETFLY_PORT": str(port)}
    with os.fdopen(os.open(CONFIG, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), "w") as stream:
        json.dump(settings, stream)
    return settings


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--setup", action="store_true", help="Issue a new setup or admin recovery token")
    parser.add_argument("--port", type=port_number, metavar="PORT",
                        help="Change the local port of an existing installation")
    args = parser.parse_args()
    if args.port and not CONFIG.exists():
        parser.error("Run the launcher once to configure the Firefly connection first.")
    subprocess.run(["docker", "compose", "version"], check=True)
    first = not CONFIG.exists()
    settings = configure() if first else json.loads(CONFIG.read_text())
    changed = args.port is not None and args.port != str(settings["ROCKETFLY_PORT"])
    if changed:
        with socket.socket() as probe:
            try:
                probe.bind(("127.0.0.1", int(args.port)))
            except OSError as error:
                raise RuntimeError(f"Local port {args.port} is unavailable. Choose another port.") from error
        settings["ROCKETFLY_PORT"] = args.port
    env = os.environ.copy()
    for key in ("FIREFLY_BASE_URL", "FIREFLY_API_TOKEN", "ROCKETFLY_PORT"):
        env[key] = settings[key]
    compose = ["docker", "compose", "-f", str(HOME_DIR / "compose.yaml")]
    if not args.port:
        subprocess.run(compose + ["pull"], env=env, check=True)
    subprocess.run(compose + ["up", "-d"], env=env, check=True)
    if changed:
        save_settings(settings)
    base = "http://127.0.0.1:" + env["ROCKETFLY_PORT"]
    for _ in range(60):
        try:
            with urlopen(base + "/setup.html", timeout=2):
                break
        except (URLError, TimeoutError):
            time.sleep(1)
    else:
        raise RuntimeError("Startup timed out. Check docker logs for the RocketFlyIII container.")
    if first or args.setup:
        subprocess.run(compose + ["exec", "-T", "rocketflyiii", "java", "/app/SetupToken.java"],
                       env=env, check=True)
        print("Create your first System Admin or recover an existing admin at " + base + "/setup.html")
    print("Sign in at " + base + "/login")


if __name__ == "__main__":
    try:
        main()
    except (OSError, KeyError, ValueError, RuntimeError, subprocess.CalledProcessError) as error:
        sys.exit(str(error))
