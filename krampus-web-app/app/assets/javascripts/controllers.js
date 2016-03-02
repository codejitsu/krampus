(function () {
   'use strict';

    /** Controllers */
    angular.module('wikiWatch.controllers', ['wikiWatch.services']).
        controller('WikiCtrl', function ($scope, $http, wikiModel) {
            $scope.channels = wikiModel.getChannels();
            $scope.currentChannel = $scope.channels[0];
            $scope.msgs = [{user: 'alex', text: 'hi'}, {user: 'bob', text: 'hi there'}, {user: 'alice', text: 'ola'}];

            /** change current channel, restart websocket connection */
            $scope.setCurrentChannel = function (channel) {
                $scope.currentChannel = channel;
    //            $scope.wikiStream.close();
                $scope.msgs = [];
                $scope.listen();
            };

            /** handle incoming messages: add to messages array */
            $scope.addMsg = function (msg) {
                $scope.$apply(function () { $scope.msgs.push(JSON.parse(msg.data)); });
            };

            /** start listening on messages from selected channel */
            $scope.listen = function () {
    //            $scope.wikiStream = new EventSource("/stream/" + $scope.currentChannel.value);
    //            $scope.wikiStream.addEventListener("message", $scope.addMsg, false);
            };

            $scope.listen();
        });
}());
