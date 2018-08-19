Asynchronous programming in late 2018 - Scalaz ZIO
=============

Workshop on asynchronous programming using [Scalaz ZIO](https://scalaz.github.io/scalaz-zio).

It will cover parallelism, error handling, synchronization and shared state - everything in purely functional manner!

We will build system for decrypting hashed passwords. Input data - encrypted text - will be delivered by REST API.
Client library, which we will be implementing during this workshop, will be responsible for fetching encrypted passwords, decrypting them, and sending them back to the central server.


Whole workshop is divided into few self-sufficient sections. We will start with most basic prototype, and will try to gradually make it more efficient and less error prone.
Generally you should follow all sections in a given order although last ones are interchangeable.

In case of big trouble connecting to server/leader board you can use [zipped instance](/local-server/workshop-server-1.0.4.zip) to try your code locally. To do so just unpack zip, run bin/workshop-server and use localhost as host-name. 

#### Connect and register 

// TODO: Add instructions for connecting to REST API

Once correctly registered your name should appear at our leader board (at host-name:9000/?mode=remote)

![](leaderboard.png)

#### Process passwords

Once registered, use acquired token request and check decrypted passwords. **Beware!** Decrypter sometimes fails so be sure that you are able to overcome that.

Your client should decrypt passwords in endless loop. Requesting millions of passwords at once is cheating! Also everything
that is not purely functional is cheating too. So make sure any effects are contained within `IO` and then composed!

#### Work parallelization

Now we care more about speed of processing. To measure this we need to use different mode of leader board: host-name:9000/?mode=parallel

If we can't speed up decrypting process we can do it in parallel. 
Best place to start is [ZIO documentation on fibers and parallelism](https://scalaz.github.io/scalaz-zio/usage/fibers.html). 
There are several ways to introduce controlled parallelism to our application: `Semaphore` and `.fork`, `IO.parTraverse` or maybe
`Queue` and `.fork`. Each choice has a distinct impact on architecture of your solution.

Beware: `Decrypter` has limitation: It can effectively process only 4 computations in one time. 
Rest will just wait for free slot but it might make sense to have few more already waiting. Experiment and see what works
best for your solution.

For now we care about speed only!

#### Error handling

This is time to focus also on correctness of your decryption (dealing with `Decrypter` problems to be more precise).

It is time to finally use full leader board: <host-name>:9000

I assume that your are able to recover from exceptions now but did you measure error rates?

Once state in `Decrypter` gets corrupted all existing instances produce bad results. 
What is even worse all work that was already in progress when state gets corrupted will also yield bad results.
It means that we need to throw away some output from corrupted `Decrypter` instances. 

Once one of the `IO` fails we should stop the other ones. We can pass information about failure in parallel thread 
manually using a `Ref` construct, which provides functional atomic reference. On the other hand Scalaz ZIO features
an interruption model which allows for stopping of running processes that are no longer needed. Check out the docs about
[IO racing](https://scalaz.github.io/scalaz-zio/usage/fibers.html#racing) for a hint. There's a purely functional 
`Promise` available in ZIO. Can you leverage this construct?  
 
100% of correctness is really hard to achieve with parallelization. Everything over 90% is fine :)

#### Squeezing more performance

With error handling our correctness rise but we become slower. We should try to fix that.
Instead of discarding passwords that we failed to decrypt we could save them to try again with fresh `Decrypter`. 

We need some kind of `List` or `Queue` to accomplish that and don't be afraid to modify part of the code responsible for
requesting passwords! Sometimes HTTP call won't be necessary.