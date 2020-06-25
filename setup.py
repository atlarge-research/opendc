from codecs import open
from os import path

from setuptools import setup

# Get the long description from the README file
here = path.abspath(path.dirname(__file__))
with open(path.join(here, 'README.md'), encoding='utf-8') as f:
    long_description = f.read()

setup(
    name='opendc-web-server',
    version='0.2.0',
    description='Python web server for the OpenDC project',
    long_description=long_description,
    url='http://opendc.org',
    author='The OpenDC team',
    author_email='opendc@atlarge-research.com',
    license='MIT',
    classifiers=[
        'License :: OSI Approved :: MIT License',
        'Programming Language :: Python :: 3',
        'Programming Language :: Python :: 3.7',
    ],
    keywords='opendc datacenter simulation web-server',
    packages=['opendc'],
    # yapf: disable
    install_requires=[
        'flask==1.0.2',
        'flask-socketio==3.0.2',
        'oauth2client==4.1.3',
        'eventlet==0.24.1',
        'flask-compress==1.4.0',
        'flask-cors==3.0.8',
        'pyasn1-modules==0.2.2',
        'six==1.15.0',
        'pymongo==3.10.1',
        'bson==0.5.10',
        'yapf==0.30.0',
        'pytest==5.4.3',
        'pytest-mock==3.1.1',
        'pytest-env==0.6.2',
        'pylint==2.5.3',
    ],
    # yapf: enable
)
