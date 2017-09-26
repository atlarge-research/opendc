import React from "react";
import jQuery from "../../../../../util/jquery";

class UnitComponent extends React.Component {
    componentDidMount() {
        jQuery(".unit-info-popover").popover({
            trigger: "focus"
        });
    }

    render() {
        let unitInfo;
        if (this.props.unitType === "cpu" || this.props.unitType === "gpu") {
            unitInfo = (
                "<strong>Clockrate:</strong> <code>" + this.props.unit.clockRateMhz + " MHz</code><br/>" +
                "<strong>Num. Cores:</strong> <code>" + this.props.unit.numberOfCores + "</code><br/>" +
                "<strong>Energy Cons.:</strong> <code>" + this.props.unit.energyConsumptionW + " W</code>"
            );
        } else if (this.props.unitType === "memory" || this.props.unitType === "storage") {
            unitInfo = (
                "<strong>Speed:</strong> <code>" + this.props.unit.speedMbPerS + " Mb/s</code><br/>" +
                "<strong>Size:</strong> <code>" + this.props.unit.sizeMb + " MB</code><br/>" +
                "<strong>Energy Cons.:</strong> <code> " + this.props.unit.energyConsumptionW + " W</code>"
            );
        }

        return (
            <li className="d-flex list-group-item justify-content-between align-items-center">
                <span style={{maxWidth: "60%"}}>
                    {
                        this.props.unit.manufacturer
                        + " " + this.props.unit.family
                        + " " + this.props.unit.model
                        + " " + this.props.unit.generation
                    }
                </span>
                <span>
                    <a
                        tabIndex="0"
                        className="unit-info-popover btn btn-outline-info mr-1"
                        role="button"
                        data-toggle="popover"
                        data-trigger="focus"
                        title="Unit information"
                        data-content={unitInfo}
                        data-html="true"
                    >
                        <span className="fa fa-info-circle"/>
                    </a>
                    {this.props.inSimulation ?
                        undefined :
                        <span className="btn btn-outline-danger" onClick={this.props.onDelete}>
                            <span className="fa fa-trash"/>
                        </span>
                    }
                </span>
            </li>
        );
    }
}

export default UnitComponent;
