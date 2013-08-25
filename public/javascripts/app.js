/*jslint nomen: true, regexp: true */
/*global window, angular */

(function () {
    'use strict';

    var url = 'upload';

    var myApp = angular.module('uploadModule', [
            'blueimp.fileupload', 'ui.bootstrap'
        ])
        .config([
            '$httpProvider', 'fileUploadProvider',
            function ($httpProvider, fileUploadProvider) {
                delete $httpProvider.defaults.headers.common['X-Requested-With'];
                fileUploadProvider.defaults.redirect = window.location.href.replace(
                    /\/[^\/]*$/,
                    '/cors/result.html?%s'
                );
            }
        ])

        .controller('AppFileUploadController', [
            '$scope', '$http', '$filter', '$window',
            function ($scope, $http) {
                $scope.options = {
                    url: url
                };
            }
        ])

        .controller('FileDestroyController', [
            '$scope', '$http',
            function ($scope, $http) {
                var file = $scope.file,
                    state;
                if (file.url) {
                    file.$state = function () {
                        return state;
                    };
                    file.$destroy = function () {
                        state = 'pending';
                        return $http({
                            url: file.deleteUrl,
                            method: file.deleteType
                        }).then(
                            function () {
                                state = 'resolved';
                                $scope.clear(file);
                            },
                            function () {
                                state = 'rejected';
                            }
                        );
                    };
                } else if (!file.$cancel && !file._index) {
                    file.$cancel = function () {
                        $scope.clear(file);
                    };
                }
            }
        ])
//
        .factory('progressSocketService', ['$rootScope', function ($rootScope) {
            console.log("Progress - Websocket factory...");
            // We return this object to anything injecting our service
            var Service = {};

            //using play framework we get the absolute URL
            var wsUrl = jsRoutes.controllers.Upload.transform().absoluteURL();
            //replace the protocol to http ws
            wsUrl = wsUrl.replace("http", "ws");

            // Create our websocket object with the address to the websocket
            var ws = new WebSocket(wsUrl);

            ws.onopen = function () {
                console.log("Socket has been opened!");
                var message = 0;
                ws.send(JSON.stringify(message));
            };

            ws.onmessage = function (message) {
                listener(JSON.parse(message.data));
            };

            function listener(data) {
                var messageObj = data;
                console.log("Received data from websocket: ", messageObj);
                //update the progress bar
                $rootScope.dynamicObject.value = messageObj.value;
            }

            return Service;
        }])

        .controller('ProgressController', [ '$rootScope', 'progressSocketService',
            function ($rootScope, progressSocketService) {
                console.log("progresssssss");
                $rootScope.dynamicObject = {
                    value: 5,
                    type: 'success'
                };
                var service = progressSocketService;
                console.log("service running");
            }
        ]);

}());
