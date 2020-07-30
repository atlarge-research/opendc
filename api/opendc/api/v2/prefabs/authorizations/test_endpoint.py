from opendc.util.database import DB

def test_get_authorizations(client, mocker):
    mocker.patch.object(DB, 'fetch_one', return_value={'_id': '1'})
    mocker.patch.object(DB,
                        'fetch_all',
                        return_value=[{
                            '_id': '1',
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'authorId': 1,
                            'visibility' : 'private'
                        },
                        {
                            '_id': '2',
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'authorId': 2,
                            'visibility' : 'private'
                        },
                        {
                            '_id': '3',
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'authorId': 1,
                            'visibility' : 'public'
                        },
                        {
                            '_id': '2',
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'authorId': 2,
                            'visibility' : 'public'
                        }])
    res = client.get('/api/v2/prefabs/authorizations', json={'prefab': {'name': 'test prefab'}})
    assert '200' in res.status

