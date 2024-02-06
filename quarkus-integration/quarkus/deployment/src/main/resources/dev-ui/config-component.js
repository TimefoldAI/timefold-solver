import {css, html, LitElement} from 'lit';
import {JsonRpc} from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import {until} from 'lit/directives/until.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import {columnBodyRenderer} from '@vaadin/grid/lit.js';

export class ConfigComponent extends LitElement {

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
        "_config": {state: true}
    }

    // Components callbacks

    /**
     * Called when displayed
     */
    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getConfig().then(jsonRpcResponse => {
            this._config = jsonRpcResponse.result.config;
        });
    }

    /**
     * Called when it needs to render the components
     * @returns {*}
     */
    render() {
        return html`${until(this._renderConfig(), html`<span>Loading config...</span>`)}`;
    }

    // View / Templates

    _renderConfig() {
        if (this._config) {
            let config = this._config;
            const keys = Object.keys(config);
            if (keys.length === 1) {
                return html`
                <pre>${config[keys[0]]}</pre>`;
            } else {
                return html`
                    <vaadin-grid .items="${keys.sort()}" class="datatable" theme="no-border" all-rows-visible>
                        <vaadin-grid-column auto-width
                                            header=""
                                            ${columnBodyRenderer(this._configRenderer, [])}>
                                                                    </vaadin-grid-column>
                    </vaadin-grid>`;
            }

        }
    }

    _configRenderer(key) {
        let content = this._config[key];

        return html`
            ${key}
            <p>
            <pre>${content}</pre>`;
    }
}

customElements.define('config-component', ConfigComponent);