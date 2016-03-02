(function () {
   'use strict';

    /** wikiModel service, provides channels (could as well be loaded from server) */
    angular.module('wikiWatch.services', []).service('wikiModel', function () {
        var getChannels = function() {
            return [
                  { name: "en", value: "en" },
                  { name: "sv", value: "sv" },
                  { name: "de", value: "de" },
                  { name: "nl", value: "nl" },
                  { name: "fr", value: "fr" },
                  { name: "war", value: "war" },
                  { name: "ru", value: "ru" },
                  { name: "ceb", value: "ceb" },
                  { name: "it", value: "it" },
                  { name: "es", value: "es" },
                  { name: "vi", value: "vi" },
                  { name: "pl", value: "pl" },
                  { name: "ja", value: "ja" },
                  { name: "pt", value: "pt" },
                  { name: "zh", value: "zh" },
                  { name: "uk", value: "uk" },
                  { name: "ca", value: "ca" },
                  { name: "fa", value: "fr" },
                  { name: "sh", value: "sh" },
                  { name: "no", value: "no" },
                  { name: "ar", value: "ar" },
                  { name: "fi", value: "fi" },
                  { name: "id", value: "id" },
                  { name: "ro", value: "ro" },
                  { name: "hu", value: "hu" },
                  { name: "cs", value: "cs" },
                  { name: "ko", value: "ko" },
                  { name: "sr", value: "sr" },
                  { name: "ms", value: "ms" },
                  { name: "tr", value: "tr" },
                  { name: "min", value: "min" },
                  { name: "eo", value: "eo" },
                  { name: "kk", value: "kk" },
                  { name: "eu", value: "eu" },
                  { name: "da", value: "da" },
                  { name: "bg", value: "bg" },
                  { name: "sk", value: "sk" },
                  { name: "hy", value: "hy" },
                  { name: "he", value: "he" },
                  { name: "lt", value: "lt" },
                  { name: "hr", value: "hr" },
                  { name: "sl", value: "sl" },
                  { name: "et", value: "et" },
                  { name: "uz", value: "uz" },
                  { name: "gl", value: "gl" },
                  { name: "nn", value: "nn" },
                  { name: "la", value: "la" },
                  { name: "vo", value: "vo" },
                  { name: "simple", value: "simple" },
                  { name: "el", value: "el" },
                  { name: "ce", value: "ce" },
                  { name: "hi", value: "hi" },
                  { name: "be", value: "be" }
            ];
        };

        return { getChannels: getChannels };
    });
}());
