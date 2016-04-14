akka-workshop
=============

Workshop on akka remoting, clustering, and supervision

The purpose of this workshop is to try out few basic, but probably most useful Akka mechanisms.
We will build distributed system for decrypting hashed passwords. Input data - encrypted text - will be delivered by the external remote Akka server. Client library, which we will be implementing during this workshop, will be responsible for fetching encrypted passwords, decrypting them, and sending them back to the central server.


Whole workshop is divided into few self-sufficient sections. We will start with most basic prototype, and will try to gradually make it more efficient and less error prone.
Generally you should follow all sections in a given order although last ones are interchangeable.

Introduction can be found [here](http://virtuslab.github.io/akka-workshop/#/intro).

In case of big trouble connecting to server/leader board you can use [zipped instance](/local-server/workshop-server-1.0.4.zip) to try your code locally. To do so just unpack zip, run bin/workshop-server and use localhost as host-name. 

#### Connect to remote actor: 
[Presentation](http://virtuslab.github.io/akka-workshop/#/remote)

At the beginning you need to connect to remote actor by creating ActorSelection and send Register message with your name.

This might require changing application configuration, dependecies and code inself. 
 
More can be found in [documentation](http://doc.akka.io/docs/akka/snapshot/scala/remoting.html).

Once correctly registered your name should appear at our leader board (at host-name:9000/?mode=remote)

![](leaderboard.png)

#### Register and process passwords
[Presentation](http://virtuslab.github.io/akka-workshop/#/register)

Once registered, use acquired token request and check decrypted passwords. **Beware!** Decryper sometimes fails so be sure that you are able to overcome that.

Your client should decrypt passwords in endless loop. Requesting millions of passwords at once is cheating!

#### Work parallelization
[Presentation](http://virtuslab.github.io/akka-workshop/#/parallel)

Now we care more about speed of processing. To measure this we need to use different mode of leader board: host-name:9000/?mode=parallel

If we can't speed up decrypting process we can do it parallel. Best place to start is [akka routing](http://doc.akka.io/docs/akka/snapshot/scala/routing.html).

HINT: Before implementing your own router please consider using created routers or even pools.

Beware: Decrypter has limitation: It can effectively process only 4 computation in one time. Rest will just wait for free slot.

For now we care for speed!

#### Error handling, supervision
[Presentation](http://virtuslab.github.io/akka-workshop/#/errors)

This is time to focus also on correctness of your decryption (dealing with Decryptor problems to be more precise).

It is time to finally use full leader board: <host-name>:9000

I assume that your are able to recover from exceptions now but did you measure error rates?

Once state in Decryptor gets corrupted all existing instances produce bad results. 
What is even worse all work that was already in progress when state gets corrupted will also yield bad results.
It means that we need to throw away some output from corrupted Decryptor instances. 

Of course we don't want to give up our prallelizaion here.

Hints: consider using supervision strategy and maybe restart some actors. Your router/pool configuration might need to be changed here.

More about [supervision](http://doc.akka.io/docs/akka/2.4.2/general/supervision.html) and supervision in [routing/pools](http://doc.akka.io/docs/akka/snapshot/scala/routing.html#Supervision).
 
100% of correctness is realy hard to achieve with parallelization. Everything over 90% is fine :)

#### Terminating long running tasks
[Presentation](http://virtuslab.github.io/akka-workshop/#/long-tasks)

With supervision applied our correctness rise but we become slower. We should try to fix that.

To use decryption library we have to do few API calls chained together, i.e. C(B(A(encrypted_message))). Each of those functions is computationally expensive. It could be beneficial to be able to stop the actor at any point of computations, because if we already know that the result will be incorrect we have no interest in continuing.  
We can obtain the desired result by using simple [pattern](http://letitcrash.com/post/37854845601/little-pattern-message-based-loop). But be aware - additional messages in the mailbox can break our supervision strategy (if we are just restarting actors mailboxes are not cleared). Luckily fixing this will be simple.  


#### Clustering
[Presentation](http://virtuslab.github.io/akka-workshop/#/clustering)

Because decrypting passwords is computationally expensive only so much work can be done on the single machine. But each team has more than one laptop and can use that to its advantage. We can build Akka cluster which will distribute work between many nodes. For the central server it will be visible as a single machine.  
There is no single valid design which would suit our needs, but we will go with one Cluster Singleton actor to coordinate work of our machines. It will ensure that one supervisor was created on every machine and will be responsible for login into central server (we need common identity for our cluster). Then all supervisors can communicate with central server directly (using obtained Id) or through Cluster Singleton actor.  
We will implement:
* Cluster Singleton actor which will spawn all the workers and login to the central server
* changes to messages routing (varies depending on chosen strategy)
