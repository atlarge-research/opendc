FROM python:3.8
MAINTAINER OpenDC Maintainers <opendc@atlarge-research.com>

# Ensure the STDOUT is not buffered by Python so that our logs become visible
# See https://stackoverflow.com/q/29663459/10213073
ENV PYTHONUNBUFFERED 1

# Copy OpenDC directory
COPY ./ /opendc

# Fetch web server dependencies
RUN pip install -r /opendc/requirements.txt

# Set working directory
WORKDIR /opendc

CMD ["python3", "main.py"]
