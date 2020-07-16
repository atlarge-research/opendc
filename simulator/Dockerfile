FROM gradle:jdk14
MAINTAINER OpenDC Maintainers <opendc@atlarge-research.com>

# Set the home directory to our gradle user's home.
ENV HOME=/home/gradle
ENV APP_HOME=$HOME/simulator

# Copy OpenDC simulator
COPY ./ $APP_HOME

# Build as root
USER root

# Set the working directory to the simulator
WORKDIR $APP_HOME

# Build the application
RUN gradle --no-daemon assemble installDist

# Fix permissions
RUN chown -R gradle:gradle $APP_HOME

# Downgrade user
USER gradle

# Start the Gradle application on run
CMD opendc/opendc-runner-web/build/install/opendc-runner-web/bin/opendc-runner-web
