(function () {
   'use strict';

    /** Controllers */
    angular.module('wikiWatch.controllers', ['wikiWatch.services']).
        controller('WikiCtrl', function ($scope, $http, wikiModel) {
            $scope.channels = wikiModel.getChannels();
            $scope.currentChannel = $scope.channels[0];
            $scope.msgs = [];
            $scope.connectionAttempt = 0;
            $scope.wikiStream = null;

            /** change current channel, restart websocket connection */
            $scope.setCurrentChannel = function (channel) {
                $scope.currentChannel = channel;
                $scope.msgs = [];
                $scope.listen(1);
            };

            /** handle incoming messages: add to messages array */
            $scope.addMsg = function (msg) {
                $scope.$apply(function () {
                    $scope.msgs.push(
                        {
                            user: msg.user,
                            text: msg.page
                        }
                    );
                });
            };

            /** start listening on messages from selected channel */
            $scope.listen = function (attempt) {
                $scope.connectionAttempt = attempt;

                if ($scope.wikiStream !== null) {
                    $scope.wikiStream.send("unsubscribe");
                    $scope.wikiStream.close();
                    $scope.wikiStream = null;
                }

                var loc = window.location, stream_uri;
                if (loc.protocol === "https:") {
                    stream_uri = "wss:";
                } else {
                    stream_uri = "ws:";
                }
                stream_uri += "//" + loc.host;
                stream_uri += "/stream/" + $scope.currentChannel.value;

                $scope.wikiStream = new WebSocket(stream_uri);

                $scope.wikiStream.onmessage = function(event) {
                    console.log(event);
                    var data = JSON.parse(event.data);
                    $scope.addMsg(data);
                };

                $scope.wikiStream.onopen = function() {
                    $scope.connectionAttempt = 1;
                    $scope.wikiStream.send("subscribe");
                };
            };

            $scope.listen(1);
        });
}());
