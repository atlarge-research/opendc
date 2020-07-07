import { convertSecondsToFormattedTime, parseDateTime } from './date-time'

describe('date-time parsing', () => {
    it('reads components properly', () => {
        const dateString = '2017-09-27T20:55:01'
        const parsedDate = parseDateTime(dateString)

        expect(parsedDate.getUTCFullYear()).toEqual(2017)
        expect(parsedDate.getUTCMonth()).toEqual(8)
        expect(parsedDate.getUTCDate()).toEqual(27)
        expect(parsedDate.getUTCHours()).toEqual(20)
        expect(parsedDate.getUTCMinutes()).toEqual(55)
        expect(parsedDate.getUTCSeconds()).toEqual(1)
    })
})

describe('tick formatting', () => {
    it('returns \'0s\' for numbers <= 0', () => {
        expect(convertSecondsToFormattedTime(-1)).toEqual('0s')
        expect(convertSecondsToFormattedTime(0)).toEqual('0s')
    })
    it('returns only seconds for values under a minute', () => {
        expect(convertSecondsToFormattedTime(1)).toEqual('1s')
        expect(convertSecondsToFormattedTime(59)).toEqual('59s')
    })
    it('returns seconds and minutes for values under an hour', () => {
        expect(convertSecondsToFormattedTime(60)).toEqual('1m00s')
        expect(convertSecondsToFormattedTime(61)).toEqual('1m01s')
        expect(convertSecondsToFormattedTime(3599)).toEqual('59m59s')
    })
    it('returns full time for values over an hour', () => {
        expect(convertSecondsToFormattedTime(3600)).toEqual('1h00m00s')
        expect(convertSecondsToFormattedTime(3601)).toEqual('1h00m01s')
    })
})
