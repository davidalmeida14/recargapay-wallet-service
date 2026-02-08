package br.com.recargapay.wallet.domain.transaction.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import software.amazon.awssdk.services.sqs.SqsClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferCreditPendingEventListener {

  private final SqsClient sqsClient;
  private final ObjectMapper objectMapper;

  @Value("${aws.sqs.queues.transfer-credit-pending}")
  private String queueName;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
  public void handleTransferCreditPendingEvent(TransferCreditPendingEvent event) {
    log.info("Processing TransferCreditPendingEvent for transactionId: {}", event.transactionId());
    try {
      var messageBody = objectMapper.writeValueAsString(event);

      var queueUrl = resolveQueueUrl();

      var sendMessageResponse =
          sqsClient.sendMessage(
              builder -> builder.queueUrl(queueUrl).messageBody(messageBody).build());

      log.info(
          "Sent TransferCreditPendingEvent to SQS for transactionId: {}, messageId: {}",
          event.transactionId(),
          sendMessageResponse.messageId());
    } catch (Exception e) {
      log.error(
          "Error processing TransferCreditPendingEvent for transactionId: {}",
          event.transactionId(),
          e);
      return;
    }
  }

  private String resolveQueueUrl() {
    return sqsClient.getQueueUrl(builder -> builder.queueName(queueName).build()).queueUrl();
  }
}
