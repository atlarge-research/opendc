///<reference path="../../typings/index.d.ts" />
///<reference path="./definitions.ts" />
import * as $ from "jquery";
import {APIController} from "./controllers/connection/api";
window["jQuery"] = $;
require("jquery.easing");


// Variable to check whether user actively logged in by clicking the login button
let hasClickedLogin = false;


$(document).ready(() => {
    /**
     * jQuery for page scrolling feature
     */
    $('a.page-scroll').bind('click', function (event) {
        const $anchor = $(this);
        $('html, body').stop().animate({
            scrollTop: $($anchor.attr('href')).offset().top
        }, 1000, 'easeInOutExpo', () => {
            if ($anchor.attr('href') === "#page-top") {
                location.hash = '';
            } else {
                location.hash = $anchor.attr('href');
            }
        });
        event.preventDefault();
    });

    const checkScrollState = () => {
        const startY = 100;

        if ($(window).scrollTop() > startY || window.innerWidth < 768) {
            $('.navbar').removeClass("navbar-transparent");
        } else {
            $('.navbar').addClass("navbar-transparent");
        }
    };

    $(window).on("scroll load resize", function () {
        checkScrollState();
    });

    checkScrollState();

    const googleSigninBtn = $("#google-signin");
    googleSigninBtn.click(() => {
        hasClickedLogin = true;
    });

    /**
     * Display appropriate user buttons
     */
    if (localStorage.getItem("googleToken") !== null) {
        googleSigninBtn.hide();
        $(".navbar .logged-in").css("display", "inline-block");
        $(".logged-in .sign-out").click(() => {
            const auth2 = gapi.auth2.getAuthInstance();

            auth2.signOut().then(() => {
                // Remove session storage items
                localStorage.removeItem("googleToken");
                localStorage.removeItem("googleTokenExpiration");
                localStorage.removeItem("googleName");
                localStorage.removeItem("googleEmail");
                localStorage.removeItem("userId");
                localStorage.removeItem("simulationId");

                location.reload();
            });
        });

        // Check whether Google auth. token has expired and signin again if necessary
        const currentTime = (new Date()).getTime();
        if (parseInt(localStorage.getItem("googleTokenExpiration")) - currentTime <= 0) {
            gapi.auth2.getAuthInstance().signIn().then(() => {
                const authResponse = gapi.auth2.getAuthInstance().currentUser.get().getAuthResponse();
                localStorage.setItem("googleToken", authResponse.id_token);
                const expirationTime = (new Date()).getTime() / 1000 + parseInt(authResponse.expires_in) - 5;
                localStorage.setItem("googleTokenExpiration", expirationTime.toString());
            });
        }
    }
});

/**
 * Google signin button
 */
window["renderButton"] = () => {
    gapi.signin2.render('google-signin', {
        'scope': 'profile email',
        'width': 100,
        'height': 30,
        'longtitle': false,
        'theme': 'dark',
        'onsuccess': (googleUser) => {
            let api;
            new APIController((apiInstance: APIController) => {
                api = apiInstance;
                const email = googleUser.getBasicProfile().getEmail();

                const getUser = (userId: number) => {
                    const reload = localStorage.getItem("userId") === null;

                    localStorage.setItem("userId", userId.toString());

                    // Redirect to the projects page
                    if (hasClickedLogin) {
                        window.location.href = "projects";
                    } else if (reload) {
                        window.location.reload();
                    }

                };

                // Send the token to the server
                const id_token = googleUser.getAuthResponse().id_token;
                // Calculate token expiration time (in seconds since epoch)
                const expirationTime = (new Date()).getTime() / 1000 + googleUser.getAuthResponse().expires_in - 5;

                $.post('SERVER_BASE_URL/tokensignin', {
                    idtoken: id_token
                }, (data: any) => {
                    // Save user information in session storage for later use on other pages
                    localStorage.setItem("googleToken", id_token);
                    localStorage.setItem("googleTokenExpiration", expirationTime.toString());
                    localStorage.setItem("googleName", googleUser.getBasicProfile().getGivenName() + " " +
                        googleUser.getBasicProfile().getFamilyName());
                    localStorage.setItem("googleEmail", email);

                    if (data.isNewUser === true) {
                        api.addUser({
                            id: -1,
                            email,
                            googleId: googleUser.getBasicProfile().getId(),
                            givenName: googleUser.getBasicProfile().getGivenName(),
                            familyName: googleUser.getBasicProfile().getFamilyName()
                        }).then((userData: any) => {
                            getUser(userData.id);
                        });
                    } else {
                        getUser(data.userId);
                    }
                });
            });
        },
        'onfailure': () => {
            console.log("Oops, something went wrong with your Google signin... Try again?");
        }
    });
};

// Set the language of the GAuth button to be English
window["___gcfg"] = {
    lang: 'en'
};
