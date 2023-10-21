/**
 * Parses and formats the given date-time string representation.
 *
 * The format assumed is "YYYY-MM-DDTHH:MM:SS".
 *
 * @param dateTimeString A string expressing a date and a time, in the above mentioned format.
 * @returns {string} A human-friendly string version of that date and time.
 */
export function parseAndFormatDateTime(dateTimeString) {
    return formatDateTime(new Date(dateTimeString))
}

/**
 * Serializes the given date and time value to a human-friendly string.
 *
 * @param dateTime An object representation of a date and time.
 * @returns {string} A human-friendly string version of that date and time.
 */
export function formatDateTime(dateTime) {
    let date
    const currentDate = new Date()

    date =
        addPaddingToTwo(dateTime.getDay()) +
        '/' +
        addPaddingToTwo(dateTime.getMonth()) +
        '/' +
        addPaddingToTwo(dateTime.getFullYear())

    if (dateTime.getFullYear() === currentDate.getFullYear() && dateTime.getMonth() === currentDate.getMonth()) {
        if (dateTime.getDate() === currentDate.getDate()) {
            date = 'Today'
        } else if (dateTime.getDate() === currentDate.getDate() - 1) {
            date = 'Yesterday'
        }
    }

    return date + ', ' + addPaddingToTwo(dateTime.getHours()) + ':' + addPaddingToTwo(dateTime.getMinutes())
}

/**
 * Formats the given number of seconds/ticks to a formatted time representation.
 *
 * @param seconds The number of seconds.
 * @returns {string} A string representation of that amount of second, in the from of HH:MM:SS.
 */
export function convertSecondsToFormattedTime(seconds) {
    if (seconds <= 0) {
        return '0s'
    }

    let hour = Math.floor(seconds / 3600)
    let minute = Math.floor(seconds / 60) % 60
    let second = seconds % 60

    hour = isNaN(hour) ? 0 : hour
    minute = isNaN(minute) ? 0 : minute
    second = isNaN(second) ? 0 : second

    if (hour === 0 && minute === 0) {
        return second + 's'
    } else if (hour === 0) {
        return minute + 'm' + addPaddingToTwo(second) + 's'
    } else {
        return hour + 'h' + addPaddingToTwo(minute) + 'm' + addPaddingToTwo(second) + 's'
    }
}

/**
 * Pads the given integer to have at least two digits.
 *
 * @param integer An integer to be padded.
 * @returns {string} A string containing the padded integer.
 */
function addPaddingToTwo(integer) {
    if (integer < 10) {
        return '0' + integer.toString()
    } else {
        return integer.toString()
    }
}
