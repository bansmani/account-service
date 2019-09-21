object CreditService {

    fun start(){
        DomainEventManager.startCreditMessageLister {
            CrudRepsitory.save(it)

        }
    }

}

fun main() {
    CreditService.start()
}