FROM jenkins/jenkins:lts-jdk17

USER root

# Base tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl ca-certificates gnupg jq unzip tar git bash yq python3 python3-pip \
 && rm -rf /var/lib/apt/lists/*

# # Docker CLI
# RUN install -m 0755 -d /etc/apt/keyrings && \
#    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg && \
#    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian bookworm stable" \
#    > /etc/apt/sources.list.d/docker.list && \
#    apt-get update && apt-get install -y --no-install-recommends docker-ce-cli && \
#    rm -rf /var/lib/apt/lists/*
# # kubectl
# RUN curl -fsSL https://storage.googleapis.com/kubernetes-release/release/stable.txt -o /tmp/kver \
#  && curl -fsSL https://storage.googleapis.com/kubernetes-release/release/$(cat /tmp/kver)/bin/linux/amd64/kubectl -o /usr/local/bin/kubectl \
#  && chmod +x /usr/local/bin/kubectl

# # Helm
# RUN curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# # Conftest
# RUN set -eux; \
#    VERSION="0.63.0"; \
#    ARCH=$(arch); \
#    SYSTEM=$(uname); \
#    curl -fsSL -o "conftest_${VERSION}_${SYSTEM}_${ARCH}.tar.gz" "https://github.com/open-policy-agent/conftest/releases/download/v0.63.0/conftest_0.63.0_Linux_arm64.tar.gz"; \
#    tar xzf "conftest_${VERSION}_${SYSTEM}_${ARCH}.tar.gz"; \
#    mv conftest /usr/local/bin/; \
#    rm -f "conftest_${VERSION}_${SYSTEM}_${ARCH}.tar.gz"

# # Trivy
# # Trivy: install latest release binary from GitHub to avoid relying on apt repo DNS
# RUN set -eux; \
#     LATEST=$(curl -s "https://api.github.com/repos/aquasecurity/trivy/releases/latest" | grep '"tag_name":' | sed -E 's/.*"([^"]+)".*/\1/' | tr -d 'v'); \
#     ARCH="Linux-64bit"; \
#     curl -fsSL -o /tmp/trivy.tar.gz "https://github.com/aquasecurity/trivy/releases/download/v${LATEST}/trivy_${LATEST}_${ARCH}.tar.gz"; \
#     tar -xzf /tmp/trivy.tar.gz -C /tmp; \
#     mv /tmp/trivy /usr/local/bin/; \
#     chmod +x /usr/local/bin/trivy; \
#     rm -rf /tmp/trivy*

# # Horusec CLI
# RUN set -eux; \
#     REPO="ZupIT/horusec"; \
#     API_URL="https://api.github.com/repos/${REPO}/releases/latest"; \
#     ASSET_URL=$(curl -s "$API_URL" | grep '"browser_download_url"' | grep -E "linux.*(amd64|x64)" | head -n1 | cut -d '"' -f4); \
#     if [ -z "$ASSET_URL" ]; then echo "No matching Horusec asset found for linux amd64/x64"; exit 1; fi; \
#     curl -fsSL "$ASSET_URL" -o /usr/local/bin/horusec; \
#     chmod +x /usr/local/bin/horusec

# # Python linters
# RUN pip3 install --no-cache-dir --break-system-packages \
#       pylint bandit

USER jenkins
