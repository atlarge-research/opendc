///<reference path="../../typings/index.d.ts" />
import * as $ from "jquery";
import {APIController} from "./controllers/connection/api";
import {removeUserInfo} from "./user";
window["jQuery"] = $;


$(document).ready(() => {
    let api = new APIController(() => {
    });

    $("#delete-account").on("click", () => {
        let modalDialog = <any>$("#confirm-delete-account");

        // Function called on delete confirmation
        let callback = () => {
            api.deleteUser(parseInt(localStorage.getItem("userId"))).then(() => {
                removeUserInfo();
                gapi.auth2.getAuthInstance().signOut().then(() => {
                    window.location.href = "/";
                });
            }, (reason: any) => {
                modalDialog.find("button.confirm").off();
                modalDialog.modal("hide");

                let alert = $(".account-delete-alert");
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
