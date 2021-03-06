(function () {
   'use strict';

    /** Controllers */
    angular.module('wikiWatch.controllers', ['wikiWatch.services']).
        controller('WikiCtrl', function ($scope, $http, $websocket, $routeParams, $location, wikiModel) {
            $scope.channels = wikiModel.getChannels();
            $scope.currentChannel = $scope.channels[0];

            var index;
            var found = false;
            for (index = 0; index < $scope.channels.length; ++index) {
                var ch = $scope.channels[index];

                if (ch.value === $routeParams.channel) {
                    $scope.currentChannel = ch;
                    found = true;
                    break;
                }
            }

            if (found === false) {
                $location.path("/channel/all");
            }

            $scope.msgs = [];
            $scope.counter = 0;
            $scope.connectionAttempt = 0;
            $scope.wikiStream = null;

            /** change current channel */
            $scope.setCurrentChannel = function (channel) {
                $scope.currentChannel = channel;
                $location.path("/channel/" + channel.value);
            };

            /** change current channel, restart websocket connection */
            $scope.changeChannel = function () {
                $scope.msgs = [];
                $scope.counter = 0;
                $scope.listen(1);
            };

            /** handle incoming messages: add to messages array */
            $scope.addMsg = function (msg) {
                $scope.$apply(function () {
                    $scope.msgs.push(
                        {
                            user: msg.user,
                            text: msg.page,
                            timestamp: $scope.time(msg.timestamp),
                            diffUrl: msg.diffUrl
                        }
                    );
                    $scope.counter = $scope.counter + 1;
                });
            };

            $scope.time = function (ms) {
                return new Date(ms).toString();
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

                $scope.wikiStream = $websocket(stream_uri);

                $scope.wikiStream.onMessage(function(event) {
                    console.log(event);
                    var data = JSON.parse(event.data);
                    $scope.addMsg(data);
                });

                $scope.wikiStream.onOpen(function() {
                    $scope.connectionAttempt = 1;
                    $scope.wikiStream.send("subscribe");
                });
            };

            $scope.listen(1);
        });
}());
