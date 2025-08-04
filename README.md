# Music Distribution Service

Core module of Music Distribution Service 

Implements the following behaviours:
1. artist adds songs to a release;
2. artist proposes a release date;
3. proposed release date has to be agreed by record label (unlabeled artists are out of
   scope);
4. only when release date is agreed and reached, then songs from release are
   distributed for streaming;
5. released songs can be searched by title using Levenshtein distance algorithm;
6. keep track of streamed released songs for the distribution - only stream longer than
   30sec is considered for monetization (assume streams are unique between each other);
7. artist can request a report of streamed songs - both monetized and not;
8. artist can file for payment for all monetized streams since last payment;
9. finally, artist can take out release from distribution meaning that songs cannot be
   streamed anymore.

As an initial prototype of the relative MVP, it implements the above requirements as pure functions, without actual integrations to databases or message queues, 
thus it can be verified only through its test suite.

The implementation is done using the tagless final encoding, in order to prepare leveraging in future releases the interpreter pattern in the codebase, and uses
cats-effect as effect system to maximise composability and use structured concurrency as means to streamlining the coginitive burden of the development

## Compile project

```shell
sbt compile
```

## Run tests

```shell
sbt test
```