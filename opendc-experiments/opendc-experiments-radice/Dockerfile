FROM archlinux:base-20220306.0.49442
MAINTAINER OpenDC Maintainers <opendc@atlarge-research.com>

# Copy capsule into image
COPY . /opt/radice
WORKDIR /opt/radice

# Install necessary packages
RUN pacman -S - < packages.txt
