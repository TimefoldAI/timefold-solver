function replaceTimefoldAutoHeaderFooter() {
  const timefoldHeader = $("header#timefold-auto-header");
  if (timefoldHeader != null) {
    timefoldHeader.addClass("bg-black")
    timefoldHeader.append(
      $(`<div class="container-fluid">
        <nav class="navbar sticky-top navbar-expand-lg navbar-dark shadow mb-3">
          <a class="navbar-brand" href="https://timefold.ai">
            <img src="webjars/timefold/img/timefold-logo-horizontal-negative.svg" alt="Timefold logo" width="200">
          </a>
        </nav>
      </div>`));
  }
  const timefoldFooter = $("footer#timefold-auto-footer");
  if (timefoldFooter != null) {
    timefoldFooter.append(
      $(`<footer class="bg-black text-white-50">
           <div class="container">
             <div class="hstack gap-3 p-4">
               <div class="ms-auto"><a class="text-white" href="https://timefold.ai">Timefold</a></div>
               <div class="vr"></div>
               <div><a class="text-white" href="https://timefold.ai/docs">Documentation</a></div>
               <div class="vr"></div>
               <div><a class="text-white" href="https://github.com/TimefoldAI/timefold-quickstarts">Code</a></div>
               <div class="vr"></div>
               <div class="me-auto"><a class="text-white" href="https://timefold.ai/product/support/">Support</a></div>
             </div>
           </div>
           <div id="applicationInfo" class="container text-center"></div>
         </footer>`));

      applicationInfo();
  }

}

function showSimpleError(title) {
    const notification = $(`<div class="toast" role="alert" aria-live="assertive" aria-atomic="true" style="min-width: 50rem"/>`)
        .append($(`<div class="toast-header bg-danger">
                 <strong class="me-auto text-dark">Error</strong>
                 <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
               </div>`))
        .append($(`<div class="toast-body"/>`)
            .append($(`<p/>`).text(title))
        );
    $("#notificationPanel").append(notification);
    notification.toast({delay: 30000});
    notification.toast('show');
}

function showError(title, xhr) {
  var serverErrorMessage = !xhr.responseJSON ? `${xhr.status}: ${xhr.statusText}` : xhr.responseJSON.message;
  var serverErrorCode = !xhr.responseJSON ? `unknown` : xhr.responseJSON.code;
  var serverErrorId = !xhr.responseJSON ? `----` : xhr.responseJSON.id;
  var serverErrorDetails = !xhr.responseJSON ? `no details provided` : xhr.responseJSON.details;

  if (xhr.responseJSON && !serverErrorMessage) {
	  serverErrorMessage = JSON.stringify(xhr.responseJSON);
	  serverErrorCode = xhr.statusText + '(' + xhr.status + ')';
	  serverErrorId = `----`;
  }

  console.error(title + "\n" + serverErrorMessage + " : " + serverErrorDetails);
  const notification = $(`<div class="toast" role="alert" aria-live="assertive" aria-atomic="true" style="min-width: 50rem"/>`)
    .append($(`<div class="toast-header bg-danger">
                 <strong class="me-auto text-dark">Error</strong>
                 <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
               </div>`))
    .append($(`<div class="toast-body"/>`)
      .append($(`<p/>`).text(title))
      .append($(`<pre/>`)
        .append($(`<code/>`).text(serverErrorMessage + "\n\nCode: " + serverErrorCode + "\nError id: " + serverErrorId))
      )
    );
  $("#notificationPanel").append(notification);
  notification.toast({delay: 30000});
  notification.toast('show');
}

// ****************************************************************************
// Application info
// ****************************************************************************

function applicationInfo() {
  $.getJSON("info", function (info) {
       $("#applicationInfo").append("<small>" + info.application + " (version: " + info.version + ", built at: " + info.built + ")</small>");
   }).fail(function (xhr, ajaxOptions, thrownError) {
       console.warn("Unable to collect application information");
   });
}

// ****************************************************************************
// TangoColorFactory
// ****************************************************************************

const SEQUENCE_1 = [0x8AE234, 0xFCE94F, 0x729FCF, 0xE9B96E, 0xAD7FA8];
const SEQUENCE_2 = [0x73D216, 0xEDD400, 0x3465A4, 0xC17D11, 0x75507B];

var colorMap = new Map;
var nextColorCount = 0;

function pickColor(object) {
  let color = colorMap[object];
  if (color !== undefined) {
    return color;
  }
  color = nextColor();
  colorMap[object] = color;
  return color;
}

function nextColor() {
  let color;
  let colorIndex = nextColorCount % SEQUENCE_1.length;
  let shadeIndex = Math.floor(nextColorCount / SEQUENCE_1.length);
  if (shadeIndex === 0) {
    color = SEQUENCE_1[colorIndex];
  } else if (shadeIndex === 1) {
    color = SEQUENCE_2[colorIndex];
  } else {
    shadeIndex -= 3;
    let floorColor = SEQUENCE_2[colorIndex];
    let ceilColor = SEQUENCE_1[colorIndex];
    let base = Math.floor((shadeIndex / 2) + 1);
    let divisor = 2;
    while (base >= divisor) {
      divisor *= 2;
    }
    base = (base * 2) - divisor + 1;
    let shadePercentage = base / divisor;
    color = buildPercentageColor(floorColor, ceilColor, shadePercentage);
  }
  nextColorCount++;
  return "#" + color.toString(16);
}

function buildPercentageColor(floorColor, ceilColor, shadePercentage) {
  let red = (floorColor & 0xFF0000) + Math.floor(shadePercentage * ((ceilColor & 0xFF0000) - (floorColor & 0xFF0000))) & 0xFF0000;
  let green = (floorColor & 0x00FF00) + Math.floor(shadePercentage * ((ceilColor & 0x00FF00) - (floorColor & 0x00FF00))) & 0x00FF00;
  let blue = (floorColor & 0x0000FF) + Math.floor(shadePercentage * ((ceilColor & 0x0000FF) - (floorColor & 0x0000FF))) & 0x0000FF;
  return red | green | blue;
}
