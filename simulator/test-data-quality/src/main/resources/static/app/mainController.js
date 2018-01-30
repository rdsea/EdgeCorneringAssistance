//creates the module with the name 'taxiApp'
var taxiApp = angular.module('simulatorApp', ['ngRoute','leaflet-directive']);

taxiApp.config(['$routeProvider', function($routeProvider) {
	$routeProvider
    	.when('/', {
    		templateUrl: '/pages/dashboard.html',
    		controller : 'dashboardController',
			controllerAs: 'tc'
    	})
    	.otherwise('/');
}]);

taxiApp.controller('mainController', function() {

});

