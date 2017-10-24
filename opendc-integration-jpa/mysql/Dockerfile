# Docker mysql image for the OpenDC simulator project
# This image requires the context to be set to the root directory of the project in order to correctly build.
FROM gradle:alpine
MAINTAINER Fabian Mastenbroek <f.s.mastenbroek@student.tudelft.nl>

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
RUN gradle --no-daemon :opendc-integration-jpa:mysql:installDist

# Fix permissions
RUN chown -R gradle:gradle $APP_HOME

# Downgrade user
USER gradle

# Start the Gradle application on run
CMD opendc-integration-jpa/mysql/build/install/mysql/bin/mysql

