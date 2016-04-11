/*
 * Copyright 2013-2016, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.service;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableList;
import com.facebook.presto.jdbc.internal.guava.collect.ImmutableMap;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.google.common.collect.Lists.asList;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

@Component
public class BenchmarkServiceClient
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkServiceClient.class);

    @Value("${benchmark-service.url}")
    private String serviceUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Retryable(value = RestClientException.class, backoff = @Backoff(1000))
    public List<String> generateUniqueBenchmarkNames(List<GenerateUniqueNamesRequestItem> generateUniqueNamesRequestItems)
    {
        Map<String, String> requestParams = ImmutableMap.of("serviceUrl", serviceUrl);
        String[] uniqueNames = postForObject("{serviceUrl}/v1/benchmark/generate-unique-names", generateUniqueNamesRequestItems, String[].class, requestParams);
        return ImmutableList.copyOf(uniqueNames);
    }

    @Retryable(value = RestClientException.class, backoff = @Backoff(1000))
    public List<Duration> getBenchmarkSuccessfulExecutionAges(List<String> benchmarkUniqueNames)
    {
        Map<String, String> requestParams = ImmutableMap.of("serviceUrl", serviceUrl);
        Duration[] ages = postForObject("{serviceUrl}/v1/benchmark/get-successful-execution-ages", benchmarkUniqueNames, Duration[].class, requestParams);
        return ImmutableList.copyOf(ages);
    }

    @Retryable(value = RestClientException.class, backoff = @Backoff(1000))
    public String startBenchmark(String uniqueBenchmarkName, String benchmarkSequenceId, BenchmarkStartRequest request)
    {
        Map<String, String> requestParams = requestParams(uniqueBenchmarkName, benchmarkSequenceId);

        return postForObject("{serviceUrl}/v1/benchmark/{uniqueBenchmarkName}/{benchmarkSequenceId}/start", request, requestParams);
    }

    @Retryable(value = RestClientException.class, backoff = @Backoff(1000))
    public void finishBenchmark(String uniqueBenchmarkName, String benchmarkSequenceId, FinishRequest request)
    {
        Map<String, String> requestParams = requestParams(uniqueBenchmarkName, benchmarkSequenceId);

        postForObject("{serviceUrl}/v1/benchmark/{uniqueBenchmarkName}/{benchmarkSequenceId}/finish", request, requestParams);
    }

    @Retryable(value = RestClientException.class, backoff = @Backoff(1000))
    public void startExecution(String uniqueBenchmarkName, String benchmarkSequenceId, String executionSequenceId, ExecutionStartRequest request)
    {
        Map<String, String> requestParams = requestParams(uniqueBenchmarkName, benchmarkSequenceId);
        requestParams.put("executionSequenceId", executionSequenceId);

        postForObject("{serviceUrl}/v1/benchmark/{uniqueBenchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/start", request, requestParams);
    }

    @Retryable(value = RestClientException.class, backoff = @Backoff(1000))
    public void finishExecution(String uniqueBenchmarkName, String benchmarkSequenceId, String executionSequenceId, FinishRequest request)
    {
        Map<String, String> requestParams = requestParams(uniqueBenchmarkName, benchmarkSequenceId);
        requestParams.put("executionSequenceId", executionSequenceId);

        postForObject("{serviceUrl}/v1/benchmark/{uniqueBenchmarkName}/{benchmarkSequenceId}/execution/{executionSequenceId}/finish", request, requestParams);
    }

    private Map<String, String> requestParams(String uniqueBenchmarkName, String benchmarkSequenceId)
    {
        Map<String, String> params = newHashMap();
        params.put("serviceUrl", serviceUrl);
        params.put("uniqueBenchmarkName", uniqueBenchmarkName);
        params.put("benchmarkSequenceId", benchmarkSequenceId);
        return params;
    }

    private String postForObject(String url, Object request, Map<String, String> requestParams)
    {
        return postForObject(url, request, String.class, requestParams);
    }

    private <T> T postForObject(String url, Object request, Class<T> clazz, Map<String, String> requestParams)
    {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Post object to benchmark service on URL: {}, with request: {}",
                    new UriTemplate(url).expand(requestParams),
                    request);
        }

        return restTemplate.postForObject(url, request, clazz, requestParams);
    }

    public static class GenerateUniqueNamesRequestItem
    {
        private String name;
        private Map<String, String> variables;

        private GenerateUniqueNamesRequestItem(String name, Map<String, String> variables)
        {
            this.name = name;
            this.variables = variables;
        }

        public String getName()
        {
            return name;
        }

        public Map<String, String> getVariables()
        {
            return variables;
        }

        public static GenerateUniqueNamesRequestItem generateUniqueNamesRequestItem(String name, Map<String, String> variables)
        {
            return new GenerateUniqueNamesRequestItem(name, variables);
        }

        @Override
        public String toString()
        {
            return MoreObjects.toStringHelper(this)
                    .add("name", name)
                    .add("variables", variables)
                    .toString();
        }
    }

    @SuppressWarnings("unused")
    @JsonAutoDetect(fieldVisibility = ANY)
    public static abstract class AttributeRequest
    {
        protected Map<String, String> attributes = newHashMap();

        public static abstract class AttributeRequestBuilder<T extends AttributeRequest>
        {
            protected final T request;

            public AttributeRequestBuilder(T request)
            {
                this.request = request;
            }

            public AttributeRequestBuilder<T> addAttribute(String name, String value)
            {
                request.attributes.put(name, value);
                return this;
            }

            public T build()
            {
                return request;
            }
        }
    }

    @SuppressWarnings("unused")
    public static class BenchmarkStartRequest
            extends AttributeRequest
    {
        private String name;
        private String environmentName;
        private Map<String, String> variables = newHashMap();

        private BenchmarkStartRequest()
        {
        }

        public static class BenchmarkStartRequestBuilder
                extends AttributeRequestBuilder<BenchmarkStartRequest>
        {
            public BenchmarkStartRequestBuilder(String name)
            {
                super(new BenchmarkStartRequest());
                request.name = name;
            }

            public BenchmarkStartRequestBuilder environmentName(String environmentName)
            {
                request.environmentName = environmentName;
                return this;
            }

            public BenchmarkStartRequestBuilder addVariable(String name, String value)
            {
                request.variables.put(name, value);
                return this;
            }
        }

        @Override
        public String toString()
        {
            return MoreObjects.toStringHelper(this)
                    .add("name", name)
                    .add("environmentName", environmentName)
                    .add("variables", variables)
                    .add("attributes", attributes)
                    .toString();
        }
    }

    public static class ExecutionStartRequest
            extends AttributeRequest
    {
        private ExecutionStartRequest()
        {
        }

        public static class ExecutionStartRequestBuilder
                extends AttributeRequestBuilder<ExecutionStartRequest>
        {

            public ExecutionStartRequestBuilder()
            {
                super(new ExecutionStartRequest());
            }
        }

        @Override
        public String toString()
        {
            return MoreObjects.toStringHelper(this)
                    .add("attributes", attributes)
                    .toString();
        }
    }

    public static class FinishRequest
            extends AttributeRequest
    {
        public enum Status
        {
            STARTED, ENDED, FAILED
        }

        private Status status;
        private List<Measurement> measurements = newArrayList();

        private FinishRequest()
        {
        }

        public static class FinishRequestBuilder
                extends AttributeRequestBuilder<FinishRequest>
        {

            public FinishRequestBuilder()
            {
                super(new FinishRequest());
            }

            public FinishRequestBuilder withStatus(Status status)
            {
                request.status = status;
                return this;
            }

            public FinishRequestBuilder addMeasurement(Measurement measurement)
            {
                request.measurements.add(measurement);
                return this;
            }

            public FinishRequestBuilder addMeasurements(Collection<Measurement> measurements)
            {
                request.measurements.addAll(measurements);
                return this;
            }
        }

        @Override
        public String toString()
        {
            return MoreObjects.toStringHelper(this)
                    .add("measurements", measurements)
                    .add("status", status)
                    .add("attributes", attributes)
                    .toString();
        }
    }
}