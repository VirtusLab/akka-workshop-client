Asynchronous programming in late 2018 - Cats-effect
=============

Workshop on asynchronous programming using [cats.effect.IO](https://github.com/typelevel/cats-effect).

It will cover parallelism, error handling, synchronization and shared state - everything in purely functional manner!

We will build system for decrypting hashed passwords. Input data - encrypted text - will be delivered by REST API.
Client library, which we will be implementing during this workshop, will be responsible for fetching encrypted passwords, decrypting them, and sending them back to the central server.


Whole workshop is divided into few self-sufficient sections. We will start with most basic prototype, and will try to gradually make it more efficient and less error prone.
Generally you should follow all sections in a given order although last ones are interchangeable.

Introduction can be found [here](https://slides.com/avasil/asynchronous-programming-in-late-2018-cats-effect/fullscreen#/).
They contain few `Cats-Effect` examples that might prove helpful when trying to find a solution.

#### Connect and register 
[Presentation](https://slides.com/avasil/asynchronous-programming-in-late-2018-cats-effect/fullscreen#/5)

All you have to do is change host name to the one provided at the workshop, HTTP skeleton is already provided.

Once correctly registered your name should appear at our leader board (at host-name:9000/?mode=remote)

![](leaderboard.png)

#### Process passwords
[Presentation](https://slides.com/avasil/asynchronous-programming-in-late-2018-cats-effect/fullscreen#/6)

Once registered, use acquired token request and check decrypted passwords. **Beware!** Decrypter sometimes fails so be sure that you are able to overcome that.

Your client should decrypt passwords in endless loop. Requesting millions of passwords at once is cheating! Also everything
that is not purely functional is cheating too. So make sure any effects are contained within `IO` and then composed!

#### Work parallelization
[Presentation](https://slides.com/avasil/asynchronous-programming-in-late-2018-cats-effect/fullscreen#/7)

Now we care more about speed of processing. To measure this we need to use different mode of leader board: host-name:9000/?mode=parallel

If we can't speed up decrypting process we can do it parallel. 
Best place to start is [Cats-effect documentation](https://typelevel.org/cats-effect/datatypes/io.html#parallelism).

Beware: `Decrypter` has limitation: It can effectively process only 4 computation in one time. 
Rest will just wait for free slot but it might make sense to have few more already waiting. Experiment and see what works
best for your solution.

For now we care for speed!

#### Error handling
[Presentation](https://slides.com/avasil/asynchronous-programming-in-late-2018-cats-effect/fullscreen#/8)

This is time to focus also on correctness of your decryption (dealing with `Decrypter` problems to be more precise).

It is time to finally use full leader board: <host-name>:9000

I assume that your are able to recover from exceptions now but did you measure error rates?

Once state in `Decrypter` gets corrupted all existing instances produce bad results. 
What is even worse all work that was already in progress when state gets corrupted will also yield bad results.
It means that we need to throw away some output from corrupted `Decrypter` instances. 

Once one of the `IO` fails we should stop the other ones. 
There is a built in [cancelation](https://typelevel.org/cats-effect/datatypes/io.html#concurrency-and-cancellation) 
but it's for use cases where we no longer care about canceled `IO` - we just want it to stop in the background. That 
means it's not straightforward to know exactly when it has been canceled. 
Can you tell why this would be a problem for our use case? 

Fortunately we can signal it manually sharing something like 
[`Ref[IO, Boolean]`](https://typelevel.org/cats-effect/concurrency/ref.html) or 
[`Deferred[IO, Unit]`](https://typelevel.org/cats-effect/concurrency/deferred.html)
which could serve as a signal telling us whether we should keep going. This could give us precise control we want!
 
100% of correctness is really hard to achieve with parallelization. Everything over 90% is fine :)

#### Squeezing more performance
[Presentation](https://slides.com/avasil/asynchronous-programming-in-late-2018-cats-effect/fullscreen#/9)

With error handling our correctness rise but we become slower. We should try to fix that.
Instead of discarding passwords that we failed to decrypt we could save their previous step 
to try again with fresh `Decrypter`. 

We need to share some kind of `List` or `Queue` to accomplish that. You know by now how to share state in 
purely functional manner so let's use it to your advantage.
Also, don't be afraid to modify part of the code responsible for requesting passwords! 
Sometimes HTTP call won't be necessary.
