from opendc.util.database import DB


def test_add_simulation_missing_parameter(client):
    assert '400' in client.post('/api/v2/prefabs').status


def test_add_simulation(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'_id': '1', 'authorizations': []})
    mocker.patch.object(DB,
                        'insert',
                        return_value={
                            '_id': '1',
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000'
                        })
    mocker.patch.object(DB, 'update', return_value={})
    res = client.post('/api/v2/prefabs', json={'prefab': {'name': 'test prefab'}})
    assert 'datetimeCreated' in res.json['content']
    assert 'datetimeLastEdited' in res.json['content']
    assert '200' in res.status
