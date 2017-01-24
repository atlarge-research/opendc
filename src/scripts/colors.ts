/**
 * Class serving as a color palette for the application.
 */
export class Colors {
    public static GRID_COLOR = "rgba(0, 0, 0, 0.5)";

    public static WALL_COLOR = "rgba(0, 0, 0, 1)";

    public static ROOM_DEFAULT = "rgba(150, 150, 150, 1)";
    public static ROOM_SELECTED = "rgba(51, 153, 255, 1)";
    public static ROOM_HOVER_VALID = "rgba(51, 153, 255, 0.5)";
    public static ROOM_HOVER_INVALID = "rgba(255, 102, 0, 0.5)";
    public static ROOM_NAME_COLOR = "rgba(245, 245, 245, 1)";
    public static ROOM_TYPE_COLOR = "rgba(245, 245, 245, 1)";

    public static RACK_BACKGROUND = "rgba(170, 170, 170, 1)";
    public static RACK_BORDER = "rgba(0, 0, 0, 1)";
    public static RACK_SPACE_BAR_BACKGROUND = "rgba(222, 235, 247, 1)";
    public static RACK_SPACE_BAR_FILL = "rgba(91, 155, 213, 1)";
    public static RACK_ENERGY_BAR_BACKGROUND = "rgba(255, 242, 204, 1)";
    public static RACK_ENERGY_BAR_FILL = "rgba(255, 192, 0, 1)";

    public static COOLING_ITEM_BACKGROUND = "rgba(40, 50, 230, 1)";
    public static COOLING_ITEM_BORDER = "rgba(0, 0, 0, 1)";

    public static PSU_BACKGROUND = "rgba(230, 50, 60, 1)";
    public static PSU_BORDER = "rgba(0, 0, 0, 1)";

    public static GRAYED_OUT_AREA = "rgba(0, 0, 0, 0.6)";

    public static INFO_BALLOON_INFO = "rgba(40, 50, 230, 1)";
    public static INFO_BALLOON_WARNING = "rgba(230, 60, 70, 1)";

    public static INFO_BALLOON_MAP = {
        "info": Colors.INFO_BALLOON_INFO,
        "warning": Colors.INFO_BALLOON_WARNING
    };

    public static SIM_LOW = "rgba(197, 224, 180, 1)";
    public static SIM_MID_LOW = "rgba(255, 230, 153, 1)";
    public static SIM_MID_HIGH = "rgba(248, 203, 173, 1)";
    public static SIM_HIGH = "rgba(249, 165, 165, 1)";
}
