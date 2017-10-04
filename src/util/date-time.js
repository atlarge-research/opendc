/**
 * Parses and formats the given date-time string representation.
 *
 * The format assumed is "YYYY-MM-DDTHH:MM:SS".
 *
 * @param dateTimeString A string expressing a date and a time, in the above mentioned format.
 * @returns {string} A human-friendly string version of that date and time.
 */
export function parseAndFormatDateTime(dateTimeString) {
  return formatDateTime(parseDateTime(dateTimeString));
}

/**
 * Parses date-time string representations and returns a parsed rack.
 *
 * The format assumed is "YYYY-MM-DDTHH:MM:SS".
 *
 * @param dateTimeString A string expressing a date and a time, in the above mentioned format.
 * @returns {object} An rack with the parsed date and time information as content.
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
  output.year = parseInt(dateComponents[0], 10);
  output.month = parseInt(dateComponents[1], 10);
  output.day = parseInt(dateComponents[2], 10);

  const timeComponents = dateAndTime[1].split(":");
  output.hour = parseInt(timeComponents[0], 10);
  output.minute = parseInt(timeComponents[1], 10);
  output.second = parseInt(timeComponents[2], 10);

  return output;
}

/**
 * Serializes the given date and time value to a human-friendly string.
 *
 * @param dateTime An object representation of a date and time.
 * @returns {string} A human-friendly string version of that date and time.
 */
export function formatDateTime(dateTime) {
  let date;
  const currentDate = new Date();

  date =
    addPaddingToTwo(dateTime.day) +
    "/" +
    addPaddingToTwo(dateTime.month) +
    "/" +
    addPaddingToTwo(dateTime.year);

  if (
    dateTime.year === currentDate.getFullYear() &&
    dateTime.month === currentDate.getMonth() + 1
  ) {
    if (dateTime.day === currentDate.getDate()) {
      date = "Today";
    } else if (dateTime.day === currentDate.getDate() - 1) {
      date = "Yesterday";
    }
  }

  return (
    date +
    ", " +
    addPaddingToTwo(dateTime.hour) +
    ":" +
    addPaddingToTwo(dateTime.minute)
  );
}

/**
 * Formats the given number of seconds/ticks to a formatted time representation.
 *
 * @param seconds The number of seconds.
 * @returns {string} A string representation of that amount of second, in the from of HH:MM:SS.
 */
export function convertSecondsToFormattedTime(seconds) {
  if (seconds <= 0) {
    return "0s";
  }

  let hour = Math.floor(seconds / 3600);
  let minute = Math.floor(seconds / 60) % 60;
  let second = seconds % 60;

  hour = isNaN(hour) ? 0 : hour;
  minute = isNaN(minute) ? 0 : minute;
  second = isNaN(second) ? 0 : second;

  if (hour === 0 && minute === 0) {
    return second + "s";
  } else if (hour === 0) {
    return minute + "m" + addPaddingToTwo(second) + "s";
  } else {
    return (
      hour + "h" + addPaddingToTwo(minute) + "m" + addPaddingToTwo(second) + "s"
    );
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
    return "0" + integer.toString();
  } else {
    return integer.toString();
  }
}
