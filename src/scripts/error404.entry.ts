///<reference path="../../typings/globals/jquery/index.d.ts" />
import * as $ from "jquery";


$(document).ready(() => {
    let text =
        "    oo      oooo       oo   <br>" +
        "   oo      oo  oo     oo    <br>" +
        "  oo       oo  oo    oo     <br>" +
        " oooooo    oo  oo   oooooo  <br>" +
        "     oo    oo  oo       oo  <br>" +
        "     oo     oooo        oo  <br>";
    let charList = text.split('');

    let binary = "01001111011100000110010101101110010001000100001100100001";
    let binaryIndex = 0;

    for (let i = 0; i < charList.length; i++) {
        if (charList[i] === "o") {
            charList[i] = binary[binaryIndex];
            binaryIndex++;
        }
    }

    $(".code-block").html(charList.join(""));
});