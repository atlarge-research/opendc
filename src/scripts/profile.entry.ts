///<reference path="../../typings/index.d.ts" />
import * as $ from "jquery";
import {APIController} from "./controllers/connection/api";
import {removeUserInfo} from "./user-authentication";
window["jQuery"] = $;


$(document).ready(() => {
    const api = new APIController(() => {
    });

    $("#delete-account").on("click", () => {
        const modalDialog = <any>$("#confirm-delete-account");

        // Function called on delete confirmation
        const callback = () => {
            api.deleteUser(parseInt(localStorage.getItem("userId"))).then(() => {
                removeUserInfo();
                gapi.auth2.getAuthInstance().signOut().then(() => {
                    window.location.href = "/";
                });
            }, (reason: any) => {
                modalDialog.find("button.confirm").off();
                modalDialog.modal("hide");

                const alert = $(".account-delete-alert");
                alert.find("code").text(reason.code + ": " + reason.description);

                alert.slideDown(200);

                setTimeout(() => {
                    alert.slideUp(200);
                }, 5000);
            });
        };

        modalDialog.find("button.confirm").on("click", callback);
        modalDialog.modal("show");
    });
});
