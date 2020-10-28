from opendc.util.database import DB

test_id = 24 * '1'
test_id_2 = 24 * '2'


def test_get_project_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.get(f'/v2/projects/{test_id}').status


def test_get_project_no_authorizations(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'authorizations': []})
    res = client.get(f'/v2/projects/{test_id}')
    assert '403' in res.status


def test_get_project_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'authorizations': [{
                                'projectId': test_id_2,
                                'authorizationLevel': 'OWN'
                            }]
                        })
    res = client.get(f'/v2/projects/{test_id}')
    assert '403' in res.status


def test_get_project(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'EDIT'
                            }]
                        })
    res = client.get(f'/v2/projects/{test_id}')
    assert '200' in res.status


def test_update_project_missing_parameter(client):
    assert '400' in client.put(f'/v2/projects/{test_id}').status


def test_update_project_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.put(f'/v2/projects/{test_id}', json={'project': {'name': 'S'}}).status


def test_update_project_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'VIEW'
                            }]
                        })
    mocker.patch.object(DB, 'update', return_value={})
    assert '403' in client.put(f'/v2/projects/{test_id}', json={'project': {'name': 'S'}}).status


def test_update_project(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'OWN'
                            }]
                        })
    mocker.patch.object(DB, 'update', return_value={})

    res = client.put(f'/v2/projects/{test_id}', json={'project': {'name': 'S'}})
    assert '200' in res.status


def test_delete_project_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    assert '404' in client.delete(f'/v2/projects/{test_id}').status


def test_delete_project_different_user(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'googleId': 'other_test',
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'VIEW'
                            }],
                            'topologyIds': []
                        })
    mocker.patch.object(DB, 'delete_one', return_value=None)
    assert '403' in client.delete(f'/v2/projects/{test_id}').status


def test_delete_project(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'googleId': 'test',
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'OWN'
                            }],
                            'topologyIds': [],
                            'portfolioIds': [],
                        })
    mocker.patch.object(DB, 'update', return_value=None)
    mocker.patch.object(DB, 'delete_one', return_value={'googleId': 'test'})
    res = client.delete(f'/v2/projects/{test_id}')
    assert '200' in res.status
