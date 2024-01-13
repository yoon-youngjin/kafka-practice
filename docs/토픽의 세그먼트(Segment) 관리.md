# 토픽의 세그먼트(Segment) 관리

## 메시지 로그 세그먼트의 이해 

![image](https://github.com/yoon-youngjin/spring-study/assets/83503188/8375ef8b-5701-4c9b-9079-5df3bbfea58b)

- 카프카의 로그 메시지는 실제로는 segment로 저장
- 파티션은 단순히 파일 디렉토리로만 되어 있고, 해당 파티션 디렉토리에 메시지 저장 segment를 file로 가지고 있다
  - ./tmp/kafka-logs 디렉토리에 특정 파티션 폴더에 존재하는 *.log 가 메시지 로그가 저장되는 세그먼트이다.
  - 해당 *.log 파일은 여러 개 만들어질 수 있다.
- 파티션은 여러 개의 segment로 구성되며 개별 segment는 데이터 용량이 차거나 일정 시간이 경과하면 close 되고 새로운 segment를 생성하여 데이터를 연속적으로 저장 (**Roll**)
- 파티션내에 여러 개의 segment가 존재하지만 단 하나의 segment(Active segment)에서만 read & write가 수행된다.
  - 이전의 segment들은 only read

### Segment의 저장 크기와 roll 관련 설정

<img width="790" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/99513b42-cd49-4505-81fd-89112f2af550">

> 브로커 레벨의 파라미터가 존재하고 토픽 레벨의 파라미터가 존재하는데 토픽 레벨의 파라미터가 override된다.

## 세그먼트 구성 및 rolling 매커니즘 실습

```text
./kafka-configs --bootstrap-server localhost:9092 --entity-type topics --entity-name pizza-topic-stest --alter --add-config segment.bytes=10240
```

- `--entity-type topics`: 토픽 레벨로 설정, topic이 아닌 brokers로 지정하면 브로커 레벨로 설정된다.
- 기본 설정은 브로커 설정에 따라서 10GB로 되어있다.

```text
./kafka-configs --bootstrap-server localhost:9092 --entity-type topics --entity-name pizza-topic-stest --all --describe | grep log.segment.bytes
```

```text
segment.bytes=10240 sensitive=false synonyms={DYNAMIC_TOPIC_CONFIG:segment.bytes=10240, STATIC_BROKER_CONFIG:log.segment.bytes=1073741824, DEFAULT_CONFIG:log.segment.bytes=1073741824}
```

<img width="769" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/3dd42406-a926-4ca6-a0ce-1003b8c6776d">

- *.log 파일의 크기가 10K가 넘어가져 새로운 *.log 파일이 생김(Roll)을 확인할 수 있다.
  - 이전 파일 close
- 또한 이전 *.index, *.timeindex 파일의 사이즈가 줄어든 것도 확인할 수 있다. 
- *.log, *.index, *.timeindex 파일이 *0.log 에서 *42.log로 변경됨을 확인할 수 있는데, 이는 offset 번호다.
  - *0.log 파일에는 0 ~ 41 offset에 해당하는 메시지를 가지고 있다.
  - *42.log 파일에는 42 offset 부터 해당하는 메시지를 가지고 있다.

**topic의 segment.ms 설정을 60초로 변경**

```text
./kafka-configs --bootstrap-server localhost:9092 --entity-type topics --entity-name pizza-topic-stest --alter --add-config segment.ms=60000
```

<img width="549" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/37fc9a78-0081-4394-ba44-0344676326a9">

- 위와 같이 설정하면 아직 10K를 넘지 않았지만 60초가 지났으므로 새로운 세그먼트가 생성됨을 확인할 수 있다.

## 인덱스(Index)와 타임인덱스(TimeIndex) 세그먼트의 이해

Topic을 생성하면 파티션 디렉토리내에 메시지 내용을 가지는 segment의 offset의 위치 byte 정보를 가지는 Index 파일, record 생성 시간에 따른 위치 byte 정보를 가지는 timeindex 파일로 구성된다.

<img width="761" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/5d159ff7-0db1-4d86-88e3-1d9afb7054d4">

- 만약 특정 오프셋부터 데이터를 읽기 위해서 0번 offset 부터 읽어서 순차적으로 확인한다면 *.log 파일이 크면 클수록 많은 파일 IO가 발생하고 이에 따라서 시간이 많이 소요될 것이다.
- 이를 위해서 크기 기반으로 오프셋이 어느 위치에 있는지를 기록해두는 *.index, 시간 기반으로 오프셋 위치를 기록하는 *.timeindex가 존재한다.
  - index 파일은 offset 별로 byte position 정보를 가지고 있다. 메시지 Segemnt는 File 기반이므로 특정 offset의 데이터 파일에서 읽기 위해서는 시작 File Pointer에서 얼마만큼의 byte에 위치해 있는지 알아야한다.
  - 18번 offset은 최초 file pointer 기준으로 몇 byte(4334byte) 떨어져있음을 나타낸다. 
  - 하지만 모든 offset 정보를 가지고 있지 않다. 예를 들어 15번 offset을 찾아야 한다면 0번 부터 4334 byte 까지를 스캔 대상으로 잡는다.
    - 만약 index 파일에서 모든 offset 바이트 정보를 관리한다면 이에 따른 스캔 비용이 발생하므로 부분만 저장하는데, log.index.interval.bytes에 설정된 값만큼의 segment bytes가 만들어질 때마다 해당 offset에 대한 byte position 정보를 기록한다. (위 그림에서 4K이므로 4334이후에 새로운 offset position을 저장)
  - timeindex 파일은 메시지 생성 Unix 시간을 밀리 세컨드 단위로 가지고 있고 해당 생성 시간에 해당하는 offset 정보를 가진다.

```text
./kafka-dump-log --deep-iteration --files /tmp/kafka-logs/pizza-topic-stest-0/00000000000000000000.index --print-data-log
```

<img width="995" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/68856552-0f98-4911-a0a4-5998530caa0d">

<img width="666" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/04264f99-2470-4aca-96c5-1d67fba28e84">

- 해당 파일은 Roll 할때마다 10MB로 만들어진다. 
  - 해당 파일의 크기를 모두 사용하지는 않지만 혹시 모르니까 크기를 크게 잡아둔다.
- Roll이 수행되면 이전 Index 파일은 존재하는 데이터 크기만큼으로 줄어든다.

## 세그먼트의 생명 주기 관리 및 log.cleanup.policy의 삭제(delete) 설정 이해

<img width="777" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/7ceb2ac8-ee87-4927-88a7-3a2c98261311">

- Segment 파일은 Active -> Closed -> Deleted or Compacted 단계로 관리
  - Closed는 세그먼트 삭제
  - Compacted는 동일한 키값에 대해서 가장 최신 메시지만 남기고 삭제
- Segment 파일은 Log Cleanup 정책에 따라 지정된 특정 시간이나 파일 크기에 따라 삭제되거나 Compact 형태로 저장된다.

### Log Cleanup Policy

- 카프카 브로커는 오래된 메시지를 관리하기 위한 정책을 log.cleanup.policy로 설정(Topic레벨은 cleanup.policy)
  - log.cleanup.policy=delete로 설정하면 segment를 log.retention.hours나 log.retention.bytes 설정 값에 따라 삭제
  - log.cleanup.policy=compact로 설정하면 segment를 key 값 레벨로 가장 최신의 메시지만 유지하도록 segment를 재구성
  - log.cleanup.policy=[delete, compact]로 설정하면 먼저 compact하고 이후에 delete

**log.cleanup.policy=delete시 삭제를 위한 설정 파라미터**

<img width="775" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/ccefddc8-5834-4199-aa89-a96d9124f203">

- 브로커가 백그라운드로 돌면서 해당 설정 파라미터를 기준으로 삭제 대상을 찾는다.

```text
./kafka-configs --bootstrap-server localhost:9092 --entity-type brokers --entity-name 0 --all --describe | grep retention
```

```text
  log.cleaner.delete.retention.ms=86400000 sensitive=false synonyms={DEFAULT_CONFIG:log.cleaner.delete.retention.ms=86400000}
  log.retention.hours=168 sensitive=false synonyms={STATIC_BROKER_CONFIG:log.retention.hours=168, DEFAULT_CONFIG:log.retention.hours=168}
  log.retention.minutes=null sensitive=false synonyms={}
  offsets.retention.check.interval.ms=600000 sensitive=false synonyms={DEFAULT_CONFIG:offsets.retention.check.interval.ms=600000}
  log.retention.check.interval.ms=300000 sensitive=false synonyms={STATIC_BROKER_CONFIG:log.retention.check.interval.ms=300000, DEFAULT_CONFIG:log.retention.check.interval.ms=300000}
  log.retention.ms=null sensitive=false synonyms={}
  log.retention.bytes=-1 sensitive=false synonyms={DEFAULT_CONFIG:log.retention.bytes=-1}
  offsets.retention.minutes=10080 sensitive=false synonyms={DEFAULT_CONFIG:offsets.retention.minutes=10080}
  metadata.max.retention.bytes=-1 sensitive=false synonyms={DEFAULT_CONFIG:metadata.max.retention.bytes=-1}
  metadata.max.retention.ms=604800000 sensitive=false synonyms={DEFAULT_CONFIG:metadata.max.retention.ms=604800000}
```

**log.retention.check.interval.ms=300000 && retention.ms=180000 설정**

<img width="760" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/75326704-791e-43d3-8372-7811127b9b2c">

일정 시간이 지나면

<img width="801" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/84904a07-807c-422a-9a14-d30f9ebfd8ae">

삭제 대상으로 마킹한 뒤 삭제한다.

> 참고로 retention은 사용되지 않는 세그먼트를 대상으로 하기 때문에 active segment는 대상이 될 경우가 적지만 만약 active segment도 사용되지 않는 상태라면 삭제된다.

## Log Compaction 이해 

<img width="438" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/4044e6a4-79fe-47ad-a5f8-19f6fcc4f14c">

- log.cleanup.policy=compact로 설정 시 Segment의 key값에 따라 가장 최신의 메시지로만 compact하게 segment를 재구성
- key 값이 null인 메시지에는 적용할 수 없다.
- 백그라운드 스레드 방식으로 별도의 IO작업을 수행하므로 추가적인 IO 부하가 소모된다.
- 내부 토픽인 __consumer_offset 은 어떤 토픽의 특정 파티션에서 특정 컨슈머에서 최신으로 읽은 오프셋만 기록하면 되기 때문에 compaction을 수행하도록 설정되어있다.

### Log Compaction 수행 개요

<img width="744" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/1204d081-4d22-473e-bd7a-0891b2363fa2">

- 브로커의 백그라운드에서 수행되는 Log Cleaner라는 쓰레드가 Log Compaction을 수행한다.

<img width="774" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/0a5fc404-5f7b-459f-86b4-714f4435c7ad">

- Active Segment는 Compact 대상에서 제외
- Compaction은 파티션 레벨에서 수행 결정이 되며, 개별 세그먼트들을 새로운 세그먼트들로 재 생성한다.
  - 개별 파티션 영역에 있는 Segment 들을 조사하여 더티 비율이 특정 수준 높아지면 Log Compaction을 수행한다

### Log Compaction 수행 이후

- 메시지의 순서는 여전히 유지
- 메시지의 offset 변경X

### 언제 Log Compaction이 수행?

- 더티 비율이 log.cleaner.min.cleanble.ratio 이상이고 메시지가 생성된 지 log.cleaner.min.compaction.lag.ms이 지난 더티 메시지에 수행
- 메시지가 생성된지 log.cleaner.max.compaction.lag.ms이 지난 더티 메시지에 수행

<img width="806" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/bebf4b8b-acf3-4e2e-8781-db9d0f7b9cc0">

### Compaction 수행 시 메시지의 삭제

<img width="793" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/b348fcac-aa67-41d5-b84a-fa7d62b44560">

- Key는 존재하지만 Value가 null인 최신 메시지에 대해서 Log Compaction을 수행하면 tombstone으로 표시해둔다.
- 이후 다시 Log Compaction이 수행될 때 log.cleaner.delete.ms가 지났다면 해당 메시지를 삭제한다.

