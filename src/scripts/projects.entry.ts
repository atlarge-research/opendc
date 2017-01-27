///<reference path="../../typings/globals/jquery/index.d.ts" />
import * as $ from "jquery";
import {APIController} from "./controllers/connection/api";
import {Util} from "./util";
window["jQuery"] = $;

require("./user");


$(document).ready(() => {
    let api;
    new APIController((apiInstance: APIController) => {
        api = apiInstance;
        api.getAuthorizationsByUser(parseInt(localStorage.getItem("userId"))).then((data: any) => {
            const projectsController = new ProjectsController(data, api);
            new WindowController(projectsController, api);
        });
    });
});


/**
 * Controller class responsible for rendering the authorization list views and handling interactions with them.
 */
class ProjectsController {
    public static authIconMap = {
        "OWN": "glyphicon-home",
        "EDIT": "glyphicon-pencil",
        "VIEW": "glyphicon-eye-open"
    };

    public currentUserId: number;
    public authorizations: IAuthorization[];
    public authorizationsFiltered: IAuthorization[];
    public windowController: WindowController;

    private api: APIController;


    /**
     * 'Opens' a project, by putting the relevant simulation ID into local storage and referring to the app page.
     *
     * @param authorization The user's authorization belonging to the project to be opened
     */
    public static openProject(authorization: IAuthorization): void {
        localStorage.setItem("simulationId", authorization.simulationId.toString());
        localStorage.setItem("simulationAuthLevel", authorization.authorizationLevel);
        window.location.href = "app";
    }

    /**
     * Converts a list of authorizations into DOM objects, and adds them to the main list body of the page.
     *
     * @param list The list of authorizations to be displayed
     */
    public static populateList(list: IAuthorization[]): void {
        const body = $(".project-list .list-body");
        body.empty();

        list.forEach((element: IAuthorization) => {
            body.append(
                $('<div class="project-row">').append(
                    $('<div>').text(element.simulation.name),
                    $('<div>').text(Util.formatDateTime(element.simulation.datetimeLastEditedParsed)),
                    $('<div>').append($('<span class="glyphicon">')
                            .addClass(this.authIconMap[element.authorizationLevel]),
                        Util.toSentenceCase(element.authorizationLevel)
                    )
                ).attr("data-id", element.simulationId)
            );
        });
    };

    /**
     * Filters an authorization list based on what authorization level is required.
     *
     * Leaves the original list intact.
     *
     * @param list The authorization list to be filtered
     * @param ownedByUser Whether only authorizations should be included that are owned by the user, or whether only
     * authorizations should be included that the user has no ownership over
     * @returns {IAuthorization[]} A filtered list of authorizations
     */
    public static filterList(list: IAuthorization[], ownedByUser: boolean): IAuthorization[] {
        const resultList: IAuthorization[] = [];

        list.forEach((element: IAuthorization) => {
            if (element.authorizationLevel === "OWN") {
                if (ownedByUser) {
                    resultList.push(element);
                }
            } else {
                if (!ownedByUser) {
                    resultList.push(element);
                }
            }
        });

        return resultList;
    };

    /**
     * Activates a certain filter heading button, while deactivating the rest.
     *
     * @param target The event target to activate
     */
    private static activateFilterViewButton(target: JQuery): void {
        target.addClass("active");
        target.siblings().removeClass("active");
    };

    constructor(authorizations: IAuthorization[], api: APIController) {
        this.currentUserId = parseInt(localStorage.getItem("userId"));
        this.authorizations = authorizations;
        this.authorizationsFiltered = authorizations;
        this.api = api;

        this.updateNoProjectsAlert();

        this.handleFilterClick();

        // Show a project view upon clicking on a simulation row
        $("body").on("click", ".project-row", (event: JQueryEventObject) => {
            this.displayProjectView($(event.target));
        });
    }

    /**
     * Update the list of authorizations, by fetching them from the server and reloading the list.
     *
     * Goes to the 'All Projects' page after this refresh.
     */
    public updateAuthorizations(): void {
        this.api.getAuthorizationsByUser(this.currentUserId).then((data: any) => {
            this.authorizations = data;
            this.authorizationsFiltered = this.authorizations;

            this.updateNoProjectsAlert();

            this.goToAllProjects();
        });
    }

    /**
     * Show (or hide) the 'No projects here' alert in the list view, based on whether there are projects present.
     */
    private updateNoProjectsAlert(): void {
        if (this.authorizationsFiltered.length === 0) {
            $(".no-projects-alert").show();
            $(".project-list").hide();
        } else {
            $(".no-projects-alert").hide();
            $(".project-list").show();
        }
    }

    /**
     * Displays a project view with authorizations and entry buttons, inline within the table.
     *
     * @param target The element that was clicked on to launch this view
     */
    private displayProjectView(target: JQuery): void {
        const closestRow = target.closest(".project-row");
        const activeElement = $(".project-row.active");

        // Disable previously selected row elements and remove any project-views, to have only one view open at a time
        if (activeElement.length > 0) {
            const view = $(".project-view").first();

            view.slideUp(200, () => {
                activeElement.removeClass("active");
                view.remove();
            });

            if (closestRow.is(activeElement)) {
                return;
            }
        }

        const simulationId = parseInt(closestRow.attr("data-id"), 10);

        // Generate a list of participants of this project
        this.api.getAuthorizationsBySimulation(simulationId).then((data: any) => {
            const simAuthorizations = data;
            const participants = [];

            Util.sortAuthorizations(simAuthorizations);

            // For each participant of this simulation, include his/her name along with an icon of their authorization
            // level in the list
            simAuthorizations.forEach((authorization: IAuthorization) => {
                const authorizationString = ' (<span class="glyphicon ' +
                    ProjectsController.authIconMap[authorization.authorizationLevel] + '"></span>)';
                if (authorization.userId === this.currentUserId) {
                    participants.push(
                        'You' + authorizationString
                    );
                } else {
                    participants.push(
                        authorization.user.givenName + ' ' + authorization.user.familyName + authorizationString
                    );
                }
            });

            // Generate a project view component with participants and relevant actions
            const object = $('<div class="project-view">').append(
                $('<div class="participants">').append(
                    $('<strong>').text("Participants"),
                    $('<div>').html(participants.join(", "))
                ),
                $('<div class="access-buttons">').append(
                    $('<div class="inline-btn edit">').text("Edit"),
                    $('<div class="inline-btn open">').text("Open")
                )
            ).hide();

            closestRow.after(object);

            // Hide the 'edit' button for non-owners and -editors
            const currentAuth = this.authorizationsFiltered[closestRow.index(".project-row")];
            if (currentAuth.authorizationLevel !== "OWN") {
                $(".project-view .inline-btn.edit").hide();
            }

            object.find(".edit").click(() => {
                this.windowController.showEditProjectWindow(simAuthorizations);
            });

            object.find(".open").click(() => {
                ProjectsController.openProject(currentAuth);
            });

            closestRow.addClass("active");
            object.slideDown(200);
        });
    }

    /**
     * Controls the filtered authorization list, based on clicks from the side menu.
     */
    private handleFilterClick(): void {
        $(".all-projects").on("click", () => {
            this.goToAllProjects();
        });

        $(".my-projects").on("click", () => {
            this.goToMyProjects();
        });

        $(".shared-projects").on("click", () => {
            this.goToSharedProjects();
        });

        this.goToAllProjects();
    }

    /**
     * Show a list containing all projects (regardless of the authorization level the user has over them).
     */
    private goToAllProjects(): void {
        this.authorizationsFiltered = this.authorizations;
        ProjectsController.populateList(this.authorizations);
        this.updateNoProjectsAlert();

        ProjectsController.activateFilterViewButton($(".all-projects"));
    }

    /**
     * Show a list containing only projects that the user owns.
     */
    private goToMyProjects(): void {
        this.authorizationsFiltered = ProjectsController.filterList(this.authorizations, true);
        ProjectsController.populateList(this.authorizationsFiltered);
        this.updateNoProjectsAlert();

        ProjectsController.activateFilterViewButton($(".my-projects"));
    }

    /**
     * Show a list containing only projects that the user does not own (but can edit or view).
     */
    private goToSharedProjects(): void {
        this.authorizationsFiltered = ProjectsController.filterList(this.authorizations, false);
        ProjectsController.populateList(this.authorizationsFiltered);
        this.updateNoProjectsAlert();

        ProjectsController.activateFilterViewButton($(".shared-projects"));
    }
}


/**
 * Controller class responsible for rendering the project add/edit window and handle user interaction with it.
 */
class WindowController {
    private projectsController: ProjectsController;
    private windowOverlay: JQuery;
    private window: JQuery;
    private table: JQuery;
    private closeCallback: () => any;
    private simAuthorizations: IAuthorization[];
    private simulationId: number;
    private editMode: boolean;
    private api: APIController;


    constructor(projectsController: ProjectsController, api: APIController) {
        this.projectsController = projectsController;
        this.windowOverlay = $(".window-overlay");
        this.window = $(".projects-window");
        this.table = $(".participants-table");
        this.projectsController.windowController = this;
        this.simAuthorizations = [];
        this.editMode = false;
        this.api = api;

        $(".window-footer .btn").hide();

        $(".participant-add-form").submit((event: JQueryEventObject) => {
            event.preventDefault();
            $(".participant-add-form .btn").trigger("click");
        });

        $(".project-name-form").submit((event: JQueryEventObject) => {
            event.preventDefault();
            $(".project-name-form .btn").trigger("click");
        });

        // Clean-up actions to occur after every window-close
        this.closeCallback = () => {
            this.table.empty();
            $(".project-name-form .btn").off();
            $(".participant-add-form .btn").off();
            $(".window-footer .btn").hide().off();
            $(".participant-email-alert").hide();
            $(".participant-level div").removeClass("active").off();
            this.table.off("click", ".participant-remove div");

            $(".project-name-form input").val("");
            $(".participant-add-form input").val("");

            if (this.editMode) {
                this.projectsController.updateAuthorizations();
            }
        };

        $(".new-project-btn").click(() => {
            this.showAddProjectWindow();
        });

        // Stop click events on the window from closing it indirectly
        this.window.click((event: JQueryEventObject) => {
            event.stopPropagation();
        });

        $(".window-close, .window-overlay").click(() => {
            this.closeWindow();
        });
    }

    /**
     * Displays a window for project edits (used for adding participants and changing the name).
     *
     * @param authorizations The authorizations of the simulation project to be edited.
     */
    public showEditProjectWindow(authorizations: IAuthorization[]): void {
        this.editMode = true;
        this.simAuthorizations = [];
        this.simulationId = authorizations[0].simulation.id;

        // Filter out the user's authorization from the authorization list (not to be displayed in the list)
        authorizations.forEach((authorization: IAuthorization) => {
            if (authorization.userId !== this.projectsController.currentUserId) {
                this.simAuthorizations.push(authorization);
            }
        });

        $(".window .window-heading").text("Edit project");

        $(".project-name-form input").val(authorizations[0].simulation.name);

        $(".project-name-form .btn").css("display", "inline-block").click(() => {
            const nameInput = $(".project-name-form input").val();
            if (nameInput !== "") {
                authorizations[0].simulation.name = nameInput;
                this.api.updateSimulation(authorizations[0].simulation);
            }
        });

        $(".project-open-btn").show().click(() => {
            ProjectsController.openProject({
                userId: this.projectsController.currentUserId,
                simulationId: this.simulationId,
                authorizationLevel: "OWN"
            });
        });

        $(".project-delete-btn").show().click(() => {
            this.api.deleteSimulation(authorizations[0].simulationId).then(() => {
                this.projectsController.updateAuthorizations();
                this.closeWindow();
            });
        });

        $(".participant-add-form .btn").click(() => {
            this.handleParticipantAdd((userId: number) => {
                this.api.addAuthorization({
                    userId: userId,
                    simulationId: authorizations[0].simulationId,
                    authorizationLevel: "VIEW"
                });
            });
        });

        this.table.on("click", ".participant-level div", (event: JQueryEventObject) => {
            this.handleParticipantLevelChange(event, (authorization: IAuthorization) => {
                this.api.updateAuthorization(authorization);
            });
        });

        this.table.on("click", ".participant-remove div", (event: JQueryEventObject) => {
            this.handleParticipantDelete(event, (authorization) => {
                this.api.deleteAuthorization(authorization);
            });
        });

        this.populateParticipantList();

        this.windowOverlay.fadeIn(200);
    }

    /**
     * Shows a window to be used for creating a new project.
     */
    public showAddProjectWindow(): void {
        this.editMode = false;
        this.simAuthorizations = [];

        $(".project-name-form .btn").hide();

        $(".window .window-heading").text("Create a project");

        $(".project-create-open-btn").show().click(() => {
            if ($(".project-name-form input").val() === "") {
                this.showAlert(".project-name-alert");
                return;
            }
            this.createSimulation((simulationId: number) => {
                ProjectsController.openProject({
                    userId: this.projectsController.currentUserId,
                    simulationId,
                    authorizationLevel: "OWN"
                });
            });
        });

        $(".project-create-btn").show().click(() => {
            if ($(".project-name-form input").val() === "") {
                this.showAlert(".project-name-alert");
                return;
            }
            this.createSimulation(() => {
                this.projectsController.updateAuthorizations();
                this.closeWindow();
            });
        });

        $(".project-cancel-btn").show().click(() => {
            this.closeWindow();
        });

        this.table.empty();

        $(".project-name-form input").val("");
        $(".participant-add-form input").val("");

        $(".participant-add-form .btn").click(() => {
            this.handleParticipantAdd(() => {
            });
        });

        this.table.on("click", ".participant-level div", (event: JQueryEventObject) => {
            this.handleParticipantLevelChange(event, () => {
            });
        });

        this.table.on("click", ".participant-remove div", (event: JQueryEventObject) => {
            this.handleParticipantDelete(event, () => {
            });
        });

        this.windowOverlay.fadeIn(200);
    }

    /**
     * Creates a new simulation with the current name input and all currently present authorizations added in the
     * project 'add' window.
     *
     * @param callback The function to be called when this operation has succeeded
     */
    private createSimulation(callback: (simulationId: number) => any): void {
        this.api.addSimulation({
            id: -1,
            name: $(".project-name-form input").val(),
            datetimeCreated: Util.getCurrentDateTime(),
            datetimeLastEdited: Util.getCurrentDateTime()
        }).then((data: any) => {
            let asyncCounter = this.simAuthorizations.length;
            this.simAuthorizations.forEach((authorization: IAuthorization) => {
                authorization.simulationId = data.id;
                this.api.addAuthorization(authorization).then((data: any) => {
                    asyncCounter--;

                    if (asyncCounter <= 0) {
                        callback(data.id);
                    }
                });
            });
            if (this.simAuthorizations.length === 0) {
                callback(data.id);
            }
        });
    }

    /**
     * Displays an alert of the given class name, to disappear again after a certain pre-defined timeout.
     *
     * @param name A selector that uniquely identifies the alert body to be shown.
     */
    private showAlert(name): void {
        const alert = $(name);
        alert.slideDown(200);

        setTimeout(() => {
            alert.slideUp(200);
        }, 5000);
    }

    /**
     * Closes the window with a transition, and calls the relevant callback after that transition has ended.
     */
    private closeWindow(): void {
        this.windowOverlay.fadeOut(200, () => {
            this.closeCallback();
        });
    }

    /**
     * Handles the click on an authorization icon in the project window authorization list.
     *
     * @param event The JQuery click event
     * @param callback The function to be called after the authorization was changed
     */
    private handleParticipantLevelChange(event: JQueryEventObject,
                                         callback: (authorization: IAuthorization) => any): void {
        $(event.target).closest(".participant-level").find("div").removeClass("active");
        $(event.target).addClass("active");

        const affectedRow = $(event.target).closest(".participant-row");

        for (let level in ProjectsController.authIconMap) {
            if (!ProjectsController.authIconMap.hasOwnProperty(level)) {
                continue;
            }
            if ($(event.target).is("." + ProjectsController.authIconMap[level])) {
                this.simAuthorizations[affectedRow.index()].authorizationLevel = level;
                callback(this.simAuthorizations[affectedRow.index()]);
                break;
            }
        }
    }

    /**
     * Handles the event where a user seeks to add a participant.
     *
     * @param callback The function to be called if the participant could be found and can be added.
     */
    private handleParticipantAdd(callback: (userId: number) => any): void {
        const inputForm = $(".participant-add-form input");
        this.api.getUserByEmail(inputForm.val()).then((data: any) => {
            let insert = true;
            for (let i = 0; i < this.simAuthorizations.length; i++) {
                if (this.simAuthorizations[i].userId === data.id) {
                    insert = false;
                }
            }

            const simulationId = this.editMode ? this.simulationId : -1;
            if (data.id !== this.projectsController.currentUserId && insert) {
                this.simAuthorizations.push({
                    userId: data.id,
                    user: data,
                    simulationId: simulationId,
                    authorizationLevel: "VIEW"
                });
                callback(data.id);
                Util.sortAuthorizations(this.simAuthorizations);
                this.populateParticipantList();
            }

            // Clear input field after submission
            inputForm.val("");
        }, (reason: any) => {
            if (reason.code === 404) {
                this.showAlert(".participant-email-alert");
            }
        });
    }

    /**
     * Handles click events on the 'remove' icon next to each participant.
     *
     * @param event The JQuery click event
     * @param callback The function to be executed on removal of the participant from the internal list
     */
    private handleParticipantDelete(event: JQueryEventObject, callback: (authorization: IAuthorization) => any): void {
        const affectedRow = $(event.target).closest(".participant-row");
        const index = affectedRow.index();
        const authorization = this.simAuthorizations[index];
        this.simAuthorizations.splice(index, 1);
        this.populateParticipantList();
        callback(authorization);
    }

    /**
     * Populates the list of participants in the project edit window with all current authorizations.
     */
    private populateParticipantList(): void {
        this.table.empty();

        this.simAuthorizations.forEach((authorization: IAuthorization) => {
            this.table.append(
                '<div class="participant-row">' +
                '  <div class="participant-name">' + authorization.user.givenName + ' ' +
                authorization.user.familyName + '</div>' +
                '  <div class="participant-level">' +
                '    <div class="participant-level-view glyphicon glyphicon-eye-open ' +
                (authorization.authorizationLevel === "VIEW" ? 'active' : '') + '"></div>' +
                '    <div class="participant-level-edit glyphicon glyphicon-pencil ' +
                (authorization.authorizationLevel === "EDIT" ? 'active' : '') + '"></div>' +
                '    <div class="participant-level-own glyphicon glyphicon-home ' +
                (authorization.authorizationLevel === "OWN" ? 'active' : '') + '"></div>' +
                '  </div>' +
                '  <div class="participant-remove">' +
                '    <div class="glyphicon glyphicon-remove"></div>' +
                '  </div>' +
                '</div>'
            );
        });
    }
}
