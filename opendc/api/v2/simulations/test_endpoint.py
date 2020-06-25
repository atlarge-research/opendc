from opendc.util.database import DB


def test_add_simulation_missing_parameter(client):
    assert '400' in client.post('/api/v2/simulations').status


def test_add_simulation(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'_id': '1', 'authorizations': []})
    mocker.patch.object(DB, 'insert', return_value={'_id': '1', 'datetimeCreated': '000', 'datetimeEdit': '000', 'topologyIds': []})
    mocker.patch.object(DB, 'update', return_value={})
    res = client.post('/api/v2/simulations', json={'simulation': {'name': 'test simulation'}})
    assert 'datetimeCreated' in res.json['content']
    assert 'datetimeEdit' in res.json['content']
    assert 'topologyIds' in res.json['content']
    assert '200' in res.status
