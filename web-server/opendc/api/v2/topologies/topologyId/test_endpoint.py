from opendc.util.database import DB
'''
GET /topologies/{topologyId}
'''


def test_get_topology(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'simulationId': '1',
                            'authorizations': [{
                                'simulationId': '1',
                                'authorizationLevel': 'EDIT'
                            }]
                        })
    res = client.get('/api/v2/topologies/1')
    assert '200' in res.status


def test_get_topology_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.get('/api/v2/topologies/1').status


def test_get_topology_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': '1',
                            'simulationId': '1',
                            'authorizations': [{
                                'simulationId': '2',
                                'authorizationLevel': 'OWN'
                            }]
                        })
    res = client.get('/api/v2/topologies/1')
    assert '403' in res.status


def test_get_topology_no_authorizations(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'authorizations': []})
    res = client.get('/api/v2/topologies/1')
    assert '403' in res.status


'''
PUT /topologies/{topologyId}
'''
'''
DELETE /topologies/{topologyId}
'''
