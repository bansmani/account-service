import TransactionStatuService.getTransactionStatus
import TransactionStatuService.transactionStatusPoller

object TransactionService {

    fun start() {
        transactionStatusPoller()
    }

    fun localTransfer(localTransferDTO: LocalTransferDTO): Transaction? {
        val debitInstruction = InstructionDTO(
            localTransferDTO.fromAccNumber,
            localTransferDTO.amount,
            InstructionType.DEBIT,
            localTransferDTO.description
        )

        //blocking  for status
        val debitTrans = createNewTransaction(debitInstruction)


        val creditInstruction = InstructionDTO(
            localTransferDTO.toAccNumber,
            localTransferDTO.amount,
            InstructionType.CREDIT,
            localTransferDTO.description + " " + debitTrans.transactionId
        )
        //no need to block for credit transaction
        createNewTransaction(creditInstruction)
        return getTransactionStatus(debitTrans.transactionId)
    }


    fun createNewTransaction(instruction: InstructionDTO): Transaction {
        val transaction = Transaction(
            instruction.instructionType,
            instruction.accNumber, instruction.amount, instruction.description
        )
        //throw exception, let caller handle it
        CrudRepsitory.save(transaction, true)

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
                DomainEventManager.publishDebit(accountEntry)

                val debitStatus = TransactionStatuService.getTransactionStatusBlocking(transaction.transactionId)
                if (debitStatus?.status == TransactionStatus.FAILED || debitStatus?.status == TransactionStatus.ERROR) {
                    //make a custom error
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



