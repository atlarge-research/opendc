from opendc.util.database import DB
from unittest.mock import Mock

test_id = 24 * '1'


def test_get_authorizations(client, mocker):
    DB.fetch_all = Mock()
    mocker.patch.object(DB, 'fetch_one', return_value={'_id': test_id})
    DB.fetch_all.side_effect = [
                        [{
                            '_id': test_id,
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'authorId': test_id,
                            'visibility' : 'private'
                        },
                        {
                            '_id': '2' * 24,
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'authorId': test_id,
                            'visibility' : 'private'
                        },
                        {
                            '_id': '3' * 24,
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'authorId': test_id,
                            'visibility' : 'public'
                        },
                        {
                            '_id': '4' * 24,
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'authorId': test_id,
                            'visibility' : 'public'
                        }],
                        [{
                            '_id': '5' * 24,
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'authorId': '2' * 24,
                            'visibility' : 'public'
                        },
                        {
                            '_id': '6' * 24,
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'authorId': '2' * 24,
                            'visibility' : 'public'
                        },
                        {
                            '_id': '7' * 24,
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'authorId': '2' * 24,
                            'visibility' : 'public'
                        },
                        {
                            '_id': '8' * 24,
                            'datetimeCreated': '000',
                            'datetimeLastEdited': '000',
                            'authorId': '2' * 24,
                            'visibility' : 'public'
                        }]
                        ]
    mocker.patch.object(DB, 'fetch_one', return_value={'_id': test_id})
    res = client.get('/api/v2/prefabs/authorizations')
    assert '200' in res.status

