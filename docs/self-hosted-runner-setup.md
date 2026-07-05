# Self-Hosted Runner Setup (Debian 12)

Steps to reproduce the CI runner environment from scratch (e.g. a new machine, or
reinstalling this one). Assumes the GitHub Actions runner itself is already registered
and running as a service (via GitHub's own runner setup wizard) — this only covers the
build toolchain it needs.

## 1. JDK 21

Debian 12 (bookworm) doesn't ship OpenJDK 21 in its default repos — use Eclipse
Adoptium's apt repo instead:

```bash
sudo apt install -y wget apt-transport-https gpg
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor | sudo tee /etc/apt/trusted.gpg.d/adoptium.gpg > /dev/null
echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update
sudo apt install temurin-21-jdk
```

Find the install path (needed in step 3):

```bash
readlink -f "$(which java)"
# e.g. /usr/lib/jvm/temurin-21-jdk-amd64/bin/java — JAVA_HOME is the directory
# *containing* bin/, i.e. drop the trailing /bin/java
```

## 2. Android SDK

```bash
sudo mkdir -p /opt/android-sdk
sudo chown "$USER:$USER" /opt/android-sdk

cd /tmp
wget https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip
unzip commandlinetools-linux-14742923_latest.zip
mkdir -p /opt/android-sdk/cmdline-tools
mv cmdline-tools /opt/android-sdk/cmdline-tools/latest
rm commandlinetools-linux-14742923_latest.zip
```

The `latest/` nesting above is required — `sdkmanager` won't find itself otherwise.

```bash
export ANDROID_HOME=/opt/android-sdk
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
```

Before installing packages, confirm the exact platform package string — this Android
version ships platform packages with an explicit minor-version suffix (e.g.
`platforms;android-37.0`), and a bare `platforms;android-37` will fail to resolve:

```bash
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --list 2>&1 | grep "platforms;android-3"
```

Then install (adjust the platform version to match what `compileSdk` is at the time, and
whatever the listing above actually printed):

```bash
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"
```

Gradle/AGP auto-fetches anything else it additionally needs during a build.

## 3. Wire it into the runner service

Environment variables in `~/.bashrc` etc. are **not** visible to the runner service —
it needs them in the runner's own `.env` file (in the runner's install directory, e.g.
`~/gh-actionrunners/<repo>/.env`):

```
JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
ANDROID_HOME=/opt/android-sdk
```

Then restart the runner service so it picks up the new `.env` (only read at startup):

```bash
sudo ./svc.sh stop && sudo ./svc.sh start
```
