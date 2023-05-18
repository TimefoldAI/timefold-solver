/* chartjs-plugin-watermark | AlbinoDrought | MIT License | https://github.com/AlbinoDrought/chartjs-plugin-watermark/blob/master/LICENSE */
(function(){function r(e,n,t){function o(i,f){if(!n[i]){if(!e[i]){var c="function"==typeof require&&require;if(!f&&c)return c(i,!0);if(u)return u(i,!0);var a=new Error("Cannot find module '"+i+"'");throw a.code="MODULE_NOT_FOUND",a}var p=n[i]={exports:{}};e[i][0].call(p.exports,function(r){var n=e[i][1][r];return o(n||r)},p,p.exports,r,e,n,t)}return n[i].exports}for(var u="function"==typeof require&&require,i=0;i<t.length;i++)o(t[i]);return o}return r})()({1:[function(require,module,exports){
/**
 * Chart.js Simple Watermark plugin
 *
 * Valid options:
 *
 * options: {
 *      watermark: {
 *          // required
 *          image: new Image(),
 *
 *          x: 0,
 *          y: 0,
 *
 *          width: 0,
 *          height: 0,
 *
 *          alignX: "left"/"right"/"middle",
 *          alignY: "top"/"bottom"/"middle",
 *
 *          position: "front"/"back/between",
 *
 *          opacity: 0 to 1, // uses ctx.globalAlpha
 *      }
 * }
 *
 * Created by Sean on 12/19/2016.
 */

var watermarkPlugin = {
    id: 'watermark',

    defaultOptions: {
        x: 0,
        y: 0,

        height: false,
        width: false,

        alignX: "top",
        alignY: "left",
        alignToChartArea: false,

        position: "front",

        opacity: 1,

        image: false,
    },

    isPercentage: function (value) {
        return typeof(value) == "string" && value.charAt(value.length - 1) == "%";
    },

    calcPercentage: function (percentage, max) {
        var value = percentage.substr(0, percentage.length - 1);
        value = parseFloat(value);

        return max * (value / 100);
    },

    autoPercentage: function (value, maxIfPercentage) {
        if (this.isPercentage(value)) {
            value = this.calcPercentage(value, maxIfPercentage);
        }

        return value;
    },

    imageFromString: function (imageSrc) {
        // create the image object with this as our src
        var imageObj = new Image();
        imageObj.src = imageSrc;

        return imageObj;
    },

    drawWatermark: function (chartInstance, position) {
        var watermark = chartInstance.watermark;

        // only draw watermarks meant for us
        if (watermark.position != position) return;

        if (watermark.image) {
            var image = watermark.image;

            var context = chartInstance.ctx;
            var canvas = context.canvas;

            var cHeight, cWidth;
            var offsetX = 0, offsetY = 0;

            if(watermark.alignToChartArea) {
                var chartArea = chartInstance.chartArea;

                cHeight = chartArea.bottom - chartArea.top;
                cWidth = chartArea.right - chartArea.left;

                offsetX = chartArea.left;
                offsetY = chartArea.top;
            } else {
                cHeight = canvas.clientHeight || canvas.height;
                cWidth = canvas.clientWidth || canvas.width;
            }

            var height = watermark.height || image.height;
            height = this.autoPercentage(height, cHeight);

            var width = watermark.width || image.width;
            width = this.autoPercentage(width, cWidth);

            var x = this.autoPercentage(watermark.x, cWidth);
            var y = this.autoPercentage(watermark.y, cHeight);

            switch (watermark.alignX) {
                case "right":
                    x = cWidth - x - width;
                    break;
                case "middle":
                    x = (cWidth / 2) - (width / 2) - x;
                    break;
            }

            switch (watermark.alignY) {
                case "bottom":
                    y = cHeight - y - height;
                    break;
                case "middle":
                    y = (cHeight / 2) - (height / 2) - y;
                    break;
            }

            context.save();

            context.globalAlpha = watermark.opacity;
            context.drawImage(image, offsetX + x, offsetY + y, width, height);

            context.restore();
        }
    },

    beforeInit: function (chartInstance) {
        chartInstance.watermark = {};

        var options = chartInstance.options;

        if (options.watermark) {
            var clonedDefaultOptions = Object.assign({}, this.defaultOptions),
                watermark = Object.assign(clonedDefaultOptions, options.watermark);

            if (watermark.image) {
                var image = watermark.image;

                if (typeof(image) == "string") {
                    image = this.imageFromString(image);
                }

                // automatically refresh the chart once the image has loaded (if necessary)
                image.onload = function () {
                    if(chartInstance.ctx) {
                        chartInstance.update();
                    }
                };

                watermark.image = image;
            }

            chartInstance.watermark = watermark;
        }
    },

    // draw the image behind most chart elements
    beforeDraw: function (chartInstance) {
        this.drawWatermark(chartInstance, "back");
    },
    // draw the image in front of most chart elements
    afterDraw: function (chartInstance) {
        this.drawWatermark(chartInstance, "front");
    },
    // draw the image in front of chart elements, but before tooltips
    afterDatasetsDraw: function (chartInstance) {
        this.drawWatermark(chartInstance, "between");
    },
};

module.exports = watermarkPlugin;

// If used in browser, register globally
if (window !== undefined) {
    if (window.Chart) {
        window.Chart.register(watermarkPlugin);
    }
}

},{}]},{},[1]);