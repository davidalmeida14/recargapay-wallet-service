package br.com.recargapay.wallet.application.worker;

import br.com.recargapay.wallet.domain.transaction.event.TransferCreditPendingEvent;
import br.com.recargapay.wallet.domain.wallet.service.TransferService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProcessCreditTransferWorker {

  private final TransferService transferService;

  public ProcessCreditTransferWorker(TransferService transferService) {
    this.transferService = transferService;
  }

  @SqsListener("${aws.sqs.queues.transfer-credit-pending}")
  void process(TransferCreditPendingEvent event) {

    log.info("Processing credit for transfer of transactionId: {}", event.transactionId());

    transferService.processDestinationCredit(event.transactionId());
  }
}
