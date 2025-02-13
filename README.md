# Hadolint Plugin for IntelliJ

This IntelliJ plugin integrates **Hadolint**, a Dockerfile linter, into your IDE.
This plugin is not officially related to the [Hadolint project](https://github.com/hadolint/hadolint). 

Before using the plugin, ensure Hadolint is installed on your machine.

## Prerequisites

### Install Hadolint

#### **macOS** (via Homebrew)

1. Install Homebrew (if not already installed):
  
  ```bash
  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
  ```
  
2. Install Hadolint:
  
  ```bash
  brew install hadolint
  ```
  

#### **Linux**

1. Download Hadolint:
  
  ```bash
  curl -L -o hadolint https://github.com/hadolint/hadolint/releases/latest/download/hadolint-Linux-x86_64
  ```
  
2. Make it executable:
  
  ```bash
  chmod +x hadolint
  ```
  
3. Move it to a directory in your PATH:
  
  ```bash
  sudo mv hadolint /usr/local/bin/hadolint
  ```
  

#### **Windows** (via Scoop)

1. Install Scoop (if not already installed):
  
  ```powershell
  Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
  irm get.scoop.sh | iex
  ```
  
2. Install Hadolint:
  
  ```powershell
  scoop install hadolint
  ```
  

#### **Verify Installation**

Run this command to check if Hadolint is installed:

```bash
hadolint --version
```

---

## Using the Plugin

1. Download and install the latest **Hadolint Plugin** from the ZIP file in [Releases](https://github.com/vrdc90/HadolintIntellij/releases) (until it is pushed to Marketplace).
  
2. Open a Dockerfile in IntelliJ, and the plugin will automatically lint it using Hadolint.
  
<br><br>
Happy Dockerfile linting!
