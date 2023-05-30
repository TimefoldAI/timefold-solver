function humanizeTime(milliseconds) {
    let seconds = Math.floor(milliseconds / 1000);
    let minutes = Math.floor(seconds / 60);
    let hours = Math.floor(minutes / 60);
    let days = Math.floor(hours / 24);
    hours %= 24;
    minutes %= 60;
    seconds %= 60;
    let millisecondsRemainder = milliseconds % 1000;
    let timeArr = [];
    if (days > 0) timeArr.push(days + "d");
    if (hours > 0) timeArr.push(hours + "h");
    if (minutes > 0) timeArr.push(minutes + "m");
    if (seconds > 0) timeArr.push(seconds + "s");
    if (millisecondsRemainder > 0) timeArr.push(millisecondsRemainder + "ms");
    return timeArr.join("");
}
