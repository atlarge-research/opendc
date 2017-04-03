///<reference path="../../typings/index.d.ts" />
import * as $ from "jquery";


// Redirect the user to the splash page, if not signed in
if (localStorage.getItem("googleToken") === null) {
    window.location.replace("/");
}

// Set the username in the navbar
$("nav .user .username").text(localStorage.getItem("googleName"));


// Set the language of the GAuth button to be English
window["___gcfg"] = {
    lang: 'en'
};

/**
 * Google signin button.
 */
window["gapiSigninButton"] = () => {
    gapi.signin2.render('google-signin', {
        'scope': 'profile email',
        'onsuccess': (googleUser) => {
            const auth2 = gapi.auth2.getAuthInstance();

            // Handle signout click
            $("nav .user .sign-out").click(() => {
                removeUserInfo();
                auth2.signOut().then(() => {
                    window.location.href = "/";
                });
            });

            // Check if the token has expired
            const currentTime = (new Date()).getTime() / 1000;

            if (parseInt(localStorage.getItem("googleTokenExpiration")) - currentTime <= 0) {
                auth2.signIn().then(() => {
                    localStorage.setItem("googleToken", googleUser.getAuthResponse().id_token);
                    const expirationTime = (new Date()).getTime() / 1000 + parseInt(googleUser.getAuthResponse().expires_in) - 5;
                    localStorage.setItem("googleTokenExpiration", expirationTime.toString());
                });
            }
        },
        'onfailure': () => {
            window.location.href = "/";
            console.log("Oops, something went wrong with your Google signin... Try again?");
        }
    });
};


/**
 * Removes session storage items.
 */
export function removeUserInfo() {
    localStorage.removeItem("googleToken");
    localStorage.removeItem("googleTokenExpiration");
    localStorage.removeItem("googleName");
    localStorage.removeItem("googleEmail");
    localStorage.removeItem("userId");
    localStorage.removeItem("simulationId");
}
