#  Copyright (c) 2021 AtLarge Research
#
#  Permission is hereby granted, free of charge, to any person obtaining a copy
#  of this software and associated documentation files (the "Software"), to deal
#  in the Software without restriction, including without limitation the rights
#  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
#  copies of the Software, and to permit persons to whom the Software is
#  furnished to do so, subject to the following conditions:
#
#  The above copyright notice and this permission notice shall be included in all
#  copies or substantial portions of the Software.
#
#  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
#  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
#  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
#  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
#  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
#  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
#  SOFTWARE.

#
#  Permission is hereby granted, free of charge, to any person obtaining a copy
#  of this software and associated documentation files (the "Software"), to deal
#  in the Software without restriction, including without limitation the rights
#  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
#  copies of the Software, and to permit persons to whom the Software is
#  furnished to do so, subject to the following conditions:
#
#
from datetime import datetime

from opendc.exts import db

test_id = 24 * '1'
test_id_2 = 24 * '2'


def test_get_jobs(client, mocker):
    mocker.patch.object(db, 'fetch_all', return_value=[
        {'_id': 'a', 'scenarioId': 'x', 'portfolioId': 'y', 'simulation': {'state': 'QUEUED'}}
    ])
    res = client.get('/jobs/')
    assert '200' in res.status


def test_get_job_non_existing(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value=None)
    assert '404' in client.get(f'/jobs/{test_id}').status


def test_get_job(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value={
        '_id': 'a', 'scenarioId': 'x', 'portfolioId': 'y', 'simulation': {'state': 'QUEUED'}
    })
    res = client.get(f'/jobs/{test_id}')
    assert '200' in res.status


def test_update_job_nop(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value={
        '_id': test_id, 'scenarioId': 'x', 'portfolioId': 'y', 'simulation': {'state': 'QUEUED'}
    })
    update_mock = mocker.patch.object(db, 'fetch_and_update', return_value={
        '_id': test_id, 'scenarioId': 'x', 'portfolioId': 'y',
        'simulation': {'state': 'QUEUED', 'heartbeat': datetime.now()}
    })
    res = client.post(f'/jobs/{test_id}', json={'state': 'QUEUED'})
    assert '200' in res.status
    update_mock.assert_called_once()


def test_update_job_invalid_state(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value={
        '_id': test_id, 'scenarioId': 'x', 'portfolioId': 'y', 'simulation': {'state': 'QUEUED'}
    })
    res = client.post(f'/jobs/{test_id}', json={'state': 'FINISHED'})
    assert '400' in res.status


def test_update_job_claim(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value={
        '_id': test_id, 'scenarioId': 'x', 'portfolioId': 'y', 'simulation': {'state': 'QUEUED'}
    })
    update_mock = mocker.patch.object(db, 'fetch_and_update', return_value={
        '_id': test_id, 'scenarioId': 'x', 'portfolioId': 'y',
        'simulation': {'state': 'CLAIMED', 'heartbeat': datetime.now()}
    })
    res = client.post(f'/jobs/{test_id}', json={'state': 'CLAIMED'})
    assert '200' in res.status
    update_mock.assert_called_once()


def test_update_job_conflict(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value={
        '_id': test_id, 'scenarioId': 'x', 'portfolioId': 'y', 'simulation': {'state': 'QUEUED'}
    })
    update_mock = mocker.patch.object(db, 'fetch_and_update', return_value=None)
    res = client.post(f'/jobs/{test_id}', json={'state': 'CLAIMED'})
    assert '409' in res.status
    update_mock.assert_called_once()


def test_update_job_run(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value={
        '_id': test_id, 'scenarioId': 'x', 'portfolioId': 'y', 'simulation': {'state': 'CLAIMED'}
    })
    update_mock = mocker.patch.object(db, 'fetch_and_update', return_value={
        '_id': test_id, 'scenarioId': 'x', 'portfolioId': 'y',
        'simulation': {'state': 'RUNNING', 'heartbeat': datetime.now()}
    })
    res = client.post(f'/jobs/{test_id}', json={'state': 'RUNNING'})
    assert '200' in res.status
    update_mock.assert_called_once()


def test_update_job_finished(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value={
        '_id': test_id, 'scenarioId': 'x', 'portfolioId': 'y', 'simulation': {'state': 'RUNNING'}
    })
    update_mock = mocker.patch.object(db, 'fetch_and_update', return_value={
        '_id': test_id, 'scenarioId': 'x', 'portfolioId': 'y',
        'simulation': {'state': 'FINISHED', 'heartbeat': datetime.now()}
    })
    res = client.post(f'/jobs/{test_id}', json={'state': 'FINISHED'})
    assert '200' in res.status
    update_mock.assert_called_once()


def test_update_job_failed(client, mocker):
    mocker.patch.object(db, 'fetch_one', return_value={
        '_id': test_id, 'scenarioId': 'x', 'portfolioId': 'y', 'simulation': {'state': 'RUNNING'}
    })
    update_mock = mocker.patch.object(db, 'fetch_and_update', return_value={
        '_id': test_id, 'scenarioId': 'x', 'portfolioId': 'y',
        'simulation': {'state': 'FAILED', 'heartbeat': datetime.now()}
    })
    res = client.post(f'/jobs/{test_id}', json={'state': 'FAILED'})
    assert '200' in res.status
    update_mock.assert_called_once()
