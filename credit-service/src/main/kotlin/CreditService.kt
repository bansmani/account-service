object CreditService {

    fun start(){
        DomainEventManager.startCreditMessageLister {
            CrudRepsitory.save(it)
            BalanceService.updateBalance(it)
        }
    }

}

fun main() {
    CreditService.start()
}