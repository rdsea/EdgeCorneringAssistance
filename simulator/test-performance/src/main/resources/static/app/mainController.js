//creates the module with the name 'taxiApp'
var taxiApp = angular.module('simulatorApp', ['ngRoute']);

taxiApp.config(['$routeProvider', function($routeProvider) {
	$routeProvider
    	.when('/', {
    		templateUrl: '/pages/dashboard.html',
    		controller : 'dashboardController',
			controllerAs: 'dc'
    	})
    	.otherwise('/');
}]);

taxiApp.controller('mainController', function() {

});

