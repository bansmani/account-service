object CreditService {

    fun start(){
        DomainEventManager.startCreditMessageLister {
            CrudRepsitory.save(it)
            LockMangerService.acquireBalanceUpdateLock(it.accNumber.toString())
            
        }
    }

}

fun main() {
    CreditService.start()
}