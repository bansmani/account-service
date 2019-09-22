import TransactionStatuService.transactionStatusPoller

object TransactionService {

    fun localTransfer(localTransferDTO: LocalTransferDTO): Boolean {
        val debitInstruction = InstructionDTO(
            localTransferDTO.fromAccNumber,
            localTransferDTO.amount,
            InstructionType.DEBIT,
            localTransferDTO.description
        )
        val debitTrans = createNewTransaction(debitInstruction)
        //blocking  for status



        val creditInstruction = InstructionDTO(
            localTransferDTO.toAccNumber,
            localTransferDTO.amount,
            InstructionType.CREDIT,
            localTransferDTO.description + " " + debitTrans.transactionId
        )
        val creditTrans = createNewTransaction(creditInstruction)

        val creaditStatus = TransactionStatuService.getTransactionStatusBlocking(creditTrans.transactionId)
        if (creaditStatus?.status == TransactionStatus.FAILED || creaditStatus?.status == TransactionStatus.ERROR) {
            //make a custom error
            throw Exception(creaditStatus.errorMessage)
        }
        return true
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
        return transaction;
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
                    throw Exception(debitStatus.errorMessage)
                }
            }
            InstructionType.CREDIT -> {
                DomainEventManager.publishCredit(accountEntry)
            }
        }
        return true
    }
}

fun main() {
    transactionStatusPoller()
}

