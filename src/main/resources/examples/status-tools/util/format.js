// Left-pad a string to the desired width using the provided character (default: space).
exports.pad = function pad(value, width, char) {
  var str = (value === null || value === undefined) ? "" : String(value);
  var fill = (char === undefined || char === null || String(char).length === 0) ? " " : String(char).charAt(0);
  if (str.length >= width) {
    return str;
  }
  var needed = width - str.length;
  var prefix = new Array(needed + 1).join(fill);
  return prefix + str;
};
