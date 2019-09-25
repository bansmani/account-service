import TransactionStatuService.getTransactionStatus
import TransactionStatuService.transactionStatusPoller
import java.util.concurrent.CompletableFuture

object TransactionService {

    fun start() {
        transactionStatusPoller()
    }

    fun localTransfer(localTransferInstructionDTO: LocalTransferInstructionDTO): CompletableFuture<Transaction>? {
        val debitInstruction = localTransferInstructionDTO.toDebitInstructionDTO()
        val creditInstruction = localTransferInstructionDTO.toCreditInstructionDTO()

        return CompletableFuture.supplyAsync {
            createNewTransaction(debitInstruction)
        }.thenApply { debitTransaction ->
            createNewTransaction(creditInstruction)
            debitTransaction
        }
    }


    fun createNewTransaction(instruction: InstructionDTO): Transaction {
        val transaction = Transaction(
            instruction.instructionType,
            instruction.accNumber, instruction.amount, instruction.description
        )
        //throw exception, let caller handle it
        retry { CrudRepsitory.save(transaction, true) }

        //update transaction status
        publishPayload(transaction)
        return transaction
    }

    private fun publishPayload(transaction: Transaction): Boolean {
        val accountEntry = AccountEntry(
            transaction.accNumber,
            transaction.amount,
            transaction.initiateTime,
            transaction.transactionId,
            transaction.instructionType,
            transaction.description
        )
        when (transaction.instructionType) {
            InstructionType.DEBIT -> {
                //This could be converted to a request response topic
                DomainEventManager.publishDebit(accountEntry)
                val debitStatus = TransactionStatuService.getTransactionStatusBlocking(transaction.transactionId)
                if (debitStatus?.status == TransactionStatus.FAILED || debitStatus?.status == TransactionStatus.ERROR) {
                    throw InsufficientFundsException(debitStatus.errorMessage)
                }
            }
            InstructionType.CREDIT -> {
                DomainEventManager.publishCredit(accountEntry)
            }
        }
        return true
    }


}

@Suppress("UNUSED_PARAMETER")
class InsufficientFundsException(errorMessage: String) : Exception()



