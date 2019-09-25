# moneytransfer

####Some technology selection consideration

> Since we wanted to make it a standalone runnable solution without external dependency, we are using embedded database and messaging solution. 
*	Database :  H2 (Distributed Strong consistent database could be used) 
*	Messaging middleware :  AMQ  (RabbitMQ/AMQP, could be the best bet, whereas Kafka could also be used, which provide us steam processing and  analytical capabilities)
*	In Memory Distributed Locking :  Java HashMap  (Redis could be used along with Redlock libraries)  
*	Javalin as Http server (micronaught with reactive streams could be used ) 
 
####Components

> There are 5  microservices and 3 shared library project, all microservice is horizontally scalable and thread safe. 
Gradle multi-module project has been used to manage all services together and achieve better testability with dependency management.
 
1.	Transaction Service : entry point and containing REST controller,
2.	Debit Service  
3.	Credit Service  (Debit and Credit both could be merged together) 
4.	Balance Service (this should also have REST endpoint, but couldn’t do for now, so I am using it as embedded) 
5.	LockManager Service (Distributed locking, to avoid concurrent debit on BalanceCache for a given User)
6.	Message Broker Service : - for Event Bus,  it would not live independent, but a utility for other service    
7.	ORMK ( don’t know why did I made this component, it’s a DRY violation,  it must be replaced by some good little ORM) 
8.	Domain-model (simple pojo’s, should be replaced by Proto’s to achieve network performance) 
 
 
Solution is backed by **TDD** with for unit level cases while using Junit5 and Mockk, some integration tests, concurrency and deadlock tests, has also been added, due to time limitation, I am skipping profiling my solution against GC and memory leaks.  The solution can be surly further refactored multiple time and improved. 
 
To run the test go the root directory and say “gradle test”, it would test all parent project (integration tests ) including all sub modules unit tests (if you see javax.jms.JMSException: Transport disposed, please ignore that, its coming from AMQ, since I am using auto acknowledgement, those cases has not been handled )
 
```
gradle test
```
 