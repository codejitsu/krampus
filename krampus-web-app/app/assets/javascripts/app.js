(function () {
   'use strict';

    /** app level module which depends on services and controllers */
    var wikiWatchApp = angular.module('wikiWatch', ['ngRoute', 'wikiWatch.services', 'wikiWatch.controllers']);

    wikiWatchApp.config(['$routeProvider',
      function($routeProvider) {
        $routeProvider.
          when('/channel/:channel', {
            templateUrl: 'wiki',
            controller: 'WikiCtrl'
          }).
          otherwise({
            redirectTo: '/channel/all'
          });
      }]);
}());
