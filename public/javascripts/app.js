/*jslint nomen: true, regexp: true */
/*global window, angular */

(function () {
    'use strict';

    var url = 'upload';

    var myApp = angular.module('app', ['ngUpload', 'ui.bootstrap'])
        .controller('mainCtrl', function ($scope, $compile) {
            $scope.results = function (content, completed) {
                if (completed) {
                    console.log(content); // process content
                    $(body).append($compile("<div id=\"progress\" ng-controller=\"ProgressController\" class=\"well pagination-centered text-center\"> <progress percent=\"dynamicObject\" class=\"progress-striped active\"></progress><p>Value: {{dynamicObject.value}}</p></div>")($scope));
                    $(fileupload).hide();
                }
                else {
                    console.log("else ->" + content + " completed ->"+completed);
                    // 1. ignore content and adjust your model to show/hide UI snippets; or
                    // 2. show content as an _operation progress_ information

                }


            }

            $(file).change(function () {
                console.log("onChange")
                var fileName = $(file)[0].files[0].name;
                $(filename).html(fileName);
            });

            console.log("prepared")

        })

    console.log("progress setup")

    myApp.factory('progressSocketService', ['$rootScope', '$compile', function ($rootScope, $compile) {
            console.log(" > Progress - Websocket factory...");
            console.log(" compile : "+$compile)
            // We return this object to anything injecting our service
            var Service = {};

            $rootScope.dynamicObject = {
                value: 0,
                text : null,
                type: 'success'
            };

            //using play framework we get the absolute URL
            var wsUrl = jsRoutes.controllers.Processing.transform().absoluteURL();
            //replace the protocol to http ws
            wsUrl = wsUrl.replace("http", "ws");

            // Create our websocket object with the address to the websocket
            var ws = new WebSocket(wsUrl);

            ws.onopen = function () {
                console.log("Socket has been opened!");
            };

            ws.onmessage = function (message) {
                listener(JSON.parse(message.data));
            };

            function listener(data) {
                console.log("Received data from websocket: ", data);
                //update the progress bar
                if(data.done == true){
                    $(body).append($compile("<div id=\"done\"  class=\"well pagination-centered text-center\"><a href=\"/download/"+data.uuid+"\">Download file</a></p></div>")($rootScope));
                    $(progress).hide();
                }
                else {
                    $rootScope.dynamicObject.value = data.value;
                    $rootScope.dynamicObject.text = data.text;
                }

                $rootScope.$apply()
            }

            return Service;
        }])

        .controller('ProgressController', [ '$rootScope', 'progressSocketService',
            function ($rootScope, progressSocketService) {
                console.log("service running");
            }
        ]);

}());
