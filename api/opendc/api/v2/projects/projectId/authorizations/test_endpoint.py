from opendc.util.database import DB

test_id = 24 * '1'
test_id_2 = 24 * '2'


def test_get_authorizations_non_existing(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value=None)
    mocker.patch.object(DB, 'fetch_all', return_value=None)
    assert '404' in client.get(f'/api/v2/projects/{test_id}/authorizations').status


def test_get_authorizations_not_authorized(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'name': 'test trace',
                            'authorizations': [{
                                'projectId': test_id_2,
                                'authorizationLevel': 'OWN'
                            }]
                        })
    mocker.patch.object(DB, 'fetch_all', return_value=[])
    res = client.get(f'/api/v2/projects/{test_id}/authorizations')
    assert '403' in res.status


def test_get_authorizations(client, mocker):
    mocker.patch.object(DB,
                        'fetch_one',
                        return_value={
                            '_id': test_id,
                            'name': 'test trace',
                            'authorizations': [{
                                'projectId': test_id,
                                'authorizationLevel': 'OWN'
                            }]
                        })
    mocker.patch.object(DB, 'fetch_all', return_value=[])
    res = client.get(f'/api/v2/projects/{test_id}/authorizations')
    assert len(res.json['content']) == 0
    assert '200' in res.status
