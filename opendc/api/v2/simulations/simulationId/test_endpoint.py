from opendc.util.database import DB


def test_get_simulation_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.get('/api/v2/simulations/1').status


def test_get_simulation_no_authorizations(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'authorizations': []})
    res = client.get('/api/v2/simulations/1')
    assert '403' in res.status


def test_get_simulation_not_authorized(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'authorizations': [{'simulationId': '2', 'authorizationLevel': 'OWN'}]})
    res = client.get('/api/v2/simulations/1')
    assert '403' in res.status


def test_get_simulation(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'authorizations': [{'simulationId': '1', 'authorizationLevel': 'EDIT'}]})
    res = client.get('/api/v2/simulations/1')
    assert '200' in res.status
