FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /opendc

# Install Node.js for JSDoc generation
RUN apt-get update && apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_lts.x | bash - && \
    apt-get install -y nodejs && \
    rm -rf /var/lib/apt/lists/*

# Copy project files
COPY . .

# Generate Kotlin/Java API docs (Dokka)
RUN ./gradlew generateDocs --no-daemon

# Generate frontend docs (JSDoc)
RUN cd opendc-web/opendc-web-server/src/main/webui && \
    npm ci && \
    npx jsdoc -c jsdoc.json -r -d /opendc/build/docs/frontend

# --- Serve stage ---
FROM nginx:alpine

COPY opendc-docs/index.html /usr/share/nginx/html/index.html
COPY --from=build /opendc/build/docs/api/ /usr/share/nginx/html/api/
COPY --from=build /opendc/build/docs/frontend/ /usr/share/nginx/html/frontend/

EXPOSE 80
