# Parallel Consumer

Kafka를 사용하면서 메시지 동시 처리량을 늘릴 수 있는 가장 쉬운 방법 중 하나는 파티션을 증가시키는 것이다.
다만 파티션 수는 한번 늘어나면 줄일 수 없기에 신중해야 한다.

## 그냥 파티션을 늘리면 안되는 이유

카프카 컨슈머의 병렬 처리 단위는 파티션이다. 보통 파티션별 단일 컨슈머 스레드가 할당되는 구조이기 때문에 동시 처리량을 늘리기 위해서는 파티션 수 또한 늘려야 한다.

### 파티션 증가 시 단점

**브로커 파일 시스템 리소스 사용량 증가**

카프카 브로커는 파티션별로 데이터를 저장하는데 이때 단순 데이터 정보(.log)뿐만 아니라 메타 정보(.index, .timeindex, .snapshot)도 함께 저장한다.
따라서 파티션이 많아질수록 파일에 대한 파일 오픈 비용, 디스크 사용량 비용 등이 추가로 필요해진다.

**장애에 더 취약한 구조**

단일 브로커에 파티션 리더가 더 많이 배치되기 때문에 브로커 노드 장애 혹은 재시작으로 영향받는 범위가 더 넓다.

**복제 비용 증가**

파티션 단위로 설정된 replicas 수만큼 복제가 이루어지기 때문에 복제로 인한 디스크 사용량, latency가 증가한다.
파티션 수가 적은 환경에서는 어쩌면 큰 문제가 아닐 수 있지만, 과도하게 늘어나 있는 환경에서는 문제가 될 수 있다.

## Parallel Consumer?

Parallel Consumer는 단일 파티션에 여러 컨슈머 스레드를 사용하여 파티션을 늘리지 않고 동시 처리량을 증가시키기 위해 만들어진 라이브러리이다.

### 파티션 단위 vs. 메시지 단위

Parallel Consumer를 사용하면 병렬 처리 단위를 파티션이 아닌 개별 Kafka 메시지 또는 유사한 단위로 지정이 가능하다.

<img width="754" alt="image" src="https://github.com/yoon-youngjin/kafka-practice/assets/83503188/a909aaff-e477-4209-b11a-a28e67e5db4b">

첫번째 그림은 파티션 단위로 병렬성을 달성해서 3개 메시지를 병렬로 처리하는 것을 볼 수 있다. 두번째 그림은 Parallel Consumer를 사용해서 파티션이 한 개임에도 불구하고 복수의 스레드를 사용하여 첫번째 그림과 동일하게 3개 메시지를 동시에 처리할 수 있다.

### 메시지 단위 병렬성이 가능한 이유

기존 카프카에서는 파티션별로 마지막으로 처리한 오프셋을 관리하고 있고 브로커의 오프셋 정보는 컨슈머가 메시지를 처리한 후 커밋을 하면서 갱신된다.
일반적으로 한 번에 한 개의 메시지를 처리하며 auto 커밋 방식을 많이 사용한다.

<img width="789" alt="image" src="https://github.com/yoon-youngjin/kafka-practice/assets/83503188/3ef6ff76-4e21-4e81-9ab0-0ed5ce9cf70b">

한 번에 한 개씩 처리하지 않고 여러 개의 메시지를 처리한 후 마지막 오프셋을 커밋할 수도 있다. 이때 커밋은 수동으로 직접 수행해야 한다.

<img width="808" alt="image" src="https://github.com/yoon-youngjin/kafka-practice/assets/83503188/ca2f602d-1ed5-4428-baba-64a913d05083">

여기서 메시지 처리는 실제 Kafka Consumer가 하는 것이 아니기 때문에 사용자가 이를 병렬로 수행하면 성능이 더 올라갈 수 있다.

<img width="824" alt="image" src="https://github.com/yoon-youngjin/kafka-practice/assets/83503188/c108950a-e9f5-4ac3-b2a6-1da6f14ed506">

Parallel Consumer는 여기서 더 나아가서 오프셋 갱신을 비동기로 수행한다. 처리 결과를 임시로 저장해두고 주기적으로 오프셋을 커밋한다.
이렇게 하면 오프셋 커밋으로 인한 병목 없이 연이어서 처리할 수 있다. 아래의 그림은 11번 오프셋에 해당하는 메시지 처리 후 11번 오프셋을 저장하고 있다가 메시지 처리와 비동기로 커밋하는 과정을 보여준다. 커밋을 할 때 12, 13, 14번 오프셋에 해당하는 메시지를 동시에 처리하고 있는 것을 볼 수 있다.

<img width="795" alt="image" src="https://github.com/yoon-youngjin/kafka-practice/assets/83503188/2b6cdc7b-3a71-4dde-8210-ea6bc9ad636f">

메시지 처리를 병렬로 수행하면 어떤 오프셋을 처리해야 할지 모호할 수 있다. 예를 들어 한 파티션에서 12 ~ 14번 오프셋에 해당하는 3개의 메시지를 가져갔지만 병렬 처리로 인해 13번 오프셋을 처리하기 전에 14번 오프셋을 처리할 수 있다.
Parallel Consumer는 누적하여 이전 오프셋들에 대한 처리를 완료한 가장 마지막 오프셋을 커밋한다. 
아래 그림은 12번 오프셋까지는 완료되었고, 14번 오프셋을 처리했지만 13번 오프셋을 아직 미처리 했기에 12번 오프셋을 커밋한다.

<img width="803" alt="image" src="https://github.com/yoon-youngjin/kafka-practice/assets/83503188/9649e7e6-ab72-45bd-8b52-bc55f805c895">

만약 13번 오프셋도 처리한 상황이라면 누적하여 14번 오프셋까지 처리 완료 했기에 14번 오프셋을 커밋할 것이다.

<img width="806" alt="image" src="https://github.com/yoon-youngjin/kafka-practice/assets/83503188/b174080c-4068-4b59-80ae-bb29ccaa5b0f">

## Parallel Consumer의 순서 보장 방식

메시지 병렬 처리 및 비동기 오프셋 관리를 통해 성능을 높일 수 있다, 하지만 메시지 간의 순서가 중요한 경우가 있다.
예를 들어 상품 주문에 대한 이벤트를 처리하는 경우 주문 요청 이벤트를 처리하기 전에 취소 요청 이벤트를 처리하면 문제가 될 수 있다.
Parallel Consumer는 이를 위해 Partition, Key, Unordered 세 가지의 순서 보장 방식을 제공한다.

> Partition, Key, Unordered 순으로 순서 보장 관련 제약이 느슨해지며 성능이 향상된다.

### Partition

<img width="769" alt="image" src="https://github.com/yoon-youngjin/kafka-practice/assets/83503188/e57692da-846e-4159-9855-f482d489cb97">

Partition 방식은 말 그대로 카프카 파티션 단위로 순서 보장을 하는 것으로 원래 방식과 큰 차이는 없다.
이 방법은 서버 한 대로 여러 카프카 컨슈머를 손쉽게 띄울 수 있어서 적은 리소스로 처리할 수 있다는 점 외에는 큰 장점은 없다.

### Key

<img width="704" alt="image" src="https://github.com/yoon-youngjin/kafka-practice/assets/83503188/522b2e1f-482b-4e75-81eb-db0f26190912">

카프카 메시지에는 어떤 파티션으로 들어가야 한다는 힌트를 제공하는 Key가 있다.
Parallel Consumer는 Key 단위의 순서 보장 방식이 있으며 이는 동일 Key 기준으로 메시지를 순차적으로 처리한다. 
앞선 Partition 방식에서는 파티션 단위로만 병렬 처리가 가능한 반면에 Key 방식의 경우 동일 파티션 내에도 Key가 다르면 메시지가 병렬로 처리될 수 있다. 

### Unordered 

<img width="727" alt="image" src="https://github.com/yoon-youngjin/kafka-practice/assets/83503188/61ebfe5a-8c81-4424-a5ff-96305917cb74">

Unordered 방식은 순서를 아예 보장하지 않는 방식이며 앞서 들어온 메시지의 완료 결과를 기다리지 않는다. 즉, 메시지 단위로 병렬 처리하는 방식이다.

## Parallel Consumer의 내부 구조

### 아키텍처 

Parallel Consumer에는 Broker Poller Thread와 Controller Thread라는 2개의 중요한 스레드와 실제 사용자 코드를 처리하는 Worker Thread Pool, 그리고 오프셋 저장소인 Work State Manager가 있다.

<img width="613" alt="image" src="https://github.com/yoon-youngjin/kafka-practice/assets/83503188/8229fd43-3452-4e75-a8b8-0c12a8f5a6c8">

- Broker Poller Thread는 실제 카프카 브로커와 통신하는 스레드로, 메시지를 가져와서 Mailbox에 저장한다.
- Mailbox는 Broker Poller Thread가 Controller Thread에게 polling한 카프카 메시지를 전달하기 위한 매개체이다.
- Controller Thread는 실제 메인 로직으로, Mailbox에서 메시지를 가져와서 Worker Thread에 전달하는 작업 및 메시지 커밋을 담당한다.
- Worker Thread Pool은 실제 사용자가 등록한 작업을 하는 스레드로, Controller Thread가 전달한 메시지를 처리한다.
- Worker State Manager는 처리한 오프셋 및 순서 보장을 고려하여 다음에 처리될 메시지를 관리한다.

**ParallelEoSStreamProcessor#poll**

```java
@Override
public void poll(Consumer<PollContext<K, V>> usersVoidConsumptionFunction) {
        Function<PollContextInternal<K, V>, List<Object>> wrappedUserFunc = (context) -> {
            log.trace("asyncPoll - Consumed a consumerRecord ({}), executing void function...", context);

            carefullyRun(usersVoidConsumptionFunction, context.getPollContext());

            log.trace("asyncPoll - user function finished ok.");
            return UniLists.of(); // user function returns no produce records, so we satisfy our api
        };
        Consumer<Object> voidCallBack = ignore -> log.trace("Void callback applied.");
        supervisorLoop(wrappedUserFunc, voidCallBack);
}
```

**AbstractParallelEoSStreamProcessor#supervisorLoop**

```java
    protected <R> void supervisorLoop(Function<PollContextInternal<K, V>, List<R>> userFunctionWrapped,
                                      Consumer<R> callback) {
        if (state != State.UNUSED) {
            throw new IllegalStateException(msg("Invalid state - you cannot call the poll* or pollAndProduce* methods " +
                    "more than once (they are asynchronous) (current state is {})", state));
        } else {
            state = RUNNING;
        }

        // broker poll subsystem
        brokerPollSubsystem.start(options.getManagedExecutorService());

        ExecutorService executorService;
        try {
            executorService = InitialContext.doLookup(options.getManagedExecutorService());
        } catch (NamingException e) {
            log.debug("Using Java SE Thread", e);
            executorService = Executors.newSingleThreadExecutor();
        }


        // run main pool loop in thread
        Callable<Boolean> controlTask = () -> {
            addInstanceMDC();
            log.info("Control loop starting up...");
            Thread controlThread = Thread.currentThread();
            controlThread.setName("pc-control");
            this.getMyId().ifPresent(id -> controlThread.setName("pc-control-" + id));
            this.blockableControlThread = controlThread;
            while (state != CLOSED) {
                log.debug("Control loop start");
                try {
                    controlLoop(userFunctionWrapped, callback);
                } catch (InterruptedException e) {
                    log.debug("Control loop interrupted, closing");
                    Thread.interrupted(); //clear interrupted flag as during close need to acquire commit locks and interrupted flag will cause it to throw another interrupted exception.
                    doClose(shutdownTimeout);
                } catch (Exception e) {
                    if (Thread.interrupted()) { //clear interrupted flag
                        log.debug("Thread interrupted flag cleared in control loop error handling");
                    }
                    log.error("Error from poll control thread, will attempt controlled shutdown, then rethrow. Error: " + e.getMessage(), e);
                    failureReason = new RuntimeException("Error from poll control thread: " + e.getMessage(), e);
                    doClose(shutdownTimeout); // attempt to close
                    throw failureReason;
                }
            }
            log.info("Control loop ending clean (state:{})...", state);
            return true;
        };
        Future<Boolean> controlTaskFutureResult = executorService.submit(controlTask);
        this.controlThreadFuture = Optional.of(controlTaskFutureResult);
    }
```

supervisorLoop는 Parallel Consumer 내부 구현의 핵심 메서드로, Broker, Control Loop 호출 등 거의 모든 작업을 트리거한다.

```java
brokerPollSubsystem.start(options.getManagedExecutorService());
```

앞서 보여진 아키텍처의 Broker Poller Thread에 해당하는 부분이다.

```java
    public void start(String managedExecutorService) {
        ExecutorService executorService;
        try {
            executorService = InitialContext.doLookup(managedExecutorService);
        } catch (NamingException e) {
            log.debug("Couldn't look up an execution service, falling back to Java SE Thread", e);
            executorService = Executors.newSingleThreadExecutor();
        }
        Future<Boolean> submit = executorService.submit(this::controlLoop);
        this.pollControlThreadFuture = Optional.of(submit);
    }
```

```java
    private boolean controlLoop() throws TimeoutException, InterruptedException {
        Thread.currentThread().setName("pc-broker-poll");
        pc.getMyId().ifPresent(id -> {
            Thread.currentThread().setName("pc-broker-poll-" + id);
            MDC.put(MDC_INSTANCE_ID, id);
        });
        log.trace("Broker poll control loop start");
        committer.ifPresent(ConsumerOffsetCommitter::claim);
        try {
            while (runState != CLOSED) {
                handlePoll();

                maybeDoCommit();

                switch (runState) {
                    case DRAINING -> {
                        doPause();
                    }
                    case CLOSING -> {
                        doClose();
                    }
                }
            }
            log.debug("Broker poller thread finished normally, returning OK (true) to future...");
            return true;
        } catch (Exception e) {
            log.error("Unknown error", e);
            throw e;
        }
    }
```

```java
    private void handlePoll() {
        log.trace("Loop: Broker poller: ({})", runState);
        if (runState == RUNNING || runState == DRAINING) { // if draining - subs will be paused, so use this to just sleep
            var polledRecords = pollBrokerForRecords();
            int count = polledRecords.count();
            log.debug("Got {} records in poll result", count);

            if (count > 0) {
                log.trace("Loop: Register work");
                pc.registerWork(polledRecords);
            }
        }
    }
```

```java
    public void registerWork(EpochAndRecordsMap<K, V> polledRecords) {
        log.trace("Adding {} to mailbox...", polledRecords);
        workMailBox.add(ControllerEventMessage.of(polledRecords));
    }

```

내부 코드를 보면 handlePoll를 통해 Mailbox에 메시지를 저장한다.

AbstractParallelEoSStreamProcessor#supervisorLoop 아래쪽에서는 controlTask라는 함수를 만든 후 ExecutorService에 넘기는 것을 볼 수 있다.

```java
        Callable<Boolean> controlTask = () -> {
            addInstanceMDC();
            log.info("Control loop starting up...");
            Thread controlThread = Thread.currentThread();
            controlThread.setName("pc-control");
        ...
        Future<Boolean> controlTaskFutureResult = executorService.submit(controlTask);
```

해당 부분이 바로 Controller Thread를 생성하는 부분이다. Controller Thread는 Mailbox에서 메시지를 읽은 후 Worker Thread에 분배한다. 

```java
    protected <R> void controlLoop(Function<PollContextInternal<K, V>, List<R>> userFunction,
                                   Consumer<R> callback) throws TimeoutException, ExecutionException, InterruptedException {
        maybeWakeupPoller();

        final boolean shouldTryCommitNow = maybeAcquireCommitLock();

        // make sure all work that's been completed are arranged ready for commit
        Duration timeToBlockFor = shouldTryCommitNow ? Duration.ZERO : getTimeToBlockFor();
        processWorkCompleteMailBox(timeToBlockFor);

        //
        if (shouldTryCommitNow) {
            // offsets will be committed when the consumer has its partitions revoked
            commitOffsetsThatAreReady();
        }

        // distribute more work
        retrieveAndDistributeNewWork(userFunction, callback);

        // run call back
        log.trace("Loop: Running {} loop end plugin(s)", controlLoopHooks.size());
        this.controlLoopHooks.forEach(Runnable::run);

        log.trace("Current state: {}", state);
        switch (state) {
            case DRAINING -> {
                drain();
            }
            case CLOSING -> {
                doClose(shutdownTimeout);
            }
        }

        // sanity - supervise the poller
        brokerPollSubsystem.supervise();

        // thread yield for spin lock avoidance
        Duration duration = Duration.ofMillis(1);
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            log.trace("Woke up", e);
        }

        // end of loop
        if (log.isTraceEnabled()) {
            log.trace("End of control loop, waiting processing {}, remaining in partition queues: {}, out for processing: {}. In state: {}",
                    wm.getNumberOfWorkQueuedInShardsAwaitingSelection(), wm.getNumberOfIncompleteOffsets(), wm.getNumberRecordsOutForProcessing(), state);
        }
    }
```

```java
processWorkCompleteMailBox(timeToBlockFor);
```

위 코드에서 AbstractParallelEoSStreamProcessor#processWorkCompleteMailbox에 도달한다.
이 메서드에서 WorkManager#registerWork를 호출하여 Mailbox에서 메시지를 가져와서 WorkManager에 등록한다.

```java
    private <R> int retrieveAndDistributeNewWork(final Function<PollContextInternal<K, V>, List<R>> userFunction, final Consumer<R> callback) {
        // check queue pressure first before addressing it
        checkPipelinePressure();

        int gotWorkCount = 0;

        //
        if (state == RUNNING || state == DRAINING) {
            int delta = calculateQuantityToRequest();
            var records = wm.getWorkIfAvailable(delta);

            gotWorkCount = records.size();
            lastWorkRequestWasFulfilled = gotWorkCount >= delta;

            log.trace("Loop: Submit to pool");
            submitWorkToPool(userFunction, callback, records);
        }

        //
        queueStatsLimiter.performIfNotLimited(() -> {
            int queueSize = getNumberOfUserFunctionsQueued();
            log.debug("Stats: \n- pool active: {} queued:{} \n- queue size: {} target: {} loading factor: {}",
                    workerThreadPool.get().getActiveCount(), queueSize, queueSize, getPoolLoadTarget(), dynamicExtraLoadFactor.getCurrentFactor());
        });

        return gotWorkCount;
    }
```

```java
    protected <R> void submitWorkToPool(Function<PollContextInternal<K, V>, List<R>> usersFunction,
                                        Consumer<R> callback,
                                        List<WorkContainer<K, V>> workToProcess) {
        if (state.equals(CLOSING) || state.equals(CLOSED)) {
            log.debug("Not submitting new work as Parallel Consumer is in {} state, incoming work: {}, Pool stats: {}", state, workToProcess.size(), workerThreadPool.get());
        }
        if (!workToProcess.isEmpty()) {
            log.debug("New work incoming: {}, Pool stats: {}", workToProcess.size(), workerThreadPool.get());

            // perf: could inline makeBatches
            var batches = makeBatches(workToProcess);

            // debugging
            if (log.isDebugEnabled()) {
                var sizes = batches.stream().map(List::size).sorted().collect(Collectors.toList());
                log.debug("Number batches: {}, smallest {}, sizes {}", batches.size(), sizes.stream().findFirst().get(), sizes);
                List<Integer> integerStream = sizes.stream().filter(x -> x < (int) options.getBatchSize()).collect(Collectors.toList());
                if (integerStream.size() > 1) {
                    log.warn("More than one batch isn't target size: {}. Input number of batches: {}", integerStream, batches.size());
                }
            }

            // submit
            for (var batch : batches) {
                submitWorkToPoolInner(usersFunction, callback, batch);
            }
        }
    }
```

```java
    private <R> void submitWorkToPoolInner(final Function<PollContextInternal<K, V>, List<R>> usersFunction,
                                           final Consumer<R> callback,
                                           final List<WorkContainer<K, V>> batch) {
        // for each record, construct dispatch to the executor and capture a Future
        log.trace("Sending work ({}) to pool", batch);
        Future outputRecordFuture = workerThreadPool.get().submit(() -> {
            addInstanceMDC();
            return runUserFunction(usersFunction, callback, batch);
        });
        // for a batch, each message in the batch shares the same result
        for (final WorkContainer<K, V> workContainer : batch) {
            workContainer.setFuture(outputRecordFuture);
        }
    }
```

```java
var records = wm.getWorkIfAvailable(delta);
```
WorkManager에 등록한 메시지를 AbstractParallelEoSStreamProcessor#retrieveAndDistributeNewWork에서 WorkManager#getWorkIfAvailable를 호출하여 가져온다.
WorkManager는 순서 보장을 고려하여 메시지를 반환한다.

## 순서 보장 방식 구현

위에서 언급한 순서 보장 방식(Partition, Key, Unordered) 구현하는 방법

<img width="476" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/822b3766-58b3-459f-96a0-2ec299357e57">

Parallel Consumer는 카프카 메시지를 shard 단위로 분배하여, 각 shard 별로 작업이 병렬 수행된다.

<img width="737" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/d51bcb77-4eb1-4b46-ab94-8db4c0d9f5ce">

Key, Partition 별로 shard가 생기고 shard 내에서 작업은 순서대로 처리되기 때문에, 단일 shard 내에서 메시지 처리 순서는 보장된다.

> Unordered인 경우에 Partition 개수만큼 shard가 생기지만 Partition shard 내의 메시지를 동시에 여러 건을 처리할 수 있다.

**ProcessingShard**

```java
@Slf4j
@RequiredArgsConstructor
public class ProcessingShard<K, V> {

    /**
     * Map of offset to WorkUnits.
     * <p>
     * Uses a ConcurrentSkipListMap instead of a TreeMap as under high pressure there appears to be some concurrency
     * errors (missing WorkContainers). This is addressed in PR#270.
     * <p>
     * Is a Map because need random access into collection, as records don't always complete in order (i.e. UNORDERED
     * mode).
     */
    @Getter
    private final NavigableMap<Long, WorkContainer<K, V>> entries = new ConcurrentSkipListMap<>();

    @Getter(PRIVATE)
    private final ShardKey key;
    ...
}
```
ProcessingShard는 단일 shard를 지칭하는데, entries를 통해 작업 메시지를, key를 통해 shardKey를 관리한다.

**ShardManager**

```java
@Slf4j
public class ShardManager<K, V> {

    private final PCModule<K, V> module;


    @Getter
    private final ParallelConsumerOptions<?, ?> options;

    private final WorkManager<K, V> wm;

    /**
     * Map of Object keys to Shard
     * <p>
     * Object Type is either the K key type, or it is a {@link TopicPartition}
     * <p>
     * Used to collate together a queue of work units for each unique key consumed
     *
     * @see ProcessingShard
     * @see K
     * @see WorkManager#getWorkIfAvailable()
     */
    // performance: could disable/remove if using partition order - but probably not worth the added complexity in the code to handle an extra special case
    @Getter(AccessLevel.PRIVATE)
    private final Map<ShardKey, ProcessingShard<K, V>> processingShards = new ConcurrentHashMap<>();

    ...
    
    
}
```

모든 shard 정보를 관리하며, 각 shard 별로 메시지를 가져와 WorkPool에 넘겨준다.

**ProcessingShard#getWorkIfAvailable**

```java
    ArrayList<WorkContainer<K, V>> getWorkIfAvailable(int workToGetDelta) {
        log.trace("Looking for work on shardQueueEntry: {}", getKey());

        var slowWork = new HashSet<WorkContainer<?, ?>>();
        var workTaken = new ArrayList<WorkContainer<K, V>>();

        var iterator = entries.entrySet().iterator();
        while (workTaken.size() < workToGetDelta && iterator.hasNext()) {
            var workContainer = iterator.next().getValue();

            if (pm.couldBeTakenAsWork(workContainer)) {
                if (workContainer.isAvailableToTakeAsWork()) {
                    log.trace("Taking {} as work", workContainer);
                    workContainer.onQueueingForExecution();
                    workTaken.add(workContainer);
                } else {
                    log.trace("Skipping {} as work, not available to take as work", workContainer);
                    addToSlowWorkMaybe(slowWork, workContainer);
                }

                if (isOrderRestricted()) {
                    // can't take any more work from this shard, due to ordering restrictions
                    // processing blocked on this shard, continue to next shard
                    log.trace("Processing by {}, so have cannot get more messages on this ({}) shardEntry.", this.options.getOrdering(), getKey());
                    break;
                }
            } else {
                // break, assuming all work in this shard, is for the same ShardKey, which is always on the same
                //  partition (regardless of ordering mode - KEY, PARTITION or UNORDERED (which is parallel PARTITIONs)),
                //  so no point continuing shard scanning. This only isn't true if a non standard partitioner produced the
                //  recrods of the same key to different partitions. In which case, there's no way PC can make sure all
                //  records of that belong to the shard are able to even be processed by the same PC instance, so it doesn't
                //  matter.
                log.trace("Partition for shard {} is blocked for work taking, stopping shard scan", this);
                break;
            }
        }

        if (workTaken.size() == workToGetDelta) {
            log.trace("Work taken ({}) exceeds max ({})", workTaken.size(), workToGetDelta);
        }

        logSlowWork(slowWork);

        return workTaken;
    }
```

```java
    private boolean isOrderRestricted() {
        return options.getOrdering() != UNORDERED;
        }
```

shard 별로 처리할 task를 가져오는 메서드이다.
이때 순서 보장이 필요한 경우(Key, Partition) shard 별로 1건의 메시지만 가져오고, 순서 보장이 필요 없는 경우 (Unordered) 병렬로 수행할 수 있는 최대의 메시지, 즉 batchSize만큼 가져온다.

## batchSize, delta

- batchSize : 단일 Worker Thread에서 한 번에 처리할 카프카 메시지 개수, 즉 단일 스레드 chunk를 의미한다.
- delta : 전체 Worker Thread Pool에서 한 번에 처리할 메시지 개수를 의마한다.

즉, 전체 shard로 부터 delta만큼 task를 가져와서 각 Worker Thread에 batchSize만큼 task를 전달한다.

> delta 기본 공식 : workerThreadPoolSize * 2 * batchSize * batchSize (batchSize가 기본 1이므로 workerThreadPoolSize * 2)
>
> batchSize와 delta는 ParallelConsumerOptions으로 쉽게 수정 가능하다.

## 커밋 

커밋은 Controller Thread에서 수행한다. AbstractParallelEoSStreamProcessor#controlLoop 위쪽을 보면 commitOffsetsThatAreReady 메서드를 호출한다.

```java
    protected void commitOffsetsThatAreReady() throws TimeoutException, InterruptedException {
        log.trace("Synchronizing on commitCommand...");
        synchronized (commitCommand) {
            log.debug("Committing offsets that are ready...");
            committer.retrieveOffsetsAndCommit();
            clearCommitCommand();
            this.lastCommitTime = Instant.now();
        }
    }
```

committer에게 OffsetCommitter#retrieveOffsetsAndCommit를 호출한다.
OffsetCommitter 구현체는 앞서 설명한 Work State Manager에서 최신 오프셋을 가져와서 커밋한다.

## 오류로 인한 메시지 중복 처리 방지

예를 들어 한 파티션에서 4, 5, 6, 7번 오프셋을 처리 중 4, 6, 7번 오프셋은 성공했지만 5번 오프셋은 처리하지 못한 경우 4번 오프셋까지 완료했다고 커밋한다.

<img width="800" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/089cd75d-f354-475b-919d-835e227ad411">

이때 장애로 서버가 재시작되면, 마지막으로 커밋한 오프셋을 4번으로 인식하고 5, 6, 7번 오프셋에 해당하는 메시지를 처리하려 할 것이다.
그런데 6, 7번 오프셋에 해당하는 메시지는 이미 처리했으므로 중복하여 재처리할 필요가 없다.

<img width="806" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/05e0c427-85c4-4749-8d3b-69ca9ed025f8">

Parallel Consumer에서는 이런 상황을 방지하기 위해 완료되지 않은 오프셋들을 오프셋 메타데이터에 기록한다. 이를 incompletedOffsets라고 부른다.
여기서는 5번 오프셋이 메타데이터에 기록될 것이다.

<img width="803" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/c82b2f9c-f3ef-4678-a61c-0998e97113cb">

메시지를 처리하기 전에 오프셋 메타데이터에 있는 incompletedOfffsets 정보를 확인하여 현재 메시지를 처리할지 여부를 판단한다.
여기서 incompleteOffsets에는 5번 오프셋만 있으니 6번, 7번은 건너뛰고 5번 오프셋에 대해서만 처리한다.

<img width="809" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/b625dde7-1ae7-4815-a07c-80fedc92ac1a">

### 구현 확인

**WorkManager#collectCommitDataForDirtyPartitions**

```java
    @Override
    public void retrieveOffsetsAndCommit() throws TimeoutException, InterruptedException {
        log.debug("Find completed work to commit offsets");
        preAcquireOffsetsToCommit();
        try {
            var offsetsToCommit = wm.collectCommitDataForDirtyPartitions();
            if (offsetsToCommit.isEmpty()) {
                log.debug("No offsets ready");
            } else {
                log.debug("Will commit offsets for {} partition(s): {}", offsetsToCommit.size(), offsetsToCommit);
                ConsumerGroupMetadata groupMetadata = consumerMgr.groupMetadata();

                log.debug("Begin commit offsets");
                commitOffsets(offsetsToCommit, groupMetadata);

                log.debug("On commit success");
                onOffsetCommitSuccess(offsetsToCommit);
            }
        } finally {
            postCommit();
        }
    }
```

```java
    public Map<TopicPartition, OffsetAndMetadata> collectCommitDataForDirtyPartitions() {
        return pm.collectDirtyCommitData();
    }
```

```java
    public Map<TopicPartition, OffsetAndMetadata> collectDirtyCommitData() {
        var dirties = new HashMap<TopicPartition, OffsetAndMetadata>();
        for (var state : getAssignedPartitions().values()) {
            var offsetAndMetadata = state.getCommitDataIfDirty();
            //noinspection ObjectAllocationInLoop
            offsetAndMetadata.ifPresent(andMetadata -> dirties.put(state.getTp(), andMetadata));
        }
        return dirties;
    }
```

```java
    public Optional<OffsetAndMetadata> getCommitDataIfDirty() {
        return isDirty() ?
                of(createOffsetAndMetadata()) :
                empty();
    }
```

```java
    protected OffsetAndMetadata createOffsetAndMetadata() {
        Optional<String> payloadOpt = tryToEncodeOffsets();
        long nextOffset = getOffsetToCommit();
        return payloadOpt
                .map(encodedOffsets -> new OffsetAndMetadata(nextOffset, encodedOffsets))
                .orElseGet(() -> new OffsetAndMetadata(nextOffset));
    }
```

PartitionState#createOffsetAndMetadata에 도달한다. 여기서 커밋할 오프셋과 메타데이터를 만들어 주는 것을 확인할 수 있다.
여기서 PartitionState#tryToEncodeOffsets를 호출한다.
PartitionState#tryToEncodeOffsets에서 성공한 것 중 가장 큰 오프셋과, 실패한 오프셋 리스트를 인코딩하여 반환한다.

```java
    private Optional<String> tryToEncodeOffsets() {
        if (incompleteOffsets.isEmpty()) {
            setAllowedMoreRecords(true);
            return empty();
        }

        try {
            // todo refactor use of null shouldn't be needed. Is OffsetMapCodecManager stateful? remove null #233
            OffsetMapCodecManager<K, V> om = new OffsetMapCodecManager<>(this.module);
            long offsetOfNextExpectedMessage = getOffsetToCommit();
            var offsetRange = getOffsetHighestSucceeded() - offsetOfNextExpectedMessage;
            String offsetMapPayload = om.makeOffsetMetadataPayload(offsetOfNextExpectedMessage, this);
            ratioPayloadUsedDistributionSummary.record(offsetMapPayload.length() / (double) offsetRange);
            ratioMetadataSpaceUsedDistributionSummary.record(offsetMapPayload.length() / (double) OffsetMapCodecManager.DefaultMaxMetadataSize);
            boolean mustStrip = updateBlockFromEncodingResult(offsetMapPayload);
            if (mustStrip) {
                return empty();
            } else {
                return of(offsetMapPayload);
            }
        } catch (NoEncodingPossibleException e) {
            setAllowedMoreRecords(false);
            log.warn("No encodings could be used to encode the offset map, skipping. Warning: messages might be replayed on rebalance.", e);
            return empty();
        }
    }
```

앞서 저장한 메타데이터를 사용해서 PartitionState#isRecordPreviouslyCompleted에서 incompleteOffsets만 처리하게 필터링한다.

---

**테스트해보기**

