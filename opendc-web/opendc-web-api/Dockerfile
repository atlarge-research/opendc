FROM python:3.9-slim
MAINTAINER OpenDC Maintainers <opendc@atlarge-research.com>

# Ensure the STDOUT is not buffered by Python so that our logs become visible
# See https://stackoverflow.com/q/29663459/10213073
ENV PYTHONUNBUFFERED 1

# Copy OpenDC directory
COPY ./ /opendc

# Fetch web server dependencies
RUN pip install -r /opendc/requirements.txt && pip install pyuwsgi

# Create opendc user
RUN groupadd --gid 1000 opendc \
  && useradd --uid 1000 --gid opendc --shell /bin/bash --create-home opendc
RUN chown -R opendc:opendc /opendc
USER opendc

# Set working directory
WORKDIR /opendc

CMD uwsgi -M --socket 0.0.0.0:80 --protocol=http --wsgi-file app.py --enable-threads --processes 2 --lazy-app
