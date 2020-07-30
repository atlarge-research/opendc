from opendc.util.database import DB
from unittest.mock import Mock

def test_get_authorizations(client, mocker):
    DB.fetch_all = Mock()
    mocker.patch.object(DB, 'fetch_one', return_value={'_id': '1'})
    DB.fetch_all.side_effect = [
                        [{
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
                            'authorId': 1,
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
                            '_id': '4',
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'authorId': 1,
                            'visibility' : 'public'
                        }],
                        [{
                            '_id': '5',
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'authorId': 2,
                            'visibility' : 'public'
                        },
                        {
                            '_id': '6',
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'authorId': 2,
                            'visibility' : 'public'
                        },
                        {
                            '_id': '7',
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'authorId': 2,
                            'visibility' : 'public'
                        },
                        {
                            '_id': '8',
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'authorId': 2,
                            'visibility' : 'public'
                        }]
                        ]
    mocker.patch.object(DB, 'fetch_one', return_value={'_id': '1'})
    res = client.get('/api/v2/prefabs/authorizations')
    assert '200' in res.status

