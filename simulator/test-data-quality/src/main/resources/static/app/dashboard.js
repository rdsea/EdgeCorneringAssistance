(function() {
    'use strict';

    taxiApp.controller('dashboardController', DashboardController);
    DashboardController.$inject = ["$http", "$scope"];

    var vm = null;
    var scope = null;


    function DashboardController($http, $scope) {
        vm = this;
        scope = $scope;
        vm.testRunning = false;
        vm.reponse = "";
        vm.error = false;
        vm.showMeasuredCurves = false;
        vm.autoMoveMap = true;
        vm.toggleDefaults = function() {
            vm.precision = 6;
            vm.pollDelay = 3;
            vm.maxPoll = 3;
            vm.staticRecAddress = "35.205.177.231";
            vm.enableLogging = false;
            vm.constantSpeed = 50;
            vm.callRecServer = true;
            vm.gpsErrors = false;
            vm.gpsOutages = false;
        };
        vm.toggleDefaults();

        // Init Socket
        var socket = {
            start: function () {
                var location = "ws://localhost:9292/dashboard";
                this._ws = new WebSocket(location);
                this._ws.binaryType = 'arraybuffer';
                this._ws.onmessage = this._onmessage;
                this._ws.onclose = this._onclose;
            },

            _onmessage: function (m) {
                var str = new TextDecoder('UTF-8').decode(m.data);
                console.log(str);
                onEventReceived(str);
            },

            _onclose: function (m) {
                if (this._ws) {
                    this._ws.close();
                }
            }
        };
        socket.start();

        //Map Stuff
        vm.controls = {
            custom: new L.Control.Fullscreen()
        };
        vm.markers = {};
        vm.paths = {};
        vm.markers["car"] = {
            lat: 48.1502559,
            lng: 15.5367232
        };
        vm.center = {lat: 48.1502559,lng: 15.5367232,zoom: 14};

        vm.paths = {
                c27: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.099881, lng: 15.422875},
                    radius: 212,
                    type: 'circle',
                    label: {message: "<p>measured: 212</p><p>id: c27</p>"}
                },
                c28: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.096802, lng: 15.450801},
                    radius: 107,
                    type: 'circle',
                    label: {message: "<p>measured: 107</p><p>id: c28</p>"}
                },
                c29: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.094158, lng: 15.460351},
                    radius: 176,
                    type: 'circle',
                    label: {message: "<p>measured: 176</p><p>id: c29</p>"}
                },
                c31: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.096025, lng: 15.470488},
                    radius: 108,
                    type: 'circle',
                    label: {message: "<p>measured: 108</p><p>id: c31</p>"}
                },
                c33: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.095161, lng: 15.475041},
                    radius: 168.5,
                    type: 'circle',
                    label: {message: "<p>measured: 168.5</p><p>id: c33</p>"}
                },
                c35: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.094973, lng: 15.477507},
                    radius: 220,
                    type: 'circle',
                    label: {message: "<p>measured: 220</p><p>id: c35</p>"}
                },
                c39: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.095331, lng: 15.484366},
                    radius: 71,
                    type: 'circle',
                    label: {message: "<p>measured: 71</p><p>id: c39</p>"}
                },
                c40: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.09482, lng: 15.485075},
                    radius: 93,
                    type: 'circle',
                    label: {message: "<p>measured: 93</p><p>id: c40</p>"}
                },
                c7: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.14473, lng: 15.495242},
                    radius: 295.5,
                    type: 'circle',
                    label: {message: "<p>measured: 295.5</p><p>id: c7</p>"}
                },
                c5: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.146191, lng: 15.495941},
                    radius: 245,
                    type: 'circle',
                    label: {message: "<p>measured: 245</p><p>id: c5</p>"}
                },
                c9: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.136539, lng: 15.483416},
                    radius: 351.5,
                    type: 'circle',
                    label: {message: "<p>measured: 351.5</p><p>id: c9</p>"}
                },
                c11: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.130847, lng: 15.481313},
                    radius: 226,
                    type: 'circle',
                    label: {message: "<p>measured: 226</p><p>id: c11</p>"}
                },
                c1: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.151677, lng: 15.525706},
                    radius: 286,
                    type: 'circle',
                    label: {message: "<p>measured: 286</p><p>id: c1</p>"}
                },
                c4: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.151296, lng: 15.517207},
                    radius: 286,
                    type: 'circle',
                    label: {message: "<p>measured: 286</p><p>id: c4</p>"}
                },
                c13: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.129647, lng: 15.47886},
                    radius: 245.5,
                    type: 'circle',
                    label: {message: "<p>measured: 245.5</p><p>id: c13</p>"}
                },
                c15: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.116701, lng: 15.459248},
                    radius: 150,
                    type: 'circle',
                    label: {message: "<p>measured: 150</p><p>id: c15</p>"}
                },
                c17: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.116552, lng: 15.456408},
                    radius: 228.5,
                    type: 'circle',
                    label: {message: "<p>measured: 228.5</p><p>id: c17</p>"}
                },
                c19: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.115907, lng: 15.446606},
                    radius: 294,
                    type: 'circle',
                    label: {message: "<p>measured: 294</p><p>id: c19</p>"}
                },
                c21: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.114155, lng: 15.439727},
                    radius: 249.5,
                    type: 'circle',
                    label: {message: "<p>measured: 249.5</p><p>id: c21</p>"}
                },
                c23: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.111861, lng: 15.433888},
                    radius: 197,
                    type: 'circle',
                    label: {message: "<p>measured: 197</p><p>id: c23</p>"}
                },
                c25: {
                    weight: 1,
                    color: '#ff2215',
                    latlngs: {lat: 48.111403, lng: 15.42859},
                    radius: 160.5,
                    type: 'circle',
                    label: {message: "<p>measured: 160.5</p><p>id: c25</p>"}
                }
        };



        vm.startSimulation = function() {
            vm.curveCount = 0;
            vm.enteredCurveCount = 0;
            vm.gpsLocations = 0;
            vm.outageCount = 0;
            $http({
                url: "/startSimulation",
                method: "GET",
                params: {
                    driverSpeed: vm.constantSpeed,
                    driverGeoHashPrecision: vm.precision,
                    sendRecPollDelaySeconds: vm.pollDelay,
                    maxRecPolls: vm.maxPoll,
                    staticRecAddress: vm.staticRecAddress,
                    enableLogging: vm.enableLogging,
                    callRecommendationServer: vm.callRecServer,
                    addGPSInaccuracies: vm.gpsErrors,
                    addGPSOutages: vm.gpsOutages
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
        };

        vm.stopSimulation = function() {
            $http.get('/stopSimulation').
            then(function(response) {
                if (response.status == 200) {
                    vm.testRunning = false;
                }
            });
        };

    }

    function onEventReceived(str) {
        var json = JSON.parse(str);
        scope.$apply(function(){

            switch (json["event"].toString()) {
                case "location":
                    vm.gpsLocations++;
                    var gpsId = "gps:" + vm.gpsLocations;
                    vm.markers["car"] = {
                        lat: json["lat"],
                        lng: json["lon"]
                    };
                    vm.paths[gpsId] = {
                        weight:1,
                        color:'#000000',
                        latlngs:{lat:json["lat"],lng:json["lon"]},
                        label: {message: "<p>entered: " + json["radius"]+ "</p>"},
                        radius:3,
                        type:'circle'
                    };
                    if (vm.autoMoveMap) {
                        vm.center = {
                            lat: json["lat"],
                            lng: json["lon"],
                            zoom: 17
                        };
                    };
                    break;
                case "curve":
                    vm.enteredCurveCount++;
                    var id = "detectedCurveId:" + vm.enteredCurveCount;
                    vm.paths[id] = {
                        label: {message: "<p>entered: " + json["radius"]+ "</p>" + "</p><p>recSpeed: " + json["speed"]    + "</p>"},
                        color: '#803873',
                        weight: 8,
                        latlngs: [
                            { lat: json["lat1"], lng: json["lon1"]},
                            { lat: json["lat2"], lng: json["lon2"] },
                            { lat: json["lat3"], lng: json["lon3"] }
                        ]
                    };
                    break;
                case "curves":
                    var curves = json["curves"];
                    curves.forEach(function(curve){
                        vm.curveCount++;
                        var id = "id:" + vm.curveCount;
                        vm.paths[id] = {
                            label: {message: "<p>detected: " + curve["radius"]+ "</p><p>recSpeed: " + curve["speed"]    + "</p>"},
                            color: '#008000',
                            weight: 3,
                            latlngs: [
                                { lat: curve["lat1"], lng: curve["lon1"]},
                                { lat: curve["lat2"], lng: curve["lon2"] },
                                { lat: curve["lat3"], lng: curve["lon3"] }
                            ]
                        };

                    });

                    break;
                case "status":
                    vm.status = json["status"];
                    break;
                case "bb":
                    vm.paths["bb"] = {
                        color: '#000000',
                        weight: 2,
                        latlngs: [
                            { lat: json["minLat"], lng: json["minLon"] },
                            { lat: json["minLat"], lng: json["maxLon"] },
                            { lat: json["maxLat"], lng: json["maxLon"] },
                            { lat: json["maxLat"], lng: json["minLon"] },
                            { lat: json["minLat"], lng: json["minLon"] }
                        ]
                    };
                    break;
                case "outage":
                    vm.outageCount++;
                    var outageId = "outageId: " + vm.outageCount;
                    vm.markers[outageId] = {
                        lat: json["lat"],
                        lng: json["lon"],
                        icon: {
                            type: 'awesomeMarker',
                            icon: 'cog',
                            markerColor: 'black'
                        },
                        label: {message: "<p>GPS outage started here</p>"}
                    };
                    break;
            }
        });
    }



})();
