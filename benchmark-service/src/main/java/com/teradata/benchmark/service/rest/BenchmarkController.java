/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.service.rest;

import com.teradata.benchmark.service.BenchmarkService;
import com.teradata.benchmark.service.model.Benchmark;
import com.teradata.benchmark.service.model.BenchmarkRun;
import com.teradata.benchmark.service.rest.requests.BenchmarkStartRequest;
import com.teradata.benchmark.service.rest.requests.ExecutionStartRequest;
import com.teradata.benchmark.service.rest.requests.FinishRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.List;

import static com.teradata.benchmark.service.utils.CollectionUtils.failSafeEmpty;
import static java.util.Optional.ofNullable;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class BenchmarkController
{

    @Autowired
    private BenchmarkService benchmarkService;

    @RequestMapping(value = "/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/start", method = POST)
    public void startBenchmark(
            @PathVariable("benchmarkName") String benchmarkName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId,
            @RequestBody @Valid BenchmarkStartRequest startRequest)
    {
        benchmarkService.startBenchmarkRun(benchmarkName,
                benchmarkSequenceId,
                ofNullable(startRequest.getEnvironmentName()),
                failSafeEmpty(startRequest.getAttributes()));
    }

    @RequestMapping(value = "/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/finish", method = POST)
    public void finishBenchmark(
            @PathVariable("benchmarkName") String benchmarkName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId,
            @RequestBody @Valid FinishRequest finishRequest)
    {
        benchmarkService.finishBenchmarkRun(benchmarkName,
                benchmarkSequenceId,
                finishRequest.getStatus(),
                failSafeEmpty(finishRequest.getMeasurements()),
                failSafeEmpty(finishRequest.getAttributes()));
    }

    @RequestMapping(value = "/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/start", method = POST)
    public void startExecution(
            @PathVariable("benchmarkName") String benchmarkName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId,
            @PathVariable("executionSequenceId") String executionSequenceId,
            @RequestBody @Valid ExecutionStartRequest startRequest)
    {
        benchmarkService.startExecution(benchmarkName,
                benchmarkSequenceId,
                executionSequenceId,
                failSafeEmpty(startRequest.getAttributes()));
    }

    @RequestMapping(value = "/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/finish", method = POST)
    public void finishExecution(
            @PathVariable("benchmarkName") String benchmarkName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId,
            @PathVariable("executionSequenceId") String executionSequenceId,
            @RequestBody @Valid FinishRequest finishRequest)
    {
        benchmarkService.finishExecution(benchmarkName,
                benchmarkSequenceId,
                executionSequenceId,
                finishRequest.getStatus(),
                failSafeEmpty(finishRequest.getMeasurements()),
                failSafeEmpty(finishRequest.getAttributes()));
    }

    @RequestMapping(value = "/v1/benchmark/{benchmarkName}/{benchmarkSequenceId}", method = GET)
    public BenchmarkRun findBenchmark(
            @PathVariable("benchmarkName") String benchmarkName,
            @PathVariable("benchmarkSequenceId") String benchmarkSequenceId)
    {
        return benchmarkService.findBenchmarkRun(benchmarkName, benchmarkSequenceId);
    }

    @RequestMapping(value = "/v1/benchmark/{benchmarkName}", method = GET)
    public Benchmark findBenchmarks(
            @PathVariable("benchmarkName") String benchmarkName)
    {
        return benchmarkService.findBenchmark(benchmarkName);
    }

    @RequestMapping(value = "/v1/benchmark/latest", method = GET)
    public List<BenchmarkRun> findLatestBenchmarkRuns()
    {
        return benchmarkService.findLatest();
    }
}
