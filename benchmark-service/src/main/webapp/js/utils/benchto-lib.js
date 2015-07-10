/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */

var CartHelper = (function () {
    'use strict';

    return {
        setCartAddedFlag: function (CartCompareService, benchmarkRuns) {
            _.forEach(benchmarkRuns, function (benchmarkRun) {
                benchmarkRun.addedToCompare = CartCompareService.contains(benchmarkRun);
            });
        },
        toggleCartAddedFlag: function (CartCompareService, benchmarkRuns, changedBenchmarkRun, addedToCompate) {
            var sameBenchmarkRun = CartCompareService.findInCollection(benchmarkRuns, changedBenchmarkRun);
            if (sameBenchmarkRun) {
                sameBenchmarkRun.addedToCompare = addedToCompate;
            }
        },
        updateBenchmarkCartSelection: function (CartCompareService, changedBenchmarkRun) {
            if (changedBenchmarkRun.addedToCompare) {
                CartCompareService.add(changedBenchmarkRun);
            }
            else {
                CartCompareService.remove(changedBenchmarkRun);
            }
        }
    };
}).call('CartHelper');

var BenchmarkRunsHelper = function (benchmarkRuns) {
    'use strict';

    this.benchmarkRuns = benchmarkRuns;

    this.aggregatedExecutionsMeasurementKeys = function () {
        return _.uniq(_.flatten(_.map(this.benchmarkRuns,
            function (benchmarkRun) {
                return _.allKeys(benchmarkRun.aggregatedMeasurements);
            }
        )));
    };

    this.aggregatedExecutionsMeasurementUnit = function (measurementKey) {
        for (var i = 0; i < this.benchmarkRuns.length; ++i) {
            var aggregatedMeasurement = this.benchmarkRuns[i].aggregatedMeasurements[measurementKey];
            if (aggregatedMeasurement) {
                return aggregatedMeasurement.unit;
            }
        }
        throw new Error("Could not find unit for measurement key: " + measurementKey);
    };

    this.extractAggregatedExecutionsAggregatedMeasurements = function (measurementKey) {
        return _.map(this.benchmarkRuns, function (benchmarkRun) {
            return benchmarkRun.aggregatedMeasurements[measurementKey];
        });
    };

// benchmark measurements graph data
    this.benchmarkMeasurementKeys = function () {
        return _.chain(this.benchmarkRuns)
            .map(function (benchmarkRun) { return _.pluck(benchmarkRun.measurements, 'name'); })
            .flatten()
            .uniq()
            .value();
    };

    this.benchmarkMeasurementUnit = function (measurementKey) {
        for (var i = 0; i < this.benchmarkRuns.length; ++i) {
            var measurement = _.findWhere(this.benchmarkRuns[i].measurements, {name: measurementKey});
            if (measurement) {
                return measurement.unit;
            }
        }
        throw new Error("Could not find unit for measurement key: " + measurementKey);
    };

    this.extractBenchmarkMeasurements = function (measurementKey) {
        return _.map(this.benchmarkRuns, function (benchmarkRun) {
            return _.findWhere(benchmarkRun.measurements, {name: measurementKey});
        });
    };

    this.dataForSingleMeasurementKey = function (measurements, singleMeasurement) {
        return {
            "key": singleMeasurement,
            "values": _.zip(
                _.map(_.range(this.benchmarkRuns.length), function (i) { return i + 1; }),
                _.pluck(measurements, singleMeasurement)
            )
        };
    };

    this.dataForAggregatedMeasurementKey = function (measurementKey) {
        var aggregatedMeasurements = this.extractAggregatedExecutionsAggregatedMeasurements(measurementKey);
        return [
            this.dataForSingleMeasurementKey(aggregatedMeasurements, 'mean'),
            this.dataForSingleMeasurementKey(aggregatedMeasurements, 'min'),
            this.dataForSingleMeasurementKey(aggregatedMeasurements, 'max'),
            this.dataForSingleMeasurementKey(aggregatedMeasurements, 'stdDev')
        ];
    };

    this.aggregatedExecutionsMeasurementGraphsData = function (chartType, $filter) {
        var benchmarkRuns = this.benchmarkRuns;
        return _.map(this.aggregatedExecutionsMeasurementKeys(), function (measurementKey) {
            var super_this = new BenchmarkRunsHelper(benchmarkRuns);
            var unit = super_this.aggregatedExecutionsMeasurementUnit(measurementKey);
            var data = super_this.dataForAggregatedMeasurementKey(measurementKey);
            return {
                data: data,
                options: super_this.optionsFor(data, chartType, measurementKey, unit, $filter)
            }
        });
    };

    this.benchmarkMeasurementGraphsData = function (chartType, $filter) {
        var benchmarkRuns = this.benchmarkRuns;
        return _.map(this.benchmarkMeasurementKeys(), function (measurementKey) {
            var super_this = new BenchmarkRunsHelper(benchmarkRuns);
            var unit = super_this.benchmarkMeasurementUnit(measurementKey);
            var measurements = super_this.extractBenchmarkMeasurements(measurementKey);
            var data = [super_this.dataForSingleMeasurementKey(measurements, "value")];
            return {
                data: data,
                options: super_this.optionsFor(data, chartType, measurementKey, unit, $filter)
            };
        });
    };

    this.optionsFor = function (data, chartType, measurementKey, unit, $filter) {
        var maxY = _.chain(data)
            .map(function (singleData) { return singleData.values; })
            .flatten()
            .max()
            .value();

        var filteredMaxYWithUnit;
        if (measurementKey == 'duration') {
            filteredMaxYWithUnit = $filter('duration')(maxY);
        }
        else {
            filteredMaxYWithUnit = $filter('unit')(maxY, unit);
        }

        var filteredMaxY = parseFloat(filteredMaxYWithUnit);
        var valueScaleFactor = filteredMaxY / maxY;
        unit = filteredMaxYWithUnit.split(' ')[1];

        return {
            chart: {
                type: chartType,
                height: 250,
                width: null,
                x: function (d) {
                    return d[0];
                },
                y: function (d) {
                    return d[1] * valueScaleFactor;
                },
                yDomain: [0, filteredMaxY],
                useInteractiveGuideline: true,
                stacked: false,
                yAxis: {
                    axisLabel: unit,
                    tickFormat: function (d) {
                        return d3.format('.01f')(d);
                    }
                },
                xAxis: {
                    axisLabel: 'benchmark execution id',
                }
            },
            title: {
                enable: true,
                text: measurementKey
            }
        };
    };

    return this;
};
