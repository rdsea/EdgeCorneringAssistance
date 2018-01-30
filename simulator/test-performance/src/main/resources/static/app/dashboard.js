(function() {
	'use strict';

	taxiApp.controller('dashboardController', DashboardController);
    DashboardController.$inject = ["$http"];

    var vm = null;

	function DashboardController($http) {
		vm = this;
        vm.testRunning = false;
        vm.reponse = "";
        vm.error = false;
        vm.useDefaults = false;
        vm.toggleDefaults = function() {
            //if (vm.useDefaults) {
                vm.numDrivers = 10;
                vm.precision = 6;
                vm.pollDelay = 3;
                vm.maxPoll = 3;
                vm.refreshConsul = 300;
                vm.maxRetries = 3;
                vm.staticRecAddress = "grpc-server";
                vm.enableLogging = false;
                vm.grpcTimeout = 5;
                vm.loadbalance = false;
            //}
        };
        vm.toggleDefaults();

        vm.start = function() {
            if (!vm.useDefaults) {
                $http({
                    url: "/startWithConfigs",
                    method: "GET",
                    params: {
                        numDrivers: vm.numDrivers,
                        driverGeoHashPrecision: vm.precision,
                        refreshConsulPeriodSeconds: vm.refreshConsul,
                        sendRecPollDelaySeconds: vm.pollDelay,
                        maxRecPolls: vm.maxPoll,
                        maxConsulRetries: vm.maxRetries,
                        staticRecAddress: vm.staticRecAddress,
                        enableLogging: vm.enableLogging,
                        grpcTimeout: vm.grpcTimeout,
                        loadbalance: vm.loadbalance
                    }
                }).then(function (response) {
                    console.log(response.data);
                    if (response.status == 200) {
                        vm.status=response.data.message;
                        vm.testRunning = true;
                    } else {
                        vm.status = "Error occured. Please try again";
                        vm.error = true;
                    }
                })
            } else {
                $http.get("/start").
                then(function(response) {
                    console.log(response.data);
                    if (response.status == 200) {
                        vm.status=response.data.message;
                        vm.testRunning = true;
                    } else {
                        vm.status = "Error occured. Please try again";
                        vm.error = true;
                    }
                });
            }

        };

        vm.stop = function() {
            $http.get('/stop').
            then(function(response) {
                if (response.status == 200) {
                    vm.testRunning = false;
                }
            });
        };



	}

})();
