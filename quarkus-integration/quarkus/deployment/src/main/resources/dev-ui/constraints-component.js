import {css, html, LitElement} from 'lit';
import {JsonRpc} from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import {until} from 'lit/directives/until.js';
import '@vaadin/grid';
import {columnBodyRenderer} from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';

export class ConstraintsComponent extends LitElement {

    jsonRpc = new JsonRpc("Timefold Solver");

    // Component style
    static styles = css`
        .button {
            background-color: transparent;
            cursor: pointer;
        }

        .clearIcon {
            color: orange;
        }
    `;

    // Component properties
    static properties = {
        "_constraints": {state: true}
    }

    // Components callbacks

    /**
     * Called when displayed
     */
    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getConstraints().then(jsonRpcResponse => {
            this._constraints = jsonRpcResponse.result;
        });
    }

    /**
     * Called when it needs to render the components
     * @returns {*}
     */
    render() {
        return html`${until(this._renderConstraintTable(), html`<span>Loading constraints...</span>`)}`;
    }

    // View / Templates

    _renderConstraintTable() {
        if (this._constraints) {
            let constraints = this._constraints;
            const keys = Object.keys(constraints);
            if (keys.length === 1) {
                return html`
                    <vaadin-grid .items="${constraints[keys[0]]}" class="datatable" theme="no-border">
                        <vaadin-grid-column auto-width
                                            header="Constraint"
                                            ${columnBodyRenderer(this._nameRenderer, [])}>
                        </vaadin-grid-column>
                    </vaadin-grid>`;
            } else {
                let constraintValues = {};
                keys.sort().forEach(k => {
                    constraints[k].forEach(v => {
                        if (!(v in constraintValues)) {
                            constraintValues[v] = k;
                        } else {
                            constraintValues[v] += ", " + k;
                        }
                    });
                });
                let constraintList = Object.keys(constraintValues).map(key => {
                    return {name: key, values: constraintValues[key]}
                });
                return html`
                    <vaadin-grid .items="${constraintList}" class="datatable" theme="no-border" all-rows-visible>
                        <vaadin-grid-column auto-width
                                            header="Constraint"
                                            ${columnBodyRenderer(this._constraintNameRenderer, [])}>
                        </vaadin-grid-column>
                        <vaadin-grid-column auto-width
                                            header="Solver Config"
                                            ${columnBodyRenderer(this._solverConfigValueRenderer, [])}>
                        </vaadin-grid-column>

                    </vaadin-grid>`;
            }
        }
    }

    _nameRenderer(constraint) {
        return html`
            ${constraint}`;
    }

    _constraintNameRenderer(constraint) {
        return html`
            ${constraint['name']}`;
    }

    _solverConfigValueRenderer(constraint) {
        return html`
            ${constraint['values']}`;
    }

}

customElements.define('constraints-component', ConstraintsComponent);