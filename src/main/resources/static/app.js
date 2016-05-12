//
//
// Data structure notes:
// 
// $scope has the following structure:
// $scope.jobParams       : used to read the job parameters from the GUI
// $scope.jobs            : array of simulation jobs that have been run
// $scope.intervalPromise : used to poll server for job data
// $scope.colors          : array of available colours
// $scope.plots           : array with an entry for each plot
// 
// 
// Each job in the $scope.jobs array has:
//    jobId       : UUID for the job
//    haveData    : boolean - true if have any data from simulation
//    lastTime    : last time value that we have data for
//    isRunning   : boolean - is job still running
//    jobParams   : parameters given to the simulation job
//    label       : user-specified label for the job
//    downloadurl : when job is finished this will contain url to downlaod the data
//    color       : colour used for the job in the plots
//

// TODO - update the notes above on the data structures as they are wrong   
// TODO - delete the job directory 5 mins after running
// TODO - clean up visuals
// POPUP ERROR IF FILE TO CONTACT THE SERVER TO SUBMIT THE JOB

var myApp = angular.module('FMSimulator', ['nvd3']);

// This is required to support the download feature
myApp.config(['$compileProvider',
    function ($compileProvider) {
        $compileProvider.aHrefSanitizationWhitelist(/^\s*(https?|ftp|mailto|tel|file|blob):/);
}]);


myApp.controller('mainController', ['$scope', '$log', '$http', '$interval', function($scope, $log, $http, $interval) {
    
    $scope.jobParams = { 
        dayLength : 12,
        light        : {day:  145,  night :     0, twilight: 0, showGraphOption : true,  showNightValue : true },
        temperature  : {day:  19.0, night :  19.0, twilight: 0, showGraphOption : true,  showNightValue : true },
        co2          : {day:  42.0,                twilight: 0, showGraphOption : false, showNightValue : false } 
    };
    
    $scope.jobLabel = "SimulationName";
    $scope.jobs = [];
    $scope.intervalPromise = null;
    
    // Need these in an object to overcome shallow $scope cloning in directives such as ng-if
    $scope.flags = {showParameters: false, showEnvironmentalPlots: false };

    $scope.runSimulation = function() {
        
        var requestJobParams = {};
        requestJobParams.light = {};
        requestJobParams.light.day       = $scope.jobParams.light.day;
        requestJobParams.light.night     = $scope.jobParams.light.night;
        requestJobParams.light.dayLength = $scope.jobParams.dayLength;
        requestJobParams.light.twilight  = $scope.jobParams.light.twilight;

        requestJobParams.temperature = {};
        requestJobParams.temperature.day       = $scope.jobParams.temperature.day;
        requestJobParams.temperature.night     = $scope.jobParams.temperature.night;
        requestJobParams.temperature.dayLength = $scope.jobParams.dayLength;
        requestJobParams.temperature.twilight  = $scope.jobParams.temperature.twilight;

        requestJobParams.co2 = {};
        requestJobParams.co2.day       = $scope.jobParams.co2.day;
        requestJobParams.co2.night     = $scope.jobParams.co2.day;  // Use day for night value
        requestJobParams.co2.dayLength = $scope.jobParams.dayLength;
        requestJobParams.co2.twilight  = $scope.jobParams.co2.twilight;
                
        // Ask web service to run the model simulation
        $http.post('/modelRunner', requestJobParams).success(function newSim(result) {
            newJob = {};
            newJob.jobId     = result.id;
            newJob.haveData  = false;
            newJob.isRunning = true;
            newJob.lastTime  = 0;
            newJob.jobParams = JSON.parse(JSON.stringify(requestJobParams));
            newJob.label     = getLabel($scope.jobLabel);
            newJob.downloadName = newJob.label.replace(/ /g,"_");
            addNewJob(newJob);
        });
        // TODO: need to handle errors from that request
    };
    
    getLabel = function(label) {
        if (label === undefined || label.length === 0) label = "simulation";

        // Put all labels into an object
        var allLabels = [];
        for (var i=0; i<$scope.jobs.length; i++) {
            allLabels.push($scope.jobs[i].label);
        }
        // If label is unique then we are ok to use it
        if (allLabels.indexOf(label) === -1) {
            return label;
        }
        
        // We have to append to it until unique
        var version = 2;
        while( true ) {
            var newLabel = label+"_v" + version;
            if (allLabels.indexOf(newLabel) === -1 ) return newLabel;
            version++;
        }
    };
    
    $scope.deleteSimulation = function(job) {
        // Remove job for the jobs array
        for(var i = $scope.jobs.length - 1; i >= 0; i--) {
            if($scope.jobs[i].jobId === job.jobId) {
                $scope.jobs.splice(i, 1);
            }
        }
        // Remove job from all the plots
        removeJobFromPlots(job);
        
        // Delete the object URL if there is one
        if (job.downloadurl !== undefined) {
            (window.URL || window.webkitURL).revokeObjectURL(job.downloadurl);
        }
    };
    
    addNewJob = function(newJob) {
        newJob.color = getNextColor();
        $scope.jobs.push(newJob);
        
        // Set up for polling for job data
        if ($scope.intervalPromise === null) {
            $scope.intervalPromise = $interval(pollForData, 1000);
        }
    };

        
    $scope.colors = ["red", "blue", "orange", "purple", "cyan", "HotPink", "FireBrick", "Yellow", "black", "teal", "thistle", "tan"];
    getNextColor = function() {
        // Collect the used colors
        var usedColors = {};
        for (var i=0; i < $scope.jobs.length; i++ ) {
            usedColors[$scope.jobs[i].color] = true;
        }
        // Now find an unused color
        for (var i=0; i < $scope.colors.length; i++) {
            if (!usedColors.hasOwnProperty($scope.colors[i])) {
                return $scope.colors[i];
            }
        }
        // Didn't find a free color - just return something
        return $scope.colors[$scope.jobs.length % $scope.colors.length];
    };
    
    pollForData = function() {
        haveRunningJob = false;
        
        for (var i=0; i<$scope.jobs.length; i++) {
            var job = $scope.jobs[i];
            if (job.isRunning) {
                $http.get('/modelRunner/'+job.jobId).success((function(job) {
                    return function(result) {
                        job.haveData = true;
                        job.jsondata = result; 
                        job.lastTime = findLastTime(job);
                        if (job.jsondata.status === "FINISHED") {
                            job.isRunning = false;
                            addJobToPlots(job);
                            updateJobInPlots(job);
                            
                            // Build download blob
                            //var content = 'file content for example : ' + job.label;
                            //var blob = new Blob([ content ], { type : 'text/plain' });
                            var blob = buildDataDownload(job);
                            job.downloadurl = (window.URL || window.webkitURL).createObjectURL( blob );
                        }
                    };
                })(job));
            haveRunningJob = true;
            }
        }
        if (!haveRunningJob) {
            $interval.cancel($scope.intervalPromise);
            $scope.intervalPromise = null;
        }
    };
    
    findLastTime = function(job) {
        var numDataPoints = job.jsondata.data.length;
        if (numDataPoints > 0) {
            return job.jsondata.data[numDataPoints-1]['t'];
        }
        return 0;
    };
    
    buildDataDownload = function(job) {
        var csvData = [];
        var columns = [];
        // Add all columns except time 
        for (var key in job.jsondata.data[0]) {
            if (job.jsondata.data[0].hasOwnProperty(key)) {
                if (key !== 't') columns.push(key);
            }
        }
        // Write the parameters
        csvData.push("# Light PAR (micromol m-2 s-1):");
        addIssfToDataDownload(csvData, job.jobParams.light);
        csvData.push("\n");
        csvData.push("# Temperature (deg C):");
        addIssfToDataDownload(csvData, job.jobParams.temperature);
        csvData.push("\n");
        csvData.push("# CO2 (Pa):");
        addIssfToDataDownload(csvData, job.jobParams.co2);
        csvData.push("\n");
        
        // Write the header
        csvData.push("t,");
        csvData.push(columns.join());
        csvData.push("\n");
        // Write the data
        for (var i = 0, l = job.jsondata.data.length; i < l; i++) {
            var row = job.jsondata.data[i];
            
            // If the data has nulls then we stop here
            if (row[columns[0]] === null) break;
            
            var row = job.jsondata.data[i];
            csvData.push(row['t'].toString());
            for (var j=0; j < columns.length; j++) {
                var key = columns[j];
                csvData.push(",");
                csvData.push(row[key].toString());
            }
            csvData.push("\n");
        }
        return new Blob( csvData, { type : 'text/plain' });
    };
    
    addIssfToDataDownload = function(csvData, issf) {
        csvData.push(" day: "        + issf.day);
        csvData.push(" night: "      + issf.night);
        csvData.push(" day length: " + issf.dayLength);
        csvData.push(" twilight: "  + issf.twilight);
    };
     
    addJobToPlots = function(job) {
        $scope.plotData.push(
                { 
                    values : [], 
                    key    : job.label, 
                    color  : job.color,
                    jobId  : job.jobId
                });
    };
    
    updateJobInPlots = function(job) {
        for(var i=0; i<$scope.plotData.length; i++) {
            if($scope.plotData[i].jobId === job.jobId) {
                if (job.haveData) {
                    $scope.plotData[i].values = job.jsondata.data;
                }
            }
        }
        
        // To get tooltips to work nicely we need entries in each dataset for
        // each time point and null values in them as well
        
        // First fine the longest plot
        var index = -1;
        var longestSize = -1;
        for (var i=0; i<$scope.plotData.length; i++) {
            var size = $scope.plotData[i].values.length;
            if (size > longestSize) {
                index = i;
                longestSize = size;
            }
        }
        // Pad the shorter plots to match the longest one
        for (var i=0; i<$scope.plotData.length; i++) {
            var size = $scope.plotData[i].values.length;
            if (size < longestSize) {
                // Copy everything over from the longest but with null values
                for (var j=size; j<longestSize; j++) {
                    var time = $scope.plotData[index].values[j]['t'];
                    var obj = {};
                    for (var key in $scope.plotData[index].values[j]) {
                        if ($scope.plotData[index].values[j].hasOwnProperty(key)) {
                            obj[key] = null;
                        }
                    }
                    obj['t'] = time;
                    $scope.plotData[i].values.push(obj);
                }
            }
        }
    };
    
    removeJobFromPlots = function(job) {
        for(var i= $scope.plotData.length - 1; i >= 0; i--) {
            if($scope.plotData[i].jobId === job.jobId) {
                $scope.plotData.splice(i, 1);
                break;
            }
        }
        
        // Need to resize if the removed job was the longest
        var maxTime = -1;
        for (var i=0; i<$scope.jobs.length; i++) {
            var j = $scope.jobs[i];
            if ((!j.isRunning) && j.lastTime > maxTime) {
                maxTime = j.lastTime;
            }
        }
        for (var i=0; i<$scope.jobs.length; i++) {
            var j = $scope.jobs[i];
            if ((!j.isRunning)) {
                var plotData = getJobPlotData(j.jobId);
                if (plotData.values.length > maxTime) {
                    // Need to trim the data for this job
                    reduceJobPlotDataSize(plotData, maxTime);
                }
            }
        }
    };
    
    getJobPlotData = function(jobId) {
        for(var i=0; i<$scope.plotData.length; i++) {
            if($scope.plotData[i].jobId === jobId) {
                return $scope.plotData[i];
            }
        }
        return null;
    };
    
    reduceJobPlotDataSize = function(jobPlotData, maxTime) {
        jobPlotData.values.splice(maxTime, jobPlotData.values.length-maxTime);
    };
    

    $scope.plotData = [];
            
    $scope.plots = [
        {
            chart: {
                type: 'lineChart',
                margin : {
                    top: 20,
                    right: 20,
                    bottom: 40,
                    left: 70
                },
                x: function(d){ return d.t; },
                y: function(d){ if (d.rosette_fw === null) return null; return 1000 * d.rosette_fw; },
                useInteractiveGuideline: true,
                xAxis: {
                    axisLabel: 'Time (hours)',
                },
                yAxis: {
                    axisLabel: 'Mass (mg)',
                    tickFormat: function(d){
                        return d3.format('.03f')(d);
                    },
                    axisLabelDistance: 10
                }
            },
            title: {
                enable: true,
                text: 'Rosette Biomass (fresh weight)'
            },
            environmental : false
        },        
        {
            chart: {
                type: 'lineChart',
               
                margin : {
                    top: 20,
                    right: 20,
                    bottom: 40,
                    left: 70
                },
                x: function(d){ return d.t; },
                y: function(d){ if (d.root_fw === null) return null; return 1000 * d.root_fw; },
                useInteractiveGuideline: true,
                xAxis: {
                    axisLabel: 'Time (hours)'
                },
                yAxis: {
                    axisLabel: 'Mass (mg)',
                    tickFormat: function(d){
                        return d3.format('.03f')(d);
                    },
                    axisLabelDistance: 10
                }
            },
            title: {
                enable: true,
                text: 'Root Biomass (fresh weight)'
            },
            environmental : false
        },
        {
            chart: {
                type: 'lineChart',
               
                margin : {
                    top: 20,
                    right: 20,
                    bottom: 40,
                    left: 70
                },
                x: function(d){ return d.t; },
                y: function(d){ if (d.starch === null) return null; return 1000 * d.starch; },
                useInteractiveGuideline: true,
                xAxis: {
                    axisLabel: 'Time (hours)'
                },
                yAxis: {
                    axisLabel: 'Mass (mg)',
                    tickFormat: function(d){
                        return d3.format('.03f')(d);
                    },
                    axisLabelDistance: 10
                }
            },
            title: {
                enable: true,
                text: 'Starch'
            },
            environmental : false
        },
        {
            chart: {
                type: 'lineChart',
               
                margin : {
                    top: 20,
                    right: 20,
                    bottom: 40,
                    left: 70
                },
                x: function(d){ return d.t; },
                y: function(d){ return d.light; },
                useInteractiveGuideline: true,
                xAxis: {
                    axisLabel: 'Time (hours)'
                },
                yAxis: {
                    axisLabel: 'Light (\u00B5mol m\u207B\u00B2 s\u207B\u00B9)',
                    tickFormat: function(d){
                        return d3.format('.01f')(d);
                    },
                    axisLabelDistance: 10
                }
            },
            title: {
                enable: true,
                text: 'Light (PAR absortption)'
            },
            environmental : true
        },
        {
            chart: {
                type: 'lineChart',
               
                margin : {
                    top: 20,
                    right: 20,
                    bottom: 40,
                    left: 70
                },
                x: function(d){ return d.t; },
                y: function(d){ return d.temp; },
                useInteractiveGuideline: true,
                xAxis: {
                    axisLabel: 'Time (hours)'
                },
                yAxis: {
                    axisLabel: 'Temperature (\u00b0C)',
                    tickFormat: function(d){
                        return d3.format('.01f')(d);
                    },
                    axisLabelDistance: 10
                }
            },
            title: {
                enable: true,
                text: 'Temperature'
            },
            environmental : true
        },
        {
            chart: {
                type: 'lineChart',
               
                margin : {
                    top: 20,
                    right: 20,
                    bottom: 40,
                    left: 70
                },
                x: function(d){ return d.t; },
                y: function(d){ return d.co2; },
                useInteractiveGuideline: true,
                xAxis: {
                    axisLabel: 'Time (hours)'
                },
                yAxis: {
                    axisLabel: 'CO\u2082 partial pressure (Pa)',
                    tickFormat: function(d){
                        return d3.format('.01f')(d);
                    },
                    axisLabelDistance: 10
                }
            },
            title: {
                enable: true,
                text: 'CO\u2082'
            },
            environmental : true
        }
    ];
}]);

myApp.directive("issf", function() {
    
    var controller = ['$scope', function ($scope) {
            
        $scope.computePlotData = function() {
            $scope.plotData[0].values = [];
            
            var dayValue   = $scope.values.day;
            var nightValue = $scope.values.night;
            var dayLength  = $scope.daylength;
            var twilight   = $scope.values.twilight;
            
            // Initially assume day value is high
            var theta0 = nightValue;
            var theta1 = dayValue - nightValue;
            Tp = dayLength;
            Tc = 24;
            T = 0.0001;
            if (twilight > T) T = twilight;

            for(var i=0; i<25; ++i) {
                t = (i+0.5) % 24;
                if (t<0) t = 24+t;
                v = theta0 + 0.5 * theta1 * ( ( 1 + Math.tanh(t/T)) - (1+ Math.tanh((t-Tp)/T)) + (1 + Math.tanh((t-Tc)/T)));
                $scope.plotData[0].values.push({ "t": i, "v" : v});
            }
        };

        function init() {
            $scope.flags = {showGraph : false};
            $scope.plotData = [ {key: "data", values : []} ];
            $scope.computePlotData();
        }

        init();

        $scope.change = function () {
            $scope.computePlotData();
        };
        
        $scope.$watch('daylength', function() {
            $scope.computePlotData();
        });
          
        $scope.plot = {
            chart: {
                type: 'lineChart',
                margin : {
                    top: 20,
                    right: 20,
                    bottom: 40,
                    left: 50
                },
                x: function(d){ return d.t; },
                y: function(d){ return d.v; }, 
                useInteractiveGuideline: true,
                xAxis: {
                    axisLabel: 'Time (hours)'
                },
                yAxis: {
                    tickFormat: function(d){
                        return d3.format('.01f')(d);
                    },
                    axisLabelDistance: -10
                }
            }
        };
    }];
  
   return {
       restrict : 'E',
       templateUrl: 'directives/issf.html',
       replace: true,
       controller: controller,
       scope: {
           label : "@",
           values : "=",
           daylength : "="
       }
   }; 
});

