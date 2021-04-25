from opendc.util.database import DB

test_id = 24 * '1'


def test_add_topology_missing_parameter(client):
    assert '400' in client.post(f'/v2/projects/{test_id}/topologies').status


def test_add_topology(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'OWN'
                            }],
                            'topologyIds': []
                        })
    mocker.patch.object(DB,
                        'insert',
                        return_value={
                            '_id': test_id,
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'topologyIds': []
                        })
    mocker.patch.object(DB, 'update', return_value={})
    res = client.post(f'/v2/projects/{test_id}/topologies', json={'topology': {'name': 'test project', 'rooms': []}})
    assert 'rooms' in res.json['content']
    assert '200' in res.status


def test_add_topology_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'projectId': test_id,
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'VIEW'
                            }]
                        })
    assert '403' in client.post(f'/v2/projects/{test_id}/topologies',
                                json={
                                    'topology': {
                                        'name': 'test_topology',
                                        'rooms': {}
                                    }
                                }).status
