# Run RocketFlyIII locally

Install Docker Desktop in Linux container mode or Docker Engine with Compose v2, and Python 3. Start Docker. Have your Firefly III base URL and personal access token ready.

1. Download `rocketflyiii-local.zip` from the [latest release](https://github.com/blakbro2k/rocketflyIII/releases/latest). Extract it into a permanent folder under your own user account.
2. Open a terminal in the extracted folder. On Windows run `py -3 launch.py`. On macOS or Linux run `python3 launch.py`.
3. Enter the Firefly address reachable from Docker, your token, and a free local port (default 8080). For Firefly on the same machine use e.g. `http://host.docker.internal:8081` if Firefly publishes port 8081 on an interface reachable from Docker. For remote Firefly use its HTTPS URL.
4. The launcher pulls the image, starts RocketFlyIII, and prints a one-time token. Open the printed setup URL (default `http://127.0.0.1:8080/setup.html`) within 15 minutes. Enter the token and choose your System Admin username and password. Then open `http://127.0.0.1:8080/login` and sign in. Use your chosen port if you changed it.

Run the same launcher from the same folder to pull the current stable image and restart. The Compose volume persists profiles and grants. Keep the folder private: `connection.json` contains your Firefly token in plaintext. Edit this file with the app stopped to update connection settings. To retry interrupted first-time setup, replace an expired token, or recover an admin, run the launcher with `--setup`. An occupied port can be changed in `connection.json` before retrying.
