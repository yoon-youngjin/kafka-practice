# 아파치 카프카 CLI 활용

주키퍼, 카프카 브로커는 모두 JVM 위에서 동작한다.
즉, 자바 실행 환경(JRE, JDK)이 구축되어 있지않다면 실행할 수 없다.

## 로컬에서 카프카 브로커 실행

프로듀서가 데이터를 보내면 카프카 브로커로 전달되고 카프카 브로커의 데이터는 파일 시스템에 저장된다.
이때 파일시스템의 위치는 server.properties의 적힌 log.dirs로 정해진다.

`config/server.properties`

```text
listeners = PLAINTEXT://localhost:9092 -> 카프카 브로커가 통신을 통해 데이터를 받을 주소와 포트 
advertised.listeners=PLAINTEXT://localhost:9092 -> 카프카 브로커가 다른 브로커 또는 클라이언트에게 제공하는 자신의 주소
... 
log.dirs=/Users/dudwls143/kafka_2.12-2.5.0/data -> 저장할 파일시스템 위치
num.partitions=3 -> 토픽을 만들때 기본으로 만들 파티션 개수
...
zookeeper.connect=localhost:2181 -> 주키퍼 주소
```

### 주키퍼

주키퍼 : 카프카 클러스터 내의 브로커 상태를 추적하고, 토픽 메타데이터, 컨슈머 그룹, 파티션 리더 정보 등의 중요한 정보를 저장한다.

**역할** 
- 브로커를 동적으로 추가/제거
- 토픽 메타데이터의 관리(파티션 위치, 리더 브로커 정보 등)
- 브로커 장애 발생 시 리더 재선출

**주키퍼 실행**

```text
./bin/zookeeper-server-start.sh config/zookeeper.properties
```

**브로커 실행**

```text
./bin/kafka-server-start.sh config/server.properties 
```

**브로커가 정상적으로 실행되었는지 파악하는 방법**

```text
./bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

<img width="783" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/f9e1dc26-4b66-49a1-af87-737730f923b1">

## kafka-topics.sh

```text
./bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --topic hello_kafka
```

hello_kafka 토픽처럼 카프카 클러스터 정보와 토픽 이름만으로 토픽을 생성할 수 있다. 클러스터 정보와 토픽 이름은 토픽을 만들기 위한 필수 값이다.
이렇게 만들어진 토픽은 파티션 개수, 복제 개수 등과 같이 다양한 옵션이 포함되어 있지만 모두 브로커에 설정된 기본값으로 생성되었다.

**토픽 생성 정보 확인**

```text
./bin/kafka-topics.sh --bootstrap-server localhost:9092 --topic hello_kafka --describe
```

<img width="746" alt="image" src="https://github.com/yoon-youngjin/kafka-practice/assets/83503188/47b6887b-a991-48b5-85c2-d1a856d06ce4">

**파티션 개수, 복제 개수, 토픽 데이터 유지 기간 옵션 지정하여 토픽 생성**

```text
./bin/kafka-topics.sh --create \
    --bootstrap-server localhost:9092 \
    --partitions 10 \
    --replication-factor 1 \
    --topic hello_kafka \
    --config retention.ms=172800000 
```

**토픽 리스트 확인**

```text
./bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

**파티션 개수 늘리기**

```text
./bin/kafka-topics.sh --bootstrap-server localhost:9092 --topic hello_kafka --alter -partitions 4
```

> 컨슈머의 데이터 처리량을 늘리는 가장 쉬운 방법은 파티션의 개수와 컨슈머의 개수를 늘리는 방법이다.

파티션 개수를 늘릴 수 있지만 줄일 수 없다. 줄이는 명령을 내리면 InvalidPartitionsException이 발생한다. 
분산 시스템에서 이미 분산된 데이터를 줄이는 방법은 매우 복잡하다. 삭제 대상 파티션을 지정해야할 뿐만 아니라 기존에 저장되어 있던 레코드를 분산하여 저장하는 로직이 필요하기 때문이다.
이 때문에 카프카에서는 파티션을 줄이는 로직은 제공하지 않는다. 만약 파티션 개수를 줄어야만 할 때는 토픽을 새로 만드는편이 좋다.

## kafka-configs.sh

토픽의 일부 옵션을 설정하기 위해서는 kafka-configs.sh 명령어를 사용해야 한다. --alter --add-config 옵션을 사용하여 min.insync.replicas 옵션을 토픽별로 설정할 수 있다.

min.insync.replicas는 프로듀서로 데이터를 보낼 때 컨슈머가 데이터를 읽을 때 워터마크 용도 혹은 안전하게 데이터를 보내야되는지도 명확하게 설정할 때 활용된다.

> min.insync.replicas : 쓰기 작업이 성공으로 간주되기 전에 동기화되어야 하는 레플리카(replica)의 최소 수
> 
> 예를 들어 2로 설정된 경우, 프로듀서가 데이터를 쓸 때 해당 주제에 두 개 이상의 레플리카에 데이터가 동기화되어야만 쓰기 작업이 성공으로 간주된다.

```text
./bin/kafka-configs.sh --bootstrap-server localhost:9092 --alter --add-config min.insync.replicas=2 --topic hello_kafka
```

<img width="756" alt="image" src="https://github.com/yoon-youngjin/kafka-practice/assets/83503188/cb91b719-4739-4b83-84e2-e12dcfe93b9d">

브로커에 적용한 server.properties를 직접 확인하지 않더라도 브로커에 설정된 각종 기본값은 --broker, --all, --describe 옵션을 사용하여 조회할 수 있다.

```text
./bin/kafka-configs.sh --bootstrap-server localhost:9092 --broker 0 --all --describe
```

## kafka-console-producer.sh

```text
./bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic hello_kafka
```

**메시지 키를 가지는 레코드를 전송**

key.separator를 선언하지 않으면 기본 설정은 Tab delimiter(\t)이므로 key.seperator를 선언하지 않고 메시지를 보내려면 메시지 키를 작성하고 탭키를 누른 뒤 메시지 값을 작성하고 엔터를 누른다.
여기서는 명시적으로 확인하기 위해 콜론(:)을 구분자로 선언했다.

```text
./bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic hello_kafka --property "parse.key=true" --property "key.separator=:"
```

<img width="761" alt="image" src="https://github.com/yoon-youngjin/kafka-practice/assets/83503188/18bc6511-53da-414a-b1ee-178d0e8c1c6f">
해당 옵션을 주지 않으면 메시지 키가 null로 전달된다.
메시지 키는 데이터 구분자로 사용된다. 따라서 필수값은 아니다. 하지만 특정 레코드들이 순서를 지키고 싶다거나 반드시 같이 처리되어야 하는 경우(예를 들어 계좌 잔액 변경은 키를 계좌번호로 설정하여 반드시 같은 파티션으로 할당)에 메시지 키가 필요하다.

**메시지 키와 메시지 값이 포함된 레코드가 파티션에 전송된 상황**

<img width="465" alt="image" src="https://github.com/yoon-youngjin/kafka-practice/assets/83503188/0417735a-f118-454c-8b7b-639f8978eb46">

메시지 키와 메시지 값을 함께 전송한 레코드는 토픽의 파티션에 저장된다. 메시지 키가 null인 경우에는 프로듀서가 파티션으로 전송할 때 레코드 배치 단위(레코드 전송 묶음)로 라운드 로빈 방식으로 전송한다.
메시지 키가 존재하는 경우에는 키의 해시값을 적용하여 파티션 중 한개에 할당된다. 이로 인해 메시지키가 동일한 경우에는 동일한 파티션으로 전송된다.

이로 인해 달성할 수 있는 목표는 동일한 메시지 키가 있는 파티션 내에서 순서를 보장할 수 있다.
따라서 컨슈머 입장에서는 파티션과 일대일 관계를 갖게 되므로 K1 메시지 키를 갖는 레코드에 대해서 파티션에 들어온 순서대로 읽어갈 수 있다.

## kafka-console-consumer.sh

특정 토픽으로 전송한 데이터는 kafka-console-consumer.sh 명령어로 확인할 수 있다. 이때 필수 옵션으로 --bootstrap-server에 카프카 클러스터 정보, --topic에 토픽 이름이 필요하다. 추가로 --from-beginning 옵션을 주면 토픽에 저장된 가장 처음 데이터부터 출려한다.

> auto.offset.reset = earliest : 처음 offset 부터 읽음
> 
> auto.offset.reset = latest : 마지막 offset 부터 읽음 (default)
> 
> --from-beginning을 사용해야만 auto.offset.reset이 ealiest로 지정된다. 

```text
./bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic hello_kafka --from-beginning
```

**레코드의 메시지 키와 메시지 값을 확인하고 싶은 경우**

```text
./bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic hello_kafka --property print.key=true --property key.separator="-" --from-beginning
```

<img width="777" alt="image" src="https://github.com/yoon-youngjin/kafka-practice/assets/83503188/a7f100a6-c3c1-4be7-9e37-351ee9a1efaf">

--max-messages 옵션을 사용하면 최대 컨슘 메시지 개수를 설정할 수 있다.

해당 옵션 없이 데이터를 보내게 되면 실시간 데이터가 모두 출력되는데 이를 해당 옵션을 통해서 제한할 수 있다.

```text
./bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic hello_kafka --from-beginning --max-messages 1
```

--partition 옵션을 사용하면 특정 파티션만 컨슘할 수 있다. 
해당 번호에 해당하는 파티션의 데이터만 가져올 수 있다.

```text
./bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic hello_kafka --partition 2 --from-beginning
```

--group 옵션을 사용하면 컨슈머 그룹을 기반으로 kafka-console-consumer가 동작한다.
해당 옵션을 주면 컨슈머 그룹이 활성화된다. 컨슈머 그룹을 통해 특정 오프셋까지 데이터를 읽었음을 커밋하기 위해 사용한다.

> 컨슈머 그룹 : 특정 목정을 가진 컨슈머들을 묶음으로 사용하는 것을 뜻한다. 컨슈머 그룹으로 토픽의 레코드를 가져갈 경우 어느 레코드까지 읽었는지에 대한 데이터가 카프카 브로커에 저장된다.

```text
./bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic hello_kafka --group hello-group --from-beginning
```

위와 같이 group 옵션을 주면 특정 데이터가 까지 읽고 해당 컨슈머가 종료되었다가 다시 실행되어도 마지막으로 읽은 레코드 다음 레코드부터 읽게된다.

위 명령어를 실행하면 hello-group이라는 컨슈머 그룹이 현재의 마지막 데이터까지 읽었음을 커밋 수행한 것이다.
해당 커밋 데이터는 __consumer_offset 라는 토픽에 저장된다.

<img width="590" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/15938e17-06f6-4554-8f78-1f41f0e6599f">

- 새로운 토픽이 생성됨을 확인

## kafka-consumer-groups.sh

앞서서 hello-group 이름의 컨슈머 그룹으로 생성된 컨슈머로 hello_kafka 토픽의 데이터를 가져갔다.
컨슈머 그룹은 따로 생성하는 명령을 날리지 않고 컨슈머로를 동작할 때 컨슈머 그룹이름을 지정하면 새로 생성된다.

생성된 컨슈머 그룹의 리스트는 kafka-consumer-groups.sh 명령어로 확인할 수 있다. 추가적으로 --describe 옵션을 주면 상세정보를 확인할 수 있다.

```text
./bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

./bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group hello-group --describe
```

--describe 옵션을 사용하면 해당 컨슈머 그룹이 어떤 토픽을 대상으로 레코드를 가져갔는지 상태를 확인할 수 있다.
파티션 번호, 현재까지 가져간 레코드의 오프셋, 파티션 마지막 레코드의 오프셋, 컨슈머 랙, 컨슈머 ID, 호스트를 알 수 있기 때문에 컨슈머 상태를 조회할 때 유용하다.

> 컨슈머 랙? 마지막 레코드의 오프셋과 현재까지 가져간 레코드의 오프셋의 차이다.
> 
> 컨슈머 렉이 크면 클수록 프로듀서가 보낸 데이터보다 이전의 데이터를 처리하고 있기 때문에 지연 정도를 확인할 수 있다. 따라서 컨슈머 랙의 모니터링이 중요하며 그에 따라서 컨슈머 개수를 늘리거나 파티션 개수를 늘리는 등의 조치를 취해야 한다.

**kafka-consumer-groups.sh 오프셋 리셋**

```text
./bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic hello_kafka --group hello-group --reset-offset --to-earliest --execute
```

위와 같이 명령어를 실행하면 특정 오프셋까지 읽었더라도 처음부터 다시(--to-earliest) 데이터를 재처리하도록 실행할 수 있다. 
또는 옵션에 따라서 이전 데이터가 아닌 최신 데이터부터 처리할 수도 있다.

<img width="719" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/1a884cc7-d643-4f4d-a8b7-7c991a833932">

컨슈머가 1번 오프셋을 처리하고 있는데 파티션에 10억건의 데이터가 밀린 상황이라고 가정해보자.
이때 1번 ~ 100억 사이의 데이터를 무시하고 최신 데이터를 처리하고 싶다면 해당 옵션을 사용해볼 수 있다.

### kafka-consumer-groups.sh 테스트

<img width="979" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/b5122723-3f8e-4d4c-98cc-f70748c57477">

- describe 옵션을 통해서 현재까지 가져간 레코드의 오프셋과 파티션 마지막 레코드의 오프셋 정보를 확인해볼 수 있고, 해당 컨슈머 그룹에서 현재까지의 레코드를 모두 처리했으므로 LAG이 모두 0임을 확인할 수 있다.

<img width="930" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/2beee207-2516-4dcc-9fb9-88372b6c8766">

- 해당 토픽에 임의로 7개의 레코드를 생성

<img width="1019" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/37bb8a3d-a71f-45fc-922b-4a9f0eae8d30">

- 현재 해당 컨슈머 그룹이 데이터를 처리하지 못했으므로 총 7개의 LAG이 생성됨을 확인할 수 있다.

**hello_kafka 토픽에서 데이터를 가져가는 hello-group 실행**

<img width="1013" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/323b0344-0fcb-4ffe-b32f-b7ca4d3b13ed">

<img width="1019" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/6c9d9bfd-351e-46f2-9d70-dbfbd8039036">

**오프셋 리셋 테스트**

오프셋 리셋을 통해 처음 데이터부터 다시 읽어야하는 상황이 운영 페이지에서는 존재할 수 있다.
카프카는 기본적으로 토픽에 존재하는 데이터는 retention 기간동안 삭제되지 않기 때문이다. 따라서 토픽에서 가장 낮은숫자의 오프셋부터 가져와서 재처리하고 싶다면 오프셋 리셋을 사용해야한다.

<img width="1008" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/6ab0e8ce-2460-43b8-9b3a-eec8cdc04858">

<img width="1017" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/4fd10a7c-d02d-4e39-8dc1-5f73600503dd">

## 그 외 커맨드 라인 툴

### kafka-producer-perf-test.sh

kafka-producer-perf-test.sh는 카프카 프로듀서로 퍼포먼스를 측정할 때 사용된다.
특정 지점부터 해당 브로커까지 네트워크 사용량, 처리량 등을 확인할 수 있다.

```text
./bin/kafka-producer-perf-test.sh --num-records 10 --throughput 1 --record-size 1024 --print-metric --topic hello_kafka --producer-props bootstrap.servers=localhost:9092
```
- --topic : 레코드를 발행할 대상 토픽을 지정
- --num-records : 총 발행할 레코드 수
- --throughput : 초당 발행될 레코드의 수를 지정 -> 현재는 초당 1개의 레코드가 발행되도록 설정
- --record-size : 레코드 사이즈 

### kafka-consumer-perf-test.sh

kafka-consumer-perf-test.sh는 카프카 컨슈머로 퍼포먼스를 측정할 때 사용된다. 카프카 브로커와 컨슈머(여기서는 해당 스크립트를 돌리는 호스트)간의 네트워크를 체크할 때 사용할 수 있다.

```text
./bin/kafka-consumer-perf-test.sh --bootstrap-server localhost:9092 --topic hello_kafka --messages 10 --show-detailed-stats
```

현재 테스트 환경에서는 브로커와 컨슈머를 모두 하나의 PC에 띄우지만 실제 운영환경에서는 실제 host가 존재하고 멀리떨어진 client와 네트워크 통신을 하는데, 이때 이슈가 없는지 테스트해볼 수 있다.

### kafka-reassign-partitions.sh

<img width="710" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/69e7ec7b-adce-4068-982a-2e3cb57d3e6a">

특정 브로커에 파티션이 몰리는 현상이 발생할 수 있는데, 즉 카프카 클라이언트와 직접적으로 통신하는 리더 파티션이 특정 브로커에 몰릴 수 있는데, 이때 리더 파티션과 팔로워 파티션을 여러 브로커로 적당히 분산시킬때 해당 스크립트를 사용해볼 수 있다.

```text
kafka-reassign-partitions.sh --zookeeper localhost:2181 --reassignment-json-file partitions.json --execute
```

**partitions.json**

파일명은 상관없다. 

```json
{
  "partitions": [
    {
      "topic": "hello_kafka",
      "partition": 0,
      "replicas": [ 0 ] 
      
    } 
  ], "version": 1
}
```

kafka-reassign-partitions.sh를 사용하면 리더 파티션과 팔로워 파티션이 위치를 변경할 수 있다. 카프카 브러커에는 auto.leader.rebalance.enable 옵션이 있는데 이 옵션의 기본값은 true로써 클러스터 단위에서 리더 파티션을 자동 리밸런싱하도록 도와준다.
브로커의 백그라운드 스레드가 일정한 간격으로 리더의 위치를 파악하고 필요시 리더 리밸런싱을 통해 리더의 위치가 알맞게 배분된다.

### kafka-delete-record.sh

해당 스크립트를 사용하면 지정한 오프셋까지 레코드를 지울 수 있다. 예를 들어 offset 5인 경우 0~5까지 레코드를 삭제한다.

```text
bin/kafka-delete-records.sh --bootstrap-server localhost:9092 --offset-json-file delete-topic.json
```

**delete-topic.json**

파일명은 상관없다.

```json
{
  "partitions": [
    {
      "topic": "test",
      "partition": 0,
      "offset": 6
    }
  ],
  "version": 1
}
```

### kafka-dump-log.sh

카프카를 직접적으로 운영하지 않는 애플리케이션 개발자라면 dump를 확인할일은 거의 없지만 만약에라도 로그를 확인해야한다면 해당 스크립트를 활용해볼 수 있다.

<img width="658" alt="image" src="https://github.com/yoon-youngjin/spring-study/assets/83503188/a4b0fc68-84d3-45c8-98e9-e5575037e6d4">

```text
./bin/kafka-dump-log.sh --files data/hello_kakfa-0/00000000000000000000.log --deep-iteration
```

## 토픽을 생성하는 두가지 방법

1. 카프카 컨슈머 또는 프로듀서가 카프카 브로커에 생성되지 않은 토픽에 대해 데이터를 요청 (이때 브로커에 설정된 기본 옵션으로 생성)
2. 커맨드 라인 툴(kafka-topic.sh)로 명시적으로 토픽을 생성

토픽을 효과적으로 유지보수하기 위햐서는 토픽을 명시적으로 생성하는 것을 추천한다. 토픽마다 처리되어야 하는 데이터의 특성이 다르기 때문이다.
토픽을 생성할 때는 데이터 특성에 따라 옵션을 다르게 설정할 수 있다. 예를 들어, 동시 데이터 처리량이 많아야 하는 토픽의 경우 파티션의 개수를 100으로 설정할 수 있다.
단기간 데이터 처리만 필요한 경우에는 토픽에 들어온 데이터의 보관기간 옵션(retention)을 짧게 설정할 수도 있다. 그러므로 토픽에 들어오는 데이터양과 병렬로 처리되어야 하는 용량을 잘 파악하여 생성하는 것이 중요하다.

> 브로커의 옵션 중 auto create 옵션을 통해 자동 생성을 막을 수도 있다. 기본은 true

## 카프카 브로커와 로컬 커맨드 라인 툴(*.sh) 버전을 맞춰야 하는 이유

카프카 브로커로 커맨드 라인 툴 명령을 내릴 때 브로커의 버전과 커맨드 라인 툴 버전을 반드시 맞춰서 사용하는 것을 권장한다.
브로커의 버전이 업그레이드 됨에 따라 커맨드 라인 툴의 상세 옵션이 달라지기 때문에 버전 차이로 인해 명령이 정상적으로 실행되지 않을 수 있다.

> 이번 실습에서는 카프카 2.5.0을 기준으로 실행하기 때문에 카프카 2.5.0 바이너리 패키지에 들어있는 카프카 커맨드라인 툴을 이용한다.


----

1. 컨슈머 랙 모니터링을 통해 지연 여부를 확인할 수 있고, 이에 따라 컨슈머 개수를 늘리거나 파티션 개수를 늘리는등의 조치를 취할 수 있다.
- https://www.notion.so/tossteam/Burrow-6ed4f1dd9e5d48f8928d78b24c40462e

