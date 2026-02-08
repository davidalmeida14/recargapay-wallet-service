package br.com.recargapay.wallet.infrastructure.configuration;

import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class SqsConfiguration {

  @Value("${aws.sqs.endpoint.configuration}")
  private String SQS_ENDPOINT_CONFIGURATION;

  @Bean
  @Primary
  public SqsAsyncClient getSqsClientAsync(
      AwsCredentialsProvider credentialsProvider, AwsRegionProvider regionProvider)
      throws URISyntaxException {
    final var region = regionProvider.getRegion();
    final var sqsEndpoint = new URI(SQS_ENDPOINT_CONFIGURATION);
    final var retryPolicy = createRetryPolicy();
    final var clientConfiguration =
        ClientOverrideConfiguration.builder().retryPolicy(retryPolicy).build();
    return SqsAsyncClient.builder()
        .endpointOverride(sqsEndpoint)
        .region(region)
        .credentialsProvider(credentialsProvider)
        .overrideConfiguration(clientConfiguration)
        .build();
  }

  @Bean
  @Primary
  public SqsClient getSqsClient(
      AwsCredentialsProvider credentialsProvider, AwsRegionProvider regionProvider)
      throws URISyntaxException {
    final var sqsEndpoint = new URI(SQS_ENDPOINT_CONFIGURATION);
    final var clientConfiguration =
        ClientOverrideConfiguration.builder().retryPolicy(createRetryPolicy()).build();
    return SqsClient.builder()
        .endpointOverride(sqsEndpoint)
        .region(regionProvider.getRegion())
        .credentialsProvider(credentialsProvider)
        .overrideConfiguration(clientConfiguration)
        .build();
  }

  private RetryPolicy createRetryPolicy() {
    return RetryPolicy.builder()
        .numRetries(10)
        .retryCapacityCondition(RetryCondition.defaultRetryCondition())
        .backoffStrategy(BackoffStrategy.defaultStrategy())
        .build();
  }
}
