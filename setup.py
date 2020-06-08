from codecs import open
from os import path

from setuptools import setup

# Get the long description from the README file
here = path.abspath(path.dirname(__file__))
with open(path.join(here, 'README.md'), encoding='utf-8') as f:
    long_description = f.read()

setup(
    name='opendc-web-server',
    version='0.1.0',

    description='Python web server for the OpenDC project',
    long_description=long_description,

    url='http://opendc.org',

    author='The OpenDC team',
    author_email='opendc@atlarge-research.com',

    license='MIT',

    classifiers=[
        'License :: OSI Approved :: MIT License',

        'Programming Language :: Python :: 2',
        'Programming Language :: Python :: 2.7',
    ],

    keywords='opendc datacenter simulation web-server',

    packages=['opendc'],

    install_requires=[
        'flask==1.0.2',
        'flask-socketio==3.0.2',
        'oauth2client==4.1.3',
        'eventlet==0.24.1',
        'flask-compress==1.4.0',
        'flask-cors==3.0.8',
        'mysql-connector-python-rf==2.2.2',
        'pyasn1-modules==0.2.2',
        'six==1.11.0'
    ],
)
