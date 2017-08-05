/**
 * Parses date-time string representations and returns a parsed object.
 *
 * The format assumed is "YYYY-MM-DDTHH:MM:SS".
 *
 * @param dateTimeString A string expressing a date and a time, in the above mentioned format.
 * @returns {object} An object with the parsed date and time information as content.
 */
export function parseDateTime(dateTimeString) {
    const output = {
        year: 0,
        month: 0,
        day: 0,
        hour: 0,
        minute: 0,
        second: 0
    };

    const dateAndTime = dateTimeString.split("T");
    const dateComponents = dateAndTime[0].split("-");
    output.year = parseInt(dateComponents[0]);
    output.month = parseInt(dateComponents[1]);
    output.day = parseInt(dateComponents[2]);

    const timeComponents = dateAndTime[1].split(":");
    output.hour = parseInt(timeComponents[0]);
    output.minute = parseInt(timeComponents[1]);
    output.second = parseInt(timeComponents[2]);

    return output;
}

/**
 * Serializes the given date and time value to a string.
 *
 * The format assumed is "YYYY-MM-DDTHH:MM:SS".
 *
 * @param dateTime An object representation of a date and time.
 * @returns {string} A string representation of that date and time.
 */
export function formatDateTime(dateTime) {
    let date;
    const currentDate = new Date();

    date = addPaddingToTwo(dateTime.day) + "/" +
        addPaddingToTwo(dateTime.month) + "/" +
        addPaddingToTwo(dateTime.year);

    if (dateTime.year === currentDate.getFullYear() &&
        dateTime.month === currentDate.getMonth() + 1) {
        if (dateTime.day === currentDate.getDate()) {
            date = "Today";
        } else if (dateTime.day === currentDate.getDate() - 1) {
            date = "Yesterday";
        }
    }

    return date + ", " +
        addPaddingToTwo(dateTime.hour) + ":" +
        addPaddingToTwo(dateTime.minute);
}

/**
 * Returns a string representation of the current date and time.
 *
 * The format assumed is "YYYY-MM-DDTHH:MM:SS".
 *
 * @returns {string} A string representation of the current date and time.
 */
export function getCurrentDateTime() {
    const currentDate = new Date();
    return currentDate.getFullYear() + "-" + addPaddingToTwo(currentDate.getMonth() + 1) + "-" +
        addPaddingToTwo(currentDate.getDate()) + "T" + addPaddingToTwo(currentDate.getHours()) + ":" +
        addPaddingToTwo(currentDate.getMinutes()) + ":" + addPaddingToTwo(currentDate.getSeconds());
}

/**
 * Pads the given integer to have at least two digits.
 *
 * @param integer An integer to be padded.
 * @returns {string} A string containing the padded integer
 */
export function addPaddingToTwo(integer) {
    if (integer < 10) {
        return "0" + integer.toString();
    } else {
        return integer.toString();
    }
}
