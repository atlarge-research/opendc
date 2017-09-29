FROM gradle:alpine
MAINTAINER Fabian Mastenbroek <f.s.mastenbroek@student.tudelft.nl>

# Copy OpenDC simulator
COPY ./ /home/gradle/simulator

# Fix permissions
USER root
RUN chown -R gradle:gradle /home/gradle/simulator && \
	chmod -R 771 /home/gradle/simulator
USER gradle

# Set the working directory to the JPA integration
WORKDIR /home/gradle/simulator/opendc-integration-jpa

# Build the application
RUN gradle --no-daemon installDist

# Run the application
CMD build/install/opendc-integration-jpa/bin/opendc-integration-jpa
