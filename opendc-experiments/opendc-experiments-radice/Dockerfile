FROM ${ARCH}debian:bookworm-slim
MAINTAINER OpenDC Maintainers <opendc@atlarge-research.com>

# Copy capsule into image
COPY analysis.ipynb /opt/radice/
COPY portfolios /opt/radice/portfolios
COPY scripts /opt/radice/scripts
COPY bin /opt/radice/bin
COPY lib /opt/radice/lib
WORKDIR /opt/radice

# Install necessary packages
RUN apt-get update \
    && apt-get install -y openjdk-17-jre-headless jupyter python3-pip python3-numpy python3-pandas python3-scipy python3-matplotlib python3-seaborn \
    && pip3 install --no-cache-dir --break-system-packages pyarrow

# Run Jupyter lab
CMD jupyter notebook --allow-root --ip 0.0.0.0
