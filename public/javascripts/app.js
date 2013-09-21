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
                    url: url,
                    maxFileSize: 50000000,
                    acceptFileTypes: /(\.|\/)(zip)$/i,
                    done: new function (e, data) {
                        console.log("file upload done");
                        console.log("e " + e);
                        console.log("data " + data);
                        if (data != undefined) {
                            $(body).append("<div ng-controller=\"ProgressController\" class=\"well pagination-centered text-center\"> <progress percent=\"dynamicObject\" class=\"progress-striped active\"></progress><p>Value: {{dynamicObject.value}}</p></div>");
                            $(fileupload).hide();
                            setupProgress();
                        }
                        console.log("replaced");
                    },
                    start: new function (e, data) {
                        console.log("start");
                    },
                    stop: new function (e, data) {
                        console.log("stop");
                    },
                    change: new function (e, data) {
                        console.log("change");
                    }
                };

                $('#fileupload')
                    .bind('fileuploadcompleted', function (e, data) {
                        console.log('Processing ' + data.files[data.index].name + ' done.');
                    });

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
        ]);
//
    function setupProgress() {
        myApp.factory('progressSocketService', ['$rootScope', function ($rootScope) {
                console.log("Progress - Websocket factory...");
                // We return this object to anything injecting our service
                var Service = {


                };

                $rootScope.dynamicObject = {
                    value: 0,
                    type: 'success'
                };

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
                    console.log("new value : " + $rootScope.dynamicObject.value);
                    $rootScope.$apply()
                }

                return Service;
            }])

            .controller('ProgressController', [ '$rootScope', 'progressSocketService',
                function ($rootScope, progressSocketService) {
                    console.log("service running");
                }
            ]);
    }

}());
