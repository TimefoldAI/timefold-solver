import {css, html, LitElement} from 'lit';
import {JsonRpc} from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import {until} from 'lit/directives/until.js';
import '@vaadin/grid';
import {columnBodyRenderer} from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';

export class ModelComponent extends LitElement {

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
        "_model": {state: true}
    }

    // Components callbacks

    /**
     * Called when displayed
     */
    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getModelInfo().then(jsonRpcResponse => {
            this._model = {};
            Object.keys(jsonRpcResponse.result).forEach(key => {
                this._model[key] = {};
                this._model[key].solutionClass = jsonRpcResponse.result[key].solutionClass;
                this._model[key].entityInfoList = [];
                jsonRpcResponse.result[key].entityClassList.forEach(entityClass => {
                    const entityInfo = {};
                    entityInfo.name = entityClass;
                    entityInfo.genuineVariableList = jsonRpcResponse.result[key].entityClassToGenuineVariableListMap[entityClass];
                    entityInfo.shadowVariableList = jsonRpcResponse.result[key].entityClassToShadowVariableListMap[entityClass];
                    this._model[key].entityInfoList.push(entityInfo);
                });
            });
        });
    }

    /**
     * Called when it needs to render the components
     * @returns {*}
     */
    render() {
        return html`${until(this._renderModel(), html`<span>Loading model...</span>`)}`;
    }

    // View / Templates

    _renderModel() {
        if (this._model) {
            let model = this._model;
            const keys = Object.keys(model);
            if (keys.length === 1) {
                return html`
                    <div>
                        <span>Solution Class:</span>
                        <span>${model[keys[0]].solutionClass}</span>
                    </div>
                    <vaadin-grid .items="${model[keys[0]].entityInfoList}" class="datatable" theme="no-border">
                        <vaadin-grid-column auto-width
                                            header="Entity Class"
                                            ${columnBodyRenderer(this._entityClassNameRenderer, [])}>
                        </vaadin-grid-column>
                        <vaadin-grid-column auto-width
                                            header="Genuine Variables"
                                            ${columnBodyRenderer(this._genuineVariablesRenderer, [])}>
                        </vaadin-grid-column>
                        <vaadin-grid-column auto-width
                                            header="Shadow Variables"
                                            ${columnBodyRenderer(this._shadowVariablesRenderer, [])}>
                        </vaadin-grid-column>
                    </vaadin-grid>`;
            } else {
                let modelValues = [];
                keys.sort().forEach(k => {
                    model[k].entityInfoList.forEach(e => {
                        modelValues.push({
                            name: k,
                            solutionClass: model[k].solutionClass,
                            entityClassName: e.name,
                            genuineVariableList: e.genuineVariableList,
                            shadowVariableList: e.shadowVariableList,
                        });
                    });
                });
                return html`
                    <vaadin-grid .items="${modelValues}" class="datatable" theme="no-border" all-rows-visible>
                        <vaadin-grid-column auto-width
                                            header="Name"
                                            ${columnBodyRenderer(this._nameRenderer, [])}>
                        </vaadin-grid-column>
                        <vaadin-grid-column auto-width
                                            header="Solution Class"
                                            ${columnBodyRenderer(this._solutionClassNameRenderer, [])}>
                        </vaadin-grid-column>
                        <vaadin-grid-column auto-width
                                            header="Entity Class"
                                            ${columnBodyRenderer(this._cassNameRendererPerEntry, [])}>
                        </vaadin-grid-column>
                        <vaadin-grid-column auto-width
                                            header="Genuine Variables"
                                            ${columnBodyRenderer(this._genuineVariablesRendererPerEntry, [])}>
                        </vaadin-grid-column>
                        <vaadin-grid-column auto-width
                                            header="Shadow Variables"
                                            ${columnBodyRenderer(this._shadowVariablesRendererPerEntry, [])}>
                        </vaadin-grid-column>
                    </vaadin-grid>`;
            }
        }
    }

    _nameRenderer(entry) {
        return html`
            ${entry.name}`;
    }

    _solutionClassNameRenderer(entry) {
        return html`
            ${entry.solutionClass}`;
    }

    _cassNameRendererPerEntry(entry) {
        return html`
            ${entry.entityClassName}`;
    }

    _genuineVariablesRendererPerEntry(entry) {
        return html`
            ${entry.genuineVariableList.join(', ')}`;
    }

    _shadowVariablesRendererPerEntry(entry) {
        return html`
            ${entry.shadowVariableList.join(', ')}`;
    }

    _entityClassNameRenderer(entityInfo) {
        return html`
            ${entityInfo.name}`;
    }

    _genuineVariablesRenderer(entityInfo) {
        return html`
            ${entityInfo.genuineVariableList.join(', ')}`;
    }

    _shadowVariablesRenderer(entityInfo) {
        return html`
            ${entityInfo.shadowVariableList.join(', ')}`;
    }

}

customElements.define('model-component', ModelComponent);