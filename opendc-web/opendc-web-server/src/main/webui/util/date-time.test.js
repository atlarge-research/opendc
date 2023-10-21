import { convertSecondsToFormattedTime } from './date-time'

describe('tick formatting', () => {
    it("returns '0s' for numbers <= 0", () => {
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
