package io.github.zeromok.benchmark;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class LatencyBenchmark {


	/// ### 뮤텍스 락/언락
	/// #### 측정하는 것:
	/// - `synchronized` 키워드로 락 획득
	/// - 즉시 락 해제
	/// - 위 과정의 순수 시간
	/// #### 왜 빈 블록인가?
	/// - 락 자체의 오버헤드만 측정
	private final Object lock = new Object();

	@Benchmark
	public void mutexLockUnlock() {
		synchronized (lock) {
			// 빈 블록 - 락 획득/해제 시간만 측정
		}
	}

	/// ### 메모리 순차 읽기
	/// #### 측정하는 것:
	/// - RAM 에서 1MB 데이터를 순차적으로 읽기
	/// - CPU 캐시 효과 포함
	/// #### sum 을 계산하는 이유:
	/// - JVM 이 "결과를 안 쓰네?" 하고 최적화로 제거하는 것 방지
	/// - 실제로 데이터를 읽도록 강제
	/// #### 캐시 효과
	/// - 순차 접근 -> L1/L2 캐시 효율적 사용
	/// - CPU prefetcher 가 다음 데이터 미리 가져옴
	private byte[] memoryData;

	@Setup(Level.Trial)
	public void setupMemory() {
		memoryData = new byte[1024 * 1024]; // 1MB
		new Random().nextBytes(memoryData);
	}

	@Benchmark
	public long sequentialMemoryRead() {
		long sum = 0;
		for (byte b : memoryData) {
			sum += b;
		}
		return sum;
	}

	/// ### 디스크 순차 읽기
	/// #### 측정하는 것:
	/// - 디스크(SSD/HDD) 에서 1MB 파일 읽기
	/// - OS 파일 시스템 캐시 포함
	/// #### 왜 느린가?
	/// - 디스크 seek time
	/// - 데이터 전송 시간
	/// - 시스템 콜 오버헤드
	/// #### 주의
	/// - OS가 파일을 캐시할 수 있음
	/// - 두 번째 실행부터는 메모리에서 읽을 수도 있음
	/// - `@Setup(Level.Iteration)` 으로 바꾸면 매번 새 파일
	private Path diskFile;

	@Setup(Level.Trial)
	public void setupDisk() throws IOException {
		diskFile = Files.createTempFile("benchmark", ".dat");
		byte[] data = new byte[1024 * 1024];
		Files.write(diskFile, data);
	}

	@TearDown(Level.Trial)
	public void cleanupDisk() throws IOException {
		if (diskFile != null) {
			Files.deleteIfExists(diskFile);
		}
	}

	@Benchmark
	public long sequentialDiskRead() throws IOException {
		byte[] data = Files.readAllBytes(diskFile);
		long sum = 0;
		for (byte b : data) {
			sum += b;
		}
		return sum;
	}

	/// ### GZIP 압축
	/// #### 측정하는 것:
	/// - 1KB 데이터를 GZIP 으로 압축
	/// - 압축 알고리즘 CPU 연산
	/// #### byte[]를 반환하는 이유:
	/// - JVM 이 최적화로 제거하지 못하게
	/// - 실제 압축 결과 생성 강제
	private byte[] uncompressedData;

	@Setup(Level.Trial)
	public void setupCompression() {
		uncompressedData = new byte[1024]; // 1KB
		new Random().nextBytes(uncompressedData);
	}

	@Benchmark
	public byte[] gzipCompression() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
			gzip.write(uncompressedData);
		}
		return baos.toByteArray();
	}

	/// ### 네트워크 왕복
	/// #### 측정하는 것:
	/// - localhost 로 ICMP ping
	/// - 네트워크 스택 오버헤드
	/// #### 한계
	/// - 실제 네트워크가 아닌 loopback
	/// - 실제 2KB 전송은 구현 복잡
	/// - 개념 확인용
	@Benchmark
	public boolean networkRoundTrip() throws IOException {
		InetAddress localhost = InetAddress.getByName("localhost");
		return localhost.isReachable(1000);
	}

	public static void main(String[] args) throws Exception {
		Options opt = new OptionsBuilder()
			.include(LatencyBenchmark.class.getSimpleName())
			.build();
		new Runner(opt).run();
	}
}
