from opendc.util.database import DB

test_id = 24 * '1'


def test_get_user_projects(client, mocker):
    mocker.patch.object(DB, 'fetch_all', return_value={'_id': test_id, 'authorizations': [{'userId': 'test',
                                                                                           'authorizationLevel': 'OWN'}]})
    res = client.get('/v2/projects')
    assert '200' in res.status


def test_add_project_missing_parameter(client):
    assert '400' in client.post('/v2/projects').status


def test_add_project(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'_id': test_id, 'authorizations': []})
    mocker.patch.object(DB,
                        'insert',
                        return_value={
                            '_id': test_id,
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'topologyIds': []
                        })
    mocker.patch.object(DB, 'update', return_value={})
    res = client.post('/v2/projects', json={'project': {'name': 'test project'}})
    assert 'datetimeCreated' in res.json['content']
    assert 'datetimeLastEdited' in res.json['content']
    assert 'topologyIds' in res.json['content']
    assert '200' in res.status
