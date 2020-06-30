from opendc.util.database import DB


def test_get_authorizations_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    mocker.patch.object(DB, 'fetch_all', return_value=None)
    assert '404' in client.get('/api/v2/simulations/1/authorizations').status


def test_get_authorizations_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'name': 'test trace',
                            'authorizations': [{
                                'simulationId': '2',
                                'authorizationLevel': 'OWN'
                            }]
                        })
    mocker.patch.object(DB, 'fetch_all', return_value=[])
    res = client.get('/api/v2/simulations/1/authorizations')
    assert '403' in res.status


def test_get_authorizations(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'name': 'test trace',
                            'authorizations': [{
                                'simulationId': '1',
                                'authorizationLevel': 'OWN'
                            }]
                        })
    mocker.patch.object(DB, 'fetch_all', return_value=[])
    res = client.get('/api/v2/simulations/1/authorizations')
    assert len(res.json['content']) == 0
    assert '200' in res.status
