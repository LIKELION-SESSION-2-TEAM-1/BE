FROM eclipse-temurin:21-jdk

# 1. Install Python3, Pip, and Chromium
# Chromium is used because it's easier to install on Debian/Ubuntu based images than Google Chrome
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    chromium \
    chromium-driver \
    && rm -rf /var/lib/apt/lists/*

# 2. Install Python dependencies
# --break-system-packages is required for recent Python versions in managed environments
RUN pip3 install selenium webdriver-manager --break-system-packages

# 3. Set working directory
WORKDIR /app

# 4. Copy project files
COPY . .

# 5. Grant execution permission to gradlew
RUN chmod +x gradlew

# 6. Build the application (skipping tests to speed up)
RUN ./gradlew clean build -x test

# 7. Set Environment Variables
# Tell the Java app to use 'python3' command
ENV PYTHON_EXECUTABLE=python3

# 8. Expose port
EXPOSE 8080

# 9. Run the application
# Adjust the jar name if your version or project name changes
CMD ["java", "-jar", "build/libs/service-0.0.1-SNAPSHOT.jar"]
