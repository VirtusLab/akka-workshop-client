akka-workshop
=============

Workshop on akka remoting, clustering, and supervision

The purpose of this workshop is to try out few basic, but probably most useful Akka mechanisms.
We will build distributed system for decrypting hashed passwords. Input data - encrypted text - will be delivered by the external remote Akka server. Client library, which we will be implementing during this workshop, will be responsible for fetching encrypted passwords, decrypting them, and sending them back to the central server.

Whole workshop is divided into few self-sufficient sections. We will start with most basic prototype, and will try to gradually make it more efficient and less error prone.
Generally you should follow all sections in a given order although two last ones are interchangeable.

#### Akka remoting
During this step we will implement complete communication with remote server.
We login to the central server and fetch encrypted password from it. Then we decrypt that password using provided decryption library and send it back to the central server. For now we skip all optimizations and error handling (just catch potential exceptions to not crash miserably) and focus on proper message passing and basic work-flow. API for communication (messages signatures) will be provided.  
We will implement:
* Login to the remote server (we will obtain unique Id to sign all our requests)
* Sending requests for new encrypted password to the central server
* Receiving encrypted password
* Decrypting given password using decryption library
* Sending decrypted password back to the central server
* Receiving information if password was properly decrypted

This should give us working client library capable of decrypting passwords in cooperation with server. But our library is not very efficient and it's error prone as well. Rate of incorrectly encrypted (or not encrypted at all) passwords is probably quite high. Soon we will work on improving that.

#### Work parallelization
Modern machines usually have more than one core, but our application process data using just one thread. Such a waste! We want to parallelize our work by splitting it between multiple worker actors. We cannot split singe password decryption, but we can ask central server for multiple encrypted passwords and process each in a separate thread/actor. Let's do it!  
We will implement:
* supervisor actor responsible for spawning multiple workers, dispatching work, and communication with central server
* multiple worker actors responsible for password decryption

That should help a lot with the performance. But if you are measuring error rates (do you?) from the server responses you will notice that they jumped up. This is caused by the buggy decryption library, problem which we will try to overcome in the next step.

#### Error handling, supervision
As we already mentioned decryption library can throw exception from time to time. But it's not the end of the story. Internal implementation of that library is based on global shared mutable state, and each exception signalize that the state is broken.  
To decrypt message we have to compose three function calls (i.e. C(B(A(encrypted_message)))). Exception can be thrown by each of those functions, and if that happens it invalidates whole chain of calls (so you need to start with A function again). And because of that global shared mutable state every exception affects all the clients - in our case all the workers. To overcome that problem we need to be sure that once the exception happens we will be able to discard all the broken results and retry failed computations.  
We will implement:
* supervision strategy, so supervisor will be able to restart all workers in case of any error
* retrying all broken computations

#### Terminating long running tasks
To use decryption library we have to do few API calls chained together, i.e. C(B(A(encrypted_message))). Each of those functions is computationally expensive. It could be beneficial to be able to stop the actor at any point of computations, because if we already know that the result will be incorrect we have no interest in continuing.  
We can obtain the desired result by using simple pattern: http://letitcrash.com/post/37854845601/little-pattern-message-based-loop. But be aware - additional messages in the mailbox can break our supervision strategy (if we are just restarting actors mailboxes are not cleared). Luckily fixing this will be simple.  
We will implement:
* pattern which will allow us to stop computations in the middle of the work
* minor tweaks for supervision strategy

#### Clustering
Because decrypting passwords is computationally expensive only so much work can be done on the single machine. But each team has more than one laptop and can use that to its advantage. We can build Akka cluster which will distribute work between many nodes. For the central server it will be visible as a single machine.  
There is no single valid design which would suit our needs, but we will go with one Cluster Singleton actor to coordinate work of our machines. It will ensure that one supervisor was created on every machine and will be responsible for login into central server (we need common identity for our cluster). Then all supervisors can communicate with central server directly (using obtained Id) or through Cluster Singleton actor.  
We will implement:
* Cluster Singleton actor which will spawn all the workers and login to the central server
* changes to messages routing (varies depending on chosen strategy)
