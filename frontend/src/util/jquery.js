/**
 * Binding of the global jQuery variable for use within React.
 *
 * This should be used instead of '$', to address ESLint warnings relating to undefined global variables.
 */
const jQuery = window["$"];

export default jQuery;
