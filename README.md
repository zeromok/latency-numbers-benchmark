# latency-numbers-benchmark


[![블로그 포스트](https://img.shields.io/badge/Blog-Read%20Full%20Story-green)](https://b-mokk.tistory.com/86)

> 📊 "모든 프로그래머가 알아야 할 응답 지연 시간"을 JMH로 실제 측정

## 프로젝트 소개

『가상 면접 사례로 배우는 대규모 시스템 설계 기초』 2장에 나오는 **응답 지연 시간 표**를 보고, "실제로 얼마나 차이 날까?" 궁금해서 직접 측정해봤습니다.

이론으로만 알던 "메모리가 디스크보다 빠르다"를 **실제 숫자**로 확인할 수 있습니다.

---

## 측정 항목

| 연산 | 설명 | 측정 결과 (M1 Mac) |
|------|------|--------------------|
| 🔒 뮤텍스 락/언락 | `synchronized` 블록 | ~8.9 ns |
| 💾 메모리 1MB 읽기 | RAM 순차 읽기 | ~667 μs |
| 💿 디스크 1MB 읽기 | SSD 파일 읽기 | ~750 μs |
| 🗜️ 1KB GZIP 압축 | 데이터 압축 | ~17.7 μs |
| 🌐 네트워크 왕복 | localhost ping | ~23.6 μs |

---

## 빠른 시작

### 실행 방법
```bash
# 1. 클론
git clone https://github.com/zeromok/latency-numbers-benchmark.git
cd latency-numbers-benchmark

# 2. 벤치마크 JAR 빌드
./gradlew jmhJar

# 3. 실행
java -jar build/libs/benchmarks-jmh.jar
```

### IDE에서 실행

`LatencyBenchmark.java` 파일의 `main()` 메서드를 직접 실행하세요.

---

## 측정 환경

- **JMH** 1.37
- **Java** 17
- **Gradle** 8.x

---

## 주요 기능

### JMH 설정
```java
@BenchmarkMode(Mode.AverageTime)      // 평균 시간 측정
@OutputTimeUnit(TimeUnit.NANOSECONDS) // 나노초 단위
@Warmup(iterations = 3, time = 1)     // JIT 최적화
@Measurement(iterations = 5, time = 1) // 실제 측정
@Fork(1)                               // 새 JVM 1회
```

### 벤치마크 예시
```java
// 뮤텍스 락/언락
@Benchmark
public void mutexLockUnlock() {
    synchronized (lock) {
        // 락 자체 오버헤드만 측정
    }
}

// 메모리 순차 읽기
@Benchmark
public long sequentialMemoryRead() {
    long sum = 0;
    for (byte b : memoryData) {
        sum += b;
    }
    return sum;  // JVM 최적화 방지
}
```

---

## 프로젝트 구조
```
latency-numbers-benchmark/
├── src/main/java/io/github/zeromok/benchmark/
│   └── LatencyBenchmark.java
├── build.gradle
└── README.md
```

---

## 측정 결과
```
Benchmark                              Mode  Cnt       Score       Error  Units
LatencyBenchmark.mutexLockUnlock       avgt    5       8.942 ±     0.295  ns/op
LatencyBenchmark.sequentialMemoryRead  avgt    5  666793.944 ± 12861.761  ns/op
LatencyBenchmark.sequentialDiskRead    avgt    5  749600.453 ± 37121.522  ns/op
LatencyBenchmark.gzipCompression       avgt    5   17660.563 ±  3575.033  ns/op
LatencyBenchmark.networkRoundTrip      avgt    5   23644.446 ±  5620.726  ns/op
```

> ⚠️ 실제 결과는 하드웨어, OS, JVM 버전에 따라 다릅니다.

---

## 핵심 인사이트

- **SSD의 위력**: HDD 시대와 달리 SSD는 작은 파일에서 메모리와 비슷한 성능
- **동기화 비용**: 8.9ns는 작지만 고빈도 작업에서는 누적될 수 있음
- **측정의 중요성**: 이론값과 실제값은 환경에 따라 크게 다를 수 있음

---

## 참고 자료

- [블로그 포스트](https://b-mokk.tistory.com/86) - 자세한 분석과 설명
- [JMH 공식 문서](https://github.com/openjdk/jmh)
- [Jeff Dean's Latency Numbers](https://gist.github.com/jboner/2841832)
- 『가상 면접 사례로 배우는 대규모 시스템 설계 기초』 2장

---

- Blog: [b-mokk.tistory.com](https://b-mokk.tistory.com)