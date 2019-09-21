object TransactionService {


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
            transaction.id,
            transaction.instructionType,
            transaction.description
        )
        when (transaction.instructionType) {
            InstructionType.DEBIT -> {
                DomainEventManager.publishDebit(accountEntry)
            }
            InstructionType.CREDIT -> {
                DomainEventManager.publishCredit(accountEntry)
            }
        }
        return true
    }
}

