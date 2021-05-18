#!/usr/bin/env python3
import mimetypes
import os

from dotenv import load_dotenv
from flask import Flask, jsonify, redirect
from flask_compress import Compress
from flask_cors import CORS
from flask_restful import Api
from flask_swagger_ui import get_swaggerui_blueprint
from marshmallow import ValidationError

from opendc.api.portfolios import Portfolio, PortfolioScenarios
from opendc.api.prefabs import Prefab, PrefabList
from opendc.api.projects import ProjectList, Project, ProjectTopologies, ProjectPortfolios
from opendc.api.scenarios import Scenario
from opendc.api.schedulers import SchedulerList
from opendc.api.topologies import Topology
from opendc.api.traces import TraceList, Trace
from opendc.auth import AuthError
from opendc.util import JSONEncoder


# Load environmental variables from dotenv file
load_dotenv()


def setup_sentry():
    """
    Setup the Sentry integration for Flask if a DSN is supplied via the environmental variables.
    """
    if 'SENTRY_DSN' not in os.environ:
        return

    import sentry_sdk
    from sentry_sdk.integrations.flask import FlaskIntegration

    sentry_sdk.init(
        integrations=[FlaskIntegration()],
        traces_sample_rate=0.1
    )


def setup_api(app):
    """
    Setup the API interface.
    """
    api = Api(app)
    # Map to ('string', 'ObjectId') passing type and format
    api.add_resource(ProjectList, '/projects/')
    api.add_resource(Project, '/projects/<string:project_id>')
    api.add_resource(ProjectTopologies, '/projects/<string:project_id>/topologies')
    api.add_resource(ProjectPortfolios, '/projects/<string:project_id>/portfolios')
    api.add_resource(Topology, '/topologies/<string:topology_id>')
    api.add_resource(PrefabList, '/prefabs/')
    api.add_resource(Prefab, '/prefabs/<string:prefab_id>')
    api.add_resource(Portfolio, '/portfolios/<string:portfolio_id>')
    api.add_resource(PortfolioScenarios, '/portfolios/<string:portfolio_id>/scenarios')
    api.add_resource(Scenario, '/scenarios/<string:scenario_id>')
    api.add_resource(TraceList, '/traces/')
    api.add_resource(Trace, '/traces/<string:trace_id>')
    api.add_resource(SchedulerList, '/schedulers/')

    @app.errorhandler(AuthError)
    def handle_auth_error(ex):
        response = jsonify(ex.error)
        response.status_code = ex.status_code
        return response

    @app.errorhandler(ValidationError)
    def handle_validation_error(ex):
        return {'message': 'Input validation failed', 'errors': ex.messages}, 400

    return api


def setup_swagger(app):
    """
    Setup Swagger UI
    """
    SWAGGER_URL = '/docs'
    API_URL = '../schema.yml'

    swaggerui_blueprint = get_swaggerui_blueprint(
        SWAGGER_URL,
        API_URL,
        config={
            'app_name': "OpenDC API v2"
        },
        oauth_config={
            'clientId': os.environ.get("AUTH0_DOCS_CLIENT_ID", ""),
        }
    )
    app.register_blueprint(swaggerui_blueprint)


def create_app(testing=False):
    app = Flask(__name__, static_url_path='/')
    app.config['TESTING'] = testing
    app.config['SECRET_KEY'] = os.environ['OPENDC_FLASK_SECRET']
    app.config['RESTFUL_JSON'] = {'cls': JSONEncoder}
    app.json_encoder = JSONEncoder

    # Define YAML content type
    mimetypes.add_type('text/yaml', '.yml')

    # Setup Sentry if DSN is specified
    setup_sentry()

    # Set up CORS support
    CORS(app)

    # Setup compression
    compress = Compress()
    compress.init_app(app)

    setup_api(app)
    setup_swagger(app)

    @app.route('/')
    def index():
        """
        Redirect the user to the API documentation if it accesses the API root.
        """
        return redirect('docs/')

    return app


application = create_app(testing="OPENDC_FLASK_TESTING" in os.environ)

if __name__ == '__main__':
    application.run()
